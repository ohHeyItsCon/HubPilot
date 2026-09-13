package dev.hubpilot.hub.admin;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.config.Destination;
import dev.hubpilot.hub.gui.AdminEntryBuilder;
import dev.hubpilot.hub.gui.AdminMenuBuilder;
import dev.hubpilot.hub.gui.IconPickerBuilder;
import dev.hubpilot.hub.util.IconMaterials;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class PromptManager implements Listener {
   private final HubPilotHubPlugin plugin;
   private final Map<UUID, PromptManager.Session> sessions = new ConcurrentHashMap<>();

   public PromptManager(HubPilotHubPlugin var1) {
      this.plugin = var1;
   }

   public void startAdd(Player var1) {
      this.sessions.put(var1.getUniqueId(), new PromptManager.AddSession());
      var1.closeInventory();
      var1.sendMessage("§6New destination setup. Type §ccancel §6at any time.");
      var1.sendMessage("§eEnter a short ID (example: survival):");
   }

   public void startFieldEdit(Player var1, Destination var2, PromptManager.Field var3) {
      this.sessions.put(var1.getUniqueId(), new PromptManager.FieldSession(var2.id(), var3));
      var1.closeInventory();
      var1.sendMessage("§eEnter the new " + var3.name().toLowerCase(Locale.ENGLISH).replace('_', ' ') + " or type §ccancel§e:");
      if (var3 == PromptManager.Field.ICON) {
         var1.sendMessage("§7Example: §fDIAMOND_SWORD§7. Use the icon browser for common items.");
      }
   }

   @EventHandler
   public void onChat(AsyncPlayerChatEvent var1) {
      PromptManager.Session var2 = this.sessions.get(var1.getPlayer().getUniqueId());
      if (var2 != null) {
         var1.setCancelled(true);
         String var3 = var1.getMessage().trim();
         Bukkit.getScheduler().runTask(this.plugin, () -> this.handleInput(var1.getPlayer(), var2, var3));
      }
   }

   private void handleInput(Player var1, PromptManager.Session var2, String var3) {
      if (this.sessions.containsKey(var1.getUniqueId())) {
         if (var3.equalsIgnoreCase("cancel")) {
            this.sessions.remove(var1.getUniqueId());
            var1.sendMessage("§cEdit cancelled.");
            var1.openInventory(AdminMenuBuilder.build(this.plugin, 0));
         } else {
            try {
               boolean var4 = var2.accept(var1, var3);
               if (var4) {
                  this.sessions.remove(var1.getUniqueId());
               }
            } catch (IllegalArgumentException var5) {
               var1.sendMessage("§c" + var5.getMessage());
               var2.repeatPrompt(var1);
            } catch (IOException var6) {
               this.sessions.remove(var1.getUniqueId());
               var1.sendMessage("§cCould not save: " + var6.getMessage());
               this.plugin.getLogger().warning("Could not save destination: " + var6.getMessage());
            }
         }
      }
   }

   private static String requireText(String var0, String var1) {
      if (var0 != null && !var0.trim().isEmpty()) {
         if (var0.length() > 120) {
            throw new IllegalArgumentException(var1 + " is too long.");
         } else {
            return var0.trim();
         }
      } else {
         throw new IllegalArgumentException(var1 + " cannot be empty.");
      }
   }

   private final class AddSession implements PromptManager.Session {
      private int step;
      private String id;
      private String label;
      private String target;

      @Override
      public boolean accept(Player var1, String var2) throws IOException {
         switch (this.step) {
            case 0:
               this.id = Destination.normalizeId(var2);
               if (PromptManager.this.plugin.getDestinationStore().find(this.id) != null) {
                  throw new IllegalArgumentException("That ID already exists.");
               }

               this.step++;
               var1.sendMessage("§eEnter the display name:");
               break;
            case 1:
               this.label = PromptManager.requireText(var2, "Display name");
               this.step++;
               var1.sendMessage("§eEnter the Velocity server target (example: survival):");
               break;
            case 2:
               this.target = Destination.validateTarget(var2);
               Destination var3 = new Destination(
                  this.id, this.label, "", "Unknown", "GRASS_BLOCK", Destination.TargetType.SERVER, this.target, this.target, true
               );
               PromptManager.this.plugin.getDestinationStore().add(var3);
               var1.sendMessage("§aDestination created. Choose its icon.");
               var1.openInventory(IconPickerBuilder.build(var3, 0));
               return true;
            default:
               throw new IllegalStateException("Unknown setup step");
         }

         return false;
      }

      @Override
      public void repeatPrompt(Player var1) {
         String var2 = switch (this.step) {
            case 0 -> "Enter a short ID:";
            case 1 -> "Enter the display name:";
            default -> "Enter the Velocity server target:";
         };
         var1.sendMessage("§e" + var2);
      }
   }

   public static enum Field {
      LABEL,
      DESCRIPTION,
      SOFTWARE,
      ICON,
      TARGET,
      STATUS_TARGET;
   }

   private final class FieldSession implements PromptManager.Session {
      private final String destinationId;
      private final PromptManager.Field field;

      private FieldSession(String nullx, PromptManager.Field nullxx) {
         this.destinationId = nullx;
         this.field = nullxx;
      }

      @Override
      public boolean accept(Player var1, String var2) throws IOException {
         Destination var3 = PromptManager.this.plugin.getDestinationStore().find(this.destinationId);
         if (var3 == null) {
            throw new IllegalArgumentException("That destination no longer exists.");
         } else {
            Destination var4 = switch (this.field) {
               case LABEL -> var3.withLabel(PromptManager.requireText(var2, "Display name"));
               case DESCRIPTION -> var3.withDescription(var2.equalsIgnoreCase("none") ? "" : PromptManager.requireText(var2, "Description"));
               case SOFTWARE -> var3.withSoftware(PromptManager.requireText(var2, "Software"));
               case ICON -> var3.withIconMaterial(IconMaterials.canonical(var2));
               case TARGET -> var3.withTarget(Destination.validateTarget(var2));
               case STATUS_TARGET -> var3.withStatusTarget(var2.equalsIgnoreCase("same") ? var3.target() : Destination.validateTarget(var2));
            };
            PromptManager.this.plugin.getDestinationStore().replace(var4);
            var1.sendMessage("§aUpdated " + this.field.name().toLowerCase(Locale.ENGLISH).replace('_', ' ') + ".");
            var1.openInventory(AdminEntryBuilder.build(var4));
            return true;
         }
      }

      @Override
      public void repeatPrompt(Player var1) {
         var1.sendMessage("§eTry again or type §ccancel§e.");
      }
   }

   private interface Session {
      boolean accept(Player var1, String var2) throws IOException;

      void repeatPrompt(Player var1);
   }
}
