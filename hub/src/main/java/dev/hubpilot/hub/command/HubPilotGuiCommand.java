/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 */
package dev.hubpilot.hub.command;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.adminui.AdminHomeBuilder;
import dev.hubpilot.hub.adminui.DiagnosticsMenuBuilder;
import dev.hubpilot.hub.adminui.HubPilotAdminRuntime;
import dev.hubpilot.hub.adminui.NavigatorMenuBuilder;
import dev.hubpilot.hub.gui.AdminMenuBuilder;
import dev.hubpilot.hub.gui.DestinationMenuBuilder;
import dev.hubpilot.hub.publicapi.PublicHubBootstrap;
import dev.hubpilot.hub.publicapi.PublicHubCommands;
import dev.hubpilot.hub.util.MenuItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class HubPilotGuiCommand
implements CommandExecutor,
TabCompleter {
    private static final String VERSION = "1.0.2";
    private final HubPilotHubPlugin plugin;

    public HubPilotGuiCommand(HubPilotHubPlugin hubPilotHubPlugin) {
        this.plugin = hubPilotHubPlugin;
    }

    public boolean onCommandLegacy(CommandSender commandSender, Command command, String string, String[] stringArray) {
        String string2;
        if (stringArray.length == 0) {
            if (commandSender instanceof Player) {
                Player player = (Player)commandSender;
                player.openInventory(DestinationMenuBuilder.build(this.plugin, 0));
            } else {
                this.help(commandSender);
            }
            return true;
        }
        switch (string2 = stringArray[0].toLowerCase(Locale.ROOT)) {
            case "help": 
            case "?": {
                this.help(commandSender);
                break;
            }
            case "version": {
                commandSender.sendMessage("\u00a7bHubPilot Hub \u00a7f1.0.2");
                break;
            }
            case "open": 
            case "menu": {
                if (commandSender instanceof Player) {
                    Player player = (Player)commandSender;
                    player.openInventory(DestinationMenuBuilder.build(this.plugin, 0));
                    break;
                }
                commandSender.sendMessage("\u00a7cOnly players can open the selector.");
                break;
            }
            case "admin": 
            case "settings": {
                if (!this.admin(commandSender)) break;
                if (commandSender instanceof Player) {
                    Player player = (Player)commandSender;
                    player.openInventory(AdminHomeBuilder.build(this.plugin));
                    break;
                }
                commandSender.sendMessage("\u00a7cOnly players can open the admin GUI.");
                break;
            }
            case "servers": {
                if (!this.admin(commandSender) || !(commandSender instanceof Player)) break;
                Player player = (Player)commandSender;
                player.openInventory(AdminMenuBuilder.build(this.plugin, 0));
                break;
            }
            case "navigator": {
                if (!this.admin(commandSender) || !(commandSender instanceof Player)) break;
                Player player = (Player)commandSender;
                player.openInventory(NavigatorMenuBuilder.build(this.plugin));
                break;
            }
            case "diagnostics": 
            case "doctor": {
                if (!this.admin(commandSender) || !(commandSender instanceof Player)) break;
                Player player = (Player)commandSender;
                player.openInventory(DiagnosticsMenuBuilder.build(this.plugin));
                break;
            }
            case "editor": 
            case "adminitem": {
                Player player;
                if (!this.admin(commandSender) || (player = this.resolveTarget(commandSender, stringArray)) == null) break;
                dev.hubpilot.hub.publicapi.AdminItemManager.toggle(this.plugin, player);
                break;
            }
            case "compass": {
                Player player;
                if (!this.admin(commandSender) || (player = this.resolveTarget(commandSender, stringArray)) == null) break;
                HubPilotAdminRuntime.navigator(this.plugin).ensure(player, true);
                commandSender.sendMessage("\u00a7aGave/repaired navigator for " + player.getName() + ".");
                break;
            }
            case "reload": {
                if (!this.admin(commandSender)) break;
                this.plugin.reloadEverything();
                commandSender.sendMessage("\u00a7aHubPilot Hub configuration reloaded.");
                break;
            }
            default: {
                commandSender.sendMessage("\u00a7cUnknown subcommand: " + stringArray[0]);
                this.help(commandSender);
            }
        }
        return true;
    }

    private boolean admin(CommandSender commandSender) {
        if (PublicHubBootstrap.canAdmin(commandSender)) {
            return true;
        }
        commandSender.sendMessage("\u00a7cYou do not have permission.");
        return false;
    }

    private void help(CommandSender commandSender) {
        commandSender.sendMessage("\u00a7b\u00a7lHubPilot Hub \u00a7f1.0.2");
        commandSender.sendMessage("\u00a7e/hp gui open \u00a77- open server selector");
        commandSender.sendMessage("\u00a7e/hp gui help \u00a77- show help");
        commandSender.sendMessage("\u00a7e/hp gui version \u00a77- show version");
        if (PublicHubBootstrap.canAdmin(commandSender)) {
            commandSender.sendMessage("\u00a76/hp gui admin \u00a77- admin dashboard");
            commandSender.sendMessage("\u00a76/hp gui servers \u00a77- destination list");
            commandSender.sendMessage("\u00a76/hp gui navigator \u00a77- navigator settings");
            commandSender.sendMessage("\u00a76/hp gui diagnostics \u00a77- Paper-side diagnostics");
            commandSender.sendMessage("\u00a76/hp adminitem \u00a77- show/hide your Hub admin item");
            commandSender.sendMessage("\u00a76/hp gui compass [player] \u00a77- give/repair navigator");
            commandSender.sendMessage("\u00a76/hp gui reload \u00a77- reload Paper settings");
            commandSender.sendMessage("\u00a77Velocity lifecycle commands: \u00a7f/hp help");
        }
    }

    private Player resolveTarget(CommandSender commandSender, String[] stringArray) {
        if (stringArray.length >= 2) {
            Player player = Bukkit.getPlayerExact((String)stringArray[1]);
            if (player == null) {
                commandSender.sendMessage("\u00a7cThat player is not online.");
            }
            return player;
        }
        if (commandSender instanceof Player) {
            Player player = (Player)commandSender;
            return player;
        }
        commandSender.sendMessage("\u00a7cSpecify an online player name.");
        return null;
    }

    public List<String> onTabComplete(CommandSender commandSender, Command command, String string, String[] stringArray) {
        if (stringArray.length == 1) {
            ArrayList<String> arrayList = new ArrayList<String>(List.of("open", "help", "version"));
            if (PublicHubBootstrap.canAdmin(commandSender)) {
                arrayList.addAll(List.of("admin", "servers", "navigator", "diagnostics", "adminitem", "editor", "compass", "reload"));
            }
            return HubPilotGuiCommand.filter(arrayList, stringArray[0]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> list, String string) {
        String string3 = string.toLowerCase(Locale.ROOT);
        return list.stream().filter(string2 -> string2.startsWith(string3)).toList();
    }

    public boolean onCommand(CommandSender commandSender, Command command, String string, String[] stringArray) {
        if (PublicHubCommands.handle(this.plugin, commandSender, stringArray)) {
            return true;
        }
        return this.onCommandLegacy(commandSender, command, string, stringArray);
    }
}
