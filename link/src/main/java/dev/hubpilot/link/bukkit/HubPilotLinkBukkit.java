package dev.hubpilot.link.bukkit;

import com.sun.management.OperatingSystemMXBean;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.util.Collection;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class HubPilotLinkBukkit extends JavaPlugin {
   private static final String CHANNEL = "hubpilot:telemetry";
   private static final int PROTOCOL = 1;
   private static final String VERSION = "1.0.2";

   public void onEnable() {
      this.getServer().getMessenger().registerOutgoingPluginChannel(this, "hubpilot:telemetry");
      this.getServer().getScheduler().runTaskTimer(this, this::heartbeatSafe, 20L, 20L);
      this.getLogger().info("HubPilot Link 1.0.2 enabled. Rich telemetry will be sent while at least one player is connected.");
      PublicLinkBootstrap.attach(this);
      PublicLinkCommand.install(this);
   }

   private void heartbeatSafe() {
      try {
         Collection var1 = this.getServer().getOnlinePlayers();
         if (var1 == null || var1.isEmpty()) {
            return;
         }

         Player var2 = (Player)var1.iterator().next();
         var2.sendPluginMessage(this, "hubpilot:telemetry", this.encode(var1.size()));
      } catch (Throwable var3) {
         this.getLogger().warning("Could not send HubPilot telemetry: " + rootMessage(var3));
      }
   }

   private byte[] encode(int var1) throws Exception {
      Runtime var2 = Runtime.getRuntime();
      long var3 = Math.max(0L, (var2.totalMemory() - var2.freeMemory()) / 1048576L);
      long var5 = Math.max(0L, var2.maxMemory() / 1048576L);
      double var7 = this.currentTps();
      double var9 = this.averageTickTime();
      double var11 = this.processCpuPercent();
      int var13 = this.loadedChunks();
      int var14 = this.entityCount();
      ByteArrayOutputStream var15 = new ByteArrayOutputStream(256);

      try (DataOutputStream var16 = new DataOutputStream(var15)) {
         var16.writeUTF("LINK_HEARTBEAT");
         var16.writeInt(1);
         var16.writeUTF("PAPER");
         var16.writeUTF(this.minecraftVersion());
         var16.writeUTF("1.0.2");
         var16.writeDouble(var7);
         var16.writeDouble(var9);
         var16.writeLong(var3);
         var16.writeLong(var5);
         var16.writeDouble(var11);
         var16.writeInt(var13);
         var16.writeInt(var14);
         var16.writeInt(var1);
         var16.writeBoolean(true);
      }

      return var15.toByteArray();
   }

   private String minecraftVersion() {
      Server var1 = this.getServer();
      String var2 = invokeString(var1, "getMinecraftVersion");
      if (!var2.isBlank()) {
         return var2;
      } else {
         var2 = invokeString(var1, "getBukkitVersion");
         return var2.isBlank() ? "unknown" : var2;
      }
   }

   private double currentTps() {
      try {
         Class var1 = Class.forName("org.bukkit.Bukkit");
         if (var1.getMethod("getTPS").invoke(null) instanceof double[] var3 && var3.length > 0) {
            return sane(var3[0]);
         }
      } catch (Throwable var4) {
      }

      return -1.0;
   }

   private double averageTickTime() {
      try {
         Class var1 = Class.forName("org.bukkit.Bukkit");
         if (var1.getMethod("getAverageTickTime").invoke(null) instanceof Number var3) {
            return sane(var3.doubleValue());
         }
      } catch (Throwable var4) {
      }

      return -1.0;
   }

   private double processCpuPercent() {
      try {
         if (ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean var2) {
            double var3 = var2.getProcessCpuLoad();
            return var3 < 0.0 ? -1.0 : Math.min(100.0, var3 * 100.0);
         }
      } catch (Throwable var5) {
      }

      return -1.0;
   }

   private int loadedChunks() {
      int var1 = 0;

      try {
         for (Object var3 : this.getServer().getWorlds()) {
            Object var4 = var3.getClass().getMethod("getLoadedChunks").invoke(var3);
            if (var4 != null && var4.getClass().isArray()) {
               var1 += Array.getLength(var4);
            }
         }

         return var1;
      } catch (Throwable var5) {
         return -1;
      }
   }

   private int entityCount() {
      int var1 = 0;

      try {
         for (Object var3 : this.getServer().getWorlds()) {
            if (var3.getClass().getMethod("getEntities").invoke(var3) instanceof Collection var5) {
               var1 += var5.size();
            }
         }

         return var1;
      } catch (Throwable var6) {
         return -1;
      }
   }

   private static String invokeString(Object var0, String var1) {
      try {
         Object var2 = var0.getClass().getMethod(var1).invoke(var0);
         return var2 == null ? "" : String.valueOf(var2);
      } catch (Throwable var3) {
         return "";
      }
   }

   private static double sane(double var0) {
      return Double.isFinite(var0) ? var0 : -1.0;
   }

   private static String rootMessage(Throwable var0) {
      Throwable var1 = var0;

      while (var1.getCause() != null && var1.getCause() != var1) {
         var1 = var1.getCause();
      }

      return var1.getMessage() == null ? var1.getClass().getSimpleName() : var1.getMessage();
   }
}
