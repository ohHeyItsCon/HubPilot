package dev.hubpilot.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

final class HubPilotConfig {
   private final Path dataDir;
   private final Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
   private volatile HubPilotConfig.Snapshot snapshot;

   HubPilotConfig(Path var1) {
      this.dataDir = var1;
   }

   synchronized void initialize() throws IOException {
      this.createDefaults();
      this.migrateLegacyIfNeeded();
      this.repairProviderMappings();
      this.reload();
   }

   synchronized void reload() throws IOException {
      Map var1 = this.loadMap(this.dataDir.resolve("config.yml"));
      Map var2 = this.loadMap(this.dataDir.resolve("defaults.yml"));
      Map var3 = this.loadMap(this.dataDir.resolve("providers/crafty.yml"));
      Map var4 = this.loadMap(this.dataDir.resolve(string(var1, "messages-file", "messages/en_US.yml")));
      LinkedHashMap var5 = new LinkedHashMap();
      Path var6 = this.dataDir.resolve("servers");

      try (DirectoryStream<Path> var7 = Files.newDirectoryStream(var6, "*.yml")) {
         ArrayList<Path> var8 = new ArrayList<>();
         var7.forEach(var8::add);
         var8.sort(Comparator.comparing(var0 -> var0.getFileName().toString()));

         for (Path var10 : var8) {
            ManagedServer var12 = this.parseServer(var10, this.loadMap(var10), var2);
            if (var5.put(var12.id().toLowerCase(Locale.ROOT), var12) != null) {
               throw new IOException("Duplicate server id: " + var12.id());
            }
         }
      }

      String var15 = string(var1, "shared-directory", "/shared/hubpilot");
      this.applyLegacyHubPilotSettings(var5, var15);
      HubProtection.repair(var5, string(var1, "hub-server", "hub"));
      LinkedHashMap var16 = new LinkedHashMap<>(flattenStrings(var4));
      this.applyMessageOverrides(var16, var15);
      this.snapshot = new HubPilotConfig.Snapshot(
         var15,
         string(var1, "hub-server", "hub"),
         bool(var1, "modules.lifecycle", true),
         bool(var1, "modules.statistics", true),
         bool(var1, "modules.idle-shutdown", true),
         integer(var1, "status-refresh-seconds", 5, 1, 60),
         integer(var1, "config-reload-seconds", 3, 1, 60),
         bool(var1, "intercept-direct-server-requests", true),
         trustedSources(var1),
         string(var1, "messages-file", "messages/en_US.yml"),
         new HubPilotConfig.CraftySettings(
            bool(var3, "enabled", true),
            string(var3, "base-url", "https://crafty:8443"),
            string(var3, "token-file", "/server/secrets/crafty.token"),
            bool(var3, "allow-insecure-tls", false),
            integer(var3, "connect-timeout-seconds", 10, 1, 120)
         ),
         Collections.unmodifiableMap(var5),
         Collections.unmodifiableMap(var16)
      );
   }

   HubPilotConfig.Snapshot snapshot() {
      return this.snapshot;
   }

   Path dataDir() {
      return this.dataDir;
   }

   synchronized void setStartupProvider(String var1, String var2, String var3) throws IOException {
      String var6 = var1 == null ? "" : var1.trim().toLowerCase(Locale.ROOT);
      if (!var6.matches("[a-z0-9_-]{1,64}")) {
         throw new IOException("Invalid server id: " + var1);
      } else {
         String var5 = var2 == null ? "" : var2.trim().toLowerCase(Locale.ROOT);
         if (!var5.equals("crafty") && !var5.equals("always-online")) {
            throw new IOException("Unsupported provider: " + var2);
         } else {
            String var4 = var3 == null ? "" : var3.trim();
            if (var5.equals("crafty") && !var4.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
               throw new IOException("Crafty requires a valid server UUID");
            } else {
               if (var5.equals("always-online")) {
                  var4 = "";
               }

               Path var10 = this.findServerFile(var6);
               this.backupServerFile(var10);
               patchStartupProvider(var10, var5, var4);
            }
         }
      }
   }

