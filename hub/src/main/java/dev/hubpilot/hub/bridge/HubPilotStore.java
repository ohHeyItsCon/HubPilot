package dev.hubpilot.hub.bridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public final class HubPilotStore {
   private final Path file;
   private final Logger logger;
   private final AtomicLong lastModified = new AtomicLong(-1L);
   private volatile Properties values = new Properties();

   public HubPilotStore(Path var1, Logger var2) {
      this.file = var1;
      this.logger = var2;
   }

   public synchronized void initialize() {
      try {
         Files.createDirectories(this.file.getParent());
         if (!Files.exists(this.file)) {
            Properties var1 = new Properties();
            var1.setProperty("format-version", "1");

            for (SettingKey var5 : SettingKey.values()) {
               var1.setProperty(globalKey(var5), var5.defaultValue());
            }

            this.save(var1);
         }

         this.load();
      } catch (IOException var6) {
         throw new IllegalStateException("Could not initialize HubPilot settings at " + this.file, var6);
      }
   }

   public synchronized boolean reloadIfChanged() {
      try {
         long var1 = Files.exists(this.file) ? Files.getLastModifiedTime(this.file).toMillis() : -1L;
         if (var1 != this.lastModified.get()) {
            this.load();
            return true;
         }
      } catch (IOException var5) {
         this.logger.warning("Could not reload HubPilot settings: " + var5.getMessage());
      }

      return false;
   }

   private void load() throws IOException {
      Properties var1 = new Properties();
      if (Files.exists(this.file)) {
         try (InputStream var2 = Files.newInputStream(this.file)) {
            var1.load(var2);
         }
      }

      var1.setProperty("format-version", "1");

      for (SettingKey var5 : SettingKey.values()) {
         var1.putIfAbsent(globalKey(var5), var5.defaultValue());
      }

      this.values = var1;
      this.lastModified.set(Files.exists(this.file) ? Files.getLastModifiedTime(this.file).toMillis() : -1L);
   }

   public String effective(String var1, SettingKey var2) {
      Properties var4 = this.values;
      String var3 = var1 == null ? null : var4.getProperty(serverKey(var1, var2));
      return var3 != null && !var3.isBlank() ? var3.trim() : var4.getProperty(globalKey(var2), var2.defaultValue()).trim();
   }

   public String global(SettingKey var1) {
      return this.values.getProperty(globalKey(var1), var1.defaultValue()).trim();
   }

   public boolean hasOverride(String var1, SettingKey var2) {
      return var1 != null && this.values.containsKey(serverKey(var1, var2));
   }

   public synchronized void setGlobal(SettingKey var1, String var2) throws IOException {
      Properties var3 = this.copyValues();
      var3.setProperty(globalKey(var1), var2.trim());
      this.save(var3);
   }

   public synchronized void setOverride(String var1, SettingKey var2, String var3) throws IOException {
      Properties var4 = this.copyValues();
      var4.setProperty(serverKey(var1, var2), var3.trim());
      this.save(var4);
   }

   public synchronized void clearOverride(String var1, SettingKey var2) throws IOException {
      Properties var3 = this.copyValues();
      var3.remove(serverKey(var1, var2));
      this.save(var3);
   }

   public synchronized void clearAllOverrides(String var1) throws IOException {
      Properties var2 = this.copyValues();
      String var3 = "server." + normalize(var1) + ".";
      var2.keySet().removeIf(var1x -> String.valueOf(var1x).startsWith(var3));
      this.save(var2);
   }

   public synchronized void copyOverrides(String var1, String var2) throws IOException {
      Properties var3 = this.copyValues();

      for (SettingKey var7 : SettingKey.values()) {
         String var8 = var3.getProperty(serverKey(var1, var7));
         if (var8 != null) {
            var3.setProperty(serverKey(var2, var7), var8);
         }
      }

      this.save(var3);
   }

   public String messageText(String var1, MessageEvent var2) {
      Properties var3 = this.values;
      if (var1 != null) {
         String var4 = var3.getProperty(messageServerKey(var1, var2, "text"));
         if (var4 != null) {
            return var4;
         }
      }

      return var3.getProperty(messageGlobalKey(var2, "text"), var2.defaultText());
   }

   public boolean messageEnabled(String var1, MessageEvent var2) {
      Properties var3 = this.values;
      if (var1 != null) {
         String var4 = var3.getProperty(messageServerKey(var1, var2, "enabled"));
         if (var4 != null) {
            return Boolean.parseBoolean(var4);
         }
      }

      return Boolean.parseBoolean(var3.getProperty(messageGlobalKey(var2, "enabled"), "true"));
   }

   public boolean hasMessageOverride(String var1, MessageEvent var2) {
      return var1 != null
         && (this.values.containsKey(messageServerKey(var1, var2, "text")) || this.values.containsKey(messageServerKey(var1, var2, "enabled")));
   }

   public synchronized void setMessageText(String var1, MessageEvent var2, String var3) throws IOException {
      Properties var4 = this.copyValues();
      var4.setProperty(var1 == null ? messageGlobalKey(var2, "text") : messageServerKey(var1, var2, "text"), var3);
      this.save(var4);
   }

   public synchronized void setMessageEnabled(String var1, MessageEvent var2, boolean var3) throws IOException {
      Properties var4 = this.copyValues();
      var4.setProperty(var1 == null ? messageGlobalKey(var2, "enabled") : messageServerKey(var1, var2, "enabled"), Boolean.toString(var3));
      this.save(var4);
   }

   public synchronized void clearMessageOverride(String var1, MessageEvent var2) throws IOException {
      Properties var3 = this.copyValues();
      if (var1 == null) {
         var3.remove(messageGlobalKey(var2, "text"));
         var3.remove(messageGlobalKey(var2, "enabled"));
      } else {
         var3.remove(messageServerKey(var1, var2, "text"));
         var3.remove(messageServerKey(var1, var2, "enabled"));
      }

      this.save(var3);
   }

   private Properties copyValues() {
      Properties var1 = new Properties();
      var1.putAll(this.values);
      return var1;
   }

   private void save(Properties var1) throws IOException {
      Path var2 = this.file.resolveSibling(this.file.getFileName() + ".tmp");

      try (OutputStream var3 = Files.newOutputStream(var2)) {
         var1.store(var3, "HubPilot shared automation settings - manage in game");
      }

      try {
         Files.move(var2, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (IOException var7) {
         Files.move(var2, this.file, StandardCopyOption.REPLACE_EXISTING);
      }

      this.values = var1;
      this.lastModified.set(Files.getLastModifiedTime(this.file).toMillis());
   }

   public Path file() {
      return this.file;
   }

   private static String globalKey(SettingKey var0) {
      return "global." + var0.property();
   }

   private static String serverKey(String var0, SettingKey var1) {
      return "server." + normalize(var0) + "." + var1.property();
   }

   private static String normalize(String var0) {
      return var0.trim().toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9_-]", "-");
   }

   private static String messageGlobalKey(MessageEvent var0, String var1) {
      return "message.global." + var0.id() + "." + var1;
   }

   private static String messageServerKey(String var0, MessageEvent var1, String var2) {
      return "message.server." + normalize(var0) + "." + var1.id() + "." + var2;
   }
}
