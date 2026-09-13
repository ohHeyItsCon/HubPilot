package dev.hubpilot.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

final class PublicCommandLayer {
   private PublicCommandLayer() {
   }

   static boolean isAdmin(Object var0) {
      return AccessManager.get().isAdminSource(var0);
   }

   static boolean handle(Object var0, Object var1) {
      Object var2;
      String[] var3;
      try {
         var2 = Reflect.call(var1, "source");
         var3 = (String[])Reflect.call(var1, "arguments");
      } catch (Throwable var9) {
         return false;
      }

      if (var3 == null) {
         var3 = new String[0];
      }

      String var4 = var3.length == 0 ? "" : var3[0].toLowerCase(Locale.ROOT);
      AccessManager var5 = AccessManager.get();
      if (var4.isEmpty()) {
         help(var2);
         return true;
      } else if (var4.equals("help") || var4.equals("?") || var4.equals("version")) {
         help(var2);
         return true;
      } else if (var4.equals("claimowner")) {
         UUID var10 = AccessManager.uuidOf(var2);
         if (var10 == null) {
            send(var2, "§cOwner claiming must be run by a player on the configured hub.");
            return true;
         } else if (var5.state() != AccessManager.SetupState.UNCLAIMED) {
            send(var2, "§cThis HubPilot installation already has an owner.");
            return true;
         } else {
            if (PublicBootstrap.requestOwnerClaim(var2)) {
               send(var2, "§eVerifying your OP/claim permission with HubPilot Hub...");
               ClaimTimeout.start(var2);
            } else {
               send(var2, "§cOwner claiming must be run in-game while connected to the configured hub.");
               send(var2, "§7Check Core's hub-server setting if you are already standing on the hub.");
            }

            return true;
         }
      } else if (var4.equals("setup")) {
         setup(var2, var3);
         return true;
      } else if (var5.state() != AccessManager.SetupState.READY) {
         send(var2, "§eHubPilot setup is not complete. Run §f/hp setup§e first.");
         if (var5.state() == AccessManager.SetupState.UNCLAIMED) {
            send(var2, "§7An OP on the configured hub must first run §f/hp claimowner§7.");
         }

         return true;
      } else if (var4.equals("staff")) {
         staff(var2, var0, var3);
         return true;
      } else if (var4.equals("owner")) {
         owner(var2, var0, var3);
         return true;
      } else if (var4.equals("components")) {
         components(var2);
         return true;
      } else if (var4.equals("providers")) {
         providers(var2, var3);
         return true;
      } else if (var4.equals("provider")) {
         provider(var2, var0, var3);
         return true;
      } else {
         String var6 = switch (var4) {
            case "reload" -> "hubpilot.reload";
            case "repair" -> "hubpilot.servers.repair";
            case "discover" -> "hubpilot.servers.discover";
            case "setprovider" -> "hubpilot.servers.provider";
            case "doctor" -> "hubpilot.status.view";
            default -> null;
         };
         if (var6 != null && !var5.can(var2, var6) && !var5.isAdminSource(var2)) {
            send(var2, "§cYou do not have permission for this HubPilot action.");
            return true;
         } else {
            if (var4.equals("reload")) {
               var5.reload(false);
               ProviderRegistry.get().reload();
            }

            return false;
         }
      }
   }

   static boolean allowPlayerCommand(Object var0) {
      if (AccessManager.get().setupComplete()) {
         return true;
      } else {
         send(var0, "§eHubPilot setup is not complete yet. Ask the server owner to run /hp setup.");
         return false;
      }
   }

