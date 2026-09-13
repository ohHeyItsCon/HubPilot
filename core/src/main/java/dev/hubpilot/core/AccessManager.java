package dev.hubpilot.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

final class AccessManager {
   private static final AccessManager INSTANCE = new AccessManager();
   private final Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
   private final Map<UUID, AccessManager.Auth> auth = new ConcurrentHashMap<>();
   private final Map<UUID, AccessManager.StaffEntry> staff = new LinkedHashMap<>();
   private final Map<UUID, String> owners = new LinkedHashMap<>();
   private final Map<String, AccessManager.Role> roles = new LinkedHashMap<>();
   private final Map<String, AccessManager.ComponentSeen> components = new ConcurrentHashMap<>();
   private Path dataDir;
   private String hubServer = "hub";
   private String opDefaultRole = "admin";
   private boolean setupComplete;
   private boolean providerSkipped;
   private long staffMtime = -1L;
   private long permMtime = -1L;
   private long setupMtime = -1L;

   static AccessManager get() {
      return INSTANCE;
   }

   synchronized void initialize(Path var1, String var2) {
      this.dataDir = var1;
      if (var2 != null && !var2.isBlank()) {
         this.hubServer = var2.toLowerCase(Locale.ROOT);
      }

      PublicDefaults.install(var1);
      this.reload(true);
      this.recordComponent("Core", "1.0.2", "velocity");
   }

   synchronized void setHubServer(String var1) {
      if (var1 != null && !var1.isBlank()) {
         this.hubServer = var1.toLowerCase(Locale.ROOT);
      }
   }

   void recordAuth(UUID var1, String var2, String var3, boolean var4, boolean var5, boolean var6, boolean var7, boolean var8) {
      if (var1 != null) {
         this.auth.put(var1, new AccessManager.Auth(var1, cleanName(var2), lower(var3), var4, var5, var6, var7, var8, System.currentTimeMillis()));
      }
   }

   AccessManager.Auth auth(UUID var1) {
      return this.auth.get(var1);
   }

   boolean isTrustedHubAuth(UUID var1) {
      AccessManager.Auth var2 = this.auth.get(var1);
      return var2 != null && System.currentTimeMillis() - var2.updatedAt() < 15000L && this.hubServer.equals(lower(var2.server()));
   }

   synchronized AccessManager.SetupState state() {
      this.reloadIfChanged();
      if (this.owners.isEmpty()) {
         return AccessManager.SetupState.UNCLAIMED;
      } else {
         return this.setupComplete ? AccessManager.SetupState.READY : AccessManager.SetupState.CLAIMED_NOT_SETUP;
      }
   }

   synchronized boolean providerSkipped() {
      this.reloadIfChanged();
      return this.providerSkipped;
   }

   synchronized boolean setupComplete() {
      return this.state() == AccessManager.SetupState.READY;
   }

   synchronized boolean hasOwners() {
      this.reloadIfChanged();
      return !this.owners.isEmpty();
   }

   synchronized boolean isOwner(UUID var1) {
      this.reloadIfChanged();
      return var1 != null && this.owners.containsKey(var1);
   }

   synchronized String role(UUID var1) {
      this.reloadIfChanged();
      if (var1 == null) {
         return "console";
      } else if (this.owners.containsKey(var1)) {
         return "owner";
      } else {
         AccessManager.StaffEntry var2 = this.staff.get(var1);
         if (var2 != null) {
            return var2.role();
         } else {
            AccessManager.Auth var3 = this.auth.get(var1);
            if (var3 != null && System.currentTimeMillis() - var3.updatedAt() < 15000L) {
               if (var3.directAdmin()) {
                  return "admin";
               }

               if (var3.directModerator()) {
                  return "moderator";
               }

               if (var3.directHelper()) {
                  return "helper";
               }

               if (var3.op() && !"none".equals(this.opDefaultRole)) {
                  return this.opDefaultRole;
               }
            }

            return "player";
         }
      }
   }

   int priority(UUID var1) {
      String var2 = this.role(var1);
      if (!"owner".equals(var2) && !"console".equals(var2)) {
         AccessManager.Role var3 = this.roles.get(var2);
         return var3 == null ? 0 : var3.priority();
      } else {
         return 1000;
      }
   }

