package dev.hubpilot.hub.util;

import java.util.Locale;
import org.bukkit.Material;

public final class IconMaterials {
   public static final Material FALLBACK = Material.GRASS_BLOCK;

   private IconMaterials() {
   }

   public static Material resolve(String var0) {
      Material var1 = find(var0);
      return var1 == null ? FALLBACK : var1;
   }

   public static String canonical(String var0) {
      if (var0 != null && var0.trim().equalsIgnoreCase("default")) {
         return FALLBACK.name();
      } else {
         Material var1 = find(var0);
         if (var1 == null) {
            throw new IllegalArgumentException("Unknown or unusable item. Use a material name such as DIAMOND_SWORD, OAK_LOG, or GRASS_BLOCK.");
         } else {
            return var1.name();
         }
      }
   }

   private static Material find(String var0) {
      if (var0 != null && !var0.trim().isEmpty()) {
         String var1 = var0.trim().toUpperCase(Locale.ENGLISH).replace(' ', '_').replace('-', '_');
         if (var1.startsWith("MINECRAFT:")) {
            var1 = var1.substring("MINECRAFT:".length());
         }

         Material var2 = Material.matchMaterial(var1);
         return var2 != null && !var2.isAir() && var2.isItem() ? var2 : null;
      } else {
         return null;
      }
   }
}
