package dev.hubpilot.hub.publicapi;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.command.HubPilotGuiCommand;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import org.bukkit.entity.Player;

public final class UnifiedGuiBridge {
   private static volatile HubPilotHubPlugin plugin;

   private UnifiedGuiBridge() {
   }

   public static void attach(HubPilotHubPlugin var0) {
      plugin = var0;
   }

   public static boolean handle(String var0, Player var1, byte[] var2) {
      if ("hubpilot:control".equalsIgnoreCase(var0) && var1 != null && var2 != null) {
         try {
            boolean var9;
            try (DataInputStream var3 = new DataInputStream(new ByteArrayInputStream(var2))) {
               String var4 = var3.readUTF();
               if (!"OPEN".equals(var4) || var3.available() <= 0) {
                  return false;
               }

               String var5 = var3.readUTF();
               if (!var5.startsWith("GUI:")) {
                  return false;
               }

               HubPilotHubPlugin var6 = plugin;
               if (var6 == null) {
                  return true;
               }

               String var7 = var5.substring(4);
               String[] var8 = var7.isEmpty() ? new String[0] : var7.split("\\u001f", -1);
               new HubPilotGuiCommand(var6).onCommandLegacy(var1, null, "hp", var8);
               var9 = true;
            }

            return var9;
         } catch (Exception var12) {
            var1.sendMessage("§cHubPilot could not open that menu. Check the hub console for the exact error.");
            var12.printStackTrace();
            return false;
         }
      } else {
         return false;
      }
   }
}