   private static void setup(Object var0, String[] var1) {
      AccessManager var2 = AccessManager.get();
      AccessManager.SetupState var3 = var2.state();
      UUID var4 = AccessManager.uuidOf(var0);
      if (var3 == AccessManager.SetupState.UNCLAIMED) {
         send(var0, "§bHubPilot Setup §f1.0.2");
         send(var0, "§e1. Claim this installation with §f/hp claimowner§e while OP on the hub.");
         send(var0, "§72. Run /hp setup again to open guided setup.");
      } else if (var4 != null && !var2.isOwner(var4)) {
         send(var0, "§cOnly a HubPilot owner can run first-time setup.");
      } else if (var1.length >= 3 && var1[1].equalsIgnoreCase("override") && var1[2].equalsIgnoreCase("confirm")) {
         var2.markSetupComplete(true);
         send(var0, "§aSetup override accepted. HubPilot commands are now unlocked using the current/default configuration.");
      } else if (var1.length >= 2 && var1[1].equalsIgnoreCase("override")) {
         send(var0, "§eThis skips the guided provider/setup checks and unlocks HubPilot with current/default settings.");
         send(var0, "§fRun /hp setup override confirm §eto continue.");
      } else if (var1.length >= 3 && var1[1].equalsIgnoreCase("provider") && var1[2].equalsIgnoreCase("skip")) {
         ProviderRegistry.get().configurePrimary("always-on", "");
         var2.setProviderSkipped(true);
         send(var0, "§aProvider automation skipped. Servers can still be used as always-on backends.");
      } else if (var1.length < 2 || !var1[1].equalsIgnoreCase("complete")) {
         send(var0, "§b§lHubPilot First-Time Setup");
         send(var0, "§7Owner: §f" + (var4 == null ? "console" : var2.displayName(var4)) + " §8| §7State: §f" + var3);
         send(var0, "§7Provider: §f" + ProviderRegistry.get().primaryType() + " §8| §7Configured: §f" + ProviderRegistry.get().primaryConfigured());
         send(var0, "§7Provider test: §f" + ProviderRegistry.get().lastTest());
         if (var4 != null && PublicBootstrap.sendOpen(var0, "SETUP")) {
            send(var0, "§aOpening the guided setup menu on your hub...");
         } else {
            send(var0, "§eGuided GUI unavailable. Configure providers.yml + secrets.yml, then use /hp setup complete.");
            send(var0, "§7Experienced users: /hp setup override confirm");
         }
      } else if (!ProviderRegistry.get().primaryConfigured() && !var2.providerSkipped()) {
         send(var0, "§cProvider setup is incomplete. Use the setup GUI, /hp setup provider skip, or the override command.");
      } else {
         var2.markSetupComplete(true);
         send(var0, "§aHubPilot setup complete. Commands are now unlocked.");
      }
   }

   private static void staff(Object var0, Object var1, String[] var2) {
      AccessManager var3 = AccessManager.get();
      UUID var4 = AccessManager.uuidOf(var0);
      if (var2.length != 1) {
         if (!var3.can(var0, "hubpilot.staff.manage") && !var3.isOwner(var4)) {
            send(var0, "§cYou do not have permission to manage staff.");
         } else if (var2.length < 3) {
            send(var0, "§eUsage: /hp staff <add|set|remove> <online-player> [helper|moderator|admin]");
         } else {
            PublicCommandLayer.PlayerRef var9 = findOnline(var1, var2[2]);
            if (var9 == null) {
               send(var0, "§cThat player must be online to assign a staff role safely.");
            } else {
               String var10 = var2[1].toLowerCase(Locale.ROOT);
               String var7 = var10.equals("remove") ? "none" : (var2.length >= 4 ? var2[3] : "helper");
               String var8 = var3.setStaff(var4, var9.uuid, var9.name, var7);
               send(var0, "SUCCESS".equals(var8) ? "§aUpdated " + var9.name + " to " + var7 + "." : "§c" + var8);
            }
         }
      } else if (var4 != null && PublicBootstrap.sendOpen(var0, "STAFF")) {
         send(var0, "§aOpening Staff Management...");
      } else {
         send(var0, "§bHubPilot Staff");

         for (AccessManager.StaffEntry var6 : var3.staffEntries()) {
            send(var0, "§7- §f" + var6.username() + " §8[§e" + var6.role() + "§8]");
         }
      }
   }

   private static void owner(Object var0, Object var1, String[] var2) {
      AccessManager var3 = AccessManager.get();
      UUID var4 = AccessManager.uuidOf(var0);
      if (!var3.isOwner(var4)) {
         send(var0, "§cOnly an owner can manage owners.");
      } else if (var2.length < 3) {
         send(var0, "§eUsage: /hp owner <add|remove> <online-player>");
      } else {
         PublicCommandLayer.PlayerRef var5 = findOnline(var1, var2[2]);
         if (var5 == null) {
            send(var0, "§cThat player must be online.");
         } else {
            String var6 = var2[1].equalsIgnoreCase("add")
               ? var3.addOwner(var4, var5.uuid, var5.name)
               : (var2[1].equalsIgnoreCase("remove") ? var3.removeOwner(var4, var5.uuid) : "Unknown owner action.");
            send(var0, "SUCCESS".equals(var6) ? "§aOwner list updated." : "§c" + var6);
         }
      }
   }

   private static void components(Object var0) {
      send(var0, "§bHubPilot Components (recently detected)");
      long var1 = System.currentTimeMillis();

      for (AccessManager.ComponentSeen var4 : AccessManager.get().components()) {
         long var5 = Math.max(0L, (var1 - var4.seenAt()) / 1000L);
         send(var0, "§7- §f" + var4.component() + " §7" + var4.version() + " §8@ §f" + var4.server() + " §8(" + var5 + "s ago)");
      }
   }