   synchronized int repairProviderMappings() throws IOException {
      Map<String, HubPilotConfig.LegacyDestination> var1 = this.readLegacyDestinations();
      Path var2 = this.dataDir.resolve("servers");
      int var3 = 0;

      try (DirectoryStream<Path> var4 = Files.newDirectoryStream(var2, "*.yml")) {
         for (Path var6 : var4) {
            Map var7 = this.loadMap(var6);
            String var8 = string(var7, "id", stripExt(var6.getFileName().toString())).toLowerCase(Locale.ROOT);
            String var9 = string(var7, "velocity-server", var8).toLowerCase(Locale.ROOT);
            String var10 = string(var7, "startup.provider-server-id", "");
            String var11 = string(var7, "startup.provider", "always-online");
            if (var10.isBlank() && !var9.equalsIgnoreCase("hub") && (var11.equalsIgnoreCase("always-online") || var11.equalsIgnoreCase("crafty"))) {
               HubPilotConfig.LegacyDestination var12 = (HubPilotConfig.LegacyDestination)var1.get(var8);
               if (var12 == null) {
                  var12 = var1.values()
                     .stream()
                     .filter(var1x -> var1x.target.equalsIgnoreCase(var9) || var1x.statusTarget.equalsIgnoreCase(var9) || var1x.id.equalsIgnoreCase(var9))
                     .findFirst()
                     .orElse(null);
               }

               ArrayList var13 = new ArrayList();
               if (var12 != null) {
                  var13.add(var12.statusTarget);
                  var13.add(var12.target);
                  var13.add(var12.id);
               }

               var13.add(var8);
               var13.add(var9);
               Optional var14 = this.findCraftyUuid(var13);
               if (var14.isPresent()) {
                  this.backupServerFile(var6);
                  patchStartupProvider(var6, "crafty", (String)var14.get());
                  var3++;
               }
            }
         }
      }

      return var3;
   }

   private Path findServerFile(String var1) throws IOException {
      Path var2 = this.dataDir.resolve("servers").resolve(var1 + ".yml");
      if (Files.isRegularFile(var2)) {
         return var2;
      } else {
         try (DirectoryStream<Path> var3 = Files.newDirectoryStream(this.dataDir.resolve("servers"), "*.yml")) {
            for (Path var5 : var3) {
               Map var6 = this.loadMap(var5);
               if (string(var6, "id", stripExt(var5.getFileName().toString())).equalsIgnoreCase(var1)) {
                  return var5;
               }
            }
         }

         throw new IOException("Unknown HubPilot server: " + var1);
      }
   }

   private void backupServerFile(Path var1) throws IOException {
      Path var2 = this.dataDir.resolve("backups");
      Files.createDirectories(var2);
      String var3 = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
      Path var4 = var2.resolve(var1.getFileName().toString() + "." + var3 + ".bak");
      if (Files.notExists(var4)) {
         Files.copy(var1, var4);
      }
   }

   String message(String var1, String var2, Map<String, String> var3) {
      HubPilotConfig.Snapshot var4 = this.snapshot;
      String var5 = var4 == null ? var2 : var4.messages().getOrDefault(var1, var2);

      for (Entry var7 : var3.entrySet()) {
         var5 = var5.replace("<" + (String)var7.getKey() + ">", (CharSequence)var7.getValue());
      }

      return var5;
   }

   private static List<String> trustedSources(Map<String, Object> var0) {
      List<String> var1 = strings(var0, "trusted-request-servers");
      return var1.isEmpty() ? List.of(string(var0, "hub-server", "hub")) : var1.stream().map(var0x -> var0x.toLowerCase(Locale.ROOT)).toList();
   }

   private static Map<String, String> flattenStrings(Map<String, Object> var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      flattenInto("", var0, var1);
      return var1;
   }

   private static void flattenInto(String var0, Map<String, Object> var1, Map<String, String> var2) {
      for (Entry var4 : var1.entrySet()) {
         String var5 = var0.isEmpty() ? (String)var4.getKey() : var0 + "." + (String)var4.getKey();
         if (var4.getValue() instanceof Map var7) {
            flattenInto(var5, normalizeMap(var7), var2);
         } else if (var4.getValue() != null) {
            var2.put(var5, String.valueOf(var4.getValue()));
         }
      }
   }

