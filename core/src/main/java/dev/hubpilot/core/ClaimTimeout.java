package dev.hubpilot.core;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

final class ClaimTimeout {
   private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(var0 -> {
      Thread var1 = new Thread(var0, "hubpilot-claim-timeout");
      var1.setDaemon(true);
      return var1;
   });

   private ClaimTimeout() {
   }

   static void start(Object var0) {
      UUID var1 = AccessManager.uuidOf(var0);
      if (var1 != null) {
         TIMER.schedule(() -> finishFromFreshHubAuth(var0, var1), 1L, TimeUnit.SECONDS);
         TIMER.schedule(() -> {
            try {
               if (AccessManager.get().state() == AccessManager.SetupState.UNCLAIMED) {
                  if (finishFromFreshHubAuth(var0, var1)) {
                     return;
                  }

                  PublicCommandLayer.send(var0, "§cHubPilot Hub did not answer the ownership verification request.");
                  PublicCommandLayer.send(var0, "§7HubPilot received neither the direct claim response nor a fresh trusted hub authorization heartbeat.");
                  PublicCommandLayer.send(var0, "§7Verify HubPilot-Hub is enabled on the configured hub, then run §f/hp claimowner§7 again.");
               }
            } catch (Throwable var3) {
            }
         }, 5L, TimeUnit.SECONDS);
      }
   }

   private static boolean finishFromFreshHubAuth(Object var0, UUID var1) {
      try {
         AccessManager var2 = AccessManager.get();
         if (var2.state() == AccessManager.SetupState.UNCLAIMED && var2.isTrustedHubAuth(var1)) {
            AccessManager.Auth var3 = var2.auth(var1);
            if (var3 != null && (var3.op() || var3.claimPermission())) {
               String var4 = var2.claim(var1, var3.username());
               if ("SUCCESS".equals(var4)) {
                  PublicCommandLayer.send(var0, "§aYou are now the HubPilot owner. Run §f/hp setup §ato continue.");
                  return true;
               } else {
                  PublicCommandLayer.send(var0, "§c" + var4);
                  return false;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      } catch (Throwable var5) {
         return false;
      }
   }
}