   boolean isAdminSource(Object var1) {
      UUID var2 = uuidOf(var1);
      if (var2 == null) {
         return true;
      } else if (this.isOwner(var2)) {
         return true;
      } else if (this.priority(var2) >= this.priorityFor("admin")) {
         return true;
      } else {
         try {
            Object var3 = Reflect.call(var1, "hasPermission", "hubpilot.admin");
            if (Boolean.TRUE.equals(var3)) {
               return true;
            }
         } catch (Throwable var4) {
         }

         return false;
      }
   }

   boolean can(Object var1, String var2) {
      UUID var3 = uuidOf(var1);
      return var3 == null ? true : this.can(var3, var2);
   }

   synchronized boolean can(UUID var1, String var2) {
      this.reloadIfChanged();
      if (var1 != null && !this.owners.containsKey(var1)) {
         String var3 = this.role(var1);
         return "player".equals(var3) ? false : this.roleHas(var3, var2, new HashSet<>());
      } else {
         return true;
      }
   }

   private boolean roleHas(String var1, String var2, Set<String> var3) {
      if (!var3.add(var1)) {
         return false;
      } else {
         AccessManager.Role var4 = this.roles.get(var1);
         if (var4 == null) {
            return false;
         } else {
            for (String var6 : var4.permissions()) {
               if (matches(var6, var2)) {
                  return true;
               }
            }

            return var4.inherits() != null && !var4.inherits().isBlank() && this.roleHas(var4.inherits(), var2, var3);
         }
      }
   }

   private static boolean matches(String var0, String var1) {
      if (var0 == null || var1 == null) {
         return false;
      } else if (var0.equalsIgnoreCase("hubpilot.*") || var0.equalsIgnoreCase(var1)) {
         return true;
      } else if (var0.endsWith(".*")) {
         String var2 = var0.substring(0, var0.length() - 1);
         return var1.regionMatches(true, 0, var2, 0, var2.length());
      } else {
         return false;
      }
   }

   synchronized String claim(UUID var1, String var2) {
      this.reloadIfChanged();
      if (!this.owners.isEmpty()) {
         return "This HubPilot installation already has an owner.";
      } else {
         AccessManager.Auth var3 = this.auth.get(var1);
         if (var3 == null || System.currentTimeMillis() - var3.updatedAt() > 15000L || !this.hubServer.equals(lower(var3.server()))) {
            return "Owner claiming must be run in-game while connected to the configured hub.";
         } else if (!var3.op() && !var3.claimPermission()) {
            return "You must be an OP on the hub (or have hubpilot.claimowner) to claim an unowned installation.";
         } else {
            this.owners.put(var1, cleanName(var2));
            this.writeStaff();
            return "SUCCESS";
         }
      }
   }

   synchronized String setStaff(UUID var1, UUID var2, String var3, String var4) {
      this.reloadIfChanged();
      String var5 = lower(var4);
      if (var2 == null) {
         return "Target player UUID is missing.";
      } else if (this.owners.containsKey(var2)) {
         return "Owners cannot be changed through the staff role editor.";
      } else {
         int var6 = this.priority(var1);
         if (var6 < this.priorityFor("admin")) {
            return "You do not have permission to manage staff.";
         } else {
            int var7 = this.priority(var2);
            int var8 = !"none".equals(var5) && !"player".equals(var5) ? this.priorityFor(var5) : 0;
            if (var8 < 0) {
               return "Unknown role: " + var4;
            } else if (this.isOwner(var1) || var8 < this.priorityFor("admin") && var7 < var6) {
               if (!"none".equals(var5) && !"player".equals(var5)) {
                  this.staff.put(var2, new AccessManager.StaffEntry(var2, cleanName(var3), var5));
               } else {
                  this.staff.remove(var2);
               }

               this.writeStaff();
               return "SUCCESS";
            } else {
               return "Only an owner can grant/remove administrator-level staff.";
            }
         }
      }
   }

