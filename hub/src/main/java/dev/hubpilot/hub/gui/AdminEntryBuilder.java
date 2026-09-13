package dev.hubpilot.hub.gui;

import dev.hubpilot.hub.bridge.HubPilotMenuBuilder;
import dev.hubpilot.hub.config.Destination;
import dev.hubpilot.hub.publicapi.ServerControlUi;
import dev.hubpilot.hub.util.IconMaterials;
import dev.hubpilot.hub.util.MenuItems;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

public final class AdminEntryBuilder {
   private AdminEntryBuilder() {
   }

   public static Inventory build(Destination var0) {
      AdminEntryHolder var1 = new AdminEntryHolder(var0.id());
      Inventory var2 = Bukkit.createInventory(var1, 36, MenuItems.colorize("&8Edit: " + var0.label()));
      var1.setInventory(var2);
      GuiCommon.fill(var2, Material.BLACK_STAINED_GLASS_PANE);
      var2.setItem(10, MenuItems.named(Material.NAME_TAG, "&e&lDisplay Name", List.of("&7" + var0.label(), "&eClick to type")));
      var2.setItem(11, MenuItems.named(Material.WRITABLE_BOOK, "&e&lDescription", List.of("&7" + var0.description(), "&eClick to type")));
      var2.setItem(12, MenuItems.named(Material.ANVIL, "&e&lSoftware / Version", List.of("&7" + var0.software(), "&eClick to type")));
      var2.setItem(
         13,
         MenuItems.named(
            var0.targetType() == Destination.TargetType.SERVER ? Material.ENDER_PEARL : Material.COMPASS,
            "&e&lDestination Type",
            List.of("&7" + var0.targetType(), "&eClick to toggle")
         )
      );
      var2.setItem(14, MenuItems.named(Material.ENDER_EYE, "&e&lVelocity Target", List.of("&7" + var0.target(), "&eClick to type")));
      var2.setItem(
         15, MenuItems.named(Material.CLOCK, "&e&lStatus Target", List.of("&7" + var0.statusTarget(), "&7Usually matches Velocity target", "&eClick to type"))
      );
      var2.setItem(
         16,
         MenuItems.named(
            var0.enabled() ? Material.LIME_DYE : Material.GRAY_DYE, "&e&lVisible", List.of(var0.enabled() ? "&aEnabled" : "&cHidden", "&eClick to toggle")
         )
      );
      var2.setItem(
         17,
         MenuItems.named(
            IconMaterials.resolve(var0.iconMaterial()),
            "&e&lServer Icon",
            List.of("&7" + var0.iconMaterial(), "&aClick to browse icons", "&bRight-click: use off-hand item", "&eShift-click: type custom")
         )
      );
      var2.setItem(20, MenuItems.named(Material.SPYGLASS, "&a&lServer Card / Telemetry", List.of("&7Choose which status lines appear", "&eClick to configure")));
      var2.setItem(21, HubPilotMenuBuilder.automationButton(null, var0.id()));
      var2.setItem(22, HubPilotMenuBuilder.cloneButton());
      var2.setItem(23, MenuItems.named(Material.HOPPER, "&b&lNavigator Slot", List.of("&7Place this server in any Navigator slot", "&eClick to choose a slot")));
      var2.setItem(27, MenuItems.named(Material.ARROW, "&e&lBack", List.of("&7Return to destination list")));
      var2.setItem(31, MenuItems.named(Material.TNT, "&c&lDelete Destination", List.of("&cShift-click to permanently delete")));
      ServerControlUi.decorate(var2);
      return var2;
   }
}
