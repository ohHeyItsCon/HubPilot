package dev.hubpilot.core;

import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent.ForwardResult;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.UUID;

final class DirectAuthHandler {
   private static final String CHANNEL = "hubpilot:control";

   private DirectAuthHandler() {
   }

   static void dispatch(PluginMessageEvent var0) {
      if (var0 != null && var0.getIdentifier() != null && "hubpilot:control".equalsIgnoreCase(var0.getIdentifier().getId())) {
         var0.setResult(ForwardResult.handled());
         if (var0.getSource() instanceof ServerConnection var1) {
            Player var11 = var1.getPlayer();
            if (var11 != null && var1.getServerInfo() != null) {
               UUID var3 = var11.getUniqueId();
               if (var3 != null) {
                  String var4 = var1.getServerInfo().getName();
                  if (var4 == null) {
                     var4 = "";
                  }

                  var4 = var4.toLowerCase(Locale.ROOT);

                  try (DataInputStream var5 = new DataInputStream(new ByteArrayInputStream(var0.getData()))) {
                     String var6 = var5.readUTF();
                     if ("AUTH".equals(var6)) {
                        handleAuth(var1, var3, var4, var5, var0.getIdentifier());
                     } else if ("COMPONENT".equals(var6)) {
                        handleComponent(var3, var4, var5);
                     } else if ("ACTION".equals(var6)) {
                        handleAction(var1, var3, var4, var5, var0.getIdentifier());
                     }
                  } catch (Exception var10) {
                  }
               }
            }
         }
      }
   }

   private static boolean trustedHub(String var0) {
      try {
         Object var1 = null;
      } catch (Throwable var3) {
      }

      try {
         Field var5 = AccessManager.class.getDeclaredField("hubServer");
         var5.setAccessible(true);
         Object var2 = var5.get(AccessManager.get());
         return var2 != null && String.valueOf(var2).equalsIgnoreCase(var0);
      } catch (Throwable var4) {
         return "hub".equalsIgnoreCase(var0);
      }
   }

   private static void handleAuth(ServerConnection var0, UUID var1, String var2, DataInputStream var3, ChannelIdentifier var4) throws Exception {
      if (trustedHub(var2)) {
         UUID var5 = UUID.fromString(var3.readUTF());
         if (var1.equals(var5)) {
            String var6 = var3.readUTF();
            boolean var7 = var3.readBoolean();
            boolean var8 = var3.readBoolean();
            boolean var9 = var3.readBoolean();
            boolean var10 = var3.readBoolean();
            boolean var11 = var3.readBoolean();
            AccessManager.get().recordAuth(var1, var6, var2, var7, var8, var9, var10, var11);
            sendState(var0, var4, var1);
         }
      }
   }

   private static void handleComponent(UUID var0, String var1, DataInputStream var2) throws Exception {
      UUID var3 = UUID.fromString(var2.readUTF());
      if (var0.equals(var3)) {
         String var4 = var2.readUTF();
         String var5 = var2.readUTF();
         if (var4.length() <= 32 && var5.length() <= 32) {
            AccessManager.get().recordComponent(var4, var5, var1);
         }
      }
   }

