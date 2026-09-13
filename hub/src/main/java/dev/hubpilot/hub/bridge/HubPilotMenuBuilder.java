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
import org.bukkit.inventory.ItemStack;

public final class HubPilotMenuBuilder {
   private HubPilotMenuBuilder() {
   }

   public static Inventory buildServer(HubPilotHubPlugin var0, Destination var1) {
      HubPilotMenuHolder var2 = new HubPilotMenuHolder(var1.id(), false);
      Inventory var3 = Bukkit.createInventory(var2, 54, MenuItems.colorize("&8Automation: &f" + var1.label()));
      var2.setInventory(var3);
      populate(var0, var3, var1.id(), false);
      return var3;
   }

   public static Inventory buildGlobal(HubPilotHubPlugin var0) {
      HubPilotMenuHolder var1 = new HubPilotMenuHolder(null, true);
      Inventory var2 = Bukkit.createInventory(var1, 54, MenuItems.colorize("&8HubPilot Global Defaults"));
      var1.setInventory(var2);
      populate(var0, var2, null, true);
      return var2;
   }

   private static void populate(HubPilotHubPlugin var0, Inventory var1, String var2, boolean var3) {
      GuiCommon.fill(var1, Material.BLACK_STAINED_GLASS_PANE);
      HubPilotStore var4 = var0.getHubPilotStore();
      put(var1, 10, Material.CLOCK, var4, var2, var3, SettingKey.COUNTDOWN_SECONDS, List.of("&eLeft/right-click to cycle", "&bShift-click for a custom value"));
      put(
         var1,
         11,
         Material.NOTE_BLOCK,
         var4,
         var2,
         var3,
         SettingKey.COUNTDOWN_SOUND,
         List.of("&eLeft/right-click to cycle sounds", "&bShift-click to type a sound")
      );
      put(var1, 12, Material.JUKEBOX, var4, var2, var3, SettingKey.SOUND_VOLUME, List.of("&eLeft/right-click to cycle", "&bShift-click for a custom value"));
      put(var1, 13, Material.REPEATER, var4, var2, var3, SettingKey.PITCH_STYLE, List.of("&eClick to cycle rising / flat / falling / none"));
      put(var1, 14, Material.WRITABLE_BOOK, var4, var2, var3, SettingKey.COUNTDOWN_MESSAGE, List.of("&eClick to type", "&7Use {server} and {seconds}"));
      put(var1, 15, Material.KNOWLEDGE_BOOK, var4, var2, var3, SettingKey.REQUIRED_VERSION, List.of("&eClick to type", "&7Use any to disable strict matching"));
      put(
         var1,
         19,
         Material.RESPAWN_ANCHOR,
         var4,
         var2,
         var3,
         SettingKey.STARTUP_TIMEOUT_SECONDS,
         List.of("&eLeft/right-click to cycle", "&bShift-click for a custom value")
      );
      put(var1, 20, Material.TARGET, var4, var2, var3, SettingKey.RETRY_COUNT, List.of("&eLeft/right-click to cycle", "&bShift-click for a custom value"));
      put(
         var1,
         21,
         Material.REDSTONE_TORCH,
         var4,
         var2,
         var3,
         SettingKey.RETRY_DELAY_SECONDS,
         List.of("&eLeft/right-click to cycle", "&bShift-click for a custom value")
      );
      put(
         var1,
         22,
         boolMaterial(value(var4, var2, var3, SettingKey.STOP_AFTER_FAILURE)),
         var4,
         var2,
         var3,
         SettingKey.STOP_AFTER_FAILURE,
         List.of("&eClick to toggle")
      );
      put(
         var1,
         23,
         Material.DAYLIGHT_DETECTOR,
         var4,
         var2,
         var3,
         SettingKey.IDLE_SHUTDOWN_MINUTES,
         List.of("&eLeft/right-click to cycle", "&bShift-click for a custom value", "&70 disables idle shutdown")
      );
      put(
         var1,
         24,
         boolMaterial(value(var4, var2, var3, SettingKey.AUTOSTART_ENABLED)),
         var4,
         var2,
         var3,
         SettingKey.AUTOSTART_ENABLED,
         List.of("&eClick to toggle")
      );
      put(
         var1,
         25,
         boolMaterial(value(var4, var2, var3, SettingKey.ALWAYS_ON_SERVER)),
         var4,
         var2,
         var3,
         SettingKey.ALWAYS_ON_SERVER,
         List.of("&eClick to toggle", "&7Disables automatic HubPilot shutdown", "&7Provider startup still works")
      );
      var1.setItem(45, MenuItems.named(Material.ARROW, "&e&lBack", List.of()));
      var1.setItem(
         46,
         MenuItems.named(
            Material.PAPER,
            "&6&lJoin & Queue Messages",
            List.of("&7Choose which lifecycle messages appear", "&7Edit text, formatting and server overrides", "&eClick to configure")
         )
      );
      var1.setItem(
         48,
         MenuItems.named(
            Material.BARRIER, "&cClear Overrides", List.of(var3 ? "&7Not available for global defaults" : "&eShift-click to inherit every global default")
         )
      );
      var1.setItem(49, MenuItems.named(Material.NETHER_STAR, "&bPreview Countdown", List.of("&7Preview the effective message and sound")));
      if (!var3) {
         var1.setItem(
            50,
            MenuItems.named(
               Material.BLAZE_POWDER, "&6&lDebug Request (No Transfer)", List.of("&7Starts this server without moving you; use for status testing")
            )
         );
      }

      var1.setItem(52, globalButton(var0));
   }

   private static Material boolMaterial(String var0) {
      return Boolean.parseBoolean(var0) ? Material.LIME_DYE : Material.GRAY_DYE;
   }

   private static String value(HubPilotStore var0, String var1, boolean var2, SettingKey var3) {
      return var2 ? var0.global(var3) : var0.effective(var1, var3);
   }

   private static void put(Inventory var0, int var1, Material var2, HubPilotStore var3, String var4, boolean var5, SettingKey var6, List<String> var7) {
      String var8 = value(var3, var4, var5, var6);
      String var9 = var5 ? "&7Global default" : (var3.hasOverride(var4, var6) ? "&dServer override" : "&7Inherited global default");
      ArrayList var10 = new ArrayList();
      var10.add("&7Current: &f" + var8);
      var10.add(var9);
      var10.addAll(var7);
      var0.setItem(var1, MenuItems.named(var2, "&e&l" + var6.label(), var10));
   }

   public static ItemStack automationButton(HubPilotHubPlugin var0, String var1) {
      return MenuItems.named(Material.REDSTONE, "&b&lAutomation Settings", List.of("&7Countdown, retries, startup and shutdown", "&eClick to configure"));
   }

   public static ItemStack cloneButton() {
      return MenuItems.named(Material.STRUCTURE_BLOCK, "&a&lClone Destination", List.of("&7Copies this entry and its overrides", "&eClick to clone"));
   }

   public static ItemStack globalButton(HubPilotHubPlugin var0) {
      return MenuItems.named(Material.COMMAND_BLOCK, "&d&lGlobal Defaults", List.of("&7Configure settings inherited by servers", "&eClick to open"));
   }
}
