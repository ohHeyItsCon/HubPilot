package dev.hubpilot.core;

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent.ForwardResult;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.scheduler.ScheduledTask;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;

public final class HubPilotCorePlugin {
   static final String VERSION = "1.0.2";
   static final MinecraftChannelIdentifier REQUEST_CHANNEL = MinecraftChannelIdentifier.create("hubpilot", "request");
   static final MinecraftChannelIdentifier LEGACY_REQUEST_CHANNEL = MinecraftChannelIdentifier.create("hubpilot", "request");
   static final MinecraftChannelIdentifier CONTROL_CHANNEL = MinecraftChannelIdentifier.create("hubpilot", "control");
   private final ProxyServer proxy;
   private final Logger logger;
   private final Path dataDirectory;
   private final List<ScheduledTask> tasks = new ArrayList<>();
   private HubPilotConfig config;
   private StatsStore stats;
   private CraftyProvider crafty;
   private StatusStore status;
   private LifecycleManager lifecycle;
   private volatile boolean initialized;

   @Inject
   public HubPilotCorePlugin(ProxyServer var1, Logger var2, @DataDirectory Path var3) {
      this.proxy = var1;
      this.logger = var2;
      this.dataDirectory = var3;
   }

   @Subscribe
   public void onInitialize(ProxyInitializeEvent var1) {
      PublicDefaults.install(this.dataDirectory);
      PublicDefaultsRepair.install(this.dataDirectory, this.logger);

      try {
         this.config = new HubPilotConfig(this.dataDirectory);
         this.config.initialize();
         PublicBootstrap.prepare(this.dataDirectory, this.logger, this.config);
         this.stats = new StatsStore(this.dataDirectory, this.logger);
         this.stats.load();
         this.crafty = new CraftyProvider(this.logger, this.config.snapshot().crafty());
         this.status = new StatusStore(this.proxy, this.logger, this.stats, this.config.snapshot().sharedDirectory());
         this.lifecycle = new LifecycleManager(this, this.proxy, this.logger, this.config, this.crafty, this.status, this.stats);
         TelemetryBootstrap.attach(this);
         this.proxy
            .getChannelRegistrar()
            .register(new ChannelIdentifier[]{REQUEST_CHANNEL, LEGACY_REQUEST_CHANNEL, CONTROL_CHANNEL, MinecraftChannelIdentifier.from("hubpilot:auth")});
         PublicBootstrap.attach(this, this.proxy, this.logger, this.config);
         new HubPilotCommands(this, this.proxy, this.logger, this.config, this.lifecycle, this.crafty, this.status).register();
         this.tasks
            .add(this.proxy.getScheduler().buildTask(this, this.lifecycle::tick).delay(Duration.ofSeconds(1L)).repeat(Duration.ofSeconds(1L)).schedule());
         this.tasks
            .add(
               this.proxy
                  .getScheduler()
                  .buildTask(this, this::refreshStatus)
                  .delay(Duration.ofSeconds(1L))
                  .repeat(Duration.ofSeconds(this.config.snapshot().statusRefreshSeconds()))
                  .schedule()
            );
         this.tasks
            .add(
               this.proxy
                  .getScheduler()
                  .buildTask(this, this::reloadConfigurationQuietly)
                  .delay(Duration.ofSeconds(this.config.snapshot().configReloadSeconds()))
                  .repeat(Duration.ofSeconds(this.config.snapshot().configReloadSeconds()))
                  .schedule()
            );
         this.tasks.add(this.proxy.getScheduler().buildTask(this, this.stats::save).delay(Duration.ofSeconds(60L)).repeat(Duration.ofSeconds(60L)).schedule());
         this.initialized = true;
         this.logger
            .info(
               "HubPilot Core {} enabled with {} managed servers. AutoServer is not required.", new Object[]{"1.0.2", this.config.snapshot().servers().size()}
            );
      } catch (Exception var3) {
         this.logger.error("HubPilot Core could not initialize: {}", new Object[]{var3.getMessage()});
         this.logger.error("HubPilot Core initialization exception", new Object[]{var3});
      }
   }

   @Subscribe(
      order = PostOrder.FIRST
   )
   public void onPreConnect(ServerPreConnectEvent var1) {
      if (this.initialized && this.config.snapshot().lifecycleEnabled()) {
         this.lifecycle.onPreConnect(var1);
      }
   }

   @Subscribe
   public void onPostConnect(ServerPostConnectEvent var1) {
      if (this.initialized) {
         this.lifecycle.onPostConnect(var1);
      }
   }

   @Subscribe
   public void onDisconnect(DisconnectEvent var1) {
      if (this.initialized) {
         this.lifecycle.onDisconnect(var1);
      }
   }

