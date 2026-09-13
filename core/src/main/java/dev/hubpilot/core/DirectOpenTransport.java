package dev.hubpilot.core;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.util.Optional;

final class DirectOpenTransport {
   private static final MinecraftChannelIdentifier AUTH = MinecraftChannelIdentifier.from("hubpilot:control");

   private DirectOpenTransport() {
   }

   static boolean send(Object var0, String var1) {
      if (var0 instanceof Player var2 && var1 != null && !var1.isBlank()) {
         try {
            Optional var3 = var2.getCurrentServer();
            if (var3.isEmpty()) {
               return false;
            } else {
               ServerConnection var4 = (ServerConnection)var3.get();
               if (var4.getServerInfo() != null && hubServer().equalsIgnoreCase(var4.getServerInfo().getName())) {
                  ByteArrayOutputStream var5 = new ByteArrayOutputStream();

                  try (DataOutputStream var6 = new DataOutputStream(var5)) {
                     var6.writeUTF("OPEN");
                     var6.writeUTF(var1);
                  }

                  return var4.sendPluginMessage(AUTH, var5.toByteArray());
               } else {
                  return false;
               }
            }
         } catch (Exception var11) {
            return false;
         }
      } else {
         return false;
      }
   }

   private static String hubServer() {
      try {
         Field var0 = AccessManager.class.getDeclaredField("hubServer");
         var0.setAccessible(true);
         Object var1 = var0.get(AccessManager.get());
         return var1 == null ? "hub" : String.valueOf(var1);
      } catch (Throwable var2) {
         return "hub";
      }
   }
}
