package dev.hubpilot.core;

import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent.ServerResult;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Emitter;
import net.kyori.adventure.sound.Sound.Source;
import org.slf4j.Logger;

final class LifecycleManager {
   private final Object plugin;
   private final ProxyServer proxy;
   private final Logger logger;
   private final HubPilotConfig config;
   private final CraftyProvider crafty;
   private final StatusStore status;
   private final StatsStore stats;
   private final ConcurrentHashMap<String, LifecycleManager.StartSession> sessions = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<UUID, LifecycleManager.TransferPermit> transferPermits = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<String, Long> lastActivity = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<String, Set<UUID>> pendingTransfers = new ConcurrentHashMap<>();
   private volatile boolean shuttingDown;

   LifecycleManager(Object var1, ProxyServer var2, Logger var3, HubPilotConfig var4, CraftyProvider var5, StatusStore var6, StatsStore var7) {
      this.plugin = var1;
      this.proxy = var2;
      this.logger = var3;
      this.config = var4;
      this.crafty = var5;
      this.status = var6;
      this.stats = var7;
   }

   void request(Player var1, String var2, String var3) {
      if (var1 != null && var1.isActive()) {
         ManagedServer var4 = this.findServer(var2);
         if (var4 == null) {
            var1.sendPlainMessage("HubPilot does not know a server named '" + var2 + "'.");
         } else if (this.validateAccess(var1, var4)) {
            if (isCurrentServer(var1, var4.velocityServer())) {
               this.send(var1, "already-connected", "&7You are already connected to {server}&7.", var4, Map.of());
            } else {
               this.stats.increment(var4.id(), "requests");
               this.logger.info("HubPilot request: player={}, server={}, source={}", new Object[]{var1.getUsername(), var4.id(), var3});
               StatusStore.ServerStatus var5 = this.status.get(var4.id());
               if (var5.online()) {
                  this.send(var1, "joining", "&7Joining {server}&7...", var4, Map.of());
                  this.connect(var1, var4, 0);
               } else {
                  Optional var6 = this.proxy.getServer(var4.velocityServer());
                  if (var6.isEmpty()) {
                     this.send(var1, "unavailable", "&c{server}&c is not registered on Velocity.", var4, Map.of());
                  } else {
                     ((RegisteredServer)var6.get()).ping().orTimeout(var4.pingTimeoutSeconds(), TimeUnit.SECONDS).whenComplete((var3x, var4x) -> {
                        if (var1.isActive()) {
                           if (var4x == null) {
                              this.status.setState(var4.id(), StatusStore.State.ONLINE);
                              this.connect(var1, var4, 0);
                           } else {
                              this.queueAndStart(var1, var4);
                           }
                        }
                     });
                  }
               }
            }
         }
      }
   }

   void onPreConnect(ServerPreConnectEvent var1) {
      if (!this.shuttingDown && this.config.snapshot().interceptDirectRequests()) {
         Player var3 = var1.getPlayer();
         String var4 = var1.getOriginalServer().getServerInfo().getName();
         LifecycleManager.TransferPermit var5 = this.transferPermits.get(var3.getUniqueId());
         if (var5 != null) {
            if (var5.expiresAt > System.currentTimeMillis() && var5.velocityServer.equalsIgnoreCase(var4)) {
               this.transferPermits.remove(var3.getUniqueId(), var5);
               return;
            }

            this.transferPermits.remove(var3.getUniqueId(), var5);
         }

         ManagedServer var2;
         if ((var2 = this.findServer(var4)) != null) {
            if (!this.validateAccess(var3, var2)) {
               var1.setResult(ServerResult.denied());
            } else if (!var2.provider().equalsIgnoreCase("always-online") && !this.status.get(var2.id()).online()) {
               var1.setResult(ServerResult.denied());
               this.proxy.getScheduler().buildTask(this.plugin, () -> this.request(var3, var2.id(), "direct-server-request")).schedule();
            }
         }
      }
   }

