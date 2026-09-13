package dev.hubpilot.hub.adminui;

import dev.hubpilot.hub.HubPilotHubPlugin;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class UiPromptManager implements Listener {
   private final HubPilotHubPlugin plugin;
   private final Map<UUID, UiPromptManager.Type> sessions = new ConcurrentHashMap<>();

   UiPromptManager(HubPilotHubPlugin var1) {
      this.plugin = var1;
   }

   public void begin(Player var1, UiPromptManager.Type var2) {
      this.sessions.put(var1.getUniqueId(), var2);
      var1.closeInventory();
      var1.sendMessage("§eType the new value in chat, or §ccancel§e.");
   }

   @EventHandler
   public void chat(AsyncPlayerChatEvent var1) {
      UiPromptManager.Type var2 = this.sessions.get(var1.getPlayer().getUniqueId());
      if (var2 != null) {
         var1.setCancelled(true);
         String var3 = var1.getMessage().trim();
         Bukkit.getScheduler().runTask(this.plugin, () -> this.apply(var1.getPlayer(), var2, var3));
      }
   }

   private void apply(Player var1, UiPromptManager.Type var2, String var3) {
      if (var3.equalsIgnoreCase("cancel")) {
         this.sessions.remove(var1.getUniqueId());
         var1.openInventory(ThemeMenuBuilder.build(this.plugin));
      } else if (!var3.isEmpty() && var3.length() <= 120) {
         String var4 = switch (var2) {
            case GUI_TITLE -> "gui-title";
            case NAVIGATOR_NAME -> "compass.name";
            case EDITOR_NAME -> "admin-item.name";
         };
         this.plugin.getConfig().set(var4, var3);
         this.plugin.saveConfig();
         this.plugin.reloadEverything();
         this.sessions.remove(var1.getUniqueId());
         var1.sendMessage("§aUpdated setting.");
         var1.openInventory(ThemeMenuBuilder.build(this.plugin));
      } else {
         var1.sendMessage("§cValue must be 1-120 characters.");
      }
   }

   public static enum Type {
      GUI_TITLE,
      NAVIGATOR_NAME,
      EDITOR_NAME;
   }
}