   private ManagedServer parseServer(Path var1, Map<String, Object> var2, Map<String, Object> var3) throws IOException {
      String var4 = string(var2, "id", stripExt(var1.getFileName().toString())).toLowerCase(Locale.ROOT);
      if (!var4.matches("[a-z0-9_-]{1,64}")) {
         throw new IOException("Invalid server id in " + var1 + ": " + var4);
      } else {
         String var5 = string(var2, "velocity-server", var4);
         String var6 = string(var2, "startup.provider", string(var3, "startup.provider", "always-online"));
         String var7 = string(var2, "startup.provider-server-id", "");
         String var8 = string(var2, "display-name", var4);
         String var9 = string(var2, "compatibility.version", "any");
         boolean var10 = bool(var2, "compatibility.strict-version", bool(var3, "compatibility.strict-version", false));
         String var11 = string(var2, "compatibility.loader", "unknown");
         String var12 = string(var2, "access.permission", "");
         boolean var13 = bool(var2, "access.maintenance", false);
         boolean var14 = bool(var2, "startup.enabled", true);
         int var15 = integer(var2, "startup.expected-seconds", integer(var3, "startup.expected-seconds", 60, 1, 3600), 1, 3600);
         int var16 = integer(var2, "startup.timeout-seconds", integer(var3, "startup.timeout-seconds", 120, 5, 7200), 5, 7200);
         int var17 = integer(var2, "startup.ping-timeout-seconds", integer(var3, "startup.ping-timeout-seconds", 3, 1, 30), 1, 30);
         int var18 = integer(var2, "connection.retry-count", integer(var3, "connection.retry-count", 3, 0, 20), 0, 20);
         int var19 = integer(var2, "connection.retry-delay-seconds", integer(var3, "connection.retry-delay-seconds", 15, 1, 300), 1, 300);
         boolean var20 = bool(var2, "startup.stop-after-failure", bool(var3, "startup.stop-after-failure", true));
         boolean var21 = bool(var2, "startup.stop-when-queue-empty", bool(var3, "startup.stop-when-queue-empty", true));
         int var22 = integer(var2, "idle-shutdown.minutes", integer(var3, "idle-shutdown.minutes", 30, 0, 10080), 0, 10080);
         int var23 = integer(var2, "countdown.duration-seconds", integer(var3, "countdown.duration-seconds", 5, 0, 300), 0, 300);
         String var24 = string(var2, "countdown.sound", string(var3, "countdown.sound", "minecraft:block.note_block.pling"));
         float var25 = decimal(var2, "countdown.volume", decimal(var3, "countdown.volume", 1.0F, 0.0F, 10.0F), 0.0F, 10.0F);
         String var26 = string(var2, "countdown.pitch-style", string(var3, "countdown.pitch-style", "rising"));
         String var27 = string(var2, "countdown.message", string(var3, "countdown.message", "Joining <server> in <seconds>..."));
         boolean var28 = bool(var2, "countdown.announce-every-second", bool(var3, "countdown.announce-every-second", false));
         List var29 = strings(var2, "tags");
         return new ManagedServer(
            var4,
            var8,
            var5,
            var6,
            var7,
            var9,
            var10,
            var11,
            var12,
            var13,
            var14,
            var15,
            var16,
            var17,
            var18,
            var19,
            var20,
            var21,
            var22,
            var23,
            var24,
            var25,
            var26,
            var27,
            var28,
            var29
         );
      }
   }

