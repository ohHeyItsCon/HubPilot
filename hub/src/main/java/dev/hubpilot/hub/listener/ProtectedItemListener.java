package dev.hubpilot.hub.listener;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.gui.AdminEntryHolder;
import dev.hubpilot.hub.gui.AdminMenuHolder;
import dev.hubpilot.hub.gui.DestinationMenuHolder;
import dev.hubpilot.hub.publicapi.SetupItemManager;
import dev.hubpilot.hub.util.MenuItems;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class ProtectedItemListener implements Listener {
   private final HubPilotHubPlugin plugin;
   private final Map<UUID, ProtectedItemListener.Pending> pending = new ConcurrentHashMap<>();

   public ProtectedItemListener(HubPilotHubPlugin var1) {
      this.plugin = var1;
   }

   @EventHandler
   public void onDrop(PlayerDropItemEvent var1) {
      if (SetupItemManager.isProtected(this.plugin, var1.getItemDrop().getItemStack())) {
         var1.setCancelled(true);
      }
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent var1) {
      InventoryHolder var2 = var1.getInventory().getHolder();
      if (!(var2 instanceof DestinationMenuHolder) && !(var2 instanceof AdminMenuHolder) && !(var2 instanceof AdminEntryHolder)) {
         if (var1.getWhoClicked() instanceof Player var3) {
            Inventory var8 = var1.getClickedInventory();
            if (var8 != null) {
               ItemStack var5 = var1.getCurrentItem();
               ItemStack var6 = var1.getCursor();
               if (var1.getClick().isShiftClick() && var8.getType() == InventoryType.PLAYER && SetupItemManager.isProtected(this.plugin, var5)) {
                  var1.setCancelled(true);
               } else if (var8.getType() != InventoryType.PLAYER && SetupItemManager.isProtected(this.plugin, var6)) {
                  var1.setCancelled(true);
               } else {
                  if (var1.getHotbarButton() >= 0 && var8.getType() != InventoryType.PLAYER) {
                     ItemStack var7 = var3.getInventory().getItem(var1.getHotbarButton());
                     if (SetupItemManager.isProtected(this.plugin, var7)) {
                        var1.setCancelled(true);
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onHopperMove(InventoryMoveItemEvent var1) {
      if (SetupItemManager.isProtected(this.plugin, var1.getItem())) {
         var1.setCancelled(true);
      }
   }

   @EventHandler
   public void onItemFramePlace(PlayerInteractEntityEvent var1) {
      if (var1.getHand() == EquipmentSlot.HAND && var1.getRightClicked() instanceof ItemFrame) {
         if (SetupItemManager.isProtected(this.plugin, var1.getPlayer().getInventory().getItemInMainHand())) {
            var1.setCancelled(true);
         }
      }
   }

   @EventHandler
   public void onDeath(PlayerDeathEvent var1) {
      boolean var2 = var1.getDrops().removeIf(var1x -> MenuItems.isMenuOpener(this.plugin, var1x));
      boolean var3 = var1.getDrops().removeIf(var1x -> SetupItemManager.isAdminOrSetup(this.plugin, var1x));
      if (var2 || var3) {
         this.pending.put(var1.getEntity().getUniqueId(), new ProtectedItemListener.Pending(var2, var3));
      }
   }

   @EventHandler
   public void onRespawn(PlayerRespawnEvent var1) {
      ProtectedItemListener.Pending var2 = this.pending.remove(var1.getPlayer().getUniqueId());
      if (var2 != null) {
         Bukkit.getScheduler().runTask(this.plugin, () -> SetupItemManager.afterRespawn(this.plugin, var1.getPlayer(), var2.menu(), var2.admin()));
      }
   }

   private record Pending(boolean menu, boolean admin) {
   }
}
