package dev.hubpilot.hub.bridge;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.gui.NavigatorLiveRefresh;
import dev.hubpilot.hub.status.ServerStatus;
import dev.hubpilot.hub.util.MenuItems;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.lang.reflect.Method;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public final class HubPilotFeedbackListener implements PluginMessageListener {
   private final HubPilotHubPlugin plugin;

   public HubPilotFeedbackListener(HubPilotHubPlugin var1) {
      this.plugin = var1;
   }

   public void onPluginMessageReceived(String var1, Player var2, byte[] var3) {
      if (var2 != null && var3 != null) {
         if ("hubpilot:status".equalsIgnoreCase(var1)) {
            this.readStatus(var3);
         } else if ("hubpilot:control".equalsIgnoreCase(var1)) {
            this.readControl(var2, var3);
         }
      }
   }

   private void readStatus(byte[] var1) {
      try {
         try (DataInputStream var2 = new DataInputStream(new ByteArrayInputStream(var1))) {
            String var4 = var2.readUTF();
            if ("STATUS_SYNC_END".equals(var4)) {
               NavigatorLiveRefresh.refreshOpen(this.plugin);
               return;
            }

            if ("STATUS_ROW".equals(var4)) {
               String var5 = var2.readUTF();

               ServerStatus.State var3;
               try {
                  var3 = ServerStatus.State.valueOf(var2.readUTF());
               } catch (IllegalArgumentException var15) {
                  var3 = ServerStatus.State.UNKNOWN;
               }

               long var6 = var2.readLong();
               long var8 = var2.readLong();
               long var10 = var2.readLong();
               int var12 = var2.readInt();
               int var13 = var2.readInt();
               var2.readLong();
               var2.readInt();
               var2.readLong();
               var2.readInt();
               var2.readUTF();
               this.plugin.getStatusStore().applyRemote(var5, new ServerStatus(var3, var6, var8, var10, var12, var13));
               return;
            }
         }
      } catch (Exception var17) {
         this.plugin.getLogger().warning("Could not read HubPilot live status: " + var17.getMessage());
      }
   }

   private void readControl(Player var1, byte[] var2) {
      try {
         try (DataInputStream var3 = new DataInputStream(new ByteArrayInputStream(var2))) {
            String var4 = var3.readUTF();
            if ("COUNTDOWN_FEEDBACK".equals(var4)) {
               String var5 = var3.readUTF();
               boolean var6 = var3.readBoolean();
               String var7 = var3.readUTF();
               float var8 = var3.readFloat();
               float var9 = var3.readFloat();
               String var10 = MenuItems.colorize(var5);
               if (!var10.isBlank() && !sendActionBar(var1, var10)) {
                  var1.sendMessage(var10);
               }

               if (var6 && var7 != null && !var7.equalsIgnoreCase("none") && var8 > 0.0F) {
                  this.playSoundCompat(var1, var7, var8, var9);
               }

               return;
            }
         }
      } catch (Exception var13) {
         this.plugin.getLogger().warning("Could not read HubPilot feedback: " + var13.getMessage());
      }
   }

   private void playSoundCompat(Player var1, String var2, float var3, float var4) {
      try {
         Object var5 = var1.getClass().getMethod("getLocation").invoke(var1);
         Method var6 = null;

         for (Method var10 : var1.getClass().getMethods()) {
            Class[] var11;
            if (var10.getName().equals("playSound")
               && var10.getParameterCount() == 4
               && (var11 = var10.getParameterTypes())[1] == String.class
               && (var11[2] == float.class || var11[2] == Float.class)
               && (var11[3] == float.class || var11[3] == Float.class)
               && var11[0].isInstance(var5)) {
               var6 = var10;
               break;
            }
         }

         if (var6 != null) {
            var6.invoke(var1, var5, var2, var3, var4);
         }
      } catch (Exception var12) {
         this.plugin.getLogger().warning("Could not play HubPilot countdown sound " + var2 + ": " + var12.getMessage());
      }
   }

   private static boolean sendActionBar(Player var0, String var1) {
      try {
         var0.getClass().getMethod("sendActionBar", String.class).invoke(var0, var1);
         return true;
      } catch (ReflectiveOperationException var8) {
         try {
            ClassLoader var2 = var0.getClass().getClassLoader();
            Class var3 = Class.forName("net.kyori.adventure.text.Component", true, var2);
            Class var4 = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer", true, var2);
            Object var5 = var4.getMethod("legacySection").invoke(null);
            Object var6 = var4.getMethod("deserialize", String.class).invoke(var5, var1);
            var0.getClass().getMethod("sendActionBar", var3).invoke(var0, var6);
            return true;
         } catch (ReflectiveOperationException var7) {
            return false;
         }
      }
   }
}