   void onPostConnect(ServerPostConnectEvent var1) {
      Player var2 = var1.getPlayer();
      var2.getCurrentServer().ifPresent(var1x -> {
         ManagedServer var2x = this.findServer(var1x.getServerInfo().getName());
         if (var2x != null) {
            this.lastActivity.put(var2x.id(), System.currentTimeMillis());
            this.status.setState(var2x.id(), StatusStore.State.ONLINE);
         }
      });
      this.removeFromQueues(var2.getUniqueId(), false);
   }

   void onDisconnect(DisconnectEvent var1) {
      this.removeFromQueues(var1.getPlayer().getUniqueId(), true);
   }

   void testStart(Player var1, String var2) {
      ManagedServer var3 = this.findServer(var2);
      if (var3 == null) {
         var1.sendPlainMessage("Unknown HubPilot server: " + var2);
      } else if (!ProviderRegistry.isManaged(var3.provider())) {
         var1.sendPlainMessage(var3.label() + " does not use the Crafty startup provider.");
      } else if (this.status.get(var3.id()).online()) {
         var1.sendPlainMessage(var3.label() + " is already online.");
      } else {
         this.status.setStartup(var3.id(), System.currentTimeMillis(), var3.expectedStartupSeconds());
         var1.sendPlainMessage("Test-starting " + var3.label() + "...");
         this.crafty.start(var3).whenComplete((var3x, var4) -> {
            if (var4 == null && var3x != null && var3x.success()) {
               this.stats.increment(var3.id(), "startup.test-requests");
               var1.sendPlainMessage("Crafty accepted the test-start request for " + var3.label() + ".");
            } else {
               String var5 = var4 != null ? rootMessage(var4) : (var3x == null ? "No Crafty response" : var3x.error());
               this.status.setState(var3.id(), StatusStore.State.OFFLINE);
               var1.sendPlainMessage("Test start failed: " + var5);
            }
         });
      }
   }

   void tick() {
      if (!this.shuttingDown) {
         long var1 = System.currentTimeMillis();
         this.transferPermits.entrySet().removeIf(var2 -> var2.getValue().expiresAt < var1);

         for (LifecycleManager.StartSession var4 : new ArrayList<>(this.sessions.values())) {
            this.tickSession(var4, var1);
         }

         if (this.config.snapshot().idleShutdownEnabled()) {
            this.tickIdleShutdown(var1);
         }
      }
   }

   void shutdown() {
      this.shuttingDown = true;
      this.stats.save();
      this.sessions.clear();
      this.transferPermits.clear();
      this.pendingTransfers.clear();
   }

   int queueSize(String var1) {
      LifecycleManager.StartSession var2 = this.sessions.get(var1.toLowerCase(Locale.ROOT));
      return var2 == null ? 0 : var2.size();
   }

   String describe(String var1) {
      ManagedServer var2 = this.findServer(var1);
      if (var2 == null) {
         return "Unknown server: " + var1;
      } else {
         StatusStore.ServerStatus var3 = this.status.get(var2.id());
         return var2.id()
            + ": state="
            + var3.state()
            + ", players="
            + var3.playersOnline()
            + "/"
            + var3.playersMax()
            + ", backendPing="
            + var3.backendPingMs()
            + "ms, queue="
            + this.queueSize(var2.id())
            + ", provider="
            + var2.provider()
            + ", countdown="
            + var2.countdownSeconds()
            + "s, sound="
            + var2.countdownSound();
      }
   }

   void cancel(Player var1) {
      boolean var2 = this.removeFromQueues(var1.getUniqueId(), true);
      this.send(
         var1,
         var2 ? "request-cancelled" : "no-pending-request",
         var2 ? "&eYour pending HubPilot request was cancelled." : "&7You do not have a pending HubPilot request.",
         null,
         Map.of()
      );
   }

