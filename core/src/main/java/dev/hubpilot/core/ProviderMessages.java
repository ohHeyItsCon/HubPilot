package dev.hubpilot.core;

final class ProviderMessages {
   private ProviderMessages() {
   }

   static String unavailable(String var0) {
      String var1 = ProviderAliases.canonical(var0);
      ProviderRegistry var2 = ProviderRegistry.get();
      return var2.known(var0)
         ? "Startup provider '" + var1 + "' exists, but it is not enabled/configured. Run /hp setup to configure it, or edit providers.yml and secrets.yml."
         : "Startup provider '" + var1 + "' is not available. Run /hp setup or choose a provider listed by /hp providers.";
   }
}
