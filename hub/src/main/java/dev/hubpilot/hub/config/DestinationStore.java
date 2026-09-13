package dev.hubpilot.hub.config;

import dev.hubpilot.hub.HubPilotHubPlugin;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

public final class DestinationStore {
   private final HubPilotHubPlugin plugin;
   private final Path file;
   private volatile List<Destination> destinations = List.of();
   private final AtomicLong lastModified = new AtomicLong(-1L);

   public DestinationStore(HubPilotHubPlugin var1, Path var2) {
      this.plugin = var1;
      this.file = var2;
   }

   public synchronized void initialize() {
      try {
         Files.createDirectories(this.file.getParent());
         if (!Files.exists(this.file)) {
            List var1 = this.migrateLegacyEntries();
            if (var1.isEmpty()) {
               var1 = this.defaultEntries();
            }

            this.saveInternal(var1);
         }

         this.reload(true);
      } catch (IOException var2) {
         throw new IllegalStateException("Could not initialize destinations file " + this.file, var2);
      }
   }

   public synchronized boolean reloadIfChanged() {
      return this.reload(false);
   }

   private boolean reload(boolean var1) {
      try {
         if (!Files.exists(this.file)) {
            return false;
         } else {
            long var2 = Files.getLastModifiedTime(this.file).toMillis();
            if (!var1 && var2 == this.lastModified.get()) {
               return false;
            } else {
               Properties var4 = new Properties();

               try (InputStream var5 = Files.newInputStream(this.file)) {
                  var4.load(var5);
               }

               this.destinations = Collections.unmodifiableList(this.parse(var4));
               this.lastModified.set(var2);
               return true;
            }
         }
      } catch (Exception var10) {
         this.plugin.getLogger().warning("Could not reload destinations: " + var10.getMessage());
         return false;
      }
   }

   public List<Destination> all() {
      return this.destinations;
   }

   public List<Destination> enabled() {
      ArrayList var1 = new ArrayList();

      for (Destination var3 : this.destinations) {
         if (var3.enabled()) {
            var1.add(var3);
         }
      }

      return Collections.unmodifiableList(var1);
   }

   public Destination find(String var1) {
      if (var1 == null) {
         return null;
      } else {
         for (Destination var3 : this.destinations) {
            if (var3.id().equalsIgnoreCase(var1)) {
               return var3;
            }
         }

         return null;
      }
   }

   public synchronized void add(Destination var1) throws IOException {
      if (this.find(var1.id()) != null) {
         throw new IllegalArgumentException("A destination with that ID already exists");
      } else {
         ArrayList<Destination> var2 = new ArrayList<>(this.destinations);
         var2.add(var1);
         this.saveInternal(var2);
      }
   }

   public synchronized void replace(Destination var1) throws IOException {
      ArrayList<Destination> var2 = new ArrayList<>(this.destinations);
      boolean var3 = false;

      for (int var4 = 0; var4 < var2.size(); var4++) {
         if (((Destination)var2.get(var4)).id().equals(var1.id())) {
            var2.set(var4, var1);
            var3 = true;
            break;
         }
      }

      if (!var3) {
         throw new IllegalArgumentException("Destination no longer exists");
      } else {
         this.saveInternal(var2);
      }
   }

   public synchronized void delete(String var1) throws IOException {
      ArrayList<Destination> var2 = new ArrayList<>(this.destinations);
      boolean var3 = var2.removeIf(var1x -> var1x.id().equalsIgnoreCase(var1));
      if (!var3) {
         throw new IllegalArgumentException("Destination no longer exists");
      } else {
         this.saveInternal(var2);
      }
   }

   public synchronized void move(String var1, int var2) throws IOException {
      ArrayList var3 = new ArrayList<>(this.destinations);
      int var4 = -1;

      for (int var5 = 0; var5 < var3.size(); var5++) {
         if (((Destination)var3.get(var5)).id().equalsIgnoreCase(var1)) {
            var4 = var5;
         }
      }

      if (var4 >= 0) {
         int var7 = Math.max(0, Math.min(var3.size() - 1, var4 + var2));
         if (var4 != var7) {
            Destination var6 = (Destination)var3.remove(var4);
            var3.add(var7, var6);
            this.saveInternal(var3);
         }
      }
   }