   private static void providers(Object var0, String[] var1) {
      AccessManager var2 = AccessManager.get();
      if (!var2.isAdminSource(var0)) {
         send(var0, "§cYou do not have permission.");
      } else if (var1.length >= 2 && var1[1].equalsIgnoreCase("test")) {
         String var5 = var1.length >= 3 ? var1[2] : ProviderRegistry.get().primaryId();
         ProviderRegistry.Result var6 = ProviderRegistry.get().test(var5);
         send(var0, var6.success() ? "§aProvider connection succeeded (HTTP " + var6.statusCode() + ")." : "§cProvider test failed: " + var6.error());
      } else {
         send(var0, "§bProviders §7(primary: §f" + ProviderRegistry.get().primaryId() + "§7)");

         for (ProviderRegistry.Definition var4 : ProviderRegistry.get().definitions()) {
            send(var0, "§7- §f" + var4.id() + " §8type=§f" + var4.type() + " §8enabled=§f" + var4.enabled());
         }
      }
   }

   private static void provider(Object var0, Object var1, String[] var2) {
      AccessManager var3 = AccessManager.get();
      if (!var3.can(var0, "hubpilot.servers.provider") && !var3.isAdminSource(var0)) {
         send(var0, "§cYou do not have permission to edit provider mappings.");
      } else if (var2.length < 3) {
         send(var0, "§eUsage: /hp provider <server> <provider-id|always-online> [provider-server-id]");
         send(var0, "§7Use /hp providers to list configured provider ids.");
      } else {
         String var4 = var2[1];
         String var5 = var2[2].toLowerCase(Locale.ROOT);
         String var6 = var2.length >= 4 ? var2[3] : "";
         if (!ProviderRegistry.get().known(var5) && !var5.equals("always-online")) {
            send(var0, "§cUnknown provider id: " + var5 + ". Check providers.yml or /hp providers.");
         } else {
            try {
               Object var7 = Reflect.get(var1, "config");
               if (var7 == null) {
                  throw new IllegalStateException("Core config unavailable");
               }

               Method var8 = var7.getClass().getDeclaredMethod("findServerFile", String.class);
               var8.setAccessible(true);
               Path var9 = (Path)var8.invoke(var7, var4);
               Method var10 = var7.getClass().getDeclaredMethod("patchStartupProvider", Path.class, String.class, String.class);
               var10.setAccessible(true);
               var10.invoke(null, var9, var5, var5.equals("always-online") ? "" : var6);
               Method var11 = var7.getClass().getDeclaredMethod("reload");
               var11.setAccessible(true);
               var11.invoke(var7);
               send(var0, "§aUpdated " + var4 + " -> provider " + var5 + (var6.isBlank() ? "" : " (" + var6 + ")") + ".");
            } catch (Throwable var12) {
               send(var0, "§cProvider mapping failed: " + root(var12));
            }
         }
      }
   }

   private static PublicCommandLayer.PlayerRef findOnline(Object var0, String var1) {
      try {
         Object var2 = Reflect.get(var0, "proxy");
         Object var3 = Reflect.call(var2, "getPlayer", var1);
         Object var4 = Reflect.call(var3, "orElse", (Object)null);
         if (var4 == null) {
            return null;
         } else {
            UUID var5 = AccessManager.uuidOf(var4);
            String var6 = AccessManager.usernameOf(var4);
            return var5 == null ? null : new PublicCommandLayer.PlayerRef(var5, var6);
         }
      } catch (Throwable var7) {
         return null;
      }
   }

   private static void help(Object var0) {
      send(var0, "§b§lHubPilot Core §f1.0.2");
      AccessManager.SetupState var1 = AccessManager.get().state();
      send(var0, "§7Setup: §f" + var1);
      if (var1 == AccessManager.SetupState.UNCLAIMED) {
         send(var0, "§e/hp claimowner §7- claim a new installation as a hub OP");
         send(var0, "§e/hp setup §7- first-time setup");
      } else if (var1 != AccessManager.SetupState.READY) {
         send(var0, "§e/hp setup §7- continue guided setup");
         send(var0, "§e/hp setup override §7- experienced-user setup skip");
      } else {
         send(var0, "§e/hp discover §7- find Velocity servers");
         send(var0, "§e/hp staff §7- staff list / GUI");
         send(var0, "§e/hp components §7- detected HubPilot components");
         send(var0, "§e/hp providers §7- provider status");
         send(var0, "§e/hp status <server> §7- server status");
         send(var0, "§e/hp request <server> §7- request a server");
         send(var0, "§e/hub §7or §e/lobby §7- return to the hub");
      }
   }

   static void send(Object var0, String var1) {
      if (var0 != null) {
         try {
            Reflect.call(var0, "sendPlainMessage", var1);
         } catch (Throwable var4) {
            try {
               Reflect.call(var0, "sendMessage", var1);
            } catch (Throwable var3) {
            }
         }
      }
   }

   private static String root(Throwable var0) {
      if (var0 instanceof InvocationTargetException var1 && var1.getCause() != null) {
         var0 = var1.getCause();
      }

      while (var0.getCause() != null && var0.getCause() != var0) {
         var0 = var0.getCause();
      }

      return var0.getMessage() == null ? var0.getClass().getSimpleName() : var0.getMessage();
   }

   private record PlayerRef(UUID uuid, String name) {
   }
}
