package dev.hubpilot.core;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DiscoveryCommand {
   private DiscoveryCommand() {
   }

   static boolean handleDiscovery(Object var0, Object var1) {
      Object var2;
      String[] var14;
      try {
         var14 = (String[])call(var1, "arguments");
         var14 = DiscoveryAliases.normalize(var14);
         var2 = call(var1, "source");
      } catch (Exception var12) {
         return false;
      }

      if (var14.length == 0 || !"discover".equalsIgnoreCase(var14[0])) {
         return false;
      } else if (!isAdmin(var2)) {
         send(var2, "This subcommand requires HubPilot Admin access.");
         return true;
      } else {
         try {
            HubPilotConfig var4 = getConfig(var0);
            List<String> var15 = CraftyDiscoverySupport.filter(var4, registeredServers(var0));
            var15.removeIf(var1x -> isConfiguredHub(var4, var1x));
            var15.sort(String.CASE_INSENSITIVE_ORDER);
            if (var14.length == 1 || "list".equalsIgnoreCase(var14[1])) {
               send(var2, "HubPilot discovery: " + var15.size() + " Velocity server(s) available (hub excluded).");
               Map var17 = var4.snapshot().servers();
               Properties var18 = loadDestinations(var4);

               for (String var9 : var15) {
                  boolean var10 = findManaged(var17, var9) != null;
                  boolean var11 = destinationExists(var18, var9);
                  send(var2, " - " + var9 + " | Core=" + (var10 ? "yes" : "no") + " | Navigator=" + (var11 ? "yes" : "no"));
               }

               send(var2, "Use /hp discover add <server|all|*> or /hp discover all");
               return true;
            }

            if ("add".equalsIgnoreCase(var14[1])) {
               if (var14.length < 3) {
                  send(var2, "Usage: /hp discover add <server|all|*>");
                  return true;
               }

               String var16 = resolve(var15, var14[2]);
               if (var16 == null) {
                  send(var2, "Velocity server not found: " + var14[2]);
                  send(var2, "Run /hp discover to see the exact registered names.");
                  return true;
               }

               DiscoveryCommand.ImportResult var7 = importServers(var4, List.of(var16));
               send(
                  var2,
                  "Linked "
                     + var16
                     + ": Core "
                     + state(var7.coreAdded)
                     + ", Navigator "
                     + state(var7.navigatorAdded)
                     + ", provider mappings repaired="
                     + var7.repaired
                     + "."
               );
               return true;
            }

            if ("all".equalsIgnoreCase(var14[1]) || "add-all".equalsIgnoreCase(var14[1])) {
               DiscoveryCommand.ImportResult var6 = importServers(var4, var15);
               send(
                  var2,
                  "Discovery complete: "
                     + var15.size()
                     + " available, Core added="
                     + var6.coreAdded
                     + ", Navigator added="
                     + var6.navigatorAdded
                     + ", provider mappings repaired="
                     + var6.repaired
                     + "."
               );
               return true;
            }

            send(var2, "Usage: /hp discover [list|add <server|all|*>|all]");
         } catch (Throwable var13) {
            Throwable var5 = unwrap(var13);
            send(var2, "HubPilot discovery failed: " + (var5.getMessage() == null ? var5.getClass().getSimpleName() : var5.getMessage()));
         }

         return true;
      }
   }

   static List<String> suggest(Object var0, Object var1) {
      try {
         String[] var2 = (String[])call(var1, "arguments");
         Object var3 = call(var1, "source");
         if (!isAdmin(var3)) {
            return null;
         }

         if (var2.length == 1 && "discover".startsWith(var2[0].toLowerCase(Locale.ROOT))) {
            return List.of("discover");
         }

         if (var2.length >= 1 && "discover".equalsIgnoreCase(var2[0])) {
            if (var2.length == 2) {
               String var8 = var2[1].toLowerCase(Locale.ROOT);
               return List.of("list", "add", "all").stream().filter(var1x -> var1x.startsWith(var8)).toList();
            }

            if (var2.length == 3 && "add".equalsIgnoreCase(var2[1])) {
               List<String> var4 = registeredServers(var0);
               HubPilotConfig var5 = getConfig(var0);
               var4.removeIf(var1x -> isConfiguredHub(var5, var1x));
               String var6 = var2[2].toLowerCase(Locale.ROOT);
               return var4.stream().filter(var1x -> var1x.toLowerCase(Locale.ROOT).startsWith(var6)).sorted(String.CASE_INSENSITIVE_ORDER).toList();
            }

            return List.of();
         }
      } catch (Throwable var7) {
      }

      return null;
   }

   private static String state(int var0) {
      return var0 > 0 ? "added" : "already linked";
   }

   private static boolean isConfiguredHub(HubPilotConfig var0, String var1) {
      if (var1 != null && !var1.isBlank()) {
         HubPilotConfig.Snapshot var2 = var0.snapshot();
         if (sameHubName(var1, var2.hubServer())) {
            return true;
         } else {
            for (ManagedServer var4 : var2.servers().values()) {
               if (sameHubName(var4.id(), var2.hubServer()) || sameHubName(var4.velocityServer(), var2.hubServer())) {
                  return sameHubName(var1, var4.id()) || sameHubName(var1, var4.velocityServer()) || sameHubName(var1, var4.label());
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean sameHubName(String var0, String var1) {
      String var2 = hubKey(var0);
      String var3 = hubKey(var1);
      return !var2.isEmpty() && (var2.equals(var3) || hubRoleKey(var2).equals(hubRoleKey(var3)));
   }

   private static String hubKey(String var0) {
      return var0 == null ? "" : var0.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
   }

   private static String hubRoleKey(String var0) {
      String var1 = var0;
      if (var0.startsWith("the") && var0.length() > 3) {
         var1 = var0.substring(3);
      }

      if (var1.startsWith("main") && var1.length() > 4) {
         var1 = var1.substring(4);
      }

      if (var1.endsWith("server") && var1.length() > 6) {
         var1 = var1.substring(0, var1.length() - 6);
      }

      return var1;
   }

   private static DiscoveryCommand.ImportResult importServers(HubPilotConfig var0, Collection<String> var1) throws Exception {
      int var2 = 0;
      Map var3 = var0.snapshot().servers();
      Path var4 = var0.dataDir().resolve("servers");
      Files.createDirectories(var4);

      for (String var6 : var1) {
         if (!isConfiguredHub(var0, var6) && findManaged(var3, var6) == null) {
            String var7 = normalizeId(var6);
            Path var8 = uniqueServerFile(var4, var7, var6);
            String var9 = "id: "
               + var7
               + "\ndisplay-name: \""
               + escapeYaml(var6)
               + "\"\nvelocity-server: "
               + var6
               + "\nstartup:\n  provider: always-online\ncompatibility:\n  version: \"any\"\n  loader: \"unknown\"\n  strict-version: false\naccess:\n  maintenance: false\n  permission: \"\"\n";
            Files.writeString(var8, var9, StandardOpenOption.CREATE_NEW);
            var2++;
         }
      }

      var0.reload();
      int var10 = var0.repairProviderMappings();
      if (var10 > 0) {
         var0.reload();
      }

      int var11 = addDestinations(var0, var1);
      return new DiscoveryCommand.ImportResult(var2, var11, var10);
   }

   private static Path uniqueServerFile(Path var0, String var1, String var2) throws IOException {
      Path var3 = var0.resolve(var1 + ".yml");
      if (!Files.exists(var3)) {
         return var3;
      } else {
         for (int var4 = 2; var4 < 1000; var4++) {
            var3 = var0.resolve(var1 + "-" + var4 + ".yml");
            if (!Files.exists(var3)) {
               return var3;
            }
         }

         throw new IOException("Could not choose a unique server config filename for " + var2);
      }
   }

   private static int addDestinations(HubPilotConfig var0, Collection<String> var1) throws IOException {
      return DiscoveryNavigatorBridge.addDestinations(var0, var1);
   }

   private static String idForStatus(Map<String, ManagedServer> var0, String var1, String var2) {
      ManagedServer var3 = findManaged(var0, var1);
      return var3 == null ? var2 : var3.id();
   }

   private static Properties loadDestinations(HubPilotConfig var0) throws IOException {
      return DiscoveryNavigatorBridge.loadDestinations(var0);
   }

   private static boolean destinationExists(Properties var0, String var1) {
      for (String var3 : var0.stringPropertyNames()) {
         String var4;
         if (var3.startsWith("entry.")
            && (var3.endsWith(".id") || var3.endsWith(".target") || var3.endsWith(".status-target"))
            && ((var4 = var0.getProperty(var3, "")).equalsIgnoreCase(var1) || var4.equalsIgnoreCase(normalizeId(var1)))) {
            return true;
         }
      }

      return false;
   }

   private static int maxEntry(Properties var0) {
      int var1 = 0;
      Pattern var2 = Pattern.compile("entry\\.(\\d+)\\..+");

      for (String var4 : var0.stringPropertyNames()) {
         Matcher var5 = var2.matcher(var4);
         if (var5.matches()) {
            try {
               var1 = Math.max(var1, Integer.parseInt(var5.group(1)));
            } catch (NumberFormatException var7) {
            }
         }
      }

      return var1;
   }

   private static ManagedServer findManaged(Map<String, ManagedServer> var0, String var1) {
      for (ManagedServer var3 : var0.values()) {
         if (var3.id().equalsIgnoreCase(var1) || var3.velocityServer().equalsIgnoreCase(var1)) {
            return var3;
         }
      }

      return null;
   }

   private static String resolve(List<String> var0, String var1) {
      for (String var3 : var0) {
         if (var3.equalsIgnoreCase(var1)) {
            return var3;
         }
      }

      return null;
   }

   private static String normalizeId(String var0) {
      String var1 = var0 == null ? "" : var0.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "-");
      if ((var1 = var1.replaceAll("^-+|-+$", "")).length() > 64) {
         var1 = var1.substring(0, 64);
      }

      return var1.isEmpty() ? "server" : var1;
   }

   private static String escapeYaml(String var0) {
      return var0.replace("\\", "\\\\").replace("\"", "\\\"");
   }

   private static HubPilotConfig getConfig(Object var0) throws Exception {
      Field var1 = var0.getClass().getDeclaredField("config");
      var1.setAccessible(true);
      return (HubPilotConfig)var1.get(var0);
   }

   private static List<String> registeredServers(Object var0) throws Exception {
      Field var1 = var0.getClass().getDeclaredField("proxy");
      var1.setAccessible(true);
      Object var2 = var1.get(var0);
      Object var3 = call(var2, "getAllServers");
      ArrayList var4 = new ArrayList();
      if (var3 instanceof Iterable) {
         for (Object var7 : (Iterable)var3) {
            Object var8 = call(var7, "getServerInfo");
            Object var9 = call(var8, "getName");
            if (var9 != null) {
               var4.add(String.valueOf(var9));
            }
         }
      }

      return var4;
   }

   private static boolean isAdmin(Object var0) {
      return PublicCommandLayer.isAdmin(var0);
   }

   private static boolean permission(Object var0, String var1) {
      try {
         Object var2 = call(var0, "hasPermission", var1);
         return Boolean.TRUE.equals(var2);
      } catch (Throwable var3) {
         return false;
      }
   }

   private static void send(Object var0, String var1) {
      if (var0 != null) {
         try {
            call(var0, "sendPlainMessage", var1);
         } catch (Throwable var8) {
            try {
               for (Method var6 : var0.getClass().getMethods()) {
                  if (var6.getName().equals("sendMessage") && var6.getParameterCount() == 1 && var6.getParameterTypes()[0] == String.class) {
                     HubPilotReflect.invokeAccessible(var6, var0, new Object[]{var1});
                     return;
                  }
               }
            } catch (Throwable var7) {
            }
         }
      }
   }

   private static Object call(Object var0, String var1, Object... var2) throws Exception {
      Method var3 = null;

      for (Method var7 : var0.getClass().getMethods()) {
         if (var7.getName().equals(var1) && var7.getParameterCount() == var2.length) {
            Class[] var8 = var7.getParameterTypes();
            boolean var9 = true;

            for (int var10 = 0; var10 < var8.length; var10++) {
               if (var2[var10] != null && !box(var8[var10]).isInstance(var2[var10])) {
                  var9 = false;
                  break;
               }
            }

            if (var9) {
               var3 = var7;
               break;
            }
         }
      }

      if (var3 == null) {
         throw new NoSuchMethodException(var0.getClass().getName() + "." + var1);
      } else {
         return HubPilotReflect.invokeAccessible(var3, var0, var2);
      }
   }

   private static boolean hasMethod(Class<?> var0, String var1, int var2) {
      for (Method var6 : var0.getMethods()) {
         if (var6.getName().equals(var1) && var6.getParameterCount() == var2) {
            return true;
         }
      }

      return false;
   }

   private static Class<?> box(Class<?> var0) {
      if (!var0.isPrimitive()) {
         return var0;
      } else if (var0 == boolean.class) {
         return Boolean.class;
      } else if (var0 == int.class) {
         return Integer.class;
      } else if (var0 == long.class) {
         return Long.class;
      } else if (var0 == double.class) {
         return Double.class;
      } else if (var0 == float.class) {
         return Float.class;
      } else if (var0 == short.class) {
         return Short.class;
      } else if (var0 == byte.class) {
         return Byte.class;
      } else {
         return var0 == char.class ? Character.class : var0;
      }
   }

   private static Throwable unwrap(Throwable var0) {
      while ((var0 instanceof InvocationTargetException || var0 instanceof ExceptionInInitializerError) && var0.getCause() != null) {
         var0 = var0.getCause();
      }

      return var0;
   }

   static boolean handle(Object var0, Object var1) {
      if (UnifiedCommandFacade.handle(var0, var1)) {
         return true;
      } else {
         return PublicCommandLayer.handle(var0, var1) ? true : handleDiscovery(var0, var1);
      }
   }

   private record ImportResult(int coreAdded, int navigatorAdded, int repaired) {
   }
}