   private void saveInternal(List<Destination> var1) throws IOException {
      Properties var2 = new Properties();
      var2.setProperty("format-version", "3");

      for (int var3 = 0; var3 < var1.size(); var3++) {
         Destination var4 = (Destination)var1.get(var3);
         String var5 = "entry." + (var3 + 1) + ".";
         var2.setProperty(var5 + "id", var4.id());
         var2.setProperty(var5 + "label", var4.label());
         var2.setProperty(var5 + "description", var4.description());
         var2.setProperty(var5 + "software", var4.software());
         var2.setProperty(var5 + "icon", var4.iconMaterial());
         var2.setProperty(var5 + "type", var4.targetType().name());
         var2.setProperty(var5 + "target", var4.target());
         var2.setProperty(var5 + "status-target", var4.statusTarget());
         var2.setProperty(var5 + "enabled", Boolean.toString(var4.enabled()));
      }

      Path var10 = this.file.resolveSibling(this.file.getFileName() + ".tmp");

      try (OutputStream var11 = Files.newOutputStream(var10)) {
         var2.store(var11, "HubPilot shared destinations - edit in game with the admin item");
      }

      try {
         Files.move(var10, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (IOException var8) {
         Files.move(var10, this.file, StandardCopyOption.REPLACE_EXISTING);
      }

      this.destinations = Collections.unmodifiableList(new ArrayList<>(var1));
      this.lastModified.set(Files.getLastModifiedTime(this.file).toMillis());
   }

   private List<Destination> parse(Properties var1) {
      LinkedHashMap<Integer, Map<String, String>> var2 = new LinkedHashMap<>();

      for (String var4 : var1.stringPropertyNames()) {
         if (var4.startsWith("entry.")) {
            String var5 = var4.substring(6);
            int var6 = var5.indexOf(46);
            if (var6 > 0) {
               try {
                  int var7 = Integer.parseInt(var5.substring(0, var6));
                  var2.computeIfAbsent(var7, var0 -> new LinkedHashMap<>()).put(var5.substring(var6 + 1), var1.getProperty(var4));
               } catch (NumberFormatException var18) {
               }
            }
         }
      }

      ArrayList<Integer> var19 = new ArrayList<>(var2.keySet());
      Collections.sort(var19);
      ArrayList var20 = new ArrayList();

      for (Integer var22 : var19) {
         Map<String, String> var23 = var2.get(var22);

         try {
            String var8 = var23.getOrDefault("id", "");
            String var9 = var23.getOrDefault("label", var8);
            String var10 = var23.getOrDefault("description", "Click to connect");
            String var11 = var23.getOrDefault("software", "Unknown");
            String var12 = var23.getOrDefault("icon", "GRASS_BLOCK");
            Destination.TargetType var13 = Destination.TargetType.valueOf(var23.getOrDefault("type", "SERVER").toUpperCase(Locale.ENGLISH));
            String var14 = var23.getOrDefault("target", "");
            String var15 = var23.getOrDefault("status-target", var14);
            boolean var16 = Boolean.parseBoolean(var23.getOrDefault("enabled", "true"));
            var20.add(new Destination(var8, var9, var10, var11, var12, var13, var14, var15, var16));
         } catch (Exception var17) {
            this.plugin.getLogger().warning("Skipping malformed destination entry " + var22 + ": " + var17.getMessage());
         }
      }

      return var20;
   }

   private List<Destination> migrateLegacyEntries() {
      ArrayList var1 = new ArrayList();
      List var2 = this.plugin.getConfig().getList("entries");
      if (var2 == null) {
         return var1;
      } else {
         int var3 = 1;

         for (Object var5 : var2) {
            if (var5 instanceof Map var6) {
               try {
                  String var7 = String.valueOf(var6.containsKey("target") ? var6.get("target") : "");
                  if (!var7.isBlank()) {
                     String var8 = var7.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9_-]", "-");
                     if (var8.isBlank()) {
                        var8 = "server" + var3;
                     }

                     String var9 = this.stripColors(String.valueOf(var6.containsKey("name") ? var6.get("name") : var7));
                     String var10 = String.valueOf(var6.containsKey("type") ? var6.get("type") : "SERVER");
                     Destination.TargetType var11 = Destination.TargetType.valueOf(var10.toUpperCase(Locale.ENGLISH));
                     String var12 = "Click to connect";
                     if (var6.get("lore") instanceof List var14 && !var14.isEmpty()) {
                        var12 = this.stripColors(String.valueOf(var14.get(0)));
                     }

                     var1.add(new Destination(var8, var9, var12, "Unknown", "GRASS_BLOCK", var11, var7, var7, true));
                     var3++;
                  }
               } catch (Exception var15) {
               }
            }
         }

         if (!var1.isEmpty()) {
            this.plugin.getLogger().info("Migrated " + var1.size() + " old config.yml entries into shared destinations.properties.");
         }

         return var1;
      }
   }

   private String stripColors(String var1) {
      return var1 == null ? "" : var1.replaceAll("(?i)&[0-9A-FK-ORX]", "").trim();
   }

   private List<Destination> defaultEntries() {
      return List.of(
         new Destination("survival", "Survival", "Main survival server", "Paper", "GRASS_BLOCK", Destination.TargetType.SERVER, "survival", "survival", true),
         new Destination("minigames", "Minigames", "Minigame server", "Paper", "GRASS_BLOCK", Destination.TargetType.SERVER, "minigames", "minigames", true)
      );
   }

   public Path file() {
      return this.file;
   }
}
