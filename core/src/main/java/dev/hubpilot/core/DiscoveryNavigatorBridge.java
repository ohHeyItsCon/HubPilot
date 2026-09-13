package dev.hubpilot.core;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

final class DiscoveryNavigatorBridge {
   private DiscoveryNavigatorBridge() {
   }

   static int addDestinations(Object var0, Collection<String> var1) {
      if (var0 != null && var1 != null && !var1.isEmpty()) {
         try {
            String var2 = hubServer(var0);
            Object var3 = hubConnection(var2);
            if (var3 == null) {
               throw new IllegalStateException(
                  "No player is currently connected to the configured hub, so the Navigator could not be synchronized. Run the command while standing on the hub."
               );
            } else {
               ArrayList<String[]> var4 = new ArrayList<>();

               for (String var6 : var1) {
                  if (var6 != null && !var6.isBlank()) {
                     String var7 = managedId(var0, var6);
                     var4.add(new String[]{var6, var7, var7});
                  }
               }

               if (var4.isEmpty()) {
                  return 0;
               } else {
                  ByteArrayOutputStream var16 = new ByteArrayOutputStream();
                  DataOutputStream var17 = new DataOutputStream(var16);

                  try {
                     var17.writeUTF("DISCOVERY_ADD");
                     var17.writeInt(var4.size());

                     for (String[] var8 : var4) {
                        var17.writeUTF(var8[0]);
                        var17.writeUTF(var8[1]);
                        var17.writeUTF(var8[2]);
                     }
                  } catch (Throwable var13) {
                     try {
                        var17.close();
                     } catch (Throwable var12) {
                        var13.addSuppressed(var12);
                     }

                     throw var13;
                  }

                  var17.close();
                  Object var18 = controlChannel();
                  Class var20 = Class.forName("com.velocitypowered.api.proxy.messages.ChannelIdentifier");
                  Class var21 = Class.forName("com.velocitypowered.api.proxy.ServerConnection");
                  Method var9 = var21.getMethod("sendPluginMessage", var20, byte[].class);
                  if (var9.invoke(var3, var18, var16.toByteArray()) instanceof Boolean var11 && !var11) {
                     throw new IllegalStateException("Velocity rejected the HubPilot Navigator synchronization packet.");
                  } else {
                     persistLocal(var0, var4);
                     return var4.size();
                  }
               }
            }
         } catch (RuntimeException var14) {
            throw var14;
         } catch (Throwable var15) {
            throw new IllegalStateException("Could not synchronize discovered servers to HubPilot Hub: " + root(var15), var15);
         }
      } else {
         return 0;
      }
   }

   static Properties loadDestinations(Object var0) {
      Properties var1 = new Properties();

      try {
         Path var2 = dataDir(var0).resolve("navigator-sync.properties");
         if (Files.isRegularFile(var2)) {
            try (InputStream var3 = Files.newInputStream(var2)) {
               var1.load(var3);
            }
         }
      } catch (Throwable var8) {
      }

      return var1;
   }