   private void queueAndStart(Player var1, ManagedServer var2) {
      if (!var2.autoStartEnabled()) {
         this.send(var1, "autostart-disabled", "&cAutomatic startup is disabled for {server}&c.", var2, Map.of());
      } else if (var2.provider().equalsIgnoreCase("always-online")) {
         this.send(var1, "unavailable", "&c{server}&c is offline and has no startup provider.", var2, Map.of());
      } else if (!ProviderRegistry.isManaged(var2.provider())) {
         this.send(var1, "unavailable", "&c" + ProviderMessages.unavailable(var2.provider()), var2, Map.of());
      } else {
         LifecycleManager.StartSession var3 = new LifecycleManager.StartSession(var2);
         LifecycleManager.StartSession var4 = this.sessions.putIfAbsent(var2.id(), var3);
         if (var4 == null) {
            var3.add(var1);
            this.status.setQueue(var2.id(), var3.size());
            this.status.setStartup(var2.id(), var3.startedAt, var2.expectedStartupSeconds());
            int var5 = var3.position(var1.getUniqueId());
            this.send(var1, "starting", "&eStarting {server}&e...", var2, this.queueValues(var5, var3.size()));
            this.send(var1, "queue-joined", "&7You are queue position &f{position}&7 of &f{queue_size}&7.", var2, this.queueValues(var5, var3.size()));
            var3.rememberPosition(var1.getUniqueId(), var5);
            this.stats.increment(var2.id(), "startup.requests");
            this.startCrafty(var3);
         } else {
            boolean var7 = var4.add(var1);
            this.status.setQueue(var2.id(), var4.size());
            int var6 = var4.position(var1.getUniqueId());
            if (var7) {
               this.send(var1, "already-starting", "&e{server}&e is already starting.", var2, this.queueValues(var6, var4.size()));
               this.send(var1, "queue-joined", "&7You are queue position &f{position}&7 of &f{queue_size}&7.", var2, this.queueValues(var6, var4.size()));
               var4.rememberPosition(var1.getUniqueId(), var6);
            } else {
               this.send(
                  var1,
                  "already-queued",
                  "&7You are already queue position &f{position}&7 of &f{queue_size}&7 for {server}&7.",
                  var2,
                  this.queueValues(var6, var4.size())
               );
            }
         }
      }
   }

   private void startCrafty(LifecycleManager.StartSession var1) {
      this.crafty.start(var1.server).whenComplete((var2, var3) -> {
         if (var3 == null && var2 != null && var2.success()) {
            var1.startAccepted = true;
            this.logger.info("Crafty accepted start request for {}", new Object[]{var1.server.id()});
         } else {
            String var4 = var3 != null ? rootMessage(var3) : (var2 == null ? "No Crafty response" : var2.error());
            this.failStart(var1, var4);
         }
      });
   }

   private void tickSession(LifecycleManager.StartSession var1, long var2) {
      if (this.sessions.get(var1.server.id()) == var1) {
         int var4 = var1.size();
         this.status.setQueue(var1.server.id(), var4);
         if (var4 == 0 && var1.server.stopWhenQueueEmpty()) {
            this.stopAndRemove(var1, "queue became empty while starting");
         } else {
            StatusStore.ServerStatus var5 = this.status.get(var1.server.id());
            if (var1.phase == LifecycleManager.Phase.STARTING) {
               if (var2 - var1.startedAt > var1.server.startupTimeoutSeconds() * 1000L) {
                  this.failStart(var1, "startup timed out after " + var1.server.startupTimeoutSeconds() + " seconds");
               } else {
                  if (var5.online()) {
                     var1.phase = LifecycleManager.Phase.COUNTDOWN;
                     var1.readyAt = var2;
                     var1.countdownEndsAt = var2 + var1.server.countdownSeconds() * 1000L;
                     var1.lastDisplayedSecond = Integer.MIN_VALUE;
                     this.stats.increment(var1.server.id(), "startup.successes");
                     this.stats.add(var1.server.id(), "startup.total-seconds", Math.max(0L, (var2 - var1.startedAt) / 1000L));
                     this.broadcast(var1, "ready", "&a{server}&a is ready.", Map.of());
                     this.logger
                        .info(
                           "{} became ready in {} seconds; queued players={}; countdown={}s; sound={}; volume={}; pitch={}",
                           new Object[]{
                              var1.server.id(),
                              (var2 - var1.startedAt) / 1000L,
                              var4,
                              var1.server.countdownSeconds(),
                              var1.server.countdownSound(),
                              var1.server.countdownVolume(),
                              var1.server.pitchStyle()
                           }
                        );
                     if (var1.server.countdownSeconds() == 0) {
                        this.finishCountdown(var1);
                     } else {
                        var1.lastDisplayedSecond = var1.server.countdownSeconds();
                        this.announceCountdown(var1, var1.server.countdownSeconds());
                     }
                  }
               }
            } else {
               if (var1.phase == LifecycleManager.Phase.COUNTDOWN) {
                  int var6 = (int)Math.max(0.0, Math.ceil((var1.countdownEndsAt - var2) / 1000.0));
                  if (var6 != var1.lastDisplayedSecond) {
                     var1.lastDisplayedSecond = var6;
                     this.announceCountdown(var1, var6);
                  }

                  if (var2 >= var1.countdownEndsAt) {
                     this.finishCountdown(var1);
                  }
               }
            }
         }
      }
   }

