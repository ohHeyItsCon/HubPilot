package dev.hubpilot.hub.publicapi;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.adminui.AdminHomeHolder;
import dev.hubpilot.hub.adminui.AdminNavigation;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

public final class PublicHubBootstrap implements Listener, PluginMessageListener {
   public static final String CHANNEL = "hubpilot:control";
   private static volatile PublicHubBootstrap INSTANCE;
   private final HubPilotHubPlugin plugin;
   private final Map<UUID, PublicHubBootstrap.State> states = new ConcurrentHashMap<>();
   private final Map<UUID, String> urlPrompts = new ConcurrentHashMap<>();
   private static final List<String> PROVIDER_TYPES = List.of("always-on", "crafty", "pterodactyl", "generic-http");

   private PublicHubBootstrap(HubPilotHubPlugin var1) {
      this.plugin = var1;
   }

   public static void attach(HubPilotHubPlugin var0) {
      DiscoverySyncBridge.attach(var0);
      if (INSTANCE == null) {
         PublicHubBootstrap var1 = new PublicHubBootstrap(var0);
         INSTANCE = var1;
         Bukkit.getPluginManager().registerEvents(var1, var0);
         SetupItemManager.attach(var0);
         Bukkit.getMessenger().registerOutgoingPluginChannel(var0, "hubpilot:control");
         Bukkit.getMessenger().registerIncomingPluginChannel(var0, "hubpilot:control", var1);
         Bukkit.getScheduler().runTaskTimer(var0, var1::heartbeat, 1L, 20L);
         var0.getLogger().info("HubPilot 1.0.2 setup/staff authorization bridge enabled.");
      }
   }

   public static boolean canAdmin(Object var0) {
      if (var0 instanceof CommandSender var1) {
         if (!(var1 instanceof Player var2)) {
            return true;
         } else {
            PublicHubBootstrap.State var3 = state(var2.getUniqueId());
            return var3 == null || !var3.role().equals("owner") && !var3.role().equals("admin") ? var1.hasPermission("hubpilot.admin") || var2.isOp() : true;
         }
      } else {
         return false;
      }
   }

   public static boolean isOwner(Player var0) {
      PublicHubBootstrap.State var1 = state(var0.getUniqueId());
      return var1 != null && "owner".equals(var1.role());
   }

   public static boolean setupReady() {
      for (PublicHubBootstrap.State var1 : instanceStates()) {
         if ("READY".equals(var1.setup())) {
            return true;
         }
      }

      return false;
   }

