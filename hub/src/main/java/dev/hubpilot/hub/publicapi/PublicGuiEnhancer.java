package dev.hubpilot.hub.publicapi;

import dev.hubpilot.hub.HubPilotHubPlugin;
import org.bukkit.inventory.Inventory;

public final class PublicGuiEnhancer {
   private PublicGuiEnhancer() {
   }

   public static Inventory enhance(HubPilotHubPlugin var0, Inventory var1) {
      PublicHubBootstrap.enhanceAdminHome(var0, var1);
      return var1;
   }
}