   private void announceCountdown(LifecycleManager.StartSession var1, int var2) {
      if (var2 > 0) {
         boolean var3 = var1.server.announceEverySecond() || var2 <= 5 || var2 == 10 || var2 == 30 || var2 == 60;
         Optional<String> var4 = QueueMessages.render(this.config, "countdown", var1.server.countdownMessage(), var1.server, Map.of("seconds", Integer.toString(var2)));
         String var5 = var4.orElse("");

         for (UUID var7 : var1.playerIds()) {
            this.proxy
               .getPlayer(var7)
               .filter(InboundConnection::isActive)
               .ifPresent(
                  var5x -> {
                     float var6 = pitch(var1.server.pitchStyle(), var1.server.countdownSeconds(), var2);
                     boolean var7x = this.sendCountdownFeedback(var5x, var1, var5, var3, var6);
                     if (!var7x) {
                        try {
                           if (!var5.isBlank()) {
                              var5x.sendActionBar(QueueMessages.component(var5));
                           }
                        } catch (RuntimeException var10) {
                           if (!var1.displayWarningLogged) {
                              var1.displayWarningLogged = true;
                              this.logger.warn("Could not display the countdown for {}: {}", new Object[]{var1.server.id(), rootMessage(var10)});
                           }
                        }

                        if (var3 && var1.server.countdownSound() != null && !var1.server.countdownSound().equalsIgnoreCase("none")) {
                           try {
                              var5x.playSound(
                                 Sound.sound(Key.key(var1.server.countdownSound()), Source.MASTER, var1.server.countdownVolume(), var6), Emitter.self()
                              );
                           } catch (RuntimeException var9) {
                              if (!var1.soundWarningLogged) {
                                 var1.soundWarningLogged = true;
                                 this.logger
                                    .warn(
                                       "Could not play countdown sound '{}' for {}: {}",
                                       new Object[]{var1.server.countdownSound(), var1.server.id(), rootMessage(var9)}
                                    );
                              }
                           }
                        }
                     }

                     if (var1.server.announceEverySecond() && !var5.isBlank()) {
                        var5x.sendMessage(QueueMessages.component(var5));
                     }
                  }
               );
         }
      }
   }

