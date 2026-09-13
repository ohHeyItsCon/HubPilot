package dev.hubpilot.core;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent.ForwardResult;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Locale;
import org.slf4j.Logger;

final class TelemetryBridge {
   static final int PROTOCOL = 1;
   static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create("hubpilot", "telemetry");
   private final Object plugin;
   private final ProxyServer proxy;
   private final Logger logger;
   private final HubPilotConfig config;
   private final StatusStore status;
   private final StatsStore stats;
   private final TelemetryStore telemetry = new TelemetryStore();

   TelemetryBridge(Object var1, ProxyServer var2, Logger var3, HubPilotConfig var4, StatusStore var5, StatsStore var6) {
      this.plugin = var1;
      this.proxy = var2;
      this.logger = var3;
      this.config = var4;
      this.status = var5;
      this.stats = var6;
   }

   void register() {
      this.proxy.getChannelRegistrar().register(new ChannelIdentifier[]{CHANNEL});
      this.proxy.getEventManager().register(this.plugin, this);
      this.logger.info("HubPilot telemetry bridge enabled on {}", new Object[]{CHANNEL.getId()});
   }

   @Subscribe
   public void onPluginMessage(PluginMessageEvent var1) {
      SettingsSyncHook.handle(var1);
      StatusSyncHook.handle(var1);
      if (CHANNEL.equals(var1.getIdentifier())) {
         var1.setResult(ForwardResult.handled());
         if (var1.getSource() instanceof ServerConnection var2) {
            byte[] var10 = var1.getData();
            if (var10 != null && var10.length != 0) {
               try (DataInputStream var4 = new DataInputStream(new ByteArrayInputStream(var10))) {
                  String var5 = var4.readUTF();
                  if ("LINK_HEARTBEAT".equals(var5)) {
                     this.readHeartbeat(var2, var4);
                  } else if ("TELEMETRY_SYNC_REQUEST".equals(var5)) {
                     if (this.isTrustedHub(var2)) {
                        this.sendSnapshot(var2);
                     } else {
                        this.logger.warn("Rejected telemetry snapshot request from untrusted backend {}", new Object[]{var2.getServerInfo().getName()});
                     }
                  }
               } catch (Exception var9) {
                  this.logger.warn("Could not process HubPilot telemetry message from {}: {}", new Object[]{var2.getServerInfo().getName(), rootMessage(var9)});
               }
            }
         }
      }
   }

   private void readHeartbeat(ServerConnection var1, DataInputStream var2) throws IOException {
      int var3 = var2.readInt();
      if (var3 != 1) {
         this.logger
            .warn("Ignored HubPilot Link heartbeat from {} using unsupported telemetry protocol {}", new Object[]{var1.getServerInfo().getName(), var3});
      } else {
         String var4 = safe(var2.readUTF(), 32);
         String var5 = safe(var2.readUTF(), 48);
         String var6 = safe(var2.readUTF(), 48);
         double var7 = sane(var2.readDouble(), -1.0, 1000.0);
         double var9 = sane(var2.readDouble(), -1.0, 60000.0);
         long var11 = clamp(var2.readLong(), -1L, 1048576L);
         long var13 = clamp(var2.readLong(), -1L, 1048576L);
         double var15 = sane(var2.readDouble(), -1.0, 100.0);
         int var17 = (int)clamp(var2.readInt(), -1L, 100000000L);
         int var18 = (int)clamp(var2.readInt(), -1L, 100000000L);
         int var19 = (int)clamp(var2.readInt(), -1L, 100000L);
         boolean var20 = var2.readBoolean();
         String var21 = var1.getServerInfo().getName().toLowerCase(Locale.ROOT);
         if (this.isManagedBackend(var21)) {
            this.telemetry
               .update(
                  var21,
                  new TelemetryStore.Snapshot(System.currentTimeMillis(), true, var4, var5, var6, var7, var9, var11, var13, var15, var17, var18, var19, var20)
               );
         }
      }
   }

