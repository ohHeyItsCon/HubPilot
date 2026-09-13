package dev.hubpilot.hub.bridge;

import dev.hubpilot.hub.HubPilotHubPlugin;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class MessagePromptManager implements Listener {
   private final HubPilotHubPlugin plugin;
   private final Map<UUID, MessagePromptManager.Session> sessions = new ConcurrentHashMap<>();

   public MessagePromptManager(HubPilotHubPlugin var1) {
      this.plugin = var1;
   }

   public void begin(Player var1, String var2, int var3, MessageEvent var4) {
      this.sessions.put(var1.getUniqueId(), new MessagePromptManager.Session(var2, var3, var4));
      var1.closeInventory();
      var1.sendMessage("§6HubPilot §8» §eType the new §f" + var4.label() + " §emessage in chat.");
      var1.sendMessage("§7Placeholders: §f{" + var4.placeholders().replace(", ", "}, {") + "}");
      var1.sendMessage("§7Use §f& §7color codes. Type §fcancel §7to return.");
   }

   @EventHandler
   public void onChat(AsyncPlayerChatEvent var1) {
      MessagePromptManager.Session var2 = this.sessions.get(var1.getPlayer().getUniqueId());
      if (var2 != null) {
         var1.setCancelled(true);
         String var3 = var1.getMessage();
         Bukkit.getScheduler().runTask(this.plugin, () -> this.save(var1.getPlayer(), var2, var3));
      }
   }

   private void save(Player var1, MessagePromptManager.Session var2, String var3) {
      if (this.sessions.remove(var1.getUniqueId(), var2)) {
         if (!var3.equalsIgnoreCase("cancel")) {
            if (var3.isBlank() || var3.length() > 500) {
               var1.sendMessage("§cMessages must contain 1 to 500 characters.");
               this.sessions.put(var1.getUniqueId(), var2);
               return;
            }

            try {
               this.plugin.getHubPilotStore().setMessageText(var2.destinationId, var2.event, var3);
               var1.sendMessage("§aMessage updated.");
            } catch (IOException var5) {
               var1.sendMessage("§cCould not save the message: " + var5.getMessage());
            }
         }

         var1.openInventory(MessageMenuBuilder.build(this.plugin, var2.destinationId, var2.page));
      }
   }

   private record Session(String destinationId, int page, MessageEvent event) {
   }
}
