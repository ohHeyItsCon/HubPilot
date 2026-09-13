package dev.hubpilot.core;

import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;

final class CraftyProvider {
   private final Logger logger;

   CraftyProvider(Logger var1, HubPilotConfig.CraftySettings var2) {
      this.logger = var1;
   }

   synchronized void update(HubPilotConfig.CraftySettings var1) {
      ProviderRegistry.get().reload();
      AccessManager.get().reload(false);
   }

   CompletableFuture<CraftyProvider.ActionResult> start(ManagedServer var1) {
      return ProviderRegistry.get().action(var1, "start").thenApply(CraftyProvider::convert);
   }

   CompletableFuture<CraftyProvider.ActionResult> stop(ManagedServer var1) {
      return ProviderRegistry.get().action(var1, "stop").thenApply(CraftyProvider::convert);
   }

   private static CraftyProvider.ActionResult convert(ProviderRegistry.Result var0) {
      return var0.success() ? CraftyProvider.ActionResult.success(var0.statusCode()) : CraftyProvider.ActionResult.failure(var0.error());
   }

   record ActionResult(boolean success, int statusCode, String error) {
      static CraftyProvider.ActionResult success(int var0) {
         return new CraftyProvider.ActionResult(true, var0, "");
      }

      static CraftyProvider.ActionResult failure(String var0) {
         return new CraftyProvider.ActionResult(false, 0, var0 == null ? "Unknown provider failure" : var0);
      }
   }
}
