package dev.hubpilot.hub.config;

import dev.hubpilot.hub.HubPilotHubPlugin;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public final class MenuConfig {
   private final String guiTitle;
   private final Material fillerMaterial;
   private final String compassName;
   private final List<String> compassLore;
   private final Integer compassCustomModelData;
   private final String adminItemName;
   private final List<String> adminItemLore;
   private final Path sharedDirectory;

   private MenuConfig(String var1, Material var2, String var3, List<String> var4, Integer var5, String var6, List<String> var7, Path var8) {
      this.guiTitle = var1;
      this.fillerMaterial = var2;
      this.compassName = var3;
      this.compassLore = var4;
      this.compassCustomModelData = var5;
      this.adminItemName = var6;
      this.adminItemLore = var7;
      this.sharedDirectory = var8;
   }

   public static MenuConfig load(HubPilotHubPlugin var0) {
      String var1 = var0.getConfig().getString("gui-title", "&8Select a Destination");
      Material var2 = Material.matchMaterial(var0.getConfig().getString("filler-material", "GRAY_STAINED_GLASS_PANE"));
      if (var2 == null) {
         var2 = Material.GRAY_STAINED_GLASS_PANE;
      }

      ConfigurationSection var3 = var0.getConfig().getConfigurationSection("compass");
      String var4 = var3 == null ? "&b&lServer Menu" : var3.getString("name", "&b&lServer Menu");
      List var5 = var3 == null ? List.of("&7Right-click to open") : var3.getStringList("lore");
      if (var5.isEmpty()) {
         var5 = List.of("&7Right-click to open");
      }

      int var6 = var3 == null ? 0 : var3.getInt("custom-model-data", 0);
      ConfigurationSection var7 = var0.getConfig().getConfigurationSection("admin-item");
      String var8 = var7 == null ? "&c&lHubPilot Admin" : var7.getString("name", "&c&lHubPilot Admin");
      List var9 = var7 == null ? List.of("&7Right-click to manage destinations") : var7.getStringList("lore");
      if (var9.isEmpty()) {
         var9 = List.of("&7Right-click to manage destinations");
      }

      String var10 = var0.getConfig().getString("shared-directory", "/shared/hubpilot");
      Path var11 = Path.of(var10);
      return new MenuConfig(var1, var2, var4, Collections.unmodifiableList(var5), var6 > 0 ? var6 : null, var8, Collections.unmodifiableList(var9), var11);
   }

   public String guiTitle() {
      return this.guiTitle;
   }

   public Material fillerMaterial() {
      return this.fillerMaterial;
   }

   public String compassName() {
      return this.compassName;
   }

   public List<String> compassLore() {
      return this.compassLore;
   }

   public Integer compassCustomModelData() {
      return this.compassCustomModelData;
   }

   public String adminItemName() {
      return this.adminItemName;
   }

   public List<String> adminItemLore() {
      return this.adminItemLore;
   }

   public Path sharedDirectory() {
      return this.sharedDirectory;
   }
}