   private void createDefaults() throws IOException {
      Files.createDirectories(this.dataDir.resolve("providers"));
      Files.createDirectories(this.dataDir.resolve("servers"));
      Files.createDirectories(this.dataDir.resolve("messages"));
      Files.createDirectories(this.dataDir.resolve("backups"));
      writeIfMissing(
         this.dataDir.resolve("config.yml"),
         "# HubPilot Core - global settings\nshared-directory: /shared/hubpilot\nhub-server: hub\nstatus-refresh-seconds: 5\nconfig-reload-seconds: 3\nintercept-direct-server-requests: true\ntrusted-request-servers:\n  - hub\nmessages-file: messages/en_US.yml\n\nmodules:\n  lifecycle: true\n  statistics: true\n  idle-shutdown: true\n"
      );
      writeIfMissing(
         this.dataDir.resolve("defaults.yml"),
         "# Values inherited by every server unless overridden.\nstartup:\n  provider: always-online\n  expected-seconds: 60\n  timeout-seconds: 120\n  ping-timeout-seconds: 3\n  stop-after-failure: true\n  stop-when-queue-empty: true\nconnection:\n  retry-count: 3\n  retry-delay-seconds: 15\nidle-shutdown:\n  minutes: 30\ncompatibility:\n  strict-version: false\ncountdown:\n  duration-seconds: 5\n  sound: minecraft:block.note_block.pling\n  volume: 1.0\n  pitch-style: rising\n  message: \"Joining <server> in <seconds>...\"\n  announce-every-second: false\n"
      );
      writeIfMissing(
         this.dataDir.resolve("providers/crafty.yml"),
         "enabled: true\nbase-url: https://crafty:8443\ntoken-file: /server/secrets/crafty.token\nallow-insecure-tls: false\nconnect-timeout-seconds: 10\n"
      );
      writeIfMissing(
         this.dataDir.resolve("messages/en_US.yml"),
         "# HubPilot Queue Update messages. Use & color codes and {placeholders}.\n# Existing 1.0.1 flat message files remain supported.\nevents:\n  already-connected:\n    enabled: true\n    text: \"&7You are already connected to {server}&7.\"\n  connecting:\n    enabled: true\n    text: \"&7Requesting {server}&7...\"\n  unavailable:\n    enabled: true\n    text: \"&c{server}&c is unavailable.\"\n  autostart-disabled:\n    enabled: true\n    text: \"&cAutomatic startup is disabled for {server}&c.\"\n  starting:\n    enabled: true\n    text: \"&eStarting {server}&e...\"\n  already-starting:\n    enabled: true\n    text: \"&e{server}&e is already starting.\"\n  queue-joined:\n    enabled: true\n    text: \"&7You are queue position &f{position}&7 of &f{queue_size}&7.\"\n  already-queued:\n    enabled: true\n    text: \"&7You are already queue position &f{position}&7 of &f{queue_size}&7 for {server}&7.\"\n  queue-position:\n    enabled: true\n    text: \"&7You are now queue position &f{position}&7 of &f{queue_size}&7 for {server}&7.\"\n  ready:\n    enabled: true\n    text: \"&a{server}&a is ready.\"\n  countdown:\n    enabled: true\n    text: \"&eJoining {server}&e in &f{seconds}&e...\"\n  joining:\n    enabled: true\n    text: \"&7Joining {server}&7...\"\n  connection-failed:\n    enabled: true\n    text: \"&cCould not connect to {server}&c.\"\n  retrying:\n    enabled: true\n    text: \"&eConnection failed. Retrying in {delay}s ({attempt}/{max})...\"\n  request-cancelled:\n    enabled: true\n    text: \"&eYour pending HubPilot request was cancelled.\"\n  no-pending-request:\n    enabled: true\n    text: \"&7You do not have a pending HubPilot request.\"\n  start-failed:\n    enabled: true\n    text: \"&c{server}&c failed to start: {error}\"\n  maintenance:\n    enabled: true\n    text: \"&c{server}&c is currently in maintenance mode.\"\n  wrong-version:\n    enabled: true\n    text: \"&c{server}&c requires Minecraft {required}. Your client supports {current}.\"\n  missing-permission:\n    enabled: true\n    text: \"&cYou do not have permission to join {server}&c.\"\n"
      );
   }

