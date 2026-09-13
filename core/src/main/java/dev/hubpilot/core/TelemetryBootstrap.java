package dev.hubpilot.core;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class TelemetryBootstrap {
   private static volatile Object telemetryBridge;

   private TelemetryBootstrap() {
   }

   public static void attach(Object var0) {
      if (var0 != null && telemetryBridge == null) {
         DynamicServerRegistry.restore(var0);

         try {
            Class var15 = var0.getClass();
            Object var16 = field(var15, "proxy").get(var0);
            Object var3 = field(var15, "logger").get(var0);
            Object var4 = field(var15, "config").get(var0);
            Object var5 = field(var15, "status").get(var0);
            Object var6 = field(var15, "stats").get(var0);
            Class var7 = Class.forName("dev.hubpilot.core.TelemetryBridge", true, var15.getClassLoader());
            Constructor var8 = null;

            for (Constructor var12 : var7.getDeclaredConstructors()) {
               if (var12.getParameterCount() == 6) {
                  var8 = var12;
                  break;
               }
            }

            if (var8 == null) {
               throw new NoSuchMethodException("TelemetryBridge constructor");
            }

            var8.setAccessible(true);
            Object var17 = var8.newInstance(var0, var16, var3, var4, var5, var6);
            Method var18 = var7.getDeclaredMethod("register");
            var18.setAccessible(true);
            HubPilotReflect.invokeAccessible(var18, var17, new Object[0]);
            telemetryBridge = var17;
            SettingsSyncHook.initialize(var4, var3);
            Object var19 = registerAuxChannel(var16, var15.getClassLoader(), "status");
            registerAuxChannel(var16, var15.getClassLoader(), "settings");
            StatusSyncHook.initialize(var4, var5, var3, var19);
            log(var3, "info", "HubPilot telemetry/status/settings compatibility bridge enabled.");
         } catch (Throwable var14) {
            Throwable var1 = var14;

            try {
               Object var2 = field(var0.getClass(), "logger").get(var0);
               log(var2, "warn", "HubPilot telemetry bridge disabled; Core lifecycle will continue: " + root(var1));
            } catch (Throwable var13) {
               System.err.println("[HubPilot] Telemetry bridge disabled; Core lifecycle will continue: " + root(var14));
            }
         }
      }
   }

   private static Object registerAuxChannel(Object var0, ClassLoader var1, String var2) throws Exception {
      Object var3 = HubPilotReflect.invokeAccessible(var0.getClass().getMethod("getChannelRegistrar"), var0, new Object[0]);
      Class var4 = Class.forName("com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier", true, var1);
      Object var5 = HubPilotReflect.invokeAccessible(var4.getMethod("create", String.class, String.class), null, new Object[]{"hubpilot", var2});
      Method var6 = null;

      for (Method var10 : var3.getClass().getMethods()) {
         if (var10.getName().equals("register") && var10.getParameterCount() == 1 && var10.getParameterTypes()[0].isArray()) {
            var6 = var10;
            break;
         }
      }

      if (var6 == null) {
         throw new NoSuchMethodException("ChannelRegistrar.register(ChannelIdentifier[])");
      } else {
         Class var11 = var6.getParameterTypes()[0].getComponentType();
         Object var12 = Array.newInstance(var11, 1);
         Array.set(var12, 0, var5);
         HubPilotReflect.invokeAccessible(var6, var3, new Object[]{var12});
         return var5;
      }
   }

   private static Field field(Class<?> var0, String var1) throws Exception {
      Field var2 = var0.getDeclaredField(var1);
      var2.setAccessible(true);
      return var2;
   }

   private static void log(Object var0, String var1, String var2) {
      if (var0 != null) {
         try {
            HubPilotReflect.invokeAccessible(var0.getClass().getMethod(var1, String.class), var0, new Object[]{var2});
         } catch (Throwable var4) {
         }
      }
   }

   private static String root(Throwable var0) {
      Throwable var1 = var0;

      while (var1.getCause() != null && var1.getCause() != var1) {
         var1 = var1.getCause();
      }

      String var2 = var1.getMessage();
      return var1.getClass().getSimpleName() + (var2 != null && !var2.isBlank() ? ": " + var2 : "");
   }
}
