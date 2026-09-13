/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.hubpilot.hub.HubPilotHubPlugin
 *  dev.hubpilot.hub.config.Destination
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Particle
 *  org.bukkit.World
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.PluginCommand
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.scheduler.BukkitTask
 */
package dev.hubpilot.interact;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.config.Destination;
import dev.hubpilot.interact.InteractCommand;
import dev.hubpilot.interact.InteractionListener;
import dev.hubpilot.interact.InteractionStore;
import dev.hubpilot.interact.UnifiedInteractCommand;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class HubPilotInteractPlugin
extends JavaPlugin {
    private HubPilotHubPlugin hub;
    private InteractionStore store;
    private final Map<UUID, Pending> pending = new HashMap<UUID, Pending>();
    private final Map<String, PortalDraft> drafts = new HashMap<String, PortalDraft>();
    private final Map<String, Long> cooldowns = new HashMap<String, Long>();
    private final Set<String> inside = new HashSet<String>();

    private InteractionEditor editor;
    private DestinationLabels labels;

    InteractionEditor editor() { return editor; }

    public void onEnable() {
        this.saveDefaultConfig();
        Plugin plugin = Bukkit.getPluginManager().getPlugin("HubPilot");
        if (!(plugin instanceof HubPilotHubPlugin)) {
            this.getLogger().severe("HubPilot Hub is required; HubPilot Interact will not enable listeners.");
            return;
        }
        this.hub = (HubPilotHubPlugin)plugin;
        this.store = new InteractionStore(this);
        this.store.load();
        this.editor = new InteractionEditor(this);
        this.labels = new DestinationLabels(this);
        Bukkit.getPluginManager().registerEvents(editor, this);
        labels.start();
        InteractCommand interactCommand = new InteractCommand(this);
        PluginCommand pluginCommand = this.getCommand("hpi");
        if (pluginCommand != null) {
            pluginCommand.setExecutor((CommandExecutor)interactCommand);
            pluginCommand.setTabCompleter((TabCompleter)interactCommand);
        }
        Bukkit.getPluginManager().registerEvents((Listener)new InteractionListener(this), (Plugin)this);
        Logger logger = this.getLogger();
        UnifiedInteractCommand.install(this);
        logger.info("HubPilot Interact 1.0.2 enabled.");
    }

    public void onDisable() {
        if (labels != null) labels.close();

    }

    HubPilotHubPlugin hub() {
        return this.hub;
    }

    InteractionStore store() {
        return this.store;
    }

    Map<UUID, Pending> pending() {
        return this.pending;
    }

    Map<String, PortalDraft> drafts() {
        return this.drafts;
    }

    boolean validDestination(String string) {
        return this.hub != null && this.hub.getDestinationStore().find(string) != null;
    }

    void request(Player player, String string) {
        if (editor != null && editor.holding(player)) return;
        Destination destination = this.hub.getDestinationStore().find(string);
        if (destination == null) {
            player.sendMessage("\u00a7cHubPilot destination '" + string + "' no longer exists.");
            return;
        }
        String string2 = String.valueOf(player.getUniqueId()) + "|" + string;
        long l = System.currentTimeMillis();
        long l2 = Math.max(0L, this.getConfig().getLong("portal-cooldown-ms", 3000L));
        Long l3 = this.cooldowns.get(string2);
        if (l3 != null && l - l3 < l2) {
            return;
        }
        this.cooldowns.put(string2, l);
        player.sendMessage("\u00a77Connecting to \u00a7b" + destination.label() + "\u00a77...");
        this.hub.getRequestSender().requestJoin(player, destination);
    }

    Set<String> inside() {
        return this.inside;
    }

    record PortalDraft(Location pos1, Location pos2) {
    }

    record Pending(BindType type, String destination) {
    }

    static enum BindType {
        ENTITY,
        SIGN;

    }
}
