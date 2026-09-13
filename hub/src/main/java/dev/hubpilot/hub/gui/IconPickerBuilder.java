package dev.hubpilot.hub.gui;

import dev.hubpilot.hub.config.Destination;
import dev.hubpilot.hub.util.IconMaterials;
import dev.hubpilot.hub.util.MenuItems;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

public final class IconPickerBuilder {
   private static final List<String> ICONS = List.of(
      "GRASS_BLOCK",
      "OAK_SAPLING",
      "DIAMOND_SWORD",
      "IRON_PICKAXE",
      "NETHERITE_PICKAXE",
      "CRAFTING_TABLE",
      "CHEST",
      "ENDER_CHEST",
      "BARREL",
      "FURNACE",
      "BLAST_FURNACE",
      "ANVIL",
      "ENCHANTING_TABLE",
      "BOOKSHELF",
      "WRITABLE_BOOK",
      "MAP",
      "COMPASS",
      "CLOCK",
      "ENDER_PEARL",
      "ENDER_EYE",
      "NETHER_STAR",
      "BEACON",
      "RESPAWN_ANCHOR",
      "LODESTONE",
      "OBSIDIAN",
      "CRYING_OBSIDIAN",
      "NETHERRACK",
      "SOUL_SAND",
      "END_STONE",
      "PURPUR_BLOCK",
      "DIAMOND",
      "EMERALD",
      "GOLD_INGOT",
      "IRON_INGOT",
      "COPPER_INGOT",
      "AMETHYST_SHARD",
      "REDSTONE",
      "LAPIS_LAZULI",
      "COAL",
      "QUARTZ",
      "EXPERIENCE_BOTTLE",
      "POTION",
      "BOW",
      "CROSSBOW",
      "TRIDENT",
      "SHIELD",
      "TOTEM_OF_UNDYING",
      "ELYTRA",
      "FIREWORK_ROCKET",
      "TNT",
      "CREEPER_HEAD",
      "SKELETON_SKULL",
      "DRAGON_HEAD",
      "PLAYER_HEAD",
      "ZOMBIE_HEAD",
      "SPAWNER",
      "BLAZE_ROD",
      "GHAST_TEAR",
      "MAGMA_CREAM",
      "SLIME_BALL",
      "HONEY_BOTTLE",
      "WHEAT",
      "CARROT",
      "POTATO",
      "APPLE",
      "GOLDEN_APPLE",
      "COOKED_BEEF",
      "CAKE",
      "WATER_BUCKET",
      "LAVA_BUCKET",
      "FISHING_ROD",
      "SADDLE",
      "MINECART",
      "OAK_BOAT",
      "RAIL",
      "REDSTONE_TORCH",
      "REPEATER",
      "COMPARATOR",
      "LEVER",
      "STONE_BUTTON",
      "WHITE_BANNER",
      "RED_BANNER",
      "BLUE_BANNER",
      "GREEN_BANNER",
      "YELLOW_BANNER",
      "PURPLE_BANNER",
      "SUNFLOWER",
      "ROSE_BUSH",
      "CHERRY_SAPLING",
      "MOSS_BLOCK",
      "SNOW_BLOCK",
      "ICE",
      "SAND",
      "PRISMARINE",
      "SEA_LANTERN",
      "SCULK",
      "SCULK_CATALYST",
      "TRIAL_KEY",
      "OMINOUS_TRIAL_KEY"
   );
   private static final int PAGE_SIZE = 45;

   private IconPickerBuilder() {
   }

   public static Inventory build(Destination var0, int var1) {
      int var2 = Math.max(1, (ICONS.size() + 45 - 1) / 45);
      int var3 = Math.max(0, Math.min(var1, var2 - 1));
      IconPickerHolder var4 = new IconPickerHolder(var0.id(), var3);
      Inventory var5 = Bukkit.createInventory(var4, 54, MenuItems.colorize("&8Choose Icon &7(" + (var3 + 1) + "/" + var2 + ")"));
      var4.setInventory(var5);
      GuiCommon.fill(var5, Material.BLACK_STAINED_GLASS_PANE);
      int var6 = var3 * 45;
      int var7 = Math.min(ICONS.size(), var6 + 45);

      for (int var8 = var6; var8 < var7; var8++) {
         String var9 = ICONS.get(var8);
         Material var10 = IconMaterials.resolve(var9);
         ArrayList var11 = new ArrayList();
         var11.add("&7Material: &f" + var9);
         if (var9.equalsIgnoreCase(var0.iconMaterial())) {
            var11.add("&aCurrently selected");
         } else {
            var11.add("&eClick to select");
         }

         var5.setItem(var8 - var6, MenuItems.named(var10, "&f" + friendly(var9), var11));
      }

      if (var3 > 0) {
         var5.setItem(45, MenuItems.named(Material.ARROW, "&ePrevious Page", List.of()));
      }

      var5.setItem(49, MenuItems.named(IconMaterials.resolve(var0.iconMaterial()), "&bCurrent Icon", List.of("&7" + var0.iconMaterial(), "&eClick to return")));
      if (var3 + 1 < var2) {
         var5.setItem(53, MenuItems.named(Material.ARROW, "&eNext Page", List.of()));
      }

      var5.setItem(47, MenuItems.named(Material.GRASS_BLOCK, "&aUse Default", List.of("&7GRASS_BLOCK")));
      var5.setItem(
         51, MenuItems.named(Material.NAME_TAG, "&eType Custom Material", List.of("&7Only needed for items not listed", "&eClick to type a material name"))
      );
      return var5;
   }

   private static String friendly(String var0) {
      String[] var1 = var0.toLowerCase().split("_");
      StringBuilder var2 = new StringBuilder();

      for (String var6 : var1) {
         if (!var2.isEmpty()) {
            var2.append(' ');
         }

         var2.append(Character.toUpperCase(var6.charAt(0))).append(var6.substring(1));
      }

      return var2.toString();
   }
}
