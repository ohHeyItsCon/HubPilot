package dev.hubpilot.hub.layout;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.config.Destination;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

public final class NavigatorLayoutStore {
   private static final String FILE_NAME = "navigator-layout.properties";
   private static final int DEFAULT_SIZE = 54;
   private static final int[] LEGACY_SLOTS = new int[]{10, 12, 14, 16, 28, 30, 32, 34};

   private NavigatorLayoutStore() {
   }

   public static int inventorySize(HubPilotHubPlugin var0) {
      int var1 = var0.getConfig().getInt("gui-slots", 54);
      if (var1 >= 9 && var1 <= 54 && var1 % 9 == 0) {
         return var1;
      } else {
         var0.getLogger().warning("HubPilot gui-slots must be one of 9, 18, 27, 36, 45, 54. Using 54 instead of " + var1 + ".");
         return 54;
      }
   }

   public static synchronized Map<String, Integer> assignments(HubPilotHubPlugin var0, Collection<Destination> var1) {
      Map var2 = load(var0);
      HashSet var3 = new HashSet();

      for (Destination var5 : var1) {
         var3.add(norm(var5.id()));
      }

      var2.keySet().removeIf(var1x -> !var3.contains(var1x));
      ensureAssignments(var0, var2, var1);
      save(var0, var2);
      return Collections.unmodifiableMap(new LinkedHashMap<>(var2));
   }

   public static synchronized int slotOf(HubPilotHubPlugin var0, Destination var1) {
      Map<String, Integer> var2 = assignments(var0, var0.getDestinationStore().all());
      return var2.getOrDefault(norm(var1.id()), -1);
   }

   public static synchronized Destination destinationAt(HubPilotHubPlugin var0, int var1, Collection<Destination> var2) {
      Map var3 = assignments(var0, var0.getDestinationStore().all());

      for (Destination var5 : var2) {
         Integer var6 = (Integer)var3.get(norm(var5.id()));
         if (var6 != null && var6 == var1) {
            return var5;
         }
      }

      return null;
   }

   public static synchronized NavigatorLayoutStore.AssignmentResult assign(HubPilotHubPlugin var0, String var1, int var2) throws IOException {
      int var3 = inventorySize(var0);
      if (var2 >= 0 && var2 < var3) {
         Destination var4 = var0.getDestinationStore().find(var1);
         if (var4 == null) {
            return new NavigatorLayoutStore.AssignmentResult(false, "That destination no longer exists.");
         } else {
            List var5 = var0.getDestinationStore().all();
            Map<String, Integer> var6 = load(var0);
            ensureAssignments(var0, var6, var5);
            String var7 = norm(var4.id());
            Integer var8 = (Integer)var6.get(var7);
            String var9 = null;

            for (Entry var11 : var6.entrySet()) {
               if (!((String)var11.getKey()).equals(var7) && var11.getValue() != null && (Integer)var11.getValue() == var2) {
                  var9 = (String)var11.getKey();
                  break;
               }
            }

            if (var9 != null) {
               if (var8 != null && var8 >= 0 && var8 < var3) {
                  var6.put(var9, var8);
               } else {
                  int var12 = firstFree(var6, var3, var2);
                  if (var12 < 0) {
                     return new NavigatorLayoutStore.AssignmentResult(false, "Navigator is full. Move another server first or increase gui-slots.");
                  }

                  var6.put(var9, var12);
               }
            }

            var6.put(var7, var2);
            saveChecked(var0, var6);
            return new NavigatorLayoutStore.AssignmentResult(
               true, var9 == null ? "Placed in slot " + var2 + "." : "Placed in slot " + var2 + " and moved the previous server."
            );
         }
      } else {
         return new NavigatorLayoutStore.AssignmentResult(false, "That slot is outside the configured Navigator size.");
      }
   }

