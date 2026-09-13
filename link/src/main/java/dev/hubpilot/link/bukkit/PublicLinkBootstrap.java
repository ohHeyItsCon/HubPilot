package dev.hubpilot.link.bukkit;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class PublicLinkBootstrap {
   private static final String CHANNEL = "hubpilot:auth";

   private PublicLinkBootstrap() {
   }

   public static void attach(JavaPlugin var0) {
      try {
         Bukkit.getMessenger().registerOutgoingPluginChannel(var0, "hubpilot:auth");
         Bukkit.getScheduler().runTaskTimer(var0, () -> heartbeat(var0), 20L, 100L);
      } catch (Throwable var2) {
         var0.getLogger().warning("HubPilot Link component heartbeat unavailable: " + var2.getMessage());
      }
   }

   private static void heartbeat(JavaPlugin var0) {
      for (Player var2 : Bukkit.getOnlinePlayers()) {
         try {
            ByteArrayOutputStream var3 = new ByteArrayOutputStream();

            try (DataOutputStream var4 = new DataOutputStream(var3)) {
               var4.writeUTF("COMPONENT");
               var4.writeUTF(var2.getUniqueId().toString());
               var4.writeUTF("Link");
               var4.writeUTF("1.0.2");
            }

            var2.sendPluginMessage(var0, "hubpilot:auth", var3.toByteArray());
            return;
         } catch (Exception var9) {
         }
      }
   }
}
