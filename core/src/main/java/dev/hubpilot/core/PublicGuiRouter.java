package dev.hubpilot.core;

final class PublicGuiRouter {
   private PublicGuiRouter() {
   }

   static boolean handle(Object var0, String[] var1) {
      if (var1 == null || var1.length == 0 || !"adminitem".equalsIgnoreCase(var1[0])) {
         return false;
      } else if (AccessManager.uuidOf(var0) == null) {
         PublicCommandLayer.send(var0, "§cOnly an in-game player can receive the HubPilot admin item.");
         return true;
      } else if (!AccessManager.get().isAdminSource(var0)) {
         PublicCommandLayer.send(var0, "§cYou do not have permission to receive the HubPilot admin item.");
         return true;
      } else if (!DirectOpenTransport.send(var0, "GUI:adminitem")) {
         PublicCommandLayer.send(var0, "§cCould not reach HubPilot Hub. Run this while connected to the configured hub.");
         return true;
      } else {
         PublicCommandLayer.send(var0, "§eAdmin item request sent to HubPilot Hub.");
         return true;
      }
   }

   static boolean sendRaw(Object var0, String var1) {
      return DirectOpenTransport.send(var0, var1);
   }
}
