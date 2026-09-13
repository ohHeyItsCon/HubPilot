package dev.hubpilot.core;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;

final class PublicBootstrap {
   static final String CHANNEL = "hubpilot:auth";
   private static volatile Object channelIdentifier;
   private static volatile Object plugin;
   private static volatile Object proxy;
   private static volatile Logger logger;
   private static volatile HubPilotConfig config;

   private PublicBootstrap() {
   }

   static void prepare(Path var0, Logger var1, HubPilotConfig var2) {
      try {
         String var3 = var2.snapshot().hubServer();
         AccessManager.get().initialize(var0, var3);
         ProviderRegistry.get().initialize(var0, var1);
      } catch (Throwable var4) {
         if (var1 != null) {
            var1.warn("HubPilot public bootstrap preparation failed: {}", new Object[]{String.valueOf(var4.getMessage())});
         }
      }
   }

   static boolean sendOpen(Object var0, String var1) {
      return DirectOpenTransport.send(var0, var1);
   }

   private static byte[] packet(String... var0) throws IOException {
      ByteArrayOutputStream var1 = new ByteArrayOutputStream();

      try (DataOutputStream var2 = new DataOutputStream(var1)) {
         for (String var6 : var0) {
            var2.writeUTF(var6 == null ? "" : var6);
         }
      }

      return var1.toByteArray();
   }

   private static void reply(Object var0, String... var1) {
      try {
         Reflect.call(var0, "sendPluginMessage", channelIdentifier, packet(var1));
      } catch (Throwable var3) {
         if (logger != null) {
            logger.debug("HubPilot auth reply failed: {}", new Object[]{root(var3)});
         }
      }
   }

   private static boolean hasMethod(Object var0, String var1) {
      for (Method var5 : var0.getClass().getMethods()) {
         if (var5.getName().equals(var1)) {
            return true;
         }
      }

      for (Method var9 : var0.getClass().getDeclaredMethods()) {
         if (var9.getName().equals(var1)) {
            return true;
         }
      }

      return false;
   }

   private static void markHandled(Object var0) {
      try {
         Class var1 = Class.forName("com.velocitypowered.api.event.connection.PluginMessageEvent$ForwardResult");
         Object var2 = Reflect.callStatic(var1, "handled");
         Reflect.call(var0, "setResult", var2);
      } catch (Throwable var3) {
      }
   }

   private static String root(Throwable var0) {
      while (var0.getCause() != null && var0.getCause() != var0) {
         var0 = var0.getCause();
      }

      return var0.getMessage() == null ? var0.getClass().getSimpleName() : var0.getMessage();
   }

   static void attach(Object var0, Object var1, Logger var2, HubPilotConfig var3) {
      plugin = var0;
      proxy = var1;
      logger = var2;
      config = var3;
      channelIdentifier = MinecraftChannelIdentifier.from("hubpilot:auth");
   }

   static boolean requestOwnerClaim(Object var0) {
      return ClaimTransport.request(var0, config);
   }

   public static final class AuthBridge {
      AuthBridge() {
      }

