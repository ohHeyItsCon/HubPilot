/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.PluginCommand
 *  org.bukkit.command.TabCompleter
 */
package dev.hubpilot.interact;

import dev.hubpilot.interact.HubPilotInteractPlugin;
import dev.hubpilot.interact.InteractCommand;
import dev.hubpilot.interact.NpcCommand;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;

final class UnifiedInteractCommand
implements CommandExecutor,
TabCompleter {
    private final InteractCommand interact;
    private final NpcCommand npc;
    private final HubPilotInteractPlugin plugin;

    private UnifiedInteractCommand(HubPilotInteractPlugin hubPilotInteractPlugin) {
        this.plugin = hubPilotInteractPlugin;
        this.interact = new InteractCommand(hubPilotInteractPlugin);
        try {
            Constructor constructor = NpcCommand.class.getDeclaredConstructor(HubPilotInteractPlugin.class);
            constructor.setAccessible(true);
            this.npc = (NpcCommand)constructor.newInstance(new Object[]{hubPilotInteractPlugin});
        }
        catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    static void install(HubPilotInteractPlugin hubPilotInteractPlugin) {
        PluginCommand pluginCommand = hubPilotInteractPlugin.getCommand("hpi");
        if (pluginCommand == null) {
            return;
        }
        UnifiedInteractCommand unifiedInteractCommand = new UnifiedInteractCommand(hubPilotInteractPlugin);
        pluginCommand.setExecutor((CommandExecutor)unifiedInteractCommand);
        pluginCommand.setTabCompleter((TabCompleter)unifiedInteractCommand);
    }

    public boolean onCommand(CommandSender commandSender, Command command, String string, String[] stringArray) {
        if (plugin.editor().command(commandSender, stringArray)) return true;
        if (stringArray != null && stringArray.length > 0 && "npc".equalsIgnoreCase(stringArray[0])) {
            return this.npc.onCommandLegacy(commandSender, command, string, Arrays.copyOfRange(stringArray, 1, stringArray.length));
        }
        return this.interact.onCommandLegacy(commandSender, command, string, stringArray == null ? new String[]{} : stringArray);
    }

    public List<String> onTabComplete(CommandSender commandSender, Command command, String string, String[] stringArray) {
        List<String> extra = plugin.editor().complete(commandSender, stringArray);
        if (extra != null) return extra;
        if (stringArray != null && stringArray.length > 0 && "npc".equalsIgnoreCase(stringArray[0])) {
            return this.npc.onTabComplete(commandSender, command, string, Arrays.copyOfRange(stringArray, 1, stringArray.length));
        }
        if (stringArray != null && stringArray.length == 1) {
            ArrayList<String> arrayList = new ArrayList<String>();
            List<String> list = this.interact.onTabComplete(commandSender, command, string, stringArray);
            if (list != null) {
                arrayList.addAll(list);
            }
            if ("npc".startsWith(stringArray[0].toLowerCase(Locale.ROOT))) {
                arrayList.add("npc");
            }
            return arrayList;
        }
        return this.interact.onTabComplete(commandSender, command, string, stringArray);
    }
}
