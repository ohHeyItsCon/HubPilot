package dev.hubpilot.hub.status;

import dev.hubpilot.hub.HubPilotHubPlugin;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

public final class StatusStore {
   private final HubPilotHubPlugin plugin;
   private final Path file;
   private volatile Map<String, ServerStatus> statuses = Map.of();
   private final AtomicLong lastModified = new AtomicLong(-1L);

   public StatusStore(HubPilotHubPlugin var1, Path var2) {
      this.plugin = var1;
      this.file = var2;
   }

   public boolean reloadIfChanged() {
      try {
         if (!Files.exists(this.file)) {
            return false;
         } else {
            long var1 = Files.getLastModifiedTime(this.file).toMillis();
            if (var1 == this.lastModified.get()) {
               return false;
            } else {
               Properties var3 = new Properties();

               try (InputStream var4 = Files.newInputStream(this.file)) {
                  var3.load(var4);
               }

               LinkedHashMap var15 = new LinkedHashMap();

               for (String var6 : var3.stringPropertyNames()) {
                  if (var6.startsWith("status.") && var6.endsWith(".state")) {
                     String var7 = var6.substring("status.".length(), var6.length() - ".state".length());
                     String var8 = "status." + var7;

                     ServerStatus.State var9;
                     try {
                        var9 = ServerStatus.State.valueOf(var3.getProperty(var8 + ".state", "UNKNOWN"));
                     } catch (IllegalArgumentException var12) {
                        var9 = ServerStatus.State.UNKNOWN;
                     }

                     var15.put(
                        var7.toLowerCase(Locale.ROOT),
                        new ServerStatus(
                           var9,
                           parseLong(var3.getProperty(var8 + ".online-since")),
                           parseLong(var3.getProperty(var8 + ".last-started")),
                           parseLong(var3.getProperty(var8 + ".last-checked")),
                           parseInt(var3.getProperty(var8 + ".players-online"), -1),
                           parseInt(var3.getProperty(var8 + ".players-max"), -1)
                        )
                     );
                  }
               }

               this.statuses = Collections.unmodifiableMap(var15);
               this.lastModified.set(var1);
               return true;
            }
         }
      } catch (Exception var14) {
         this.plugin.getLogger().warning("Could not reload status file: " + var14.getMessage());
         return false;
      }
   }

   public synchronized void applyRemote(String var1, ServerStatus var2) {
      if (var1 != null && !var1.isBlank() && var2 != null) {
         LinkedHashMap var3 = new LinkedHashMap<>(this.statuses);
         var3.put(var1.toLowerCase(Locale.ROOT), var2);
         this.statuses = Collections.unmodifiableMap(var3);
      }
   }

   public ServerStatus get(String var1) {
      return var1 == null ? ServerStatus.unknown() : this.statuses.getOrDefault(var1.toLowerCase(Locale.ROOT), ServerStatus.unknown());
   }

   private static long parseLong(String var0) {
      try {
         return Long.parseLong(var0 == null ? "0" : var0);
      } catch (NumberFormatException var2) {
         return 0L;
      }
   }

   private static int parseInt(String var0, int var1) {
      try {
         return Integer.parseInt(var0 == null ? "" : var0);
      } catch (NumberFormatException var3) {
         return var1;
      }
   }
}