      public void onPluginMessage0(PluginMessageEvent var1) {
         PluginMessageEvent var2 = var1;

         try {
            Object var3 = Reflect.call(var2, "getIdentifier");
            String var4 = String.valueOf(Reflect.call(var3, "getId"));
            if (!"hubpilot:auth".equalsIgnoreCase(var4)) {
               return;
            }

            PublicBootstrap.markHandled(var2);
            Object var5 = Reflect.call(var2, "getSource");
            if (var5 == null || !PublicBootstrap.hasMethod(var5, "getServerInfo")) {
               return;
            }

            String var6 = String.valueOf(Reflect.call(Reflect.call(var5, "getServerInfo"), "getName")).toLowerCase(Locale.ROOT);
            Object var7 = Reflect.call(var5, "getPlayer");
            if (var7 == null) {
               return;
            }

            UUID var8 = AccessManager.uuidOf(var7);
            if (var8 == null) {
               return;
            }

            byte[] var9 = (byte[])Reflect.call(var2, "getData");

            try (DataInputStream var10 = new DataInputStream(new ByteArrayInputStream(var9))) {
               String var11 = var10.readUTF();
               if ("AUTH".equals(var11)) {
                  this.handleAuth(var5, var7, var8, var6, var10);
               } else if ("COMPONENT".equals(var11)) {
                  this.handleComponent(var5, var8, var6, var10);
               } else if ("ACTION".equals(var11)) {
                  this.handleAction(var5, var7, var8, var6, var10);
               }
            }
         } catch (Throwable var15) {
            if (PublicBootstrap.logger != null) {
               PublicBootstrap.logger.warn("Rejected malformed HubPilot auth/setup message: {}", new Object[]{PublicBootstrap.root(var15)});
            }
         }
      }

      private void handleAuth(Object var1, Object var2, UUID var3, String var4, DataInputStream var5) throws Exception {
         if (this.trustedHub(var4)) {
            UUID var6 = UUID.fromString(var5.readUTF());
            if (var3.equals(var6)) {
               String var7 = var5.readUTF();
               boolean var8 = var5.readBoolean();
               boolean var9 = var5.readBoolean();
               boolean var10 = var5.readBoolean();
               boolean var11 = var5.readBoolean();
               boolean var12 = var5.readBoolean();
               AccessManager.get().recordAuth(var3, var7, var4, var8, var9, var10, var11, var12);
               this.sendState(var1, var3);
            }
         }
      }

      private void handleComponent(Object var1, UUID var2, String var3, DataInputStream var4) throws Exception {
         UUID var5 = UUID.fromString(var4.readUTF());
         if (var2.equals(var5)) {
            String var6 = var4.readUTF();
            String var7 = var4.readUTF();
            if (var6.length() <= 32 && var7.length() <= 32) {
               AccessManager.get().recordComponent(var6, var7, var3);
            }
         }
      }