   @Subscribe
   public void onPluginMessage(PluginMessageEvent var1) {
      if (!PublicControlGate.dispatch(var1)) {
         DirectAuthHandler.dispatch(var1);
         if (this.initialized) {
            boolean var2 = REQUEST_CHANNEL.equals(var1.getIdentifier()) || LEGACY_REQUEST_CHANNEL.equals(var1.getIdentifier());
            boolean var3 = CONTROL_CHANNEL.equals(var1.getIdentifier());
            if (var2 || var3) {
               var1.setResult(ForwardResult.handled());
               if (var1.getSource() instanceof ServerConnection var4) {
                  Player var13 = var4.getPlayer();
                  if (var13 != null && var13.isActive()) {
                     String var6 = var4.getServerInfo().getName().toLowerCase(Locale.ROOT);
                     if (!this.config.snapshot().trustedRequestServers().contains(var6)) {
                        this.logger.warn("Rejected HubPilot message from untrusted backend {} for player {}", new Object[]{var6, var13.getUsername()});
                     } else if (var3) {
                        this.handleControlMessage(var1, var13, var6);
                     } else {
                        try {
                           try (DataInputStream var7 = new DataInputStream(new ByteArrayInputStream(var1.getData()))) {
                              String var8 = var7.readUTF();
                              String var9 = var7.readUTF();
                              if (var7.available() > 0) {
                                 var7.readUTF();
                              }

                              if (validType(var8) && validTarget(var9)) {
                                 this.lifecycle.request(var13, var9, "plugin-message:" + var8.toLowerCase(Locale.ROOT));
                                 return;
                              }

                              this.logger.warn("Rejected malformed HubPilot request from {}: type={}, target={}", new Object[]{var6, var8, var9});
                           }
                        } catch (Exception var12) {
                           this.logger.warn("Could not read HubPilot request from {}: {}", new Object[]{var6, var12.getMessage()});
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void handleControlMessage(PluginMessageEvent var1, Player var2, String var3) {
      if (!AccessManager.get().isAdminSource(var2)
         && !var2.hasPermission("hubpilot.admin")
         && !var2.hasPermission("hubpilot.admin")
         && !var2.getUsername().equalsIgnoreCase("")) {
         var2.sendPlainMessage("You do not have permission to control HubPilot servers.");
      } else {
         try {
            try (DataInputStream var4 = new DataInputStream(new ByteArrayInputStream(var1.getData()))) {
               String var5 = var4.readUTF();
               if (var4.available() > 0) {
                  var4.readUTF();
               }

               String var6 = var4.available() > 0 ? var4.readUTF() : "";
               if ("TEST_START".equalsIgnoreCase(var5) && validTarget(var6)) {
                  this.lifecycle.testStart(var2, var6);
                  return;
               }

               this.logger.warn("Rejected invalid HubPilot control message from {}: type={}, target={}", new Object[]{var3, var5, var6});
            }
         } catch (Exception var9) {
            var2.sendPlainMessage("Invalid HubPilot control message.");
            this.logger.warn("Could not read HubPilot control message from {}: {}", new Object[]{var3, var9.getMessage()});
         }
      }
   }

   @Subscribe
   public void onShutdown(ProxyShutdownEvent var1) {
      for (ScheduledTask var3 : this.tasks) {
         try {
            var3.cancel();
         } catch (RuntimeException var5) {
         }
      }

      this.tasks.clear();
      if (this.lifecycle != null) {
         this.lifecycle.shutdown();
      } else if (this.stats != null) {
         this.stats.save();
      }
   }

   private void refreshStatus() {
      if (this.initialized) {
         HubPilotConfig.Snapshot var1 = this.config.snapshot();
         this.status.refresh(var1.servers());
         this.proxy.getScheduler().buildTask(this, () -> this.status.writeFiles(this.config.snapshot().servers())).delay(Duration.ofSeconds(1L)).schedule();
      }
   }

   private void reloadConfigurationQuietly() {
      if (this.initialized) {
         try {
            this.config.reload();
            this.crafty.update(this.config.snapshot().crafty());
            this.status.updateSharedDirectory(this.config.snapshot().sharedDirectory());
         } catch (Exception var2) {
            this.logger.warn("HubPilot configuration reload rejected; keeping the previous settings: {}", new Object[]{var2.getMessage()});
         }
      }
   }

   private static boolean validType(String var0) {
      if (var0 == null) {
         return false;
      } else {
         String var1 = var0.toUpperCase(Locale.ROOT);
         return var1.equals("SERVER") || var1.equals("WORLD") || var1.equals("REQUEST") || var1.equals("JOIN");
      }
   }

   private static boolean validTarget(String var0) {
      return var0 != null && var0.matches("[A-Za-z0-9_-]{1,64}");
   }
}
