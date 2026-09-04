/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.player.PlayerInteractEntityEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 */
package dev.hubpilot.interact;

import dev.hubpilot.interact.HubPilotInteractPlugin;
import dev.hubpilot.interact.InteractionStore;
import java.io.IOException;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

final class InteractionListener
implements Listener {
    private final HubPilotInteractPlugin plugin;

    InteractionListener(HubPilotInteractPlugin hubPilotInteractPlugin) {
        this.plugin = hubPilotInteractPlugin;
    }

    @EventHandler
    public void entity(PlayerInteractEntityEvent playerInteractEntityEvent) {
        if (playerInteractEntityEvent.getHand() != org.bukkit.inventory.EquipmentSlot.HAND || this.plugin.editor().holding(playerInteractEntityEvent.getPlayer())) return;
        Player player = playerInteractEntityEvent.getPlayer();
        HubPilotInteractPlugin.Pending pending = this.plugin.pending().get(player.getUniqueId());
        try {
            if (pending != null && pending.type() == HubPilotInteractPlugin.BindType.ENTITY) {
                if (!PublicInteractGate.canManage(player)) { this.plugin.pending().remove(player.getUniqueId()); return; }
                this.plugin.store().bindEntity(playerInteractEntityEvent.getRightClicked().getUniqueId(), pending.destination());
                this.plugin.pending().remove(player.getUniqueId());
                player.sendMessage("\u00a7aEntity linked to \u00a7f" + pending.destination() + "\u00a7a.");
                playerInteractEntityEvent.setCancelled(true);
                return;
            }
        }
        catch (IOException iOException) {
            player.sendMessage("\u00a7cCould not save entity binding: " + iOException.getMessage());
            return;
        }
        String string = this.plugin.store().entity(playerInteractEntityEvent.getRightClicked().getUniqueId());
        if (string != null) {
            playerInteractEntityEvent.setCancelled(true);
            this.plugin.request(player, string);
        }
    }

    @EventHandler
    public void block(PlayerInteractEvent playerInteractEvent) {
        if (playerInteractEvent.getHand() != org.bukkit.inventory.EquipmentSlot.HAND || this.plugin.editor().holding(playerInteractEvent.getPlayer())) return;
        if (playerInteractEvent.getAction() != Action.RIGHT_CLICK_BLOCK || playerInteractEvent.getClickedBlock() == null) {
            return;
        }
        Player player = playerInteractEvent.getPlayer();
        Block block = playerInteractEvent.getClickedBlock();
        HubPilotInteractPlugin.Pending pending = this.plugin.pending().get(player.getUniqueId());
        try {
            if (pending != null && pending.type() == HubPilotInteractPlugin.BindType.SIGN) {
                if (!PublicInteractGate.canManage(player)) { this.plugin.pending().remove(player.getUniqueId()); return; }
                if (!block.getType().name().contains("SIGN")) {
                    player.sendMessage("\u00a7eRight-click a sign to complete the binding.");
                    return;
                }
                this.plugin.store().bindSign(InteractionStore.signKey(block), pending.destination());
                this.plugin.pending().remove(player.getUniqueId());
                player.sendMessage("\u00a7aSign linked to \u00a7f" + pending.destination() + "\u00a7a.");
                playerInteractEvent.setCancelled(true);
                return;
            }
        }
        catch (IOException iOException) {
            player.sendMessage("\u00a7cCould not save sign binding: " + iOException.getMessage());
            return;
        }
        String string = this.plugin.store().sign(InteractionStore.signKey(block));
        if (string != null) {
            playerInteractEvent.setCancelled(true);
            this.plugin.request(player, string);
        }
    }

    @EventHandler
    public void move(PlayerMoveEvent playerMoveEvent) {
        Location location = playerMoveEvent.getTo();
        if (location == null || this.sameBlock(playerMoveEvent.getFrom(), location)) {
            return;
        }
        Player player = playerMoveEvent.getPlayer();
        for (InteractionStore.Portal portal : this.plugin.store().portals()) {
            String string = String.valueOf(player.getUniqueId()) + "|" + portal.name();
            boolean bl = portal.contains(location);
            boolean bl2 = this.plugin.inside().contains(string);
            if (bl && !bl2) {
                this.plugin.inside().add(string);
                this.plugin.request(player, portal.destination());
                continue;
            }
            if (bl || !bl2) continue;
            this.plugin.inside().remove(string);
        }
    }

    private boolean sameBlock(Location location, Location location2) {
        return location != null && location2 != null && location.getWorld() == location2.getWorld() && location.getBlockX() == location2.getBlockX() && location.getBlockY() == location2.getBlockY() && location.getBlockZ() == location2.getBlockZ();
    }
}
