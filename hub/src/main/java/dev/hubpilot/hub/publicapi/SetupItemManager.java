/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 */
package dev.hubpilot.hub.publicapi;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.adminui.HubPilotAdminRuntime;
import dev.hubpilot.hub.adminui.NavigatorManager;
import dev.hubpilot.hub.publicapi.PublicHubBootstrap;
import dev.hubpilot.hub.util.MenuItems;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class SetupItemManager {
    private static final String KEY = "setup_opener";
    private static final Map<UUID, Boolean> NAV_PENDING = new ConcurrentHashMap<UUID, Boolean>();

    private SetupItemManager() {
    }

    public static void attach(HubPilotHubPlugin hubPilotHubPlugin) {
        Bukkit.getPluginManager().registerEvents((Listener)new SetupInteractListener(hubPilotHubPlugin), (Plugin)hubPilotHubPlugin);
    }

    public static void guardNavigator(NavigatorManager navigatorManager, Player player, boolean bl3) {
        PublicHubBootstrap.State state = PublicHubBootstrap.state(player.getUniqueId());
        if (state == null) {
            NAV_PENDING.merge(player.getUniqueId(), bl3, (bl, bl2) -> bl != false || bl2 != false);
            navigatorManager.removeNavigator(player);
            return;
        }
        if (!"READY".equals(state.setup())) {
            NAV_PENDING.remove(player.getUniqueId());
            navigatorManager.removeNavigator(player);
            return;
        }
        NAV_PENDING.remove(player.getUniqueId());
        navigatorManager.ensureReady(player, bl3);
    }

    public static void reconcile(HubPilotHubPlugin hubPilotHubPlugin, Player player) {
        PublicHubBootstrap.State state = PublicHubBootstrap.state(player.getUniqueId());
        NavigatorManager navigatorManager = HubPilotAdminRuntime.navigator(hubPilotHubPlugin);
        if (state == null) {
            SetupItemManager.removeAdmin(hubPilotHubPlugin, player);
            navigatorManager.removeNavigator(player);
            return;
        }
        if (!"READY".equals(state.setup())) {
            NAV_PENDING.remove(player.getUniqueId());
            SetupItemManager.removeAdmin(hubPilotHubPlugin, player);
            navigatorManager.removeNavigator(player);
            if (PublicHubBootstrap.canAdmin(player)) {
                SetupItemManager.ensureSetup(hubPilotHubPlugin, player);
            } else {
                SetupItemManager.removeSetup(hubPilotHubPlugin, player);
            }
            return;
        }
        boolean bl = SetupItemManager.hasSetup(hubPilotHubPlugin, player);
        Boolean bl2 = NAV_PENDING.remove(player.getUniqueId());
        SetupItemManager.removeSetup(hubPilotHubPlugin, player);
        if (bl || bl2 != null) {
            navigatorManager.ensureReady(player, bl2 != null && bl2 != false);
        }
        if (PublicHubBootstrap.canAdmin(player)) {
            SetupItemManager.ensureAdminReady(hubPilotHubPlugin, player);
        } else {
            SetupItemManager.removeAdmin(hubPilotHubPlugin, player);
        }
    }

    public static void ensureAdmin(HubPilotHubPlugin hubPilotHubPlugin, Player player) {
        SetupItemManager.reconcile(hubPilotHubPlugin, player);
    }

    public static void afterRespawn(HubPilotHubPlugin hubPilotHubPlugin, Player player, boolean bl, boolean bl2) {
        PublicHubBootstrap.State state = PublicHubBootstrap.state(player.getUniqueId());
        if (state == null || !"READY".equals(state.setup())) {
            SetupItemManager.reconcile(hubPilotHubPlugin, player);
            return;
        }
        NavigatorManager navigatorManager = HubPilotAdminRuntime.navigator(hubPilotHubPlugin);
        if (bl) {
            navigatorManager.ensureReady(player, true);
        }
        if (bl2 || PublicHubBootstrap.canAdmin(player)) {
            SetupItemManager.ensureAdminReady(hubPilotHubPlugin, player);
        }
        SetupItemManager.removeSetup(hubPilotHubPlugin, player);
    }

    public static boolean isProtected(HubPilotHubPlugin hubPilotHubPlugin, ItemStack itemStack) {
        return MenuItems.isMenuOpener(hubPilotHubPlugin, itemStack) || MenuItems.isAdminOpener(hubPilotHubPlugin, itemStack) || SetupItemManager.isSetup(hubPilotHubPlugin, itemStack);
    }

    public static boolean isAdminOrSetup(HubPilotHubPlugin hubPilotHubPlugin, ItemStack itemStack) {
        return MenuItems.isAdminOpener(hubPilotHubPlugin, itemStack) || SetupItemManager.isSetup(hubPilotHubPlugin, itemStack);
    }

    public static boolean isSetup(HubPilotHubPlugin hubPilotHubPlugin, ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.CLOCK || !itemStack.hasItemMeta()) {
            return false;
        }
        Byte by = (Byte)itemStack.getItemMeta().getPersistentDataContainer().get(new NamespacedKey((Plugin)hubPilotHubPlugin, KEY), PersistentDataType.BYTE);
        return by != null && by == 1;
    }

    private static ItemStack create(HubPilotHubPlugin hubPilotHubPlugin) {
        ItemStack itemStack = MenuItems.named(Material.CLOCK, "&e&lHubPilot Setup", List.of("&7HubPilot still needs to be configured.", "&eRight-click to continue setup."));
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.getPersistentDataContainer().set(new NamespacedKey((Plugin)hubPilotHubPlugin, KEY), PersistentDataType.BYTE, (byte)1);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private static void ensureSetup(HubPilotHubPlugin hubPilotHubPlugin, Player player) {
        PlayerInventory playerInventory = player.getInventory();
        int n = -1;
        for (int i = 0; i < playerInventory.getSize(); ++i) {
            if (!SetupItemManager.isSetup(hubPilotHubPlugin, playerInventory.getItem(i))) continue;
            if (n < 0) {
                n = i;
                continue;
            }
            playerInventory.setItem(i, null);
        }
        if (n < 0) {
            playerInventory.addItem(new ItemStack[]{SetupItemManager.create(hubPilotHubPlugin)});
        }
    }

    private static boolean hasSetup(HubPilotHubPlugin hubPilotHubPlugin, Player player) {
        PlayerInventory playerInventory = player.getInventory();
        for (int i = 0; i < playerInventory.getSize(); ++i) {
            if (!SetupItemManager.isSetup(hubPilotHubPlugin, playerInventory.getItem(i))) continue;
            return true;
        }
        return false;
    }

    private static void removeSetup(HubPilotHubPlugin hubPilotHubPlugin, Player player) {
        PlayerInventory playerInventory = player.getInventory();
        for (int i = 0; i < playerInventory.getSize(); ++i) {
            if (!SetupItemManager.isSetup(hubPilotHubPlugin, playerInventory.getItem(i))) continue;
            playerInventory.setItem(i, null);
        }
    }

    private static void removeAdmin(HubPilotHubPlugin hubPilotHubPlugin, Player player) {
        PlayerInventory playerInventory = player.getInventory();
        for (int i = 0; i < playerInventory.getSize(); ++i) {
            if (!MenuItems.isAdminOpener(hubPilotHubPlugin, playerInventory.getItem(i))) continue;
            playerInventory.setItem(i, null);
        }
    }

    private static void ensureAdminReady(HubPilotHubPlugin hubPilotHubPlugin, Player player) {
        if (player.getPersistentDataContainer().has(new NamespacedKey(hubPilotHubPlugin, "admin_item_hidden"), PersistentDataType.BYTE)) {
            removeAdmin(hubPilotHubPlugin, player);
            return;
        }
        PlayerInventory playerInventory = player.getInventory();
        int n = -1;
        for (int i = 0; i < playerInventory.getSize(); ++i) {
            if (!MenuItems.isAdminOpener(hubPilotHubPlugin, playerInventory.getItem(i))) continue;
            if (n < 0) {
                n = i;
                continue;
            }
            playerInventory.setItem(i, null);
        }
        if (n < 0) {
            playerInventory.addItem(new ItemStack[]{MenuItems.createAdminOpener(hubPilotHubPlugin, hubPilotHubPlugin.getMenuConfig())});
        }
    }

    private static final class SetupInteractListener
    implements Listener {
        private final HubPilotHubPlugin plugin;

        private SetupInteractListener(HubPilotHubPlugin hubPilotHubPlugin) {
            this.plugin = hubPilotHubPlugin;
        }

        @EventHandler
        public void interact(PlayerInteractEvent playerInteractEvent) {
            Action action = playerInteractEvent.getAction();
            if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
                return;
            }
            if (!SetupItemManager.isSetup(this.plugin, playerInteractEvent.getItem())) {
                return;
            }
            playerInteractEvent.setCancelled(true);
            PublicHubBootstrap.openSetup(playerInteractEvent.getPlayer());
        }
    }
}