   private void sendSnapshot(ServerConnection var1) throws IOException {
      long var2 = System.currentTimeMillis();
      Player var4 = var1.getPlayer();
      long var5 = var4 == null ? -1L : var4.getPing();

      for (ManagedServer var8 : this.config.snapshot().servers().values()) {
         StatusStore.ServerStatus var9 = this.status.get(var8.id());
         TelemetryStore.Snapshot var10 = this.telemetry.get(var8.velocityServer());
         ByteArrayOutputStream var11 = new ByteArrayOutputStream();

         try (DataOutputStream var12 = new DataOutputStream(var11)) {
            var12.writeUTF("TELEMETRY_ROW");
            var12.writeInt(1);
            var12.writeUTF(var8.id());
            var12.writeLong(var9.backendPingMs());
            var12.writeInt(var9.queueSize());
            var12.writeInt(var9.startupProgress(var2));
            var12.writeLong(this.stats.averageStartupSeconds(var8.id()));
            var12.writeLong(this.stats.get(var8.id(), "requests"));
            var12.writeLong(this.stats.get(var8.id(), "joins.success"));
            var12.writeLong(this.stats.get(var8.id(), "joins.failed"));
            var12.writeUTF(var9.lastError() == null ? "" : safe(var9.lastError(), 512));
            var12.writeLong(var5);
            var12.writeBoolean(var10.linkPresent());
            var12.writeLong(var10.receivedAt());
            var12.writeUTF(safe(var10.platform(), 32));
            var12.writeUTF(safe(var10.minecraftVersion(), 48));
            var12.writeUTF(safe(var10.linkVersion(), 48));
            var12.writeDouble(var10.tps());
            var12.writeDouble(var10.mspt());
            var12.writeLong(var10.memoryUsedMb());
            var12.writeLong(var10.memoryMaxMb());
            var12.writeDouble(var10.cpuPercent());
            var12.writeInt(var10.loadedChunks());
            var12.writeInt(var10.entities());
            var12.writeInt(var10.players());
            var12.writeBoolean(var10.ready());
         }

         var1.sendPluginMessage(CHANNEL, var11.toByteArray());
      }

      ByteArrayOutputStream var19 = new ByteArrayOutputStream();

      try (DataOutputStream var20 = new DataOutputStream(var19)) {
         var20.writeUTF("TELEMETRY_SYNC_END");
         var20.writeInt(1);
      }

      var1.sendPluginMessage(CHANNEL, var19.toByteArray());
   }

   private boolean isManagedBackend(String var1) {
      return this.config.snapshot().servers().values().stream().anyMatch(var1x -> var1x.velocityServer().equalsIgnoreCase(var1));
   }

   private boolean isTrustedHub(ServerConnection var1) {
      String var2 = var1.getServerInfo().getName().toLowerCase(Locale.ROOT);
      HubPilotConfig.Snapshot var3 = this.config.snapshot();
      return var2.equals(var3.hubServer().toLowerCase(Locale.ROOT)) || var3.trustedRequestServers().contains(var2);
   }

   private static String safe(String var0, int var1) {
      if (var0 == null) {
         return "";
      } else {
         var0 = var0.replace('\u0000', ' ');
         return var0.length() <= var1 ? var0 : var0.substring(0, var1);
      }
   }

   private static long clamp(long var0, long var2, long var4) {
      return Math.max(var2, Math.min(var4, var0));
   }

   private static double sane(double var0, double var2, double var4) {
      return !Double.isFinite(var0) ? -1.0 : Math.max(var2, Math.min(var4, var0));
   }

   private static String rootMessage(Throwable var0) {
      Throwable var1 = var0;

      while (var1.getCause() != null && var1.getCause() != var1) {
         var1 = var1.getCause();
      }

      return var1.getMessage() == null ? var1.getClass().getSimpleName() : var1.getMessage();
   }
}