   private static void ensureAssignments(HubPilotHubPlugin var0, Map<String, Integer> var1, Collection<Destination> var2) {
      int var3 = inventorySize(var0);
      HashSet var4 = new HashSet();
      Iterator var5 = var1.entrySet().iterator();

      while (var5.hasNext()) {
         Entry var6 = (Entry)var5.next();
         Integer var7 = (Integer)var6.getValue();
         if (var7 != null && var7 >= 0 && var7 < var3 && !var4.contains(var7)) {
            var4.add(var7);
         } else {
            var5.remove();
         }
      }

      int var11 = 0;

      for (Destination var13 : var2) {
         String var8 = norm(var13.id());
         if (!var1.containsKey(var8)) {
            int var9 = -1;

            while (var11 < LEGACY_SLOTS.length) {
               int var10 = LEGACY_SLOTS[var11++];
               if (var10 < var3 && !var4.contains(var10)) {
                  var9 = var10;
                  break;
               }
            }

            if (var9 < 0) {
               for (int var14 = 0; var14 < var3; var14++) {
                  if (!var4.contains(var14)) {
                     var9 = var14;
                     break;
                  }
               }
            }

            if (var9 >= 0) {
               var1.put(var8, var9);
               var4.add(var9);
            }
         }
      }
   }

   private static int firstFree(Map<String, Integer> var0, int var1, int var2) {
      HashSet var3 = new HashSet(var0.values());
      var3.add(var2);

      for (int var4 = 0; var4 < var1; var4++) {
         if (!var3.contains(var4)) {
            return var4;
         }
      }

      return -1;
   }

   private static Map<String, Integer> load(HubPilotHubPlugin var0) {
      Path var1 = file(var0);
      LinkedHashMap var2 = new LinkedHashMap();
      if (!Files.exists(var1)) {
         return var2;
      } else {
         Properties var3 = new Properties();

         try (BufferedReader var4 = Files.newBufferedReader(var1, StandardCharsets.UTF_8)) {
            var3.load(var4);
            ArrayList<String> var5 = new ArrayList<>(var3.stringPropertyNames());
            Collections.sort(var5);

            for (String var7 : var5) {
               try {
                  var2.put(norm(var7), Integer.parseInt(var3.getProperty(var7).trim()));
               } catch (RuntimeException var10) {
               }
            }
         } catch (IOException var12) {
            var0.getLogger().warning("Could not read navigator-layout.properties: " + var12.getMessage());
         }

         return var2;
      }
   }

   private static void save(HubPilotHubPlugin var0, Map<String, Integer> var1) {
      try {
         saveChecked(var0, var1);
      } catch (IOException var3) {
         var0.getLogger().warning("Could not save navigator-layout.properties: " + var3.getMessage());
      }
   }

   private static void saveChecked(HubPilotHubPlugin var0, Map<String, Integer> var1) throws IOException {
      Path var2 = file(var0);
      Files.createDirectories(var2.getParent());
      Properties var3 = new Properties();
      var1.entrySet().stream().sorted(Entry.comparingByKey()).forEach(var1x -> var3.setProperty((String)var1x.getKey(), String.valueOf(var1x.getValue())));
      Path var4 = var2.resolveSibling(var2.getFileName() + ".tmp");

      try (BufferedWriter var5 = Files.newBufferedWriter(var4, StandardCharsets.UTF_8)) {
         var3.store(var5, "HubPilot Navigator slot mapping. Slot numbers are zero-based (0 = top-left). Edit in-game from a server's Navigator Slot button.");
      }

      try {
         Files.move(var4, var2, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException var9) {
         Files.move(var4, var2, StandardCopyOption.REPLACE_EXISTING);
      }
   }

   private static Path file(HubPilotHubPlugin var0) {
      return var0.getDataFolder().toPath().resolve("navigator-layout.properties");
   }

   private static String norm(String var0) {
      return var0 == null ? "" : var0.trim().toLowerCase(Locale.ROOT);
   }

   public record AssignmentResult(boolean success, String message) {
   }
}
