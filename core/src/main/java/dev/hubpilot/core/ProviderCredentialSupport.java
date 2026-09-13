package dev.hubpilot.core;

final class ProviderCredentialSupport {
   private static final String TLS_ON = "HP3510|TLS=1|";
   private static final String TLS_OFF = "HP3510|TLS=0|";

   private ProviderCredentialSupport() {
   }

   static String apply(ProviderRegistry var0, String var1) {
      ProviderRegistry.Definition var2 = var0.primaryDefinition();
      if (var2 == null) {
         return "No primary provider is selected.";
      } else {
         boolean var3 = false;
         boolean var4 = false;
         String var5 = var1;
         if (var1 != null && var1.startsWith("HP3510|TLS=1|")) {
            var3 = true;
            var4 = true;
            var5 = var1.substring("HP3510|TLS=1|".length());
         } else if (var1 != null && var1.startsWith("HP3510|TLS=0|")) {
            var3 = true;
            var5 = var1.substring("HP3510|TLS=0|".length());
         }

         String var6 = var0.setSecret(var2.id(), var5);
         if (!"SUCCESS".equals(var6)) {
            return var6;
         } else if (!var3) {
            return var6;
         } else {
            String var7 = var0.setPrimaryInsecure(var4);
            return !"SUCCESS".equals(var7)
               ? var7
               : "SUCCESS: Provider credential saved" + (var4 ? "; self-signed TLS trust enabled." : "; certificate verification enabled.");
         }
      }
   }
}