   private boolean sendCountdownFeedback(Player var1, LifecycleManager.StartSession var2, String var3, boolean var4, float var5) {
      try {
         Optional var7 = var1.getCurrentServer();
         if (var7.isEmpty()) {
            return false;
         } else {
            ByteArrayOutputStream var8 = new ByteArrayOutputStream();

            try (DataOutputStream var9 = new DataOutputStream(var8)) {
               var9.writeUTF("COUNTDOWN_FEEDBACK");
               var9.writeUTF(var3);
               var9.writeBoolean(var4);
               var9.writeUTF(var2.server.countdownSound() == null ? "none" : var2.server.countdownSound());
               var9.writeFloat(var2.server.countdownVolume());
               var9.writeFloat(var5);
            }

            Object var19 = var7.get();
            Method var10 = null;

            for (Method var14 : var19.getClass().getMethods()) {
               Class[] var15;
               if (var14.getName().equals("sendPluginMessage")
                  && var14.getParameterCount() == 2
                  && (var15 = var14.getParameterTypes())[1].equals(byte[].class)
                  && var15[0].isInstance(HubPilotCorePlugin.CONTROL_CHANNEL)) {
                  var10 = var14;
                  break;
               }
            }

            if (var10 == null) {
               return false;
            } else {
               Object var20 = HubPilotReflect.invokeAccessible(var10, var19, new Object[]{HubPilotCorePlugin.CONTROL_CHANNEL, var8.toByteArray()});
               Boolean var6;
               return !(var20 instanceof Boolean) || (var6 = (Boolean)var20);
            }
         }
      } catch (Throwable var18) {
         if (!var2.feedbackWarningLogged) {
            var2.feedbackWarningLogged = true;
            this.logger.warn("Could not send Paper countdown feedback for {}: {}", new Object[]{var2.server.id(), rootMessage(var18)});
         }

         return false;
      }
   }

   private void finishCountdown(LifecycleManager.StartSession var1) {
      if (this.sessions.remove(var1.server.id(), var1)) {
         this.status.setQueue(var1.server.id(), 0);

         for (UUID var3 : var1.playerIds()) {
            this.proxy.getPlayer(var3).filter(InboundConnection::isActive).ifPresent(var2 -> {
               this.send(var2, "joining", "&7Joining {server}&7...", var1.server, Map.of());
               this.connect(var2, var1.server, 0);
            });
         }
      }
   }

   private void connect(Player var1, ManagedServer var2, int var3) {
      this.pendingTransfers.computeIfAbsent(var2.id(), var0 -> ConcurrentHashMap.newKeySet()).add(var1.getUniqueId());
      Optional var4 = this.proxy.getServer(var2.velocityServer());
      if (var4.isEmpty()) {
         this.send(var1, "unavailable", "&c{server}&c is not registered on Velocity.", var2, Map.of());
         this.stats.increment(var2.id(), "joins.failed");
         this.finishPendingTransfer(var2.id(), var1.getUniqueId());
      } else {
         this.transferPermits.put(var1.getUniqueId(), new LifecycleManager.TransferPermit(var2.velocityServer(), System.currentTimeMillis() + 10000L));
         var1.createConnectionRequest((RegisteredServer)var4.get())
            .connect()
            .whenComplete(
               (var5, var6) -> {
                  boolean var7 = var6 == null && var5 != null && var5.isSuccessful();
                  if (var7) {
                     this.stats.increment(var2.id(), "joins.success");
                     this.lastActivity.put(var2.id(), System.currentTimeMillis());
                     this.finishPendingTransfer(var2.id(), var1.getUniqueId());
                  } else {
                     this.transferPermits.remove(var1.getUniqueId());
                     if (!var1.isActive()) {
                        this.finishPendingTransfer(var2.id(), var1.getUniqueId());
                     } else {
                        if (var3 < var2.retryCount()) {
                           int var9 = var3 + 1;
                           this.send(
                              var1,
                              "retrying",
                              "&eConnection failed. Retrying in {delay}s ({attempt}/{max})...",
                              var2,
                              Map.of(
                                 "delay",
                                 Integer.toString(var2.retryDelaySeconds()),
                                 "attempt",
                                 Integer.toString(var9),
                                 "max",
                                 Integer.toString(var2.retryCount())
                              )
                           );
                           this.proxy.getScheduler().buildTask(this.plugin, () -> {
                              if (var1.isActive()) {
                                 this.connect(var1, var2, var9);
                              } else {
                                 this.finishPendingTransfer(var2.id(), var1.getUniqueId());
                              }
                           }).delay(Duration.ofSeconds(var2.retryDelaySeconds())).schedule();
                        } else {
                           this.stats.increment(var2.id(), "joins.failed");
                           this.send(var1, "connection-failed", "&cCould not connect to {server}&c.", var2, Map.of());
                           this.finishPendingTransfer(var2.id(), var1.getUniqueId());
                           if (var2.stopAfterFailure()
                              && !this.hasPendingTransfers(var2.id())
                              && ((RegisteredServer)var4.get()).getPlayersConnected().isEmpty()) {
                              this.stopServer(var2, "all connection attempts failed");
                           }
                        }
                     }
                  }
               }
            );
      }
   }

