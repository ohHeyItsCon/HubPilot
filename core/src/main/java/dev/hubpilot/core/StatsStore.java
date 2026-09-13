package dev.hubpilot.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;

final class StatsStore {
   private final Path file;
   private final Logger logger;
   private final ConcurrentHashMap<String, AtomicLong> values = new ConcurrentHashMap<>();

   StatsStore(Path var1, Logger var2) {
      this.file = var1.resolve("data/statistics.properties");
      this.logger = var2;
   }

   synchronized void load() {
      this.values.clear();
      if (Files.isRegularFile(this.file)) {
         Properties var1 = new Properties();

         try (InputStream var2 = Files.newInputStream(this.file)) {
            var1.load(var2);

            for (String var4 : var1.stringPropertyNames()) {
               try {
                  this.values.put(var4, new AtomicLong(Long.parseLong(var1.getProperty(var4))));
               } catch (NumberFormatException var7) {
               }
            }
         } catch (IOException var9) {
            this.logger.warn("Could not load HubPilot statistics: {}", new Object[]{var9.getMessage()});
         }
      }
   }

   long increment(String var1, String var2) {
      return this.add(var1, var2, 1L);
   }

   long add(String var1, String var2, long var3) {
      return this.values.computeIfAbsent(key(var1, var2), var0 -> new AtomicLong()).addAndGet(var3);
   }

   long get(String var1, String var2) {
      AtomicLong var3 = this.values.get(key(var1, var2));
      return var3 == null ? 0L : var3.get();
   }

   long averageStartupSeconds(String var1) {
      long var2 = this.get(var1, "startup.successes");
      return var2 == 0L ? 0L : this.get(var1, "startup.total-seconds") / var2;
   }

   synchronized void save() {
      try {
         Files.createDirectories(this.file.getParent());
         Properties var1 = new Properties();
         this.values.forEach((var1x, var2x) -> var1.setProperty(var1x, Long.toString(var2x.get())));
         var1.setProperty("meta.last-saved", Long.toString(Instant.now().toEpochMilli()));
         Path var2 = this.file.resolveSibling(this.file.getFileName() + ".tmp");

         try (OutputStream var3 = Files.newOutputStream(var2, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            var1.store(var3, "HubPilot Core statistics");
         }

         try {
            Files.move(var2, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (IOException var7) {
            Files.move(var2, this.file, StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (IOException var9) {
         this.logger.warn("Could not save HubPilot statistics: {}", new Object[]{var9.getMessage()});
      }
   }

   private static String key(String var0, String var1) {
      return "server." + var0.toLowerCase() + "." + var1;
   }
}
