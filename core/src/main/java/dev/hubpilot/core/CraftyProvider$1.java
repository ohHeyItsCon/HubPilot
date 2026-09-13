package dev.hubpilot.core;

import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

class CraftyProvider$1 implements X509TrustManager {
   final CraftyProvider this$0;

   CraftyProvider$1(final CraftyProvider param1) {
      this.this$0 = param1;
   }

   @Override
   public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
   }

   @Override
   public void checkClientTrusted(X509Certificate[] var1, String var2) {
   }

   @Override
   public void checkServerTrusted(X509Certificate[] var1, String var2) {
   }
}
