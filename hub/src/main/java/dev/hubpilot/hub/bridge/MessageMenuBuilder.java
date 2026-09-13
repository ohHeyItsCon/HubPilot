package dev.hubpilot.hub.bridge;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.config.Destination;
import dev.hubpilot.hub.gui.GuiCommon;
import dev.hubpilot.hub.util.MenuItems;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

public final class MessageMenuBuilder {
   private static final int[] SLOTS = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

   private MessageMenuBuilder() {
   }

   public static Inventory build(HubPilotHubPlugin var0, String var1, int var2) {
      MessageEvent[] var3 = MessageEvent.values();
      int var4 = Math.max(1, (var3.length + SLOTS.length - 1) / SLOTS.length);
      int var5 = Math.max(0, Math.min(var2, var4 - 1));
      String var6 = "Global";
      if (var1 != null) {
         Destination var7 = var0.getDestinationStore().find(var1);
         var6 = var7 == null ? var1 : var7.label();
      }

      MessageMenuHolder var16 = new MessageMenuHolder(var1, var5);
      Inventory var8 = Bukkit.createInventory(var16, 54, MenuItems.colorize("&8Messages: &f" + var6));
      var16.setInventory(var8);
      GuiCommon.fill(var8, Material.BLACK_STAINED_GLASS_PANE);
      HubPilotStore var9 = var0.getHubPilotStore();
      int var10 = var5 * SLOTS.length;

      for (int var11 = 0; var11 < SLOTS.length && var10 + var11 < var3.length; var11++) {
         MessageEvent var12 = var3[var10 + var11];
         boolean var13 = var9.messageEnabled(var1, var12);
         String var14 = var9.messageText(var1, var12);
         ArrayList var15 = new ArrayList();
         var15.add(var13 ? "&aShown" : "&cHidden");
         var15.add(var1 == null ? "&7Global message" : (var9.hasMessageOverride(var1, var12) ? "&dServer override" : "&7Inherited global message"));
         var15.add("");
         var15.add("&f" + var14);
         var15.add("");
         var15.add("&eLeft-click: show / hide");
         var15.add("&bRight-click: edit text");
         var15.add("&dMiddle-click: preview");
         var15.add("&cShift-right: reset / inherit");
         var8.setItem(SLOTS[var11], MenuItems.named(var13 ? Material.LIME_DYE : Material.GRAY_DYE, "&f&l" + var12.label(), var15));
      }

      var8.setItem(45, MenuItems.named(Material.ARROW, "&e&lBack", List.of("&7Return to automation settings")));
      if (var5 > 0) {
         var8.setItem(48, MenuItems.named(Material.ARROW, "&ePrevious Page", List.of()));
      }

      var8.setItem(
         49,
         MenuItems.named(
            Material.NAME_TAG,
            "&6&lJoin & Queue Messages",
            List.of("&7Use & color codes", "&7{server} uses the Navigator name style", "&7Only known lifecycle events can be edited")
         )
      );
      if (var5 + 1 < var4) {
         var8.setItem(50, MenuItems.named(Material.ARROW, "&eNext Page", List.of()));
      }

      return var8;
   }

   static MessageEvent eventAt(int var0, int var1) {
      int var2 = -1;

      for (int var3 = 0; var3 < SLOTS.length; var3++) {
         if (SLOTS[var3] == var1) {
            var2 = var3;
         }
      }

      int var4 = var0 * SLOTS.length + var2;
      return var2 >= 0 && var4 < MessageEvent.values().length ? MessageEvent.values()[var4] : null;
   }
}
