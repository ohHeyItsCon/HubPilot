package dev.hubpilot.hub;

import dev.hubpilot.hub.admin.PromptManager;
import dev.hubpilot.hub.adminui.AdminHomeListener;
import dev.hubpilot.hub.adminui.DiagnosticsMenuListener;
import dev.hubpilot.hub.adminui.HelpMenuListener;
import dev.hubpilot.hub.adminui.HubPilotAdminRuntime;
import dev.hubpilot.hub.adminui.NavigatorMenuListener;
import dev.hubpilot.hub.adminui.NavigatorProtectionListener;
import dev.hubpilot.hub.adminui.TelemetryMenuListener;
import dev.hubpilot.hub.adminui.ThemeMenuListener;
import dev.hubpilot.hub.bridge.HubPilotMenuListener;
import dev.hubpilot.hub.bridge.HubPilotPromptManager;
import dev.hubpilot.hub.bridge.HubPilotRequestSender;
import dev.hubpilot.hub.bridge.HubPilotStore;
import dev.hubpilot.hub.bridge.MessageMenuListener;
import dev.hubpilot.hub.bridge.MessagePromptManager;
import dev.hubpilot.hub.command.HubPilotGuiCommand;
import dev.hubpilot.hub.config.DestinationStore;
import dev.hubpilot.hub.config.MenuConfig;
import dev.hubpilot.hub.listener.AdminEntryListener;
import dev.hubpilot.hub.listener.AdminMenuListener;
import dev.hubpilot.hub.listener.DestinationMenuListener;
import dev.hubpilot.hub.listener.ItemInteractListener;
import dev.hubpilot.hub.messaging.RequestSender;
import dev.hubpilot.hub.publicapi.PublicHubBootstrap;
import dev.hubpilot.hub.publicapi.ServerControlUi;
import dev.hubpilot.hub.publicapi.UnifiedGuiBridge;
import dev.hubpilot.hub.status.StatusStore;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class HubPilotHubPlugin extends JavaPlugin {
   private volatile MenuConfig menuConfig;
   private volatile DestinationStore destinationStore;
   private volatile StatusStore statusStore;
   private volatile HubPilotStore hubPilotStore;
   private RequestSender requestSender;
   private PromptManager promptManager;
   private HubPilotRequestSender hubPilotRequestSender;
   private HubPilotPromptManager hubPilotPromptManager;

   public void onEnable() {
      this.saveDefaultConfig();
      this.initializeStores();
      this.requestSender = new RequestSender(this);
      this.promptManager = new PromptManager(this);
      this.hubPilotRequestSender = new HubPilotRequestSender(this);
      this.hubPilotPromptManager = new HubPilotPromptManager(this);
      MessagePromptManager var1 = new MessagePromptManager(this);
      PluginManager var2 = this.getServer().getPluginManager();
      var2.registerEvents(new ItemInteractListener(this), this);
      var2.registerEvents(new DestinationMenuListener(this), this);
      var2.registerEvents(new AdminMenuListener(this), this);
      var2.registerEvents(new AdminEntryListener(this), this);
      var2.registerEvents(this.promptManager, this);
      var2.registerEvents(new HubPilotMenuListener(this), this);
      var2.registerEvents(this.hubPilotPromptManager, this);
      var2.registerEvents(var1, this);
      var2.registerEvents(new MessageMenuListener(this, var1), this);
      var2.registerEvents(new AdminHomeListener(this), this);
      var2.registerEvents(new NavigatorMenuListener(this), this);
      var2.registerEvents(new ThemeMenuListener(this), this);
      var2.registerEvents(new TelemetryMenuListener(this), this);
      var2.registerEvents(new DiagnosticsMenuListener(this), this);
      var2.registerEvents(new HelpMenuListener(this), this);
      var2.registerEvents(HubPilotAdminRuntime.prompts(this), this);
      NavigatorProtectionListener var3 = new NavigatorProtectionListener(this);
      var2.registerEvents(var3, this);
      HubPilotGuiCommand var4 = new HubPilotGuiCommand(this);
      PluginCommand var5 = this.getCommand("hp");
      if (var5 != null) {
         var5.setExecutor(var4);
         var5.setTabCompleter(var4);
      }

      this.getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
         this.destinationStore.reloadIfChanged();
         this.statusStore.reloadIfChanged();
         this.hubPilotStore.reloadIfChanged();
      }, 40L, 40L);
      this.getServer().getScheduler().runTaskTimer(this, var3::periodicRepair, 100L, 100L);
      this.getServer().getScheduler().runTaskTimer(this, () -> this.hubPilotRequestSender.requestStatusAnyPlayer(), 20L, 20L);
      this.getLogger()
         .info("HubPilot Hub 1.0.2 enabled with " + this.destinationStore.all().size() + " destinations. Admin dashboard and navigator controls enabled.");
      PublicHubBootstrap.attach(this);
      UnifiedGuiBridge.attach(this);
      ServerControlUi.attach(this);
   }

   public synchronized void reloadEverything() {
      this.reloadConfig();
      this.initializeStores();
   }

   private void initializeStores() {
      this.menuConfig = MenuConfig.load(this);
      Path var1 = this.resolveSharedDirectory(this.menuConfig.sharedDirectory());
      DestinationStore var2 = new DestinationStore(this, var1.resolve("destinations.properties"));
      var2.initialize();
      StatusStore var3 = new StatusStore(this, var1.resolve("status.properties"));
      var3.reloadIfChanged();
      HubPilotStore var4 = new HubPilotStore(var1.resolve("hubpilot.properties"), this.getLogger());
      var4.initialize();
      this.destinationStore = var2;
      this.statusStore = var3;
      this.hubPilotStore = var4;
   }

   private Path resolveSharedDirectory(Path var1) {
      try {
         Files.createDirectories(var1);
         if (Files.isWritable(var1)) {
            return var1;
         }
      } catch (Exception var5) {
      }

      Path var2 = this.getDataFolder().toPath().resolve("shared");

      try {
         Files.createDirectories(var2);
      } catch (Exception var4) {
         throw new IllegalStateException("Could not create HubPilot shared directory", var4);
      }

      this.getLogger().warning("Shared directory " + var1 + " is unavailable; using " + var2);
      return var2;
   }

   public MenuConfig getMenuConfig() {
      return this.menuConfig;
   }

   public DestinationStore getDestinationStore() {
      return this.destinationStore;
   }

   public StatusStore getStatusStore() {
      return this.statusStore;
   }

   public RequestSender getRequestSender() {
      return this.requestSender;
   }

   public PromptManager getPromptManager() {
      return this.promptManager;
   }

   public HubPilotStore getHubPilotStore() {
      return this.hubPilotStore;
   }

   public HubPilotRequestSender getHubPilotRequestSender() {
      return this.hubPilotRequestSender;
   }

   public HubPilotPromptManager getHubPilotPromptManager() {
      return this.hubPilotPromptManager;
   }
}