   private void failStart(LifecycleManager.StartSession var1, String var2) {
      if (this.sessions.remove(var1.server.id(), var1)) {
         this.status.setQueue(var1.server.id(), 0);
         this.status.setState(var1.server.id(), StatusStore.State.OFFLINE);
         this.stats.increment(var1.server.id(), "startup.failures");
         this.broadcast(var1, "start-failed", "&c{server}&c failed to start: {error}", Map.of("error", var2));
         this.logger.warn("HubPilot could not start {}: {}", new Object[]{var1.server.id(), var2});
         if (var1.server.stopAfterFailure()) {
            this.stopServer(var1.server, "failed startup cleanup");
         }
      }
   }

   private void tickIdleShutdown(long var1) {
      for (ManagedServer var4 : this.config.snapshot().servers().values()) {
         Optional var5;
         if (!this.isConfiguredHub(var4)
            && ProviderRegistry.isManaged(var4.provider())
            && var4.idleShutdownMinutes() > 0
            && this.status.get(var4.id()).online()
            && !(var5 = this.proxy.getServer(var4.velocityServer())).isEmpty()) {
            if (((RegisteredServer)var5.get()).getPlayersConnected().isEmpty() && this.queueSize(var4.id()) <= 0 && !this.hasPendingTransfers(var4.id())) {
               long var6 = this.lastActivity.computeIfAbsent(var4.id(), var2 -> var1);
               if (var1 - var6 >= var4.idleShutdownMinutes() * 60000L) {
                  this.lastActivity.put(var4.id(), var1);
                  this.stopServer(var4, "idle for " + var4.idleShutdownMinutes() + " minutes");
               }
            } else {
               this.lastActivity.put(var4.id(), var1);
            }
         }
      }
   }

   private boolean isConfiguredHub(ManagedServer var1) {
      HubPilotConfig.Snapshot var2 = this.config.snapshot();
      if (HubProtection.matches(var1, var2.hubServer())) {
         return true;
      } else {
         ManagedServer var3 = var2.servers().get(var1.id().toLowerCase(Locale.ROOT));
         return HubProtection.matches(var3, var2.hubServer());
      }
   }

   private void stopAndRemove(LifecycleManager.StartSession var1, String var2) {
      if (this.sessions.remove(var1.server.id(), var1)) {
         this.status.setQueue(var1.server.id(), 0);
         this.stopServer(var1.server, var2);
      }
   }

   private void stopServer(ManagedServer var1, String var2) {
      if (!this.isConfiguredHub(var1)) {
         Optional var3 = this.proxy.getServer(var1.velocityServer());
         if (var3.isPresent() && !((RegisteredServer)var3.get()).getPlayersConnected().isEmpty()) {
            this.logger.info("Refusing to stop {} because players are connected", new Object[]{var1.id()});
         } else if (ProviderRegistry.isManaged(var1.provider())) {
            this.status.setState(var1.id(), StatusStore.State.STOPPING);
            this.logger.info("Stopping {} ({})", new Object[]{var1.id(), var2});
            this.crafty.stop(var1).whenComplete((var2x, var3x) -> {
               if (var3x == null && var2x != null && var2x.success()) {
                  this.stats.increment(var1.id(), "stops");
                  this.status.setState(var1.id(), StatusStore.State.OFFLINE);
               } else {
                  String var4 = var3x != null ? rootMessage(var3x) : (var2x == null ? "No Crafty response" : var2x.error());
                  this.logger.warn("Could not stop {}: {}", new Object[]{var1.id(), var4});
               }
            });
         }
      }
   }

