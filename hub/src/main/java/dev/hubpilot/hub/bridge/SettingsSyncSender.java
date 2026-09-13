package dev.hubpilot.hub.bridge;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SettingsSyncSender {
   private static final String CHANNEL = "hubpilot:settings";

   private SettingsSyncSender() {
   }

   public static void register(Object var0) {
      try {
         Object var1 = plugin(var0);
         Object var2 = invoke(var1, "getServer");
         Object var3 = invoke(var2, "getMessenger");
         invokeCompatible(var3, "registerOutgoingPluginChannel", var1, "hubpilot:settings");
      } catch (Throwable var4) {
         log(var0, "Could not register HubPilot settings channel: " + root(var4));
      }
   }

   public static void send(Object var0, Object var1) {
      if (var0 != null && var1 != null) {
         try {
            Object var2 = plugin(var0);
            Object var3 = invoke(var2, "getHubPilotStore");
            Path var4 = (Path)invoke(var3, "file");
            if (var4 == null || !Files.isRegularFile(var4)) {
               return;
            }

            String var5 = Files.readString(var4, StandardCharsets.UTF_8);
            if (var5.length() > 50000) {
               log(var0, "HubPilot settings snapshot is too large to synchronize.");
               return;
            }

            ByteArrayOutputStream var6 = new ByteArrayOutputStream();

            try (DataOutputStream var7 = new DataOutputStream(var6)) {
               var7.writeUTF("SETTINGS_SNAPSHOT");
               var7.writeUTF(var5);
            }

            invokeCompatible(var1, "sendPluginMessage", var2, "hubpilot:settings", var6.toByteArray());
         } catch (Throwable var12) {
            log(var0, "Could not synchronize HubPilot GUI settings: " + root(var12));
         }
      }
   }

   private static Object plugin(Object var0) throws Exception {
      Field var1 = var0.getClass().getDeclaredField("plugin");
      var1.setAccessible(true);
      return var1.get(var0);
   }

   private static Object invoke(Object var0, String var1, Object... var2) throws Exception {
      return invokeCompatible(var0, var1, var2);
   }

   private static Object invokeCompatible(Object var0, String var1, Object... var2) throws Exception {
      Method var3 = null;

      label36:
      for (Method var7 : var0.getClass().getMethods()) {
         if (var7.getName().equals(var1) && var7.getParameterCount() == var2.length) {
            Class[] var8 = var7.getParameterTypes();

            for (int var9 = 0; var9 < var8.length; var9++) {
               if (var2[var9] != null && !var8[var9].isInstance(var2[var9])) {
                  continue label36;
               }
            }

            var3 = var7;
            break;
         }
      }

      if (var3 == null) {
         throw new NoSuchMethodException(var0.getClass().getName() + "." + var1);
      } else {
         var3.setAccessible(true);
         return var3.invoke(var0, var2);
      }
   }

   private static void log(Object var0, String var1) {
      try {
         Object var2 = plugin(var0);
         Object var3 = invoke(var2, "getLogger");
         var3.getClass().getMethod("warning", String.class).invoke(var3, var1);
      } catch (Throwable var4) {
      }
   }

   private static String root(Throwable var0) {
      Throwable var1 = var0;

      while (var1.getCause() != null && var1.getCause() != var1) {
         var1 = var1.getCause();
      }

      return var1.getClass().getSimpleName() + (var1.getMessage() == null ? "" : ": " + var1.getMessage());
   }
}
