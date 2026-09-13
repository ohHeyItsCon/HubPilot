package dev.hubpilot.hub.publicapi;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.util.MenuItems;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class AdminItemManager {
    private AdminItemManager() {}

    public static void ensure(HubPilotHubPlugin plugin, Player player) {
        SetupItemManager.ensureAdmin(plugin, player);
    }

    public static void toggle(HubPilotHubPlugin plugin, Player player) {
        if (!PublicHubBootstrap.canAdmin(player)) {
            player.sendMessage("§cYou do not have permission to use the admin item."); return;
        }
        NamespacedKey hidden = new NamespacedKey(plugin, "admin_item_hidden");
        boolean present = MenuItems.isAdminOpener(plugin, player.getItemOnCursor());
        for (ItemStack item : player.getInventory().getContents()) present |= MenuItems.isAdminOpener(plugin, item);
        if (present) {
            player.getPersistentDataContainer().set(hidden, PersistentDataType.BYTE, (byte)1);
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                if (MenuItems.isAdminOpener(plugin, player.getInventory().getItem(i))) player.getInventory().setItem(i, null);
            }
            if (MenuItems.isAdminOpener(plugin, player.getItemOnCursor())) player.setItemOnCursor(null);
            player.sendMessage("§aHub admin item hidden. Use /hp adminitem to bring it back.");
        } else {
            if (player.getInventory().firstEmpty() < 0) {
                player.sendMessage("§eMake one inventory slot available first."); return;
            }
            if (player.getInventory().addItem(MenuItems.createAdminOpener(plugin, plugin.getMenuConfig())).isEmpty()) {
                player.getPersistentDataContainer().remove(hidden);
                player.sendMessage("§aHub admin item shown.");
            }
        }
    }
}