   public static boolean allowInteractCommand(Object var0, String[] var1) {
      if (var0 instanceof CommandSender var2) {
         if (!setupReady()) {
            var2.sendMessage("§eHubPilot setup is not complete. The owner must run /hp setup first.");
            return false;
         } else if (var2 instanceof Player var3) {
            PublicHubBootstrap.State var4 = state(var3.getUniqueId());
            String var5 = var4 == null ? "player" : var4.role();
            if (!var5.equals("owner")
               && !var5.equals("admin")
               && !var2.hasPermission("hubpilot.interact.admin")
               && !var2.hasPermission("hubpilot.interact.edit")
               && !var3.isOp()) {
               var2.sendMessage("§cYou do not have permission to manage HubPilot Interact.");
               return false;
            } else {
               return true;
            }
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   public static PublicHubBootstrap.State state(UUID var0) {
      PublicHubBootstrap var1 = INSTANCE;
      return var1 == null ? null : var1.states.get(var0);
   }

   private static Collection<PublicHubBootstrap.State> instanceStates() {
      PublicHubBootstrap var0 = INSTANCE;
      return (Collection<PublicHubBootstrap.State>)(var0 == null ? List.of() : var0.states.values());
   }

   private void heartbeat() {
      for (Player var2 : Bukkit.getOnlinePlayers()) {
         try {
            this.sendAuth(var2);
            this.sendComponent(var2, "Hub", this.plugin.getDescription().getVersion());
            Plugin var3 = Bukkit.getPluginManager().getPlugin("HubPilotInteract");
            if (var3 != null) {
               this.sendComponent(var2, "Interact", var3.getDescription().getVersion());
            }
         } catch (Throwable var4) {
            this.plugin.getLogger().warning("HubPilot authorization heartbeat failed for " + var2.getName() + ": " + var4.getMessage());
         }
      }
   }

   private void sendAuth(Player var1) throws IOException {
      ByteArrayOutputStream var2 = new ByteArrayOutputStream();

      try (DataOutputStream var3 = new DataOutputStream(var2)) {
         var3.writeUTF("AUTH");
         var3.writeUTF(var1.getUniqueId().toString());
         var3.writeUTF(var1.getName());
         var3.writeBoolean(var1.isOp());
         var3.writeBoolean(var1.hasPermission("hubpilot.admin") || var1.hasPermission("hubpilot.role.admin"));
         var3.writeBoolean(var1.hasPermission("hubpilot.role.moderator"));
         var3.writeBoolean(var1.hasPermission("hubpilot.role.helper"));
         var3.writeBoolean(var1.hasPermission("hubpilot.claimowner"));
      }

      var1.sendPluginMessage(this.plugin, "hubpilot:control", var2.toByteArray());
   }

   private void sendComponent(Player var1, String var2, String var3) throws IOException {
      ByteArrayOutputStream var4 = new ByteArrayOutputStream();

      try (DataOutputStream var5 = new DataOutputStream(var4)) {
         var5.writeUTF("COMPONENT");
         var5.writeUTF(var1.getUniqueId().toString());
         var5.writeUTF(var2);
         var5.writeUTF(var3 == null ? "?" : var3);
      }

      var1.sendPluginMessage(this.plugin, "hubpilot:control", var4.toByteArray());
   }

   void action(Player var1, String var2, String... var3) {
      try {
         ByteArrayOutputStream var4 = new ByteArrayOutputStream();

         try (DataOutputStream var5 = new DataOutputStream(var4)) {
            var5.writeUTF("ACTION");
            var5.writeUTF(var1.getUniqueId().toString());
            var5.writeUTF(var2);

            for (String var9 : var3) {
               var5.writeUTF(var9 == null ? "" : var9);
            }
         }

         var1.sendPluginMessage(this.plugin, "hubpilot:control", var4.toByteArray());
      } catch (Exception var12) {
         var1.sendMessage("§cCould not send HubPilot setup action: " + var12.getMessage());
      }
   }

   public void onPluginMessageReceived(String var1, Player var2, byte[] var3) {
      if (!DiscoverySyncBridge.handle(var1, var2, var3)) {
         if (!UnifiedGuiBridge.handle(var1, var2, var3)) {
            if ("hubpilot:control".equalsIgnoreCase(var1) && var2 != null) {
               try (DataInputStream var4 = new DataInputStream(new ByteArrayInputStream(var3))) {
                  String var5 = var4.readUTF();
                  switch (var5) {
                     case "STATE":
                        UUID var18 = UUID.fromString(var4.readUTF());
                        if (var18.equals(var2.getUniqueId())) {
                           String var20 = var4.readUTF();
                           String var10 = var4.readUTF();
                           String var11 = var4.readUTF();
                           boolean var12 = Boolean.parseBoolean(var4.readUTF());
                           String var13 = var4.readUTF();
                           this.states.put(var18, new PublicHubBootstrap.State(var20, var10, var11, var12, var13, System.currentTimeMillis()));
                           AdminItemManager.ensure(this.plugin, var2);
                           return;
                        }

                        return;
                     case "FEEDBACK":
                        UUID var17 = UUID.fromString(var4.readUTF());
                        if (var17.equals(var2.getUniqueId())) {
                           String var19 = var4.readUTF();
                           var2.sendMessage(var19.startsWith("SUCCESS") ? "§a" + var19.replaceFirst("^SUCCESS:? ?", "") : "§c" + var19);
                           return;
                        }

                        return;
                     case "OPEN":
                        String var8 = var4.readUTF();
                        if ("SETUP".equalsIgnoreCase(var8)) {
                           openSetup(var2);
                        } else if ("STAFF".equalsIgnoreCase(var8)) {
                           openStaff(var2);
                        } else if ("CLAIM_OWNER".equalsIgnoreCase(var8)) {
                           boolean var9 = var2.hasPermission("hubpilot.claimowner");
                           this.action(var2, "CLAIM_OWNER", var2.getName(), String.valueOf(var2.isOp()), String.valueOf(var9));
                        }
                        break;
                     default:
                        return;
                  }
               } catch (Exception var16) {
                  var2.sendMessage("§cHubPilot Hub could not process that request. Check the hub console for the exact error.");
                  var16.printStackTrace();
               }
            }
         }
      }
   }

   public static void openSetup(Player var0) {
      PublicHubBootstrap var1 = INSTANCE;
      if (var1 != null) {
         var1.openSetup0(var0);
      }
   }

   private void openSetup0(Player var1) {
      PublicHubBootstrap.SetupHolder var2 = new PublicHubBootstrap.SetupHolder();
      Inventory var3 = Bukkit.createInventory(var2, 54, "§8§lHubPilot Setup");
      var2.inventory = var3;
      fill(var3);
      PublicHubBootstrap.State var4 = this.states.get(var1.getUniqueId());
      String var5 = var4 == null ? "SYNCING" : var4.setup();
      String var6 = var4 == null ? "unknown" : var4.role();
      String var7 = var4 == null ? "unknown" : var4.providerType();
      var3.setItem(
         10,
         item(
            Material.NETHER_STAR,
            "§b§lInstallation",
            List.of("§7State: §f" + var5, "§7Your role: §f" + var6, "", "§7Claim ownership first with §f/hp claimowner")
         )
      );
      var3.setItem(
         12,
         item(
            Material.HOPPER,
            "§e§lServer Manager",
            List.of("§7Current: §f" + var7, "", "§eClick to cycle:", "§7Always-On → Crafty → Pterodactyl → Generic HTTP")
         )
      );
      var3.setItem(
         14,
         item(
            Material.NAME_TAG,
            "§b§lPanel / API URL",
            List.of("§7Non-secret setting.", "§eClick, then type the URL in chat.", "§7(Chat input is cancelled and only used for this prompt.)")
         )
      );
      var3.setItem(
         16,
         item(
            Material.PAPER,
            "§d§lAPI Key",
            List.of("§7Entered through a cancelled chat prompt.", "§7The message is cancelled and the key is never echoed.", "§eClick to enter/replace")
         )
      );
      var3.setItem(
         28,
         item(
            Material.ENDER_EYE,
            "§a§lTest Provider",
            List.of(
               "§7Configured: §f" + (var4 != null && var4.providerConfigured()),
               "§7Last test: §f" + (var4 == null ? "Not synced" : var4.providerTest()),
               "",
               "§eClick to test"
            )
         )
      );
      var3.setItem(
         30,
         item(
            Material.CLOCK,
            "§6§lSkip Automation",
            List.of("§7Use HubPilot without remote power control.", "§7Backends are treated as always-on.", "§eClick to choose")
         )
      );
      var3.setItem(32, item(Material.PLAYER_HEAD, "§b§lStaff Management", List.of("§7Owners, admins, moderators and helpers.", "§eClick to open")));
      var3.setItem(
         49,
         item(
            Material.EMERALD,
            "§a§lFinish Setup",
            List.of("§7Validates the minimum setup and unlocks", "§7the rest of HubPilot's commands.", "§eClick to finish")
         )
      );
      var1.openInventory(var3);
   }

   public static void openStaff(Player var0) {
      AdminNavigation.markStaffAdmin(var0);
      PublicHubBootstrap var1 = INSTANCE;
      if (var1 != null) {
         var1.openStaff0(var0);
      }
   }

   private void openStaff0(Player var1) {
      PublicHubBootstrap.StaffHolder var2 = new PublicHubBootstrap.StaffHolder();
      Inventory var3 = Bukkit.createInventory(var2, 54, "§8§lHubPilot Staff");
      var2.inventory = var3;
      fill(var3);
      int var4 = 9;

      for (Player var6 : Bukkit.getOnlinePlayers()) {
         if (var4 >= 45) {
            break;
         }

         while (var4 % 9 == 8 || var4 % 9 == 0) {
            var4++;
         }

         PublicHubBootstrap.State var7 = this.states.get(var6.getUniqueId());
         String var8 = var7 == null ? "player" : var7.role();
         var2.targets.put(var4, var6.getUniqueId());
         var2.names.put(var6.getUniqueId(), var6.getName());
         ItemStack var9 = item(
            Material.PLAYER_HEAD,
            "§f§l" + var6.getName(),
            List.of(
               "§7Role: §e" + var8,
               "",
               "§eLeft-click §7promote | §eRight-click §7demote",
               "§eShift-click §7to remove staff assignment",
               "§8Owners are protected"
            )
         );

         try {
            if (var9.getItemMeta() instanceof SkullMeta var11) {
               var11.setOwningPlayer(var6);
               var9.setItemMeta(var11);
            }
         } catch (Throwable var12) {
         }

         var3.setItem(var4++, var9);
      }

      var3.setItem(49, item(Material.BARRIER, "§cBack", List.of("§7Return to HubPilot Admin")));
      var1.openInventory(var3);
   }

   public static void enhanceAdminHome(HubPilotHubPlugin var0, Inventory var1) {
      if (var1 != null && var1.getSize() >= 45) {
         var1.setItem(36, item(Material.PLAYER_HEAD, "§b§lStaff Management", List.of("§7Owners, admins, moderators and helpers", "§eClick to open")));
         var1.setItem(38, item(Material.COMMAND_BLOCK, "§a§lSetup & Providers", List.of("§7Ownership, provider connection and setup state", "§eClick to open")));
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void click(InventoryClickEvent var1) {
      Inventory var2 = var1.getInventory();
      InventoryHolder var3 = var2 == null ? null : var2.getHolder();
      if (var3 instanceof PublicHubBootstrap.SetupHolder) {
         var1.setCancelled(true);
         if (var1.getWhoClicked() instanceof Player var8) {
            Inventory var12 = var1.getClickedInventory();
            if (var12 == var12) {
               this.setupClick(var8, var1.getRawSlot());
               return;
            }
         }
      } else if (var3 instanceof PublicHubBootstrap.StaffHolder var7) {
         var1.setCancelled(true);
         if (var1.getWhoClicked() instanceof Player var9) {
            Inventory var11 = var1.getClickedInventory();
            if (var11 == var11) {
               this.staffClick(var9, var7, var1.getRawSlot(), var1.isShiftClick(), var1.isRightClick());
               return;
            }
         }
      } else {
         if (var3 instanceof AdminHomeHolder) {
            Inventory var10000 = var1.getClickedInventory();
            if (var10000 == var10000 && var1.getWhoClicked() instanceof Player var4) {
               if (var1.getRawSlot() == -100) {
                  var1.setCancelled(true);
                  this.openStaff0(var4);
               } else if (var1.getRawSlot() == -101) {
                  var1.setCancelled(true);
                  this.openSetup0(var4);
               }
            }
         }
      }
   }

   private void setupClick(Player var1, int var2) {
      PublicHubBootstrap.State var3 = this.states.get(var1.getUniqueId());
      if (var3 == null) {
         var1.sendMessage("§eWaiting for HubPilot Core authorization sync. Try again in a second.");
      } else if (!"owner".equals(var3.role())) {
         var1.sendMessage("§cOnly a HubPilot owner can change first-time setup/provider credentials.");
      } else {
         switch (var2) {
            case 12:
               int var4 = PROVIDER_TYPES.indexOf(var3.providerType());
               if (var4 < 0) {
                  var4 = 0;
               }

               String var5 = PROVIDER_TYPES.get((var4 + 1) % PROVIDER_TYPES.size());
               this.action(var1, "PROVIDER_TYPE", var5);
               var1.sendMessage("§eProvider selected: §f" + var5);
               Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.openSetup0(var1.getPlayer()), 10L);
               break;
            case 14:
               this.urlPrompts.put(var1.getUniqueId(), "url");
               var1.closeInventory();
               var1.sendMessage("§eType the provider base URL in chat, or §ccancel§e. This value is not a secret.");
               break;
            case 16:
               this.openSecret(var1);
               break;
            case 28:
               this.action(var1, "PROVIDER_TEST");
               var1.sendMessage("§eTesting provider connection...");
               Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.openSetup0(var1), 20L);
               break;
            case 30:
               this.action(var1, "PROVIDER_SKIP");
               var1.sendMessage("§aProvider automation skipped; using Always-On mode.");
               Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.openSetup0(var1.getPlayer()), 10L);
               break;
            case 32:
               AdminNavigation.openStaffFromSetup(this, var1);
               break;
            case 49:
               this.action(var1, "SETUP_COMPLETE");
               Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.openSetup0(var1), 15L);
         }
      }
   }

   private void staffClick(Player var1, PublicHubBootstrap.StaffHolder var2, int var3, boolean var4, boolean var5) {
      if (var3 == 49) {
         AdminNavigation.backFromStaff(this, var1);
      } else {
         UUID var6 = var2.targets.get(var3);
         if (var6 != null) {
            PublicHubBootstrap.State var7 = this.states.get(var1.getUniqueId());
            if (var7 != null && (var7.role().equals("owner") || var7.role().equals("admin"))) {
               PublicHubBootstrap.State var8 = this.states.get(var6);
               String var9 = var8 == null ? "player" : var8.role();
               if ("owner".equals(var9)) {
                  var1.sendMessage("§cOwners are managed with /hp owner and cannot be demoted here.");
               } else {
                  String var10 = var4
                     ? "none"
                     : (
                        var5
                           ? ("admin".equals(var9) ? "moderator" : ("moderator".equals(var9) ? "helper" : ("helper".equals(var9) ? "none" : "none")))
                           : ("helper".equals(var9) ? "moderator" : ("moderator".equals(var9) ? "admin" : ("admin".equals(var9) ? "admin" : "helper")))
                     );
                  if (!var7.role().equals("admin") || !var10.equals("admin") && !var9.equals("admin")) {
                     this.action(var1, "STAFF_SET", var6.toString(), var2.names.getOrDefault(var6, var6.toString()), var10);
                     var1.sendMessage("§eUpdating staff role...");
                     Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.openStaff0(var1), 15L);
                  } else {
                     var1.sendMessage("§cOnly an owner can grant or remove the Admin role.");
                  }
               }
            } else {
               var1.sendMessage("§cYou do not have permission to manage HubPilot staff.");
            }
         }
      }
   }

