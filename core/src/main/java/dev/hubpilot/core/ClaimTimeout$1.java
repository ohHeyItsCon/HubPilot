package dev.hubpilot.core;

import java.util.concurrent.ThreadFactory;

class ClaimTimeout$1 implements ThreadFactory {
   @Override
   public Thread newThread(Runnable var1) {
      Thread var2 = new Thread(var1, "HubPilot-ClaimTimeout");
      var2.setDaemon(true);
      return var2;
   }
}
