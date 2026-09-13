package dev.hubpilot.core;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Optional;

final class ClaimTransport {
   private static final MinecraftChannelIdentifier AUTH = MinecraftChannelIdentifier.from("hubpilot:control");

   private ClaimTransport() {
   }

   static boolean request(Object var0, HubPilotConfig var1) {
      if (var0 instanceof Player var2 && var1 != null) {
         Optional var3 = var2.getCurrentServer();
         if (var3.isEmpty()) {
            return false;
         } else {
            ServerConnection var4 = (ServerConnection)var3.get();
            String var5 = var4.getServerInfo().getName();
            String var6 = var1.snapshot().hubServer();
            if (var6 != null && var6.equalsIgnoreCase(var5)) {
               try {
                  ByteArrayOutputStream var7 = new ByteArrayOutputStream();

                  try (DataOutputStream var8 = new DataOutputStream(var7)) {
                     var8.writeUTF("OPEN");
                     var8.writeUTF("CLAIM_OWNER");
                  }

                  return var4.sendPluginMessage(AUTH, var7.toByteArray());
               } catch (Exception var13) {
                  return false;
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }
}