   private boolean validateAccess(Player var1, ManagedServer var2) {
      if (var2.maintenance()) {
         this.send(var1, "maintenance", "&c{server}&c is currently in maintenance mode.", var2, Map.of());
         return false;
      } else if (var2.permission() != null && !var2.permission().isBlank() && !var1.hasPermission(var2.permission())) {
         this.send(var1, "missing-permission", "&cYou do not have permission to join {server}&c.", var2, Map.of());
         return false;
      } else {
         LifecycleManager.VersionCheck var3;
         if (var2.strictVersion()
            && var2.requiredVersion() != null
            && !var2.requiredVersion().equalsIgnoreCase("any")
            && !(var3 = checkProtocolVersion(var1, var2.requiredVersion())).compatible()) {
            this.send(
               var1,
               "wrong-version",
               "&c{server}&c requires Minecraft {required}. Your client supports {current}.",
               var2,
               Map.of("required", var2.requiredVersion(), "current", var3.displayName())
            );
            return false;
         } else {
            return true;
         }
      }
   }

   private ManagedServer findServer(String var1) {
      if (var1 == null) {
         return null;
      } else {
         HubPilotConfig.Snapshot var2 = this.config.snapshot();
         ManagedServer var3 = var2.servers().get(var1.toLowerCase(Locale.ROOT));
         if (var3 != null) {
            return var3;
         } else {
            for (ManagedServer var5 : var2.servers().values()) {
               if (var5.velocityServer().equalsIgnoreCase(var1)) {
                  return var5;
               }
            }

            return null;
         }
      }
   }

   private void finishPendingTransfer(String var1, UUID var2) {
      Set var3 = this.pendingTransfers.get(var1);
      if (var3 != null) {
         var3.remove(var2);
         if (var3.isEmpty()) {
            this.pendingTransfers.remove(var1, var3);
         }
      }
   }

   private boolean hasPendingTransfers(String var1) {
      Set var2 = this.pendingTransfers.get(var1);
      return var2 != null && !var2.isEmpty();
   }

   private boolean removeFromQueues(UUID var1, boolean var2) {
      boolean var3 = false;

      for (LifecycleManager.StartSession var5 : new ArrayList<>(this.sessions.values())) {
         if (var5.remove(var1)) {
            var3 = true;
            this.status.setQueue(var5.server.id(), var5.size());
            this.notifyQueuePositions(var5);
            if (var2 && var5.size() == 0 && var5.server.stopWhenQueueEmpty()) {
               this.stopAndRemove(var5, "last queued player left");
            }
         }
      }

      return var3;
   }

   private void notifyQueuePositions(LifecycleManager.StartSession var1) {
      int var2 = var1.size();

      for (UUID var4 : var1.playerIds()) {
         int var5 = var1.position(var4);
         if (var5 != var1.lastPosition(var4)) {
            this.proxy
               .getPlayer(var4)
               .filter(InboundConnection::isActive)
               .ifPresent(
                  var4x -> this.send(
                     var4x,
                     "queue-position",
                     "&7You are now queue position &f{position}&7 of &f{queue_size}&7 for {server}&7.",
                     var1.server,
                     this.queueValues(var5, var2)
                  )
               );
            var1.rememberPosition(var4, var5);
         }
      }
   }

   private void broadcast(LifecycleManager.StartSession var1, String var2, String var3, Map<String, String> var4) {
      for (UUID var6 : var1.playerIds()) {
         this.proxy.getPlayer(var6).filter(InboundConnection::isActive).ifPresent(var5 -> this.send(var5, var2, var3, var1.server, var4));
      }
   }

   private boolean send(Player var1, String var2, String var3, ManagedServer var4, Map<String, String> var5) {
      return QueueMessages.send(var1, this.config, var2, var3, var4, var5);
   }

   private Map<String, String> queueValues(int var1, int var2) {
      return Map.of("position", Integer.toString(var1), "queue_size", Integer.toString(var2));
   }

   private static boolean isCurrentServer(Player var0, String var1) {
      return var0.getCurrentServer().map(var1x -> var1x.getServerInfo().getName().equalsIgnoreCase(var1)).orElse(false);
   }

