package dev.hubpilot.hub.bridge;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.config.Destination;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Iterator;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;

public final class HubPilotRequestSender {
   public static final String CHANNEL = "hubpilot:control";
   public static final String STATUS_CHANNEL = "hubpilot:status";
   public static final String TELEMETRY_CHANNEL = "hubpilot:telemetry";
   private final HubPilotHubPlugin plugin;
   private final HubPilotFeedbackListener listener;
   private final HubPilotTelemetryListener telemetryListener;

   public HubPilotRequestSender(HubPilotHubPlugin var1) {
      this.plugin = var1;
      this.listener = new HubPilotFeedbackListener(var1);
      this.telemetryListener = new HubPilotTelemetryListener(var1);
      Messenger var2 = var1.getServer().getMessenger();
      var1.getServer().getMessenger().registerOutgoingPluginChannel(var1, "hubpilot:control");
      var1.getServer().getMessenger().registerOutgoingPluginChannel(var1, "hubpilot:status");
      var1.getServer().getMessenger().registerOutgoingPluginChannel(var1, "hubpilot:telemetry");
      this.registerIncomingCompat(var2, "hubpilot:control", this.listener);
      this.registerIncomingCompat(var2, "hubpilot:status", this.listener);
      this.registerIncomingCompat(var2, "hubpilot:telemetry", this.telemetryListener);
      SettingsSyncSender.register(this);
   }

   private void registerIncomingCompat(Object var1, String var2, Object var3) {
      try {
         Method var4 = null;

         for (Method var8 : var1.getClass().getMethods()) {
            if (var8.getName().equals("registerIncomingPluginChannel") && var8.getParameterCount() == 3) {
               Class[] var9 = var8.getParameterTypes();
               if (var9[1] == String.class && var9[0].isInstance(this.plugin) && var9[2].isInstance(var3)) {
                  var4 = var8;
                  break;
               }
            }
         }

         if (var4 == null) {
            throw new NoSuchMethodException("registerIncomingPluginChannel");
         } else {
            var4.invoke(var1, this.plugin, var2, var3);
         }
      } catch (ReflectiveOperationException var10) {
         throw new IllegalStateException("Could not register HubPilot incoming channel " + var2, var10);
      }
   }

   public void requestStatusSnapshot(Player var1) {
      SettingsSyncSender.send(this, var1);
      if (var1 != null) {
         try {
            this.sendSimple(var1, "hubpilot:status", "STATUS_SYNC_REQUEST");
            this.sendSimple(var1, "hubpilot:telemetry", "TELEMETRY_SYNC_REQUEST");
         } catch (IOException var3) {
            this.plugin.getLogger().warning("Could not request HubPilot live status: " + var3.getMessage());
         }
      }
   }

   private void sendSimple(Player var1, String var2, String var3) throws IOException {
      ByteArrayOutputStream var4 = new ByteArrayOutputStream();

      try (DataOutputStream var5 = new DataOutputStream(var4)) {
         var5.writeUTF(var3);
      }

      var1.sendPluginMessage(this.plugin, var2, var4.toByteArray());
   }

   public void requestStatusAnyPlayer() {
      try {
         Iterator var1 = this.plugin.getServer().getOnlinePlayers().iterator();
         if (var1.hasNext()) {
            Player var2 = (Player)var1.next();
            this.requestStatusSnapshot(var2);
            return;
         }
      } catch (RuntimeException var3) {
      }
   }

   public void testStart(Player var1, Destination var2) {
      try {
         ByteArrayOutputStream var3 = new ByteArrayOutputStream();

         try (DataOutputStream var4 = new DataOutputStream(var3)) {
            var4.writeUTF("TEST_START");
            var4.writeUTF(var2.id());
            var4.writeUTF(var2.statusTarget());
         }

         var1.sendPluginMessage(this.plugin, "hubpilot:control", var3.toByteArray());
      } catch (IOException var9) {
         var1.sendMessage("§cCould not send the HubPilot test-start request: " + var9.getMessage());
      }
   }
}