   synchronized String addOwner(UUID var1, UUID var2, String var3) {
      this.reloadIfChanged();
      if (!this.owners.containsKey(var1)) {
         return "Only an owner can add another owner.";
      } else if (var2 == null) {
         return "Target player UUID is missing.";
      } else {
         this.staff.remove(var2);
         this.owners.put(var2, cleanName(var3));
         this.writeStaff();
         return "SUCCESS";
      }
   }

   synchronized String removeOwner(UUID var1, UUID var2) {
      this.reloadIfChanged();
      if (!this.owners.containsKey(var1)) {
         return "Only an owner can remove an owner.";
      } else if (var2 != null && this.owners.containsKey(var2)) {
         if (this.owners.size() <= 1) {
            return "HubPilot must keep at least one owner. Add another owner first.";
         } else {
            this.owners.remove(var2);
            this.writeStaff();
            return "SUCCESS";
         }
      } else {
         return "That player is not an owner.";
      }
   }

   synchronized void markSetupComplete(boolean var1) {
      this.setupComplete = var1;
      this.writeSetup();
   }

   synchronized void setProviderSkipped(boolean var1) {
      this.providerSkipped = var1;
      this.writeSetup();
   }

   synchronized List<AccessManager.StaffEntry> staffEntries() {
      this.reloadIfChanged();
      ArrayList var1 = new ArrayList();
      this.owners.forEach((var1x, var2) -> var1.add(new AccessManager.StaffEntry(var1x, var2, "owner")));
      var1.addAll(this.staff.values());
      return var1;
   }

   synchronized String displayName(UUID var1) {
      if (var1 == null) {
         return "console";
      } else if (this.owners.containsKey(var1)) {
         return this.owners.get(var1);
      } else {
         AccessManager.StaffEntry var2 = this.staff.get(var1);
         if (var2 != null) {
            return var2.username();
         } else {
            AccessManager.Auth var3 = this.auth.get(var1);
            return var3 == null ? var1.toString() : var3.username();
         }
      }
   }

   void recordComponent(String var1, String var2, String var3) {
      if (var1 != null && !var1.isBlank()) {
         this.components
            .put(
               var1.toLowerCase(Locale.ROOT) + "@" + lower(var3),
               new AccessManager.ComponentSeen(var1, var2 == null ? "?" : var2, var3 == null ? "?" : var3, System.currentTimeMillis())
            );
      }
   }

