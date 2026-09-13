package dev.hubpilot.core;

import com.velocitypowered.api.proxy.Player;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

final class QueueMessages {
   static final Set<String> EVENTS = Set.of(
      "already-connected",
      "connecting",
      "unavailable",
      "autostart-disabled",
      "starting",
      "already-starting",
      "queue-joined",
      "already-queued",
      "queue-position",
      "ready",
      "countdown",
      "joining",
      "connection-failed",
      "retrying",
      "request-cancelled",
      "no-pending-request",
      "start-failed",
      "maintenance",
      "wrong-version",
      "missing-permission"
   );
   private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

   private QueueMessages() {
   }

   static Optional<String> render(HubPilotConfig var0, String var1, String var2, ManagedServer var3, Map<String, String> var4) {
      Map var5 = var0.snapshot() == null ? Map.of() : var0.snapshot().messages();
      String var6 = normalize(var1);
      String var7 = var3 == null ? "" : "servers." + normalize(var3.id()) + ".events." + var6 + ".";
      String var8 = "events." + var6 + ".";
      String var9 = var3 == null ? "" : "message.server." + normalize(var3.id()) + "." + var6 + ".";
      String var10 = "message.global." + var6 + ".";
      boolean var11 = bool(var5, var9 + "enabled", bool(var5, var7 + "enabled", bool(var5, var10 + "enabled", bool(var5, var8 + "enabled", true))));
      if (!var11) {
         return Optional.empty();
      } else {
         String var12 = first(var5, var9 + "text", var7 + "text", var10 + "text", var8 + "text", var6);
         if (var12 == null) {
            var12 = var2;
         }

         if (var12 != null && !var12.isBlank()) {
            LinkedHashMap<String, String> var13 = new LinkedHashMap<>();
            if (var3 != null) {
               var13.put("server", "&f&l" + var3.label() + "&r");
               var13.put("server_plain", var3.label());
               var13.put("id", var3.id());
            }

            var13.putAll(var4);
            String var14 = var12;

            for (Entry var16 : var13.entrySet()) {
               String var17 = var16.getValue() == null ? "" : (String)var16.getValue();
               var14 = var14.replace("{" + (String)var16.getKey() + "}", var17).replace("<" + (String)var16.getKey() + ">", var17);
            }

            return var14.isBlank() ? Optional.empty() : Optional.of(var14);
         } else {
            return Optional.empty();
         }
      }
   }

   static boolean send(Player var0, HubPilotConfig var1, String var2, String var3, ManagedServer var4, Map<String, String> var5) {
      Optional var6 = render(var1, var2, var3, var4, var5);
      if (!var6.isEmpty() && var0 != null && var0.isActive()) {
         var0.sendMessage(component((String)var6.get()));
         return true;
      } else {
         return false;
      }
   }

   static Component component(String var0) {
      return LEGACY.deserialize(var0 == null ? "" : var0);
   }

   private static String first(Map<String, String> var0, String... var1) {
      for (String var5 : var1) {
         String var6 = (String)var0.get(var5);
         if (var6 != null) {
            return var6;
         }
      }

      return null;
   }

   private static boolean bool(Map<String, String> var0, String var1, boolean var2) {
      String var3 = (String)var0.get(var1);
      return var3 == null ? var2 : Boolean.parseBoolean(var3.trim());
   }

   private static String normalize(String var0) {
      return var0 == null ? "" : var0.trim().toLowerCase(Locale.ROOT).replace('_', '-');
   }
}