   private void migrateLegacyIfNeeded() throws IOException {
      Path var1 = this.dataDir.resolve("servers");

      try (DirectoryStream<Path> var2 = Files.newDirectoryStream(var1, "*.yml")) {
         if (var2.iterator().hasNext()) {
            return;
         }
      }

      Map<String, HubPilotConfig.LegacyDestination> var13 = this.readLegacyDestinations();
      if (var13.isEmpty()) {
         writeIfMissing(var1.resolve("hub.yml"), sampleHub());
         writeIfMissing(var1.resolve("example.yml.disabled"), sampleManaged());
      } else {
         for (HubPilotConfig.LegacyDestination var4 : var13.values()) {
            String var5 = var4.id.toLowerCase(Locale.ROOT);
            String var6 = var4.target.toLowerCase(Locale.ROOT);
            String var7 = this.findCraftyUuid(List.of(var4.statusTarget, var4.target, var4.id)).orElse("");
            boolean var8 = var6.equalsIgnoreCase("hub");
            String var9 = !var8 && !var7.isBlank() ? "crafty" : "always-online";
            String var10 = "id: %s\ndisplay-name: \"%s\"\nvelocity-server: %s\nstartup:\n  provider: %s\n  provider-server-id: \"%s\"\n  expected-seconds: %d\ncompatibility:\n  version: \"%s\"\n  loader: \"%s\"\n  strict-version: %s\naccess:\n  maintenance: false\n  permission: \"\"\ndisplay:\n  template: player-focused\n  slot: %d\n"
               .formatted(
                  var5, escape(var4.label), var6, var9, var7, var4.startupSeconds, escape(var4.version), escape(var4.loader), strictFor(var4.loader), var4.slot
               );
            Files.writeString(var1.resolve(var5 + ".yml"), var10, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
         }

         Files.writeString(
            this.dataDir.resolve("MIGRATED_FROM_LEGACY.txt"),
            "Imported server entries from the shared HubPilot destinations file. Review provider mappings after migration.\n",
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
         );
      }
   }

   private Map<String, HubPilotConfig.LegacyDestination> readLegacyDestinations() {
      for (Path var3 : List.of(Path.of("/shared/hubpilot/destinations.properties"), this.dataDir.resolve("../hubpilot/destinations.properties"))) {
         if (Files.isRegularFile(var3)) {
            try {
               InputStream var4 = Files.newInputStream(var3);

               LinkedHashMap var27;
               try {
                  Properties var6 = new Properties();
                  var6.load(var4);
                  TreeMap<Integer, Map<String, String>> var7 = new TreeMap<>();

                  for (String var9 : var6.stringPropertyNames()) {
                     Matcher var5 = Pattern.compile("(?:entry|destination)\\.(\\d+)\\.(.+)").matcher(var9);
                     if (var5.matches()) {
                        var7.computeIfAbsent(Integer.parseInt(var5.group(1)), var0 -> new HashMap<>()).put(var5.group(2), var6.getProperty(var9));
                     }
                  }

                  LinkedHashMap var25 = new LinkedHashMap();
                  int var26 = 10;

                  for (Map var11 : var7.values()) {
                     String var13 = first(var11, "target", "server", "autoserver-target", "status-target");
                     if (var13 != null && var13.matches("[A-Za-z0-9_-]{1,64}")) {
                        String var14 = Optional.ofNullable(first(var11, "status-target", "autoserver-target")).orElse(var13);
                        if (!var14.matches("[A-Za-z0-9_-]{1,64}")) {
                           var14 = var13;
                        }

                        String var12;
                        if (!(var12 = Optional.ofNullable(first(var11, "id")).orElse(var13)).matches("[A-Za-z0-9_-]{1,64}")) {
                           var12 = var13;
                        }

                        String var15 = Optional.ofNullable(first(var11, "label", "name", "display-name")).orElse(var13);
                        String var16 = Optional.ofNullable(first(var11, "software", "type")).orElse("unknown");
                        String var17 = Optional.ofNullable(first(var11, "version", "minecraft-version")).orElse(extractVersion(var16));
                        String var18 = Optional.ofNullable(first(var11, "loader")).orElse(extractLoader(var16));
                        int var19 = parseInt(first(var11, "slot"), var26);
                        var26 += 2;
                        var25.put(var12.toLowerCase(Locale.ROOT), new HubPilotConfig.LegacyDestination(var12, var13, var14, var15, var17, var18, var19, 60));
                     }
                  }

                  if (var25.isEmpty()) {
                     continue;
                  }

                  var27 = var25;
               } finally {
                  if (var4 == null) {
                     continue;
                  }

                  var4.close();
               }

               return var27;
            } catch (Exception var24) {
            }
         }
      }

      return Map.of();
   }

   private Optional<String> findCraftyUuid(Collection<String> var1) {
      return CraftyDiscoverySupport.resolveUuid(var1);
   }

   private Optional<String> uuidFromAutoServerConfigs(Set<String> var1, Pattern var2, Pattern var3) {
      LinkedHashSet<Path> var5 = new LinkedHashSet<>(
         List.of(
            Path.of("/server/plugins/autoserver/config.toml"),
            Path.of("/server/plugins/AutoServer/config.toml"),
            Path.of("/server/plugins/autoserver-velocity/config.toml"),
            Path.of("/server/plugins/AutoServer-velocity/config.toml"),
            Path.of("/server/config.toml")
         )
      );

      for (Path var7 : List.of(Path.of("/server/plugins"), this.dataDir.getParent() == null ? this.dataDir : this.dataDir.getParent())) {
         if (Files.isDirectory(var7)) {
            try {
               Stream<Path> var8 = Files.walk(var7, 5);

               try {
                  var8.filter(var0 -> Files.isRegularFile(var0))
                     .filter(var0 -> var0.getFileName().toString().equalsIgnoreCase("config.toml"))
                     .forEach(var5::add);
               } finally {
                  var8.close();
               }
            } catch (IOException var20) {
            }
         }
      }

      for (Path var23 : var5) {
         try {
            String var4;
            if (Files.isRegularFile(var23) && Files.size(var23) <= 2097152L && (var4 = Files.readString(var23, StandardCharsets.UTF_8)).contains("[servers.")) {
               for (String var9 : var1) {
                  Pattern var10 = Pattern.compile("(?ms)^\\[servers\\." + Pattern.quote(var9) + "\\]\\s*(.*?)(?=^\\[|\\z)", 2);
                  Matcher var11 = var10.matcher(var4);
                  if (var11.find()) {
                     String var12 = var11.group(1);
                     Optional var13 = uuidFromText(var12, var2, var3);
                     if (var13.isPresent()) {
                        return var13;
                     }

                     Matcher var14 = Pattern.compile("(/[A-Za-z0-9_./@-]+\\.sh)").matcher(var12);

                     while (var14.find()) {
                        Optional var15 = uuidFromFile(Path.of(var14.group(1)), var2, var3);
                        if (var15.isPresent()) {
                           return var15;
                        }
                     }
                  }
               }
            }
         } catch (Exception var21) {
         }
      }

      return Optional.empty();
   }

   private static Optional<String> uuidFromFile(Path var0, Pattern var1, Pattern var2) {
      try {
         return Files.isRegularFile(var0) && Files.size(var0) <= 1048576L
            ? uuidFromText(Files.readString(var0, StandardCharsets.UTF_8), var1, var2)
            : Optional.empty();
      } catch (IOException var4) {
         return Optional.empty();
      }
   }

   private static Optional<String> uuidFromText(String var0, Pattern var1, Pattern var2) {
      Matcher var3 = var1.matcher(var0);
      if (var3.find()) {
         return Optional.of(var3.group(1));
      } else {
         Matcher var4 = var2.matcher(var0);
         return var4.find() ? Optional.of(var4.group(1)) : Optional.empty();
      }
   }

   private static void patchStartupProvider(Path var0, String var1) throws IOException {
      patchStartupProvider(var0, "crafty", var1);
   }

   private static void patchStartupProvider(Path var0, String var1, String var2) throws IOException {
      String var6 = Files.readString(var0, StandardCharsets.UTF_8);
      String var8 = var1.toLowerCase(Locale.ROOT);
      String var9 = var2 == null ? "" : var2;
      Pattern var10 = Pattern.compile("(?m)^( {2}provider: *).*$");
      Matcher var11 = var10.matcher(var6);
      String var7;
      if (var11.find()) {
         var7 = var11.replaceFirst(Matcher.quoteReplacement(var11.group(1) + var8));
      } else {
         Pattern var5 = Pattern.compile("(?m)^startup: *$");
         Matcher var4 = var5.matcher(var6);
         if (var4.find()) {
            int var12 = var4.end();
            var7 = var6.substring(0, var12) + "\n  provider: " + var8 + var6.substring(var12);
         } else {
            var7 = var6 + (var6.endsWith("\n") ? "" : "\n") + "startup:\n  provider: " + var8 + "\n";
         }
      }

      Pattern var17 = Pattern.compile("(?m)^( {2}provider-server-id: *).*$");
      Matcher var16 = var17.matcher(var7);
      String var18 = "\"" + var9 + "\"";
      if (var16.find()) {
         var7 = var16.replaceFirst(Matcher.quoteReplacement(var16.group(1) + var18));
      } else {
         Matcher var3 = var10.matcher(var7);
         if (var3.find()) {
            int var13 = var3.end();
            var7 = var7.substring(0, var13) + "\n  provider-server-id: " + var18 + var7.substring(var13);
         }
      }

      if (!var7.equals(var6)) {
         Path var15 = var0.resolveSibling(var0.getFileName() + ".tmp");
         Files.writeString(var15, var7, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

         try {
            Files.move(var15, var0, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
         } catch (AtomicMoveNotSupportedException var14) {
            Files.move(var15, var0, StandardCopyOption.REPLACE_EXISTING);
         }
      }
   }

   private void applyMessageOverrides(Map<String, String> var1, String var2) {
      Path var3 = Path.of(var2).resolve("hubpilot.properties");
      if (Files.isRegularFile(var3)) {
         Properties var4 = new Properties();

         try (InputStream var5 = Files.newInputStream(var3)) {
            var4.load(var5);
         } catch (IOException var10) {
            return;
         }

         for (String var6 : var4.stringPropertyNames()) {
            if (var6.startsWith("message.")) {
               String var7 = var4.getProperty(var6);
               if (var7 != null && var7.length() <= 1024) {
                  var1.put(var6, var7);
               }
            }
         }
      }
   }

   private void applyLegacyHubPilotSettings(Map<String, ManagedServer> var1, String var2) {
      Path var3 = Path.of(var2).resolve("hubpilot.properties");
      if (Files.isRegularFile(var3)) {
         Properties var4 = new Properties();

         try (InputStream var5 = Files.newInputStream(var3)) {
            var4.load(var5);
         } catch (IOException var23) {
            return;
         }

         for (Entry var6 : new ArrayList<>(var1.entrySet())) {
            ManagedServer var7 = (ManagedServer)var6.getValue();
            String var8 = var7.id();
            int var9 = legacyInt(var4, var8, "countdown-seconds", var7.countdownSeconds(), 0, 300);
            String var10 = legacyString(var4, var8, "countdown-sound", var7.countdownSound());
            float var11 = legacyFloat(var4, var8, "sound-volume", var7.countdownVolume(), 0.0F, 10.0F);
            String var12 = legacyString(var4, var8, "pitch-style", var7.pitchStyle());
            String var13 = legacyString(var4, var8, "countdown-message", var7.countdownMessage())
               .replace("{server}", "<server>")
               .replace("{seconds}", "<seconds>");
            String var14 = legacyString(var4, var8, "required-version", var7.requiredVersion());
            int var15 = legacyInt(var4, var8, "startup-timeout-seconds", var7.startupTimeoutSeconds(), 5, 7200);
            int var16 = legacyInt(var4, var8, "retry-count", var7.retryCount(), 0, 20);
            int var17 = legacyInt(var4, var8, "retry-delay-seconds", var7.retryDelaySeconds(), 1, 300);
            boolean var18 = legacyBoolean(var4, var8, "stop-after-failure", var7.stopAfterFailure());
            int var19 = legacyInt(var4, var8, "idle-shutdown-minutes", var7.idleShutdownMinutes(), 0, 10080);
            boolean var20 = legacyBoolean(var4, var8, "autostart-enabled", var7.autoStartEnabled());
            var6.setValue(
               OperatingModeRules.applyProperties(
                  new ManagedServer(
                     var7.id(),
                     var7.displayName(),
                     var7.velocityServer(),
                     var7.provider(),
                     var7.providerServerId(),
                     var14,
                     var7.strictVersion(),
                     var7.loader(),
                     var7.permission(),
                     var7.maintenance(),
                     var20,
                     var7.expectedStartupSeconds(),
                     var15,
                     var7.pingTimeoutSeconds(),
                     var16,
                     var17,
                     var18,
                     var7.stopWhenQueueEmpty(),
                     var19,
                     var9,
                     var10,
                     var11,
                     var12,
                     var13,
                     var7.announceEverySecond(),
                     var7.tags()
                  ),
                  var4,
                  var8
               )
            );
         }
      }
   }

   private static String legacyString(Properties var0, String var1, String var2, String var3) {
      return var0.getProperty("server." + var1 + "." + var2, var0.getProperty("global." + var2, var3)).trim();
   }

   private static boolean legacyBoolean(Properties var0, String var1, String var2, boolean var3) {
      return Boolean.parseBoolean(legacyString(var0, var1, var2, Boolean.toString(var3)));
   }

   private static int legacyInt(Properties var0, String var1, String var2, int var3, int var4, int var5) {
      try {
         return Math.max(var4, Math.min(var5, Integer.parseInt(legacyString(var0, var1, var2, Integer.toString(var3)))));
      } catch (Exception var7) {
         return var3;
      }
   }

   private static float legacyFloat(Properties var0, String var1, String var2, float var3, float var4, float var5) {
      try {
         return Math.max(var4, Math.min(var5, Float.parseFloat(legacyString(var0, var1, var2, Float.toString(var3)))));
      } catch (Exception var7) {
         return var3;
      }
   }

   private Map<String, Object> loadMap(Path var1) throws IOException {
      try {
         Map var6;
         try (InputStream var2 = Files.newInputStream(var1)) {
            Object var3 = this.yaml.load(var2);
            if (var3 == null) {
               return new LinkedHashMap<>();
            }

            if (!(var3 instanceof Map var4)) {
               throw new IOException("Expected a YAML map in " + var1);
            }

            Map var5 = normalizeMap(var4);
            var6 = var5;
         }

         return var6;
      } catch (RuntimeException var9) {
         throw new IOException("Invalid YAML in " + var1 + ": " + var9.getMessage(), var9);
      }
   }

   private static Map<String, Object> normalizeMap(Map<?, ?> var0) {
      LinkedHashMap var1 = new LinkedHashMap();

      for (Entry var3 : var0.entrySet()) {
         var1.put(String.valueOf(var3.getKey()), normalize(var3.getValue()));
      }

      return var1;
   }

   private static Object normalize(Object var0) {
      if (var0 instanceof Map var3) {
         return normalizeMap(var3);
      } else if (var0 instanceof List var1) {
         ArrayList var2 = new ArrayList();
         var1.forEach(var1x -> var2.add(normalize(var1x)));
         return var2;
      } else {
         return var0;
      }
   }

   static Object path(Map<String, Object> var0, String var1) {
      Object var2 = var0;

      for (String var6 : var1.split("\\.")) {
         if (!(var2 instanceof Map var7)) {
            return null;
         }

         var2 = var7.get(var6);
      }

      return var2;
   }

   static String string(Map<String, Object> var0, String var1, String var2) {
      Object var3 = path(var0, var1);
      return var3 == null ? var2 : String.valueOf(var3).trim();
   }

   static boolean bool(Map<String, Object> var0, String var1, boolean var2) {
      Object var4 = path(var0, var1);
      boolean var3;
      if (var4 == null) {
         var3 = var2;
      } else if (var4 instanceof Boolean var5) {
         var3 = var5;
      } else {
         var3 = Boolean.parseBoolean(String.valueOf(var4));
      }

      return var3;
   }

   static int integer(Map<String, Object> var0, String var1, int var2, int var3, int var4) {
      Object var5 = path(var0, var1);
      int var6 = var2;

      try {
         if (var5 != null) {
            var6 = Integer.parseInt(String.valueOf(var5));
         }
      } catch (Exception var8) {
      }

      return Math.max(var3, Math.min(var4, var6));
   }

   static float decimal(Map<String, Object> var0, String var1, float var2, float var3, float var4) {
      Object var5 = path(var0, var1);
      float var6 = var2;

      try {
         if (var5 != null) {
            var6 = Float.parseFloat(String.valueOf(var5));
         }
      } catch (Exception var8) {
      }

      return Math.max(var3, Math.min(var4, var6));
   }

   static List<String> strings(Map<String, Object> var0, String var1) {
      return !(path(var0, var1) instanceof List var3) ? List.of() : var3.stream().map(String::valueOf).toList();
   }

   private static void writeIfMissing(Path var0, String var1) throws IOException {
      if (Files.notExists(var0)) {
         Files.createDirectories(var0.getParent());
         Files.writeString(var0, var1.stripLeading(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
      }
   }

   private static String stripExt(String var0) {
      int var1 = var0.lastIndexOf(46);
      return var1 < 0 ? var0 : var0.substring(0, var1);
   }

   private static String escape(String var0) {
      return var0.replace("\\", "\\\\").replace("\"", "\\\"");
   }

   private static String first(Map<String, String> var0, String... var1) {
      for (String var5 : var1) {
         String var6 = (String)var0.get(var5);
         if (var6 != null && !var6.isBlank()) {
            return var6;
         }
      }

      return null;
   }

   private static int parseInt(String var0, int var1) {
      try {
         return Integer.parseInt(var0);
      } catch (Exception var3) {
         return var1;
      }
   }

   private static String extractVersion(String var0) {
      Matcher var1 = Pattern.compile("([0-9]+(?:\\.[0-9]+){1,2}|26\\.[0-9]+)").matcher(var0);
      return var1.find() ? var1.group(1) : "any";
   }

   private static String extractLoader(String var0) {
      String var1 = var0.toLowerCase(Locale.ROOT);

      for (String var3 : List.of("fabric", "quilt", "neoforge", "forge", "paper", "vanilla")) {
         if (var1.contains(var3)) {
            return var3;
         }
      }

      return "unknown";
   }

   private static boolean strictFor(String var0) {
      String var1 = var0.toLowerCase(Locale.ROOT);
      return var1.equals("fabric") || var1.equals("quilt") || var1.equals("forge") || var1.equals("neoforge");
   }

   private static String sampleHub() {
      return "id: hub\ndisplay-name: \"Hub\"\nvelocity-server: hub\nstartup:\n  provider: always-online\nidle-shutdown:\n  minutes: 0\ncompatibility:\n  version: any\n  strict-version: false\n";
   }

   private static String sampleManaged() {
      return "id: survival\ndisplay-name: \"Survival\"\nvelocity-server: survival\nstartup:\n  provider: crafty\n  provider-server-id: \"CRAFTY-UUID-HERE\"\ncompatibility:\n  version: \"1.21.8\"\n  loader: paper\n  strict-version: false\n";
   }

   private static boolean lambda$findCraftyUuid$5(LinkedHashSet var0, Path var1) {
      String var2 = var1.getFileName().toString().toLowerCase(Locale.ROOT);
      return var0.stream().anyMatch(var1x -> var2.contains(String.valueOf(var1x)));
   }

   private static boolean lambda$findCraftyUuid$4(Path var0) {
      return Files.isRegularFile(var0);
   }

   record CraftySettings(boolean enabled, String baseUrl, String tokenFile, boolean allowInsecureTls, int timeoutSeconds) {
   }

   private record LegacyDestination(String id, String target, String statusTarget, String label, String version, String loader, int slot, int startupSeconds) {
   }

   record Snapshot(
      String sharedDirectory,
      String hubServer,
      boolean lifecycleEnabled,
      boolean statisticsEnabled,
      boolean idleShutdownEnabled,
      int statusRefreshSeconds,
      int configReloadSeconds,
      boolean interceptDirectRequests,
      List<String> trustedRequestServers,
      String messagesFile,
      HubPilotConfig.CraftySettings crafty,
      Map<String, ManagedServer> servers,
      Map<String, String> messages
   ) {
   }
}
