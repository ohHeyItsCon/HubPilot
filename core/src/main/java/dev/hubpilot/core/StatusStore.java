package dev.hubpilot.core;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.proxy.server.ServerPing.Players;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

final class StatusStore {
   private final ProxyServer proxy;
   private final Logger logger;
   private final StatsStore stats;
   private final ConcurrentHashMap<String, StatusStore.MutableStatus> statuses = new ConcurrentHashMap<>();
   private volatile Path sharedDirectory;

   StatusStore(ProxyServer var1, Logger var2, StatsStore var3, String var4) {
      this.proxy = var1;
      this.logger = var2;
      this.stats = var3;
      this.updateSharedDirectory(var4);
   }

   void updateSharedDirectory(String var1) {
      this.sharedDirectory = Path.of(var1);
   }

   StatusStore.ServerStatus get(String var1) {
      StatusStore.MutableStatus var2 = this.statuses.get(var1.toLowerCase());
      return var2 == null ? StatusStore.ServerStatus.unknown() : var2.snapshot();
   }

   void setState(String var1, StatusStore.State var2) {
      StatusStore.MutableStatus var3 = this.statuses.computeIfAbsent(var1.toLowerCase(), var0 -> new StatusStore.MutableStatus());
      synchronized (var3) {
         if (var3.state != var2) {
            if (var2 == StatusStore.State.ONLINE && var3.state != StatusStore.State.ONLINE) {
               var3.onlineSince = System.currentTimeMillis();
               var3.lastStarted = var3.onlineSince;
            }

            var3.state = var2;
         }

         var3.lastChecked = System.currentTimeMillis();
      }
   }

   void setQueue(String var1, int var2) {
      StatusStore.MutableStatus var3 = this.statuses.computeIfAbsent(var1.toLowerCase(), var0 -> new StatusStore.MutableStatus());
      var3.queueSize = Math.max(0, var2);
   }

   void setStartup(String var1, long var2, int var4) {
      StatusStore.MutableStatus var5 = this.statuses.computeIfAbsent(var1.toLowerCase(), var0 -> new StatusStore.MutableStatus());
      synchronized (var5) {
         var5.state = StatusStore.State.STARTING;
         var5.startRequestedAt = var2;
         var5.expectedStartupSeconds = var4;
         var5.lastChecked = System.currentTimeMillis();
      }
   }

   void refresh(Map<String, ManagedServer> var1) {
      for (ManagedServer var3 : var1.values()) {
         if (var3.maintenance()) {
            this.setState(var3.id().toLowerCase(), StatusStore.State.MAINTENANCE);
         } else {
            Optional var4 = this.proxy.getServer(var3.velocityServer());
            if (var4.isEmpty()) {
               StatusStore.MutableStatus var5 = this.statuses.computeIfAbsent(var3.id().toLowerCase(), var0 -> new StatusStore.MutableStatus());
               synchronized (var5) {
                  var5.state = StatusStore.State.UNKNOWN;
                  var5.lastChecked = System.currentTimeMillis();
                  var5.lastError = "Velocity server is not registered";
               }
            } else {
               RegisteredServer var9 = (RegisteredServer)var4.get();
               long var6 = System.nanoTime();
               var9.ping()
                  .orTimeout(var3.pingTimeoutSeconds(), TimeUnit.SECONDS)
                  .whenComplete((var5x, var6x) -> this.applyPing(var3, var9, var5x, var6x, var6));
            }
         }
      }
   }

