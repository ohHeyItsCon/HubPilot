package dev.hubpilot.hub.status;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeFormat {
   private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, h:mm a").withZone(ZoneId.systemDefault());

   private TimeFormat() {
   }

   public static String durationSince(long var0) {
      if (var0 <= 0L) {
         return "Unknown";
      } else {
         long var2 = Math.max(0L, Duration.between(Instant.ofEpochMilli(var0), Instant.now()).getSeconds());
         long var4 = var2 / 86400L;
         long var6 = var2 % 86400L / 3600L;
         long var8 = var2 % 3600L / 60L;
         if (var4 > 0L) {
            return var4 + "d " + var6 + "h";
         } else {
            return var6 > 0L ? var6 + "h " + var8 + "m" : Math.max(1L, var8) + "m";
         }
      }
   }

   public static String relative(long var0) {
      if (var0 <= 0L) {
         return "Never detected";
      } else {
         long var2 = Math.max(0L, Duration.between(Instant.ofEpochMilli(var0), Instant.now()).getSeconds());
         if (var2 < 60L) {
            return "just now";
         } else if (var2 < 3600L) {
            return var2 / 60L + "m ago";
         } else if (var2 < 86400L) {
            return var2 / 3600L + "h ago";
         } else {
            return var2 < 604800L ? var2 / 86400L + "d ago" : DATE.format(Instant.ofEpochMilli(var0));
         }
      }
   }
}