   private static void persistLocal(Object var0, List<String[]> var1) throws Exception {
      Path var2 = dataDir(var0).resolve("navigator-sync.properties");
      Properties var3 = loadDestinations(var0);
      int var4 = 0;

      for (String var6 : var3.stringPropertyNames()) {
         if (var6.startsWith("entry.") && var6.endsWith(".id")) {
            try {
               var4 = Math.max(var4, Integer.parseInt(var6.substring(6, var6.length() - 3)));
            } catch (Exception var13) {
            }
         }
      }

      for (String[] var17 : var1) {
         boolean var7 = false;

         for (String var9 : var3.stringPropertyNames()) {
            if (var9.endsWith(".id") || var9.endsWith(".target") || var9.endsWith(".status-target")) {
               String var10 = var3.getProperty(var9, "");
               if (var10.equalsIgnoreCase(var17[0]) || var10.equalsIgnoreCase(var17[1])) {
                  var7 = true;
                  break;
               }
            }
         }

         if (!var7) {
            String var19 = "entry." + ++var4;
            var3.setProperty(var19 + ".id", var17[1]);
            var3.setProperty(var19 + ".target", var17[0]);
            var3.setProperty(var19 + ".status-target", var17[2]);
         }
      }

      Files.createDirectories(var2.getParent());
      Path var16 = var2.resolveSibling(var2.getFileName() + ".tmp");

      try (OutputStream var18 = Files.newOutputStream(var16)) {
         var3.store(var18, "HubPilot Navigator sync cache");
      }

      try {
         Files.move(var16, var2, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException var12) {
         Files.move(var16, var2, StandardCopyOption.REPLACE_EXISTING);
      }
   }

   private static Path dataDir(Object var0) throws Exception {
      Object var1 = invokeDeclared(var0, "dataDir");
      return (Path)var1;
   }

   private static Object controlChannel() throws Exception {
      Field var0 = HubPilotCorePlugin.class.getDeclaredField("CONTROL_CHANNEL");
      var0.setAccessible(true);
      return var0.get(null);
   }

   private static String hubServer(Object var0) throws Exception {
      Object var1 = invokeDeclared(var0, "snapshot");
      Object var2 = invokeDeclared(var1, "hubServer");
      return var2 != null && !String.valueOf(var2).isBlank() ? String.valueOf(var2) : "hub";
   }

   private static String managedId(Object var0, String var1) {
      try {
         Object var2 = invokeDeclared(var0, "snapshot");
         if (invokeDeclared(var2, "servers") instanceof Map var4) {
            for (Object var6 : var4.values()) {
               if (var6 != null) {
                  String var7 = String.valueOf(invokeDeclared(var6, "velocityServer"));
                  if (var1.equalsIgnoreCase(var7)) {
                     return String.valueOf(invokeDeclared(var6, "id"));
                  }
               }
            }
         }
      } catch (Throwable var8) {
      }

      return normalizeId(var1);
   }

   private static Object hubConnection(String var0) throws Exception {
      Field var1 = PublicBootstrap.class.getDeclaredField("proxy");
      var1.setAccessible(true);
      Object var2 = var1.get(null);
      if (var2 == null) {
         return null;
      } else {
         Class var3 = Class.forName("com.velocitypowered.api.proxy.ProxyServer");
         Collection var4 = (Collection)var3.getMethod("getAllPlayers").invoke(var2);
         Class var5 = Class.forName("com.velocitypowered.api.proxy.Player");
         Class var6 = Class.forName("com.velocitypowered.api.proxy.ServerConnection");
         Method var7 = var5.getMethod("getCurrentServer");
         Method var8 = var6.getMethod("getServerInfo");
         Class var9 = Class.forName("com.velocitypowered.api.proxy.server.ServerInfo");
         Method var10 = var9.getMethod("getName");

         for (Object var12 : var4) {
            Optional var13 = (Optional)var7.invoke(var12);
            if (!var13.isEmpty()) {
               Object var14 = var13.get();
               Object var15 = var8.invoke(var14);
               if (var15 != null && var0.equalsIgnoreCase(String.valueOf(var10.invoke(var15)))) {
                  return var14;
               }
            }
         }

         return null;
      }
   }

   private static Object invokeDeclared(Object var0, String var1) throws Exception {
      for (Class var2 = var0.getClass(); var2 != null; var2 = var2.getSuperclass()) {
         try {
            Method var3 = var2.getDeclaredMethod(var1);
            var3.setAccessible(true);
            return var3.invoke(var0);
         } catch (NoSuchMethodException var4) {
         }
      }

      throw new NoSuchMethodException(var1);
   }

   private static String normalizeId(String var0) {
      String var1 = var0.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "-").replaceAll("^-+|-+$", "");
      return var1.isBlank() ? "server" : var1;
   }

   private static String root(Throwable var0) {
      while (var0.getCause() != null && var0.getCause() != var0) {
         var0 = var0.getCause();
      }

      return var0.getMessage() == null ? var0.getClass().getSimpleName() : var0.getMessage();
   }
}