   private static void handleAction(ServerConnection var0, UUID var1, String var2, DataInputStream var3, ChannelIdentifier var4) throws Exception {
      if (trustedHub(var2)) {
         UUID var5 = UUID.fromString(var3.readUTF());
         if (var1.equals(var5)) {
            String var6 = var3.readUTF().toUpperCase(Locale.ROOT);
            String var7;
            switch (var6) {
               case "CLAIM_OWNER":
                  String var16 = var3.readUTF();
                  boolean var18 = Boolean.parseBoolean(var3.readUTF());
                  boolean var19 = Boolean.parseBoolean(var3.readUTF());
                  AccessManager.get().recordAuth(var1, var16, var2, var18, false, false, false, var19);
                  var7 = AccessManager.get().claim(var1, var16);
                  if ("SUCCESS".equals(var7)) {
                     var7 = "SUCCESS: You are now the HubPilot owner. Run /hp setup to continue.";
                  }
                  break;
               case "STAFF_SET":
                  UUID var15 = UUID.fromString(var3.readUTF());
                  String var17 = var3.readUTF();
                  String var12 = var3.readUTF();
                  var7 = AccessManager.get().setStaff(var1, var15, var17, var12);
                  break;
               case "OWNER_ADD":
                  UUID var14 = UUID.fromString(var3.readUTF());
                  String var11 = var3.readUTF();
                  var7 = AccessManager.get().addOwner(var1, var14, var11);
                  break;
               case "OWNER_REMOVE":
                  UUID var13 = UUID.fromString(var3.readUTF());
                  var7 = AccessManager.get().removeOwner(var1, var13);
                  break;
               case "PROVIDER_TYPE":
                  if (!AccessManager.get().isOwner(var1)) {
                     var7 = "Only an owner can change provider settings.";
                  } else {
                     var7 = ProviderRegistry.get().configurePrimary(var3.readUTF(), "");
                     AccessManager.get().setProviderSkipped(false);
                  }
                  break;
               case "PROVIDER_URL":
                  if (!AccessManager.get().isOwner(var1)) {
                     var7 = "Only an owner can change provider settings.";
                  } else {
                     var7 = ProviderRegistry.get().setPrimaryUrl(var3.readUTF());
                  }
                  break;
               case "PROVIDER_SECRET":
                  if (!AccessManager.get().isOwner(var1)) {
                     var7 = "Only an owner can change provider credentials.";
                  } else {
                     var7 = ProviderRegistry.get().setSecretForPrimary(var3.readUTF());
                  }
                  break;
               case "PROVIDER_TEST":
                  if (!AccessManager.get().isOwner(var1)) {
                     var7 = "Only an owner can test provider credentials.";
                  } else {
                     ProviderRegistry.Result var10 = ProviderRegistry.get().test(ProviderRegistry.get().primaryId());
                     var7 = var10.success()
                        ? "SUCCESS: Provider connection succeeded (HTTP " + var10.statusCode() + ")."
                        : "Provider test failed: " + var10.error();
                  }
                  break;
               case "PROVIDER_SKIP":
                  if (!AccessManager.get().isOwner(var1)) {
                     var7 = "Only an owner can skip provider setup.";
                  } else {
                     ProviderRegistry.get().configurePrimary("always-on", "");
                     AccessManager.get().setProviderSkipped(true);
                     var7 = "SUCCESS";
                  }
                  break;
               case "SETUP_COMPLETE":
                  if (!AccessManager.get().isOwner(var1)) {
                     var7 = "Only an owner can finish setup.";
                  } else if (!ProviderRegistry.get().primaryConfigured() && !AccessManager.get().providerSkipped()) {
                     var7 = "Configure a provider, choose Always-On/Skip, or use /hp setup override confirm.";
                  } else {
                     AccessManager.get().markSetupComplete(true);
                     var7 = "SUCCESS";
                  }
                  break;
               default:
                  var7 = ServerAdminBridge.handleUnknown(var0, var1, var6, var3);
            }

            reply(var0, var4, "FEEDBACK", var1.toString(), var7);
            sendState(var0, var4, var1);
         }
      }
   }

   private static void sendState(ServerConnection var0, ChannelIdentifier var1, UUID var2) {
      String var3 = AccessManager.get().state().name();
      String var4 = AccessManager.get().role(var2);
      String var5 = ProviderRegistry.get().primaryType();
      String var6 = String.valueOf(ProviderRegistry.get().primaryConfigured());
      String var7 = ProviderRegistry.get().lastTest();
      reply(var0, var1, "STATE", var2.toString(), var3, var4, var5, var6, var7 == null ? "" : var7);
   }

   private static void reply(ServerConnection var0, ChannelIdentifier var1, String... var2) {
      try {
         ByteArrayOutputStream var3 = new ByteArrayOutputStream();

         try (DataOutputStream var4 = new DataOutputStream(var3)) {
            for (String var8 : var2) {
               var4.writeUTF(var8 == null ? "" : var8);
            }
         }

         var0.sendPluginMessage(var1, var3.toByteArray());
      } catch (Exception var11) {
      }
   }
}
