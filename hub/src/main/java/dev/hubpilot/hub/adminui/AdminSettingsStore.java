package dev.hubpilot.hub.adminui;

import dev.hubpilot.hub.HubPilotHubPlugin;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Properties;

public final class AdminSettingsStore {
   private final HubPilotHubPlugin plugin;
   private final Path file;
   private final Properties values = new Properties();

   public AdminSettingsStore(HubPilotHubPlugin var1) {
      this.plugin = var1;
      this.file = var1.getDataFolder().toPath().resolve("admin-ui.properties");
      this.load();
   }

   private synchronized void load() {
      this.values.clear();
      if (Files.exists(this.file)) {
         try (BufferedReader var1 = Files.newBufferedReader(this.file, StandardCharsets.UTF_8)) {
            this.values.load(var1);
         } catch (IOException var6) {
            this.plugin.getLogger().warning("Could not load admin-ui.properties: " + var6.getMessage());
         }
      }

      this.defaults();
      this.saveQuietly();
   }

   private void defaults() {
      this.putDefault("navigator.force-on-join", "false");
      this.putDefault("navigator.hotbar-slot", "1");
      this.putDefault("navigator.lock-to-slot", "true");
      this.putDefault("navigator.prevent-drop", "true");
      this.putDefault("navigator.prevent-moving", "true");
      this.putDefault("navigator.prevent-containers", "true");
      this.putDefault("navigator.restore-after-death", "true");
      this.putDefault("navigator.restore-if-missing", "true");
      this.putDefault("navigator.hub-world-only", "false");
      this.putDefault("navigator.hub-world", "world");
      this.putDefault("navigator.require-permission", "false");
      this.putDefault("navigator.permission", "hubpilot.navigator");
      this.putDefault("telemetry.global.description", "true");
      this.putDefault("telemetry.global.software", "true");
      this.putDefault("telemetry.global.type", "true");
      this.putDefault("telemetry.global.status", "true");
      this.putDefault("telemetry.global.players", "true");
      this.putDefault("telemetry.global.time", "true");
      this.putDefault("telemetry.global.action", "true");
      this.putDefault("telemetry.global.backend_ping", "true");
      this.putDefault("telemetry.global.startup_progress", "true");
      this.putDefault("telemetry.global.queue", "false");
      this.putDefault("telemetry.global.avg_startup", "false");
      this.putDefault("telemetry.global.tps", "false");
      this.putDefault("telemetry.global.mspt", "false");
      this.putDefault("telemetry.global.ram", "false");
      this.putDefault("telemetry.global.cpu", "false");
      this.putDefault("telemetry.global.chunks", "false");
      this.putDefault("telemetry.global.entities", "false");
      this.putDefault("telemetry.global.link", "false");
      this.putDefault("telemetry.global.history", "false");
      this.putDefault("telemetry.global.errors", "false");
   }

   private void putDefault(String var1, String var2) {
      if (!this.values.containsKey(var1)) {
         this.values.setProperty(var1, var2);
      }
   }

   public synchronized String get(String var1, String var2) {
      return this.values.getProperty(var1, var2);
   }

   public boolean getBoolean(String var1, boolean var2) {
      return Boolean.parseBoolean(this.get(var1, Boolean.toString(var2)));
   }

   public int getInt(String var1, int var2) {
      try {
         return Integer.parseInt(this.get(var1, Integer.toString(var2)));
      } catch (Exception var4) {
         return var2;
      }
   }

   public synchronized void set(String var1, String var2) throws IOException {
      this.values.setProperty(var1, var2);
      this.save();
   }

   public void setBoolean(String var1, boolean var2) throws IOException {
      this.set(var1, Boolean.toString(var2));
   }

   public void setInt(String var1, int var2) throws IOException {
      this.set(var1, Integer.toString(var2));
   }

   public boolean telemetry(String var1, String var2) {
      String var3 = this.values.getProperty("telemetry.server." + normalize(var1) + "." + var2);
      return var3 != null && !var3.equalsIgnoreCase("inherit") ? Boolean.parseBoolean(var3) : this.getBoolean("telemetry.global." + var2, true);
   }

   public String telemetrySource(String var1, String var2) {
      String var3 = this.values.getProperty("telemetry.server." + normalize(var1) + "." + var2);
      if (var3 != null && !var3.equalsIgnoreCase("inherit")) {
         return Boolean.parseBoolean(var3) ? "ON" : "OFF";
      } else {
         return "INHERIT";
      }
   }

   public synchronized void cycleTelemetry(String var1, String var2) throws IOException {
      String var3 = "telemetry.server." + normalize(var1) + "." + var2;
      String var4 = this.values.getProperty(var3, "inherit");
      String var5 = var4.equalsIgnoreCase("inherit") ? "true" : (var4.equalsIgnoreCase("true") ? "false" : "inherit");
      this.values.setProperty(var3, var5);
      this.save();
   }

   public synchronized void setGlobalTelemetry(String var1, boolean var2) throws IOException {
      this.values.setProperty("telemetry.global." + var1, Boolean.toString(var2));
      this.save();
   }

   public boolean globalTelemetry(String var1) {
      return this.getBoolean("telemetry.global." + var1, true);
   }

   public synchronized void clearTelemetryOverrides(String var1) throws IOException {
      String var2 = "telemetry.server." + normalize(var1) + ".";
      this.values.keySet().removeIf(var1x -> var1x.toString().startsWith(var2));
      this.save();
   }

   private synchronized void save() throws IOException {
      Files.createDirectories(this.file.getParent());
      Path var1 = this.file.resolveSibling(this.file.getFileName() + ".tmp");

      try (BufferedWriter var2 = Files.newBufferedWriter(var1, StandardCharsets.UTF_8)) {
         this.values.store(var2, "HubPilot Alpha 10 admin GUI settings");
      }

      try {
         Files.move(var1, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException var6) {
         Files.move(var1, this.file, StandardCopyOption.REPLACE_EXISTING);
      }
   }

   private void saveQuietly() {
      try {
         this.save();
      } catch (IOException var2) {
         this.plugin.getLogger().warning("Could not save admin-ui.properties: " + var2.getMessage());
      }
   }

   private static String normalize(String var0) {
      return var0 == null ? "global" : var0.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "-");
   }

   public Path file() {
      return this.file;
   }
}