   private static LifecycleManager.VersionCheck checkProtocolVersion(Player var0, String var1) {
      ProtocolVersion var3 = var0.getProtocolVersion();
      LinkedHashSet<String> var4 = new LinkedHashSet<>();

      try {
         Object var2 = HubPilotReflect.invokeAccessible(var3.getClass().getMethod("getVersionsSupportedBy"), var3, new Object[0]);
         if (var2 instanceof Iterable) {
            for (Object var7 : (Iterable)var2) {
               if (var7 != null && !String.valueOf(var7).isBlank()) {
                  var4.add(String.valueOf(var7));
               }
            }
         }
      } catch (ReflectiveOperationException var9) {
      }

      if (var4.isEmpty()) {
         try {
            Object var10 = HubPilotReflect.invokeAccessible(var3.getClass().getMethod("getName"), var3, new Object[0]);
            if (var10 != null && !String.valueOf(var10).isBlank()) {
               var4.add(String.valueOf(var10));
            }
         } catch (ReflectiveOperationException var8) {
         }
      }

      String var11 = normalizeVersion(var1);
      boolean var12 = var4.stream().map(LifecycleManager::normalizeVersion).anyMatch(var11::equalsIgnoreCase);
      String var13 = var4.isEmpty() ? "an unknown version" : String.join(" / ", var4);
      return new LifecycleManager.VersionCheck(var12, var13);
   }

   private static String normalizeVersion(String var0) {
      return var0 == null ? "" : var0.toLowerCase(Locale.ROOT).replace("minecraft", "").trim();
   }

   private static float pitch(String var0, int var1, int var2) {
      int var4 = Math.max(0, var1 - var2);
      String var5 = var0 == null ? "rising" : var0.toLowerCase(Locale.ROOT);

      return switch (var5) {
         case "falling" -> Math.max(0.5F, 1.6F - var4 * 0.12F);
         case "flat", "none" -> 1.0F;
         default -> Math.min(2.0F, 0.65F + var4 * 0.14F);
      };
   }

   private static String rootMessage(Throwable var0) {
      Throwable var1 = var0;

      while (var1.getCause() != null) {
         var1 = var1.getCause();
      }

      return var1.getMessage() == null ? var1.getClass().getSimpleName() : var1.getMessage();
   }

   private static enum Phase {
      STARTING,
      COUNTDOWN;
   }

   private static final class StartSession {
      final ManagedServer server;
      final long startedAt = System.currentTimeMillis();
      final LinkedHashSet<UUID> queue = new LinkedHashSet<>();
      final Map<UUID, Integer> lastPositions = new HashMap<>();
      volatile LifecycleManager.Phase phase = LifecycleManager.Phase.STARTING;
      volatile boolean startAccepted;
      volatile long readyAt;
      volatile long countdownEndsAt;
      volatile int lastDisplayedSecond = Integer.MIN_VALUE;
      volatile boolean displayWarningLogged;
      volatile boolean soundWarningLogged;
      volatile boolean feedbackWarningLogged;

      StartSession(ManagedServer var1) {
         this.server = var1;
      }

      synchronized boolean add(Player var1) {
         return this.queue.add(var1.getUniqueId());
      }

      synchronized boolean remove(UUID var1) {
         this.lastPositions.remove(var1);
         return this.queue.remove(var1);
      }

      synchronized int size() {
         return this.queue.size();
      }

      synchronized List<UUID> playerIds() {
         return List.copyOf(this.queue);
      }

      synchronized int position(UUID var1) {
         int var2 = 1;

         for (UUID var4 : this.queue) {
            if (var4.equals(var1)) {
               return var2;
            }

            var2++;
         }

         return 0;
      }

      synchronized void rememberPosition(UUID var1, int var2) {
         this.lastPositions.put(var1, var2);
      }

      synchronized int lastPosition(UUID var1) {
         return this.lastPositions.getOrDefault(var1, 0);
      }
   }

   private record TransferPermit(String velocityServer, long expiresAt) {
   }

   private record VersionCheck(boolean compatible, String displayName) {
   }
}
