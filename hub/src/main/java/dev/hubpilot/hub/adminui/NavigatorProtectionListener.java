package dev.hubpilot.hub.adminui;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.publicapi.AdminItemManager;
import dev.hubpilot.hub.util.MenuItems;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

public final class NavigatorProtectionListener implements Listener {
   private final HubPilotHubPlugin plugin;
   private final AdminSettingsStore s;
   private final NavigatorManager nav;
   private final Set<UUID> restoreAfterDeath = Collections.synchronizedSet(new HashSet<>());

   public NavigatorProtectionListener(HubPilotHubPlugin var1) {
      this.plugin = var1;
      this.s = HubPilotAdminRuntime.settings(var1);
      this.nav = HubPilotAdminRuntime.navigator(var1);
   }

   @EventHandler
   public void join(PlayerJoinEvent var1) {
      Bukkit.getScheduler().runTask(this.plugin, () -> {
         this.nav.dedupe(var1.getPlayer());
         if (this.s.getBoolean("navigator.force-on-join", false)) {
            this.nav.ensure(var1.getPlayer(), false);
         }

         AdminItemManager.ensure(this.plugin, var1.getPlayer());
      });
   }

   @EventHandler
   public void world(PlayerChangedWorldEvent var1) {
      Bukkit.getScheduler()
         .runTask(
            this.plugin,
            () -> {
               this.nav.dedupe(var1.getPlayer());
               if (this.s.getBoolean("navigator.force-on-join", false)) {
                  if (this.s.getBoolean("navigator.hub-world-only", false)
                     && !var1.getPlayer().getWorld().getName().equals(this.s.get("navigator.hub-world", "world"))) {
                     this.nav.removeNavigator(var1.getPlayer());
                  } else {
                     this.nav.ensure(var1.getPlayer(), false);
                  }
               }
            }
         );
   }

   @EventHandler
   public void drop(PlayerDropItemEvent var1) {
      if (this.nav.isNavigator(var1.getItemDrop().getItemStack()) && this.s.getBoolean("navigator.prevent-drop", true)) {
         var1.setCancelled(true);
      } else if (MenuItems.isAdminOpener(this.plugin, var1.getItemDrop().getItemStack())) {
         var1.setCancelled(true);
      }
   }

   @EventHandler
   public void pickup(EntityPickupItemEvent var1) {
      if (var1.getEntity() instanceof Player var2) {
         if (this.nav.isNavigator(var1.getItem().getItemStack())) {
            if (this.nav.hasNavigator(var2)) {
               var1.setCancelled(true);
               var1.getItem().remove();
            } else {
               Bukkit.getScheduler().runTask(this.plugin, () -> this.nav.dedupe(var2));
            }
         }
      }
   }

   @EventHandler
   public void swap(PlayerSwapHandItemsEvent var1) {
      if (this.s.getBoolean("navigator.prevent-moving", true) || this.s.getBoolean("navigator.lock-to-slot", true)) {
         if (this.nav.isNavigator(var1.getMainHandItem()) || this.nav.isNavigator(var1.getOffHandItem())) {
            var1.setCancelled(true);
         }
      }
   }

   @EventHandler
   public void click(InventoryClickEvent var1) {
      if (var1.getWhoClicked() instanceof Player var2) {
         ItemStack var8 = var1.getCurrentItem();
         ItemStack var4 = var1.getCursor();
         boolean var5 = this.s.getBoolean("navigator.prevent-moving", true) || this.s.getBoolean("navigator.lock-to-slot", true);
         boolean var6 = this.s.getBoolean("navigator.prevent-containers", true);
         if (!var5 || !this.nav.isNavigator(var8) && !this.nav.isNavigator(var4)) {
            int var7 = var1.getHotbarButton();
            if (var7 < 0 || !this.nav.isNavigator(var2.getInventory().getItem(var7)) || !var5 && !var6) {
               if (this.nav.isNavigator(var4) && var6 && var1.getClickedInventory() != null && var1.getClickedInventory().getType() != InventoryType.PLAYER) {
                  var1.setCancelled(true);
                  Bukkit.getScheduler().runTask(this.plugin, () -> this.nav.dedupe(var2));
               }
            } else {
               var1.setCancelled(true);
               Bukkit.getScheduler().runTask(this.plugin, () -> this.nav.dedupe(var2));
            }
         } else {
            var1.setCancelled(true);
            Bukkit.getScheduler().runTask(this.plugin, () -> this.nav.dedupe(var2));
         }
      }
   }

   @EventHandler
   public void drag(InventoryDragEvent var1) {
      if (var1.getWhoClicked() instanceof Player var2) {
         if ((this.s.getBoolean("navigator.prevent-moving", true) || this.s.getBoolean("navigator.lock-to-slot", true))
            && this.nav.isNavigator(var1.getOldCursor())) {
            var1.setCancelled(true);
            Bukkit.getScheduler().runTask(this.plugin, () -> this.nav.dedupe(var2));
         }
      }
   }

   @EventHandler
   public void death(PlayerDeathEvent var1) {
      if (this.s.getBoolean("navigator.restore-after-death", true)) {
         boolean var2 = var1.getDrops().removeIf(this.nav::isNavigator);
         if (var2) {
            this.restoreAfterDeath.add(var1.getEntity().getUniqueId());
         }
      }
   }

   @EventHandler
   public void respawn(PlayerRespawnEvent var1) {
      Bukkit.getScheduler().runTask(this.plugin, () -> {
         this.nav.dedupe(var1.getPlayer());
         if (this.s.getBoolean("navigator.restore-after-death", true) && this.restoreAfterDeath.remove(var1.getPlayer().getUniqueId())) {
            this.nav.ensure(var1.getPlayer(), true);
         } else if (this.s.getBoolean("navigator.force-on-join", false)) {
            this.nav.ensure(var1.getPlayer(), false);
         }
      });
   }

   public void periodicRepair() {
      for (Player var2 : Bukkit.getOnlinePlayers()) {
         this.nav.dedupe(var2);
         if (this.s.getBoolean("navigator.restore-if-missing", true)) {
            this.nav.ensure(var2, false);
         }
      }
   }
}