   private void applyPing(ManagedServer var1, RegisteredServer var2, ServerPing var3, Throwable var4, long var5) {
      StatusStore.MutableStatus var7 = this.statuses.computeIfAbsent(var1.id().toLowerCase(), var0 -> new StatusStore.MutableStatus());
      synchronized (var7) {
         var7.lastChecked = System.currentTimeMillis();
         if (var4 != null) {
            if (var7.state != StatusStore.State.STARTING && var7.state != StatusStore.State.STOPPING) {
               var7.state = StatusStore.State.OFFLINE;
            }

            var7.backendPingMs = -1L;
            var7.playersOnline = 0;
            var7.playersMax = -1;
            var7.lastError = rootMessage(var4);
         } else {
            long var9 = Math.max(0L, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - var5));
            StatusStore.State var11 = var7.state;
            var7.state = StatusStore.State.ONLINE;
            var7.backendPingMs = var9;
            var7.lastError = "";
            int var12 = var2.getPlayersConnected().size();
            var7.playersOnline = var12;
            var7.playersMax = -1;
            if (var3 != null && var3.getPlayers().isPresent()) {
               Players var13 = (Players)var3.getPlayers().get();
               var7.playersOnline = Math.max(var12, var13.getOnline());
               var7.playersMax = var13.getMax();
            }

            if (var11 != StatusStore.State.ONLINE) {
               var7.onlineSince = var7.lastChecked;
               var7.lastStarted = var7.lastChecked;
            }
         }
      }
   }

   void writeFiles(Map<String, ManagedServer> var1) {
      Properties var2 = new Properties();
      Properties var3 = new Properties();
      long var4 = System.currentTimeMillis();

      for (ManagedServer var7 : var1.values()) {
         StatusStore.ServerStatus var8 = this.get(var7.id());
         writeLegacy(var2, var7.id(), var8);
         if (!var7.velocityServer().equalsIgnoreCase(var7.id())) {
            writeLegacy(var2, var7.velocityServer(), var8);
         }

         String var9 = "server." + var7.id() + ".";
         var3.setProperty(var9 + "state", var8.state().name());
         var3.setProperty(var9 + "backend-ping-ms", Long.toString(var8.backendPingMs()));
         var3.setProperty(var9 + "players-online", Integer.toString(var8.playersOnline()));
         var3.setProperty(var9 + "players-max", Integer.toString(var8.playersMax()));
         var3.setProperty(var9 + "queue-size", Integer.toString(var8.queueSize()));
         var3.setProperty(var9 + "startup-progress", Integer.toString(var8.startupProgress(var4)));
         var3.setProperty(var9 + "average-startup-seconds", Long.toString(this.stats.averageStartupSeconds(var7.id())));
         var3.setProperty(var9 + "total-requests", Long.toString(this.stats.get(var7.id(), "requests")));
         var3.setProperty(var9 + "successful-joins", Long.toString(this.stats.get(var7.id(), "joins.success")));
         var3.setProperty(var9 + "failed-joins", Long.toString(this.stats.get(var7.id(), "joins.failed")));
         var3.setProperty(var9 + "last-error", var8.lastError() == null ? "" : var8.lastError());
      }

      var2.setProperty("generated-at", Long.toString(var4));
      var3.setProperty("meta.generated-at", Long.toString(var4));

      try {
         Files.createDirectories(this.sharedDirectory);
         atomicProperties(this.sharedDirectory.resolve("status.properties"), var2, "HubPilot status compatibility file");
         atomicProperties(this.sharedDirectory.resolve("telemetry.properties"), var3, "HubPilot extended telemetry");
      } catch (IOException var10) {
         this.logger.warn("Could not write HubPilot shared status files in {}: {}", new Object[]{this.sharedDirectory, var10.getMessage()});
      }
   }

   private static void writeLegacy(Properties var0, String var1, StatusStore.ServerStatus var2) {
      String var3 = "status." + var1.toLowerCase() + ".";
      var0.setProperty(var3 + "state", var2.state().name());
      var0.setProperty(var3 + "online-since", Long.toString(var2.onlineSince()));
      var0.setProperty(var3 + "last-started", Long.toString(var2.lastStarted()));
      var0.setProperty(var3 + "last-checked", Long.toString(var2.lastChecked()));
      var0.setProperty(var3 + "players-online", Integer.toString(var2.playersOnline()));
      var0.setProperty(var3 + "players-max", Integer.toString(var2.playersMax()));
   }

   private static void atomicProperties(Path var0, Properties var1, String var2) throws IOException {
      Path var3 = var0.resolveSibling(var0.getFileName() + ".tmp");

      try (OutputStream var4 = Files.newOutputStream(var3, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
         var1.store(var4, var2);
      }

      try {
         Files.move(var3, var0, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (IOException var8) {
         Files.move(var3, var0, StandardCopyOption.REPLACE_EXISTING);
      }
   }

   private static String rootMessage(Throwable var0) {
      Throwable var1 = var0;

      while (var1.getCause() != null) {
         var1 = var1.getCause();
      }

      return var1.getMessage() == null ? var1.getClass().getSimpleName() : var1.getMessage();
   }

   static final class MutableStatus {
      volatile StatusStore.State state = StatusStore.State.UNKNOWN;
      volatile long onlineSince;
      volatile long lastStarted;
      volatile long lastChecked;
      volatile int playersOnline = -1;
      volatile int playersMax = -1;
      volatile long backendPingMs = -1L;
      volatile int queueSize;
      volatile long startRequestedAt;
      volatile int expectedStartupSeconds;
      volatile String lastError = "";

      synchronized StatusStore.ServerStatus snapshot() {
         return new StatusStore.ServerStatus(
            this.state,
            this.onlineSince,
            this.lastStarted,
            this.lastChecked,
            this.playersOnline,
            this.playersMax,
            this.backendPingMs,
            this.queueSize,
            this.startRequestedAt,
            this.expectedStartupSeconds,
            this.lastError
         );
      }
   }

   record ServerStatus(
      StatusStore.State state,
      long onlineSince,
      long lastStarted,
      long lastChecked,
      int playersOnline,
      int playersMax,
      long backendPingMs,
      int queueSize,
      long startRequestedAt,
      int expectedStartupSeconds,
      String lastError
   ) {
      static StatusStore.ServerStatus unknown() {
         return new StatusStore.ServerStatus(StatusStore.State.UNKNOWN, 0L, 0L, 0L, -1, -1, -1L, 0, 0L, 0, "");
      }

      boolean online() {
         return this.state == StatusStore.State.ONLINE;
      }

      int startupProgress(long var1) {
         if (this.state == StatusStore.State.STARTING && this.startRequestedAt > 0L && this.expectedStartupSeconds > 0) {
            long var3 = Math.max(0L, (var1 - this.startRequestedAt) / 1000L);
            return (int)Math.min(99L, var3 * 100L / this.expectedStartupSeconds);
         } else {
            return this.state == StatusStore.State.ONLINE ? 100 : 0;
         }
      }
   }

   static enum State {
      UNKNOWN,
      OFFLINE,
      STARTING,
      ONLINE,
      STOPPING,
      MAINTENANCE;
   }
}
