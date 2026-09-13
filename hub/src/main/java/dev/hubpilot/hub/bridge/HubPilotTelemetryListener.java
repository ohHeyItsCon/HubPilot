package dev.hubpilot.hub.bridge;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.gui.NavigatorLiveRefresh;
import dev.hubpilot.hub.telemetry.RichTelemetryStore;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public final class HubPilotTelemetryListener implements PluginMessageListener {
   private final HubPilotHubPlugin plugin;

   public HubPilotTelemetryListener(HubPilotHubPlugin var1) {
      this.plugin = var1;
   }

   public void onPluginMessageReceived(String var1, Player var2, byte[] var3) {
      if ("hubpilot:telemetry".equalsIgnoreCase(var1) && var3 != null) {
         try {
            try (DataInputStream var4 = new DataInputStream(new ByteArrayInputStream(var3))) {
               String var5 = var4.readUTF();
               if ("TELEMETRY_SYNC_END".equals(var5)) {
                  NavigatorLiveRefresh.refreshOpen(this.plugin);
                  return;
               }

               if (!"TELEMETRY_ROW".equals(var5)) {
                  return;
               }

               int var6 = var4.readInt();
               if (var6 == 1) {
                  String var7 = var4.readUTF();
                  long var8 = var4.readLong();
                  int var10 = var4.readInt();
                  int var11 = var4.readInt();
                  long var12 = var4.readLong();
                  long var14 = var4.readLong();
                  long var16 = var4.readLong();
                  long var18 = var4.readLong();
                  String var20 = var4.readUTF();
                  var4.readLong();
                  boolean var21 = var4.readBoolean();
                  long var22 = var4.readLong();
                  String var24 = var4.readUTF();
                  String var25 = var4.readUTF();
                  String var26 = var4.readUTF();
                  double var27 = var4.readDouble();
                  double var29 = var4.readDouble();
                  long var31 = var4.readLong();
                  long var33 = var4.readLong();
                  double var35 = var4.readDouble();
                  int var37 = var4.readInt();
                  int var38 = var4.readInt();
                  int var39 = var4.readInt();
                  boolean var40 = var4.readBoolean();
                  RichTelemetryStore.of(this.plugin)
                     .put(
                        var7,
                        new RichTelemetryStore.Snapshot(
                           System.currentTimeMillis(),
                           var8,
                           var10,
                           var11,
                           var12,
                           var14,
                           var16,
                           var18,
                           var20,
                           var21,
                           var22,
                           var24,
                           var25,
                           var26,
                           var27,
                           var29,
                           var31,
                           var33,
                           var35,
                           var37,
                           var38,
                           var39,
                           var40
                        )
                     );
                  return;
               }
            }
         } catch (Exception var43) {
            this.plugin.getLogger().warning("Could not read HubPilot telemetry: " + var43.getMessage());
         }
      }
   }
}
