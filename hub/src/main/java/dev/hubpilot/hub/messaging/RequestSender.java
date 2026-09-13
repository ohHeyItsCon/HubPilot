package dev.hubpilot.hub.messaging;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.config.Destination;
import org.bukkit.entity.Player;

public final class RequestSender {
   public static final String CHANNEL = "hubpilot:request";
   private final HubPilotHubPlugin plugin;

   public RequestSender(HubPilotHubPlugin var1) {
      this.plugin = var1;
      var1.getServer().getMessenger().registerOutgoingPluginChannel(var1, "hubpilot:request");
   }

   public void requestJoin(Player var1, Destination var2) {
      ByteArrayDataOutput var3 = ByteStreams.newDataOutput();
      var3.writeUTF(var2.targetType().name());
      var3.writeUTF(var2.target());
      var3.writeUTF(var2.statusTarget());
      var1.sendPluginMessage(this.plugin, "hubpilot:request", var3.toByteArray());
   }
}