   @EventHandler
   public void chat(AsyncPlayerChatEvent var1) {
      UUID var2 = var1.getPlayer().getUniqueId();
      if (this.urlPrompts.containsKey(var2)) {
         var1.setCancelled(true);
         String var3 = var1.getMessage().trim();
         String var4 = this.urlPrompts.remove(var2);
         if ("secret".equals(var4)) {
            var3 = "HPSECRET:".concat(var3);
         }

         final String message = var3;
         Bukkit.getScheduler().runTask(this.plugin, () -> {
            if (message.startsWith("HPSECRET:")) {
               String var3x = message.substring(9).trim();
               if (var3x.equalsIgnoreCase("cancel")) {
                  var1.getPlayer().sendMessage("§7API key entry cancelled.");
                  this.openSetup0(var1.getPlayer());
               } else if (var3x.isBlank()) {
                  var1.getPlayer().sendMessage("§cAPI key cannot be empty.");
                  this.openSetup0(var1.getPlayer());
               } else if (var3x.length() > 4096) {
                  var1.getPlayer().sendMessage("§cAPI key is too long.");
                  this.openSetup0(var1.getPlayer());
               } else {
                  boolean var4x;
                  if (var3x.startsWith("!insecure ")) {
                     var4x = true;
                     var3x = var3x.substring(10).trim();
                  } else {
                     var4x = false;
                  }

                  if (var3x.isBlank()) {
                     var1.getPlayer().sendMessage("§cAPI key cannot be empty.");
                     this.openSetup0(var1.getPlayer());
                  } else {
                     this.action(var1.getPlayer(), "PROVIDER_SECRET", (var4x ? "HP3510|TLS=1|" : "HP3510|TLS=0|").concat(var3x));
                     var1.getPlayer().sendMessage("§7Saving provider credential...");
                     Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.openSetup0(var1.getPlayer()), 10L);
                  }
               }
            } else if (message.equalsIgnoreCase("cancel")) {
               var1.getPlayer().sendMessage("§7Provider URL change cancelled.");
               this.openSetup0(var1.getPlayer());
            } else if (!message.startsWith("http://") && !message.startsWith("https://")) {
               var1.getPlayer().sendMessage("§cURL must start with http:// or https://");
               this.openSetup0(var1.getPlayer());
            } else {
               this.action(var1.getPlayer(), "PROVIDER_URL", message);
               this.openSetup0(var1.getPlayer());
            }
         });
      }
   }

   private void openSecret(Player var1) {
      this.urlPrompts.put(var1.getUniqueId(), "secret");
      var1.closeInventory();
      var1.sendMessage(
         "§ePaste the provider API key in chat, or type §ccancel§e. The chat event is cancelled and the key is never echoed. For a local/self-signed HTTPS controller, prefix the key with §f!insecure §e."
      );
   }

   private static void fill(Inventory var0) {
      ItemStack var1 = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());

      for (int var2 = 0; var2 < var0.getSize(); var2++) {
         var0.setItem(var2, var1);
      }
   }

   private static ItemStack item(Material var0, String var1, List<String> var2) {
      ItemStack message = new ItemStack(var0);

      try {
         ItemMeta var4 = message.getItemMeta();
         if (var4 != null) {
            var4.setDisplayName(var1);
            var4.setLore(var2);
            message.setItemMeta(var4);
         }
      } catch (Throwable var5) {
      }

      return message;
   }

   static final class SetupHolder implements InventoryHolder {
      Inventory inventory;

      public Inventory getInventory() {
         return this.inventory;
      }
   }

   static final class StaffHolder implements InventoryHolder {
      Inventory inventory;
      final Map<Integer, UUID> targets = new HashMap<>();
      final Map<UUID, String> names = new HashMap<>();

      public Inventory getInventory() {
         return this.inventory;
      }
   }

   public record State(String setup, String role, String providerType, boolean providerConfigured, String providerTest, long updatedAt) {
   }
}