   List<AccessManager.ComponentSeen> components() {
      ArrayList var1 = new ArrayList<>(this.components.values());
      var1.sort(
         Comparator.comparing(AccessManager.ComponentSeen::component, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(AccessManager.ComponentSeen::server, String.CASE_INSENSITIVE_ORDER)
      );
      return var1;
   }

   static UUID uuidOf(Object var0) {
      if (var0 == null) {
         return null;
      } else {
         try {
            return Reflect.call(var0, "getUniqueId") instanceof UUID var2 ? var2 : null;
         } catch (Throwable var3) {
            return null;
         }
      }
   }

   static String usernameOf(Object var0) {
      if (var0 == null) {
         return "console";
      } else {
         try {
            Object var1 = Reflect.call(var0, "getUsername");
            return var1 == null ? "unknown" : String.valueOf(var1);
         } catch (Throwable var2) {
            return "unknown";
         }
      }
   }

   private int priorityFor(String var1) {
      if ("owner".equals(var1)) {
         return 1000;
      } else {
         AccessManager.Role var2 = this.roles.get(var1);
         return var2 == null ? -1 : var2.priority();
      }
   }

   private synchronized void reloadIfChanged() {
      if (this.dataDir != null) {
         try {
            long var1 = mtime(this.dataDir.resolve("staff.yml"));
            long var3 = mtime(this.dataDir.resolve("permissions.yml"));
            long var5 = mtime(this.dataDir.resolve("setup.yml"));
            if (var1 != this.staffMtime || var3 != this.permMtime || var5 != this.setupMtime) {
               this.reload(false);
            }
         } catch (Exception var7) {
         }
      }
   }

   synchronized void reload(boolean var1) {
      if (this.dataDir != null) {
         try {
            this.loadPermissions();
            this.loadStaff();
            this.loadSetup();
         } catch (Exception var3) {
         }
      }
   }

   private void loadPermissions() throws IOException {
      Map var1 = this.load(this.dataDir.resolve("permissions.yml"));
      String var2 = String.valueOf(var1.getOrDefault("op-default-role", "admin")).toLowerCase(Locale.ROOT);
      if (!Set.of("none", "helper", "moderator", "admin").contains(var2)) {
         var2 = "admin";
      }

      this.opDefaultRole = var2;
      this.roles.clear();
      if (var1.get("roles") instanceof Map<?, ?> var4) {
         for (Entry var6 : var4.entrySet()) {
            String var7 = lower(String.valueOf(var6.getKey()));
            if (var6.getValue() instanceof Map var8) {
               Object var10000 = var8.get("priority");

               int var16 = intVal(var10000, switch (var7) {
                  case "helper" -> 10;
                  case "moderator" -> 20;
                  case "admin" -> 30;
                  default -> 5;
               });
               String var10 = var8.get("inherits") == null ? null : lower(String.valueOf(var8.get("inherits")));
               LinkedHashSet var17 = new LinkedHashSet();
               Object var12 = var8.get("permissions");
               if (var12 instanceof Iterable) {
                  for (Object var15 : (Iterable)var12) {
                     var17.add(String.valueOf(var15));
                  }
               }

               this.roles.put(var7, new AccessManager.Role(var7, var16, var10, Set.copyOf(var17)));
            }
         }
      }

      this.roles.putIfAbsent("helper", new AccessManager.Role("helper", 10, null, Set.of("hubpilot.status.view")));
      this.roles
         .putIfAbsent(
            "moderator",
            new AccessManager.Role("moderator", 20, "helper", Set.of("hubpilot.server.request", "hubpilot.maintenance.toggle", "hubpilot.debug.request"))
         );
      this.roles
         .putIfAbsent(
            "admin",
            new AccessManager.Role(
               "admin",
               30,
               "moderator",
               Set.of("hubpilot.admin", "hubpilot.servers.*", "hubpilot.navigator.*", "hubpilot.interact.*", "hubpilot.staff.view", "hubpilot.reload")
            )
         );
      this.permMtime = mtime(this.dataDir.resolve("permissions.yml"));
   }

   private void loadStaff() throws IOException {
      Map var1 = this.load(this.dataDir.resolve("staff.yml"));
      this.owners.clear();
      this.staff.clear();
      Object var2 = var1.get("owners");
      if (var2 instanceof Iterable) {
         for (Object var5 : (Iterable)var2) {
            if (var5 instanceof Map var6) {
               UUID var7 = parseUuid(var6.get("uuid"));
               if (var7 != null) {
                  this.owners.put(var7, cleanName(String.valueOf(var6.containsKey("username") ? var6.get("username") : var7.toString())));
               }
            }
         }
      }

      Object var10 = var1.get("staff");
      if (var10 instanceof Iterable) {
         for (Object var13 : (Iterable)var10) {
            if (var13 instanceof Map var14) {
               UUID var8 = parseUuid(var14.get("uuid"));
               if (var8 != null) {
                  String var9 = lower(String.valueOf(var14.containsKey("role") ? var14.get("role") : "helper"));
                  if (this.roles.containsKey(var9)) {
                     this.staff
                        .put(
                           var8,
                           new AccessManager.StaffEntry(
                              var8, cleanName(String.valueOf(var14.containsKey("username") ? var14.get("username") : var8.toString())), var9
                           )
                        );
                  }
               }
            }
         }
      }

      this.staffMtime = mtime(this.dataDir.resolve("staff.yml"));
   }

   private void loadSetup() throws IOException {
      Map var1 = this.load(this.dataDir.resolve("setup.yml"));
      this.setupComplete = bool(var1.get("complete"), false);
      this.providerSkipped = bool(var1.get("provider-skipped"), false);
      this.setupMtime = mtime(this.dataDir.resolve("setup.yml"));
   }

   private Map<String, Object> load(Path var1) throws IOException {
      if (!Files.exists(var1)) {
         return new LinkedHashMap<>();
      } else {
         LinkedHashMap var5;
         try (BufferedReader var2 = Files.newBufferedReader(var1, StandardCharsets.UTF_8)) {
            if (this.yaml.load(var2) instanceof Map<?, ?> var4) {
               var5 = new LinkedHashMap();
               var4.forEach((var1x, var2x) -> var5.put(String.valueOf(var1x), var2x));
               return var5;
            }

            var5 = new LinkedHashMap();
         }

         return var5;
      }
   }

   private void writeStaff() {
      if (this.dataDir != null) {
         StringBuilder var1 = new StringBuilder(
            "# ============================================================\n#                       HUBPILOT STAFF\n# ============================================================\n# Managed by HubPilot. UUIDs are authoritative; usernames are labels.\n# Remove every owner entry while the proxy is stopped to return to\n# UNCLAIMED recovery mode.\n\nowners:\n"
         );
         if (this.owners.isEmpty()) {
            var1.append("  []\n");
         } else {
            this.owners
               .forEach((var1x, var2) -> var1.append("  - uuid: \"").append(var1x).append("\"\n    username: \"").append(yamlQuote(var2)).append("\"\n"));
         }

         var1.append("staff:\n");
         if (this.staff.isEmpty()) {
            var1.append("  []\n");
         } else {
            this.staff
               .values()
               .forEach(
                  var1x -> var1.append("  - uuid: \"")
                     .append(var1x.uuid())
                     .append("\"\n    username: \"")
                     .append(yamlQuote(var1x.username()))
                     .append("\"\n    role: ")
                     .append(var1x.role())
                     .append("\n")
               );
         }

         atomic(this.dataDir.resolve("staff.yml"), var1.toString());
         this.staffMtime = mtime(this.dataDir.resolve("staff.yml"));
      }
   }

   private void writeSetup() {
      if (this.dataDir != null) {
         String var1 = "# HubPilot first-run state. Normally managed by /hp setup.\nsetup-version: 1\ncomplete: "
            + this.setupComplete
            + "\nprovider-skipped: "
            + this.providerSkipped
            + "\n";
         atomic(this.dataDir.resolve("setup.yml"), var1);
         this.setupMtime = mtime(this.dataDir.resolve("setup.yml"));
      }
   }

   private static void atomic(Path var0, String var1) {
      try {
         Path var2 = var0.resolveSibling(var0.getFileName() + ".tmp");
         Files.writeString(var2, var1, StandardCharsets.UTF_8);

         try {
            Files.move(var2, var0, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (AtomicMoveNotSupportedException var4) {
            Files.move(var2, var0, StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (Exception var5) {
      }
   }

   private static long mtime(Path var0) {
      try {
         return Files.getLastModifiedTime(var0).toMillis();
      } catch (Exception var2) {
         return -1L;
      }
   }

   private static UUID parseUuid(Object var0) {
      try {
         return var0 == null ? null : UUID.fromString(String.valueOf(var0));
      } catch (Exception var2) {
         return null;
      }
   }

   private static boolean bool(Object var0, boolean var1) {
      return var0 == null ? var1 : Boolean.parseBoolean(String.valueOf(var0));
   }

   private static int intVal(Object var0, int var1) {
      try {
         return var0 == null ? var1 : Integer.parseInt(String.valueOf(var0));
      } catch (Exception var3) {
         return var1;
      }
   }

   private static String lower(String var0) {
      return var0 == null ? "" : var0.trim().toLowerCase(Locale.ROOT);
   }

   private static String cleanName(String var0) {
      if (var0 == null) {
         return "unknown";
      } else {
         var0 = var0.trim();
         return var0.length() > 64 ? var0.substring(0, 64) : var0;
      }
   }

   private static String yamlQuote(String var0) {
      return var0.replace("\\", "\\\\").replace("\"", "\\\"");
   }

   record Auth(
      UUID uuid,
      String username,
      String server,
      boolean op,
      boolean directAdmin,
      boolean directModerator,
      boolean directHelper,
      boolean claimPermission,
      long updatedAt
   ) {
   }

   record ComponentSeen(String component, String version, String server, long seenAt) {
   }

   private record Role(String name, int priority, String inherits, Set<String> permissions) {
   }

   static enum SetupState {
      UNCLAIMED,
      CLAIMED_NOT_SETUP,
      READY;
   }

   record StaffEntry(UUID uuid, String username, String role) {
   }
}