      private void handleAction(Object var1, Object var2, UUID var3, String var4, DataInputStream var5) throws Exception {
         if (this.trustedHub(var4)) {
            UUID var6 = UUID.fromString(var5.readUTF());
            if (var3.equals(var6)) {
               String var7 = var5.readUTF().toUpperCase(Locale.ROOT);
               String var8;
               switch (var7) {
                  case "CLAIM_OWNER":
                     String var23 = var5.readUTF();
                     boolean var25 = Boolean.parseBoolean(var5.readUTF());
                     boolean var26 = Boolean.parseBoolean(var5.readUTF());
                     AccessManager.get().recordAuth(var3, var23, var4, var25, false, false, false, var26);
                     var8 = AccessManager.get().claim(var3, var23);
                     if ("SUCCESS".equals(var8)) {
                        var8 = "SUCCESS: You are now the HubPilot owner. Run /hp setup to continue.";
                     }
                     break;
                  case "STAFF_SET":
                     UUID var22 = UUID.fromString(var5.readUTF());
                     String var24 = var5.readUTF();
                     String var13 = var5.readUTF();
                     var8 = AccessManager.get().setStaff(var3, var22, var24, var13);

                     try {
                        if (var8.equals("SUCCESS")) {
                           Object var14 = PublicBootstrap.proxy;
                           if (var14 != null) {
                              Object var15 = Reflect.call(var14, "getPlayer", var22);
                              if (var15 instanceof Optional) {
                                 Object var16 = ((Optional)var15).orElse(null);
                                 if (var16 != null) {
                                    Object var17 = Reflect.call(var16, "getCurrentServer");
                                    if (var17 instanceof Optional) {
                                       Object var18 = ((Optional)var17).orElse(null);
                                       if (var18 != null) {
                                          this.sendState(var18, var22);
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     } catch (Throwable var19) {
                     }
                     break;
                  case "OWNER_ADD":
                     UUID var21 = UUID.fromString(var5.readUTF());
                     String var12 = var5.readUTF();
                     var8 = AccessManager.get().addOwner(var3, var21, var12);
                     break;
                  case "OWNER_REMOVE":
                     UUID var20 = UUID.fromString(var5.readUTF());
                     var8 = AccessManager.get().removeOwner(var3, var20);
                     break;
                  case "PROVIDER_TYPE":
                     if (!AccessManager.get().isOwner(var3)) {
                        var8 = "Only an owner can change provider credentials/settings.";
                     } else {
                        var8 = ProviderRegistry.get().configurePrimary(var5.readUTF(), "");
                        AccessManager.get().setProviderSkipped(false);
                     }
                     break;
                  case "PROVIDER_URL":
                     if (!AccessManager.get().isOwner(var3)) {
                        var8 = "Only an owner can change provider settings.";
                     } else {
                        var8 = ProviderRegistry.get().setPrimaryUrl(var5.readUTF());
                     }
                     break;
                  case "PROVIDER_SECRET":
                     if (!AccessManager.get().isOwner(var3)) {
                        var8 = "Only an owner can change provider credentials.";
                     } else {
                        var8 = ProviderRegistry.get().setSecretForPrimary(var5.readUTF());
                     }
                     break;
                  case "PROVIDER_TEST":
                     if (!AccessManager.get().isOwner(var3)) {
                        var8 = "Only an owner can test provider credentials.";
                     } else {
                        ProviderRegistry.Result var11 = ProviderRegistry.get().test(ProviderRegistry.get().primaryId());
                        var8 = var11.success()
                           ? "SUCCESS: Provider connection succeeded (HTTP " + var11.statusCode() + ")."
                           : "Provider test failed: " + var11.error();
                     }
                     break;
                  case "PROVIDER_SKIP":
                     if (!AccessManager.get().isOwner(var3)) {
                        var8 = "Only an owner can skip provider setup.";
                     } else {
                        ProviderRegistry.get().configurePrimary("always-on", "");
                        AccessManager.get().setProviderSkipped(true);
                        var8 = "SUCCESS";
                     }
                     break;
                  case "SETUP_COMPLETE":
                     if (!AccessManager.get().isOwner(var3)) {
                        var8 = "Only an owner can finish setup.";
                     } else if (!ProviderRegistry.get().primaryConfigured() && !AccessManager.get().providerSkipped()) {
                        var8 = "Configure a provider, choose Always-On/Skip, or use /hp setup override confirm.";
                     } else {
                        AccessManager.get().markSetupComplete(true);
                        var8 = "SUCCESS";
                     }
                     break;
                  default:
                     var8 = ServerAdminBridge.handleUnknown(var1, var3, var7, var5);
               }

               PublicBootstrap.reply(var1, "FEEDBACK", var3.toString(), var8);
               this.sendState(var1, var3);
            }
         }
      }

      private void sendState(Object var1, UUID var2) {
         String var3 = AccessManager.get().state().name();
         String var4 = AccessManager.get().role(var2);
         String var5 = ProviderRegistry.get().primaryType();
         String var6 = String.valueOf(ProviderRegistry.get().primaryConfigured());
         String var7 = ProviderRegistry.get().lastTest();
         PublicBootstrap.reply(var1, "STATE", var2.toString(), var3, var4, var5, var6, var7);
      }

      private boolean trustedHub(String var1) {
         String var2 = "hub";

         try {
            var2 = PublicBootstrap.config.snapshot().hubServer();
         } catch (Throwable var4) {
         }

         return var2 != null && var2.equalsIgnoreCase(var1);
      }

      @Subscribe
      public void onPluginMessage(PluginMessageEvent var1) {
         AuthEventGate.dispatchBridge(this, var1);
      }
   }
}
