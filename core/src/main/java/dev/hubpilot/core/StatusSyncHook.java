package dev.hubpilot.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Locale;

public final class StatusSyncHook {
   private static volatile Object config;
   private static volatile Object status;
   private static volatile Object logger;
   private static volatile Object channel;

   private StatusSyncHook() {
   }

   static void initialize(Object var0, Object var1, Object var2, Object var3) {
      config = var0;
      status = var1;
      logger = var2;
      channel = var3;
   }

   public static boolean handle(Object var0) {
      if (var0 != null && config != null && status != null) {
         try {
            Object var1 = invoke(var0, "getIdentifier");
            String var2 = String.valueOf(invoke(var1, "getId"));
            if (!"hubpilot:status".equalsIgnoreCase(var2)) {
               return false;
            } else {
               markHandled(var0);

               Object var3;
               try {
                  var3 = invoke(var0, "getSource");
                  invoke(var3, "getServerInfo");
               } catch (Throwable var14) {
                  return true;
               }

               String var4 = String.valueOf(invoke(invoke(var3, "getServerInfo"), "getName")).toLowerCase(Locale.ROOT);
               HubPilotConfig var5 = (HubPilotConfig)config;
               StatusStore var6 = (StatusStore)status;
               HubPilotConfig.Snapshot var7 = var5.snapshot();
               if (var7.trustedRequestServers() != null
                  && !var7.trustedRequestServers().stream().map(String::valueOf).map(var0x -> var0x.toLowerCase(Locale.ROOT)).noneMatch(var4::equals)) {
                  byte[] var8 = (byte[])invoke(var0, "getData");

                  try {
                     label79: {
                        boolean var10;
                        try (DataInputStream var9 = new DataInputStream(new ByteArrayInputStream(var8 == null ? new byte[0] : var8))) {
                           if ("STATUS_SYNC_REQUEST".equals(var9.readUTF())) {
                              break label79;
                           }

                           var10 = true;
                        }

                        return var10;
                     }
                  } catch (IOException var16) {
                     return true;
                  }

                  for (ManagedServer var19 : var7.servers().values()) {
                     StatusStore.ServerStatus var11 = var6.get(var19.id());
                     sendRow(var3, var19.id(), var11);
                     String var12 = var19.velocityServer();
                     if (var12 != null && !var12.isBlank() && !var12.equalsIgnoreCase(var19.id())) {
                        sendRow(var3, var12, var11);
                     }
                  }

                  send(var3, payloadEnd());
                  return true;
               } else {
                  log("warn", "Rejected HubPilot status sync request from untrusted server " + var4);
                  return true;
               }
            }
         } catch (Throwable var17) {
            log("warn", "HubPilot status sync failed: " + root(var17));
            return true;
         }
      } else {
         return false;
      }
   }

   private static void sendRow(Object var0, String var1, StatusStore.ServerStatus var2) throws Exception {
      String var3 = String.valueOf(var2.state());
      if ("STOPPING".equals(var3) || "MAINTENANCE".equals(var3)) {
         var3 = "OFFLINE";
      }

      ByteArrayOutputStream var4 = new ByteArrayOutputStream();

      try (DataOutputStream var5 = new DataOutputStream(var4)) {
         var5.writeUTF("STATUS_ROW");
         var5.writeUTF(var1);
         var5.writeUTF(var3);
         var5.writeLong(var2.onlineSince());
         var5.writeLong(var2.lastStarted());
         var5.writeLong(var2.lastChecked());
         var5.writeInt(var2.playersOnline());
         var5.writeInt(var2.playersMax());
         var5.writeLong(var2.backendPingMs());
         var5.writeInt(var2.queueSize());
         var5.writeLong(var2.startRequestedAt());
         var5.writeInt(var2.expectedStartupSeconds());
         String var6 = var2.lastError() == null ? "" : var2.lastError();
         var5.writeUTF(var6.length() > 512 ? var6.substring(0, 512) : var6);
      }

      send(var0, var4.toByteArray());
   }

   private static byte[] payloadEnd() throws IOException {
      ByteArrayOutputStream var0 = new ByteArrayOutputStream();

      try (DataOutputStream var1 = new DataOutputStream(var0)) {
         var1.writeUTF("STATUS_SYNC_END");
      }

      return var0.toByteArray();
   }

   private static void send(Object var0, byte[] var1) throws Exception {
      if (channel != null) {
         for (Method var5 : var0.getClass().getMethods()) {
            if (var5.getName().equals("sendPluginMessage") && var5.getParameterCount() == 2 && var5.getParameterTypes()[1].equals(byte[].class)) {
               HubPilotReflect.invokeAccessible(var5, var0, new Object[]{channel, var1});
               return;
            }
         }

         throw new NoSuchMethodException("sendPluginMessage");
      }
   }

   private static void markHandled(Object var0) {
      try {
         Class var1 = Class.forName("com.velocitypowered.api.event.connection.PluginMessageEvent$ForwardResult", true, var0.getClass().getClassLoader());
         Object var2 = HubPilotReflect.invokeAccessible(var1.getMethod("handled"), null, new Object[0]);
         HubPilotReflect.invokeAccessible(var0.getClass().getMethod("setResult", var1), var0, new Object[]{var2});
      } catch (Throwable var3) {
      }
   }

   private static Object invoke(Object var0, String var1, Object... var2) throws Exception {
      Method var3 = null;

      for (Method var7 : var0.getClass().getMethods()) {
         if (matches(var7, var1, var2)) {
            var3 = var7;
            break;
         }
      }

      if (var3 == null) {
         for (Method var12 : var0.getClass().getDeclaredMethods()) {
            if (matches(var12, var1, var2)) {
               var3 = var12;
               break;
            }
         }
      }

      if (var3 == null) {
         throw new NoSuchMethodException(var0.getClass().getName() + "." + var1);
      } else {
         try {
            var3.setAccessible(true);
         } catch (Throwable var8) {
         }

         return HubPilotReflect.invokeAccessible(var3, var0, var2);
      }
   }

   private static boolean matches(Method var0, String var1, Object[] var2) {
      if (var0.getName().equals(var1) && var0.getParameterCount() == var2.length) {
         Class[] var3 = var0.getParameterTypes();

         for (int var4 = 0; var4 < var3.length; var4++) {
            if (var2[var4] != null && !wrap(var3[var4]).isInstance(var2[var4])) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static Class<?> wrap(Class<?> var0) {
      if (!var0.isPrimitive()) {
         return var0;
      } else if (var0 == boolean.class) {
         return Boolean.class;
      } else if (var0 == int.class) {
         return Integer.class;
      } else if (var0 == long.class) {
         return Long.class;
      } else if (var0 == float.class) {
         return Float.class;
      } else if (var0 == double.class) {
         return Double.class;
      } else if (var0 == short.class) {
         return Short.class;
      } else if (var0 == byte.class) {
         return Byte.class;
      } else {
         return var0 == char.class ? Character.class : var0;
      }
   }

   private static void log(String var0, String var1) {
      try {
         invoke(logger, var0, var1);
      } catch (Throwable var3) {
         System.err.println("[HubPilot] " + var1);
      }
   }

   private static String root(Throwable var0) {
      while (var0.getCause() != null && var0.getCause() != var0) {
         var0 = var0.getCause();
      }

      String var1 = var0.getMessage();
      return var0.getClass().getSimpleName() + (var1 == null ? "" : ": " + var1);
   }
}
