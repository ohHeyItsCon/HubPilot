package dev.hubpilot.core;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

public final class SettingsSyncHook {
   private static volatile Object config;
   private static volatile Object logger;
   private static Set<String> ALLOWED = Set.of(
      "countdown-seconds",
      "countdown-sound",
      "sound-volume",
      "pitch-style",
      "countdown-message",
      "required-version",
      "startup-timeout-seconds",
      "retry-count",
      "retry-delay-seconds",
      "stop-after-failure",
      "idle-shutdown-minutes",
      "autostart-enabled"
   );

   private SettingsSyncHook() {
   }

   static void initialize(Object var0, Object var1) {
      config = var0;
      logger = var1;
   }

   public static boolean handle(Object var0) {
      if (var0 == null) {
         return false;
      } else if (config == null) {
         return false;
      } else {
         try {
            Properties var7 = (Properties)invoke(var0, "getIdentifier");
            String var8 = String.valueOf(invoke(var7, "getId"));
            if (!"hubpilot:settings".equalsIgnoreCase(var8)) {
               return false;
            } else {
               markHandled(var0);
               Path var9 = (Path)invoke(var0, "getSource");

               Object var6;
               try {
                  var6 = invoke(var9, "getServerInfo");
               } catch (Throwable var26) {
                  return true;
               }

               String var5 = String.valueOf(invoke(var6, "getName")).toLowerCase(Locale.ROOT);
               Object var4 = invoke(config, "snapshot");
               if (invoke(var4, "trustedRequestServers") instanceof Collection var11
                  && !var11.stream().map(var0x -> String.valueOf(var0x).toLowerCase(Locale.ROOT)).noneMatch(var5::equals)) {
                  var6 = (byte[])invoke(var0, "getData");
                  if (var6 == null) {
                     return true;
                  } else if (((Object[])var6).length == 0) {
                     return true;
                  } else if (((Object[])var6).length > 65535) {
                     return true;
                  } else {
                     var7 = new Properties();

                     try (DataInputStream var34 = new DataInputStream(new ByteArrayInputStream((byte[])var6))) {
                        if (!"SETTINGS_SNAPSHOT".equals(var34.readUTF())) {
                           return true;
                        }

                        String var3 = var34.readUTF();
                        if (var3.length() > 50000) {
                           return true;
                        }

                        try (StringReader var36 = new StringReader(var3)) {
                           var7.load(var36);
                        }
                     }

                     Properties var35 = new Properties();
                     var35.setProperty("format-version", "2");

                     for (String var41 : var7.stringPropertyNames()) {
                        String var2;
                        if (validKey(var41) && (var2 = var7.getProperty(var41, "")).length() <= 1024) {
                           var35.setProperty(var41, var2);
                        }
                     }

                     var9 = Path.of(String.valueOf(invoke(var4, "sharedDirectory")));
                     Files.createDirectories(var9);
                     Path var42 = var9.resolve("hubpilot.properties");
                     Properties var31 = new Properties();
                     if (Files.isRegularFile(var42)) {
                        BufferedReader var1 = Files.newBufferedReader(var42, StandardCharsets.UTF_8);

                        try {
                           var31.load(var1);
                        } finally {
                           if (var1 != null) {
                              var1.close();
                           }
                        }
                     }

                     if (var31.equals(var35)) {
                        return true;
                     } else {
                        StringWriter var30 = new StringWriter();
                        var35.store(var30, "HubPilot settings synchronized from HubPilot Hub");
                        String var45 = var30.toString();
                        Path var12 = var42.resolveSibling(var42.getFileName() + ".tmp");
                        Files.writeString(var12, var45, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                        try {
                           Files.move(var12, var42, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                        } catch (AtomicMoveNotSupportedException var24) {
                           Files.move(var12, var42, StandardCopyOption.REPLACE_EXISTING);
                        }

                        invoke(config, "reload");
                        log("info", "Applied HubPilot GUI automation settings from " + var5 + ".");
                        return true;
                     }
                  }
               } else {
                  log("warn", "Rejected HubPilot settings sync from untrusted backend " + var5);
                  return true;
               }
            }
         } catch (Throwable var29) {
            log("warn", "Could not apply HubPilot GUI settings: " + root(var29));
            return true;
         }
      }
   }

   private static boolean validKey(String var0) {
      if (var0 == null) {
         return false;
      } else if (var0.matches("message\\.global\\.[a-z0-9-]{1,64}\\.(enabled|text)")) {
         return true;
      } else if (var0.matches("message\\.server\\.[A-Za-z0-9_-]{1,64}\\.[a-z0-9-]{1,64}\\.(enabled|text)")) {
         return true;
      } else if (var0.startsWith("global.")) {
         return ALLOWED.contains(var0.substring("global.".length()));
      } else if (!var0.startsWith("server.")) {
         return false;
      } else {
         int var1 = var0.lastIndexOf(46);
         if (var1 <= "server.".length()) {
            return false;
         } else {
            String var2 = var0.substring("server.".length(), var1);
            String var3 = var0.substring(var1 + 1);
            return var2.matches("[A-Za-z0-9_-]{1,64}") && ALLOWED.contains(var3);
         }
      }
   }

   private static void markHandled(Object var0) {
      try {
         ClassLoader var1 = var0.getClass().getClassLoader();
         Class var2 = Class.forName("com.velocitypowered.api.event.connection.PluginMessageEvent$ForwardResult", true, var1);
         Object var3 = HubPilotReflect.invokeAccessible(var2.getMethod("handled"), null, new Object[0]);
         HubPilotReflect.invokeAccessible(var0.getClass().getMethod("setResult", var2), var0, new Object[]{var3});
      } catch (Throwable var4) {
      }
   }

   private static Object invoke(Object var0, String var1, Object... var2) throws Exception {
      Method var3 = null;

      label54:
      for (Method var7 : var0.getClass().getMethods()) {
         if (var7.getName().equals(var1) && var7.getParameterCount() == var2.length) {
            Class[] var8 = var7.getParameterTypes();

            for (int var9 = 0; var9 < var8.length; var9++) {
               if (var2[var9] != null && !wrap(var8[var9]).isInstance(var2[var9])) {
                  continue label54;
               }
            }

            var3 = var7;
            break;
         }
      }

      if (var3 == null) {
         for (Method var13 : var0.getClass().getDeclaredMethods()) {
            if (var13.getName().equals(var1) && var13.getParameterCount() == var2.length) {
               var3 = var13;
               break;
            }
         }
      }

      if (var3 == null) {
         throw new NoSuchMethodException(var0.getClass().getName() + "." + var1);
      } else {
         var3.setAccessible(true);
         return HubPilotReflect.invokeAccessible(var3, var0, var2);
      }
   }

   private static Class<?> wrap(Class<?> var0) {
      if (!var0.isPrimitive()) {
         return var0;
      } else {
         return var0 == boolean.class
            ? Boolean.class
            : (
               var0 == int.class
                  ? Integer.class
                  : (var0 == long.class ? Long.class : (var0 == float.class ? Float.class : (var0 == double.class ? Double.class : var0)))
            );
      }
   }

   private static void log(String var0, String var1) {
      Object var2 = logger;
      if (var2 != null) {
         try {
            HubPilotReflect.invokeAccessible(var2.getClass().getMethod(var0, String.class), var2, new Object[]{var1});
         } catch (Throwable var4) {
         }
      }
   }

   private static String root(Throwable var0) {
      Throwable var1 = var0;

      while (var1.getCause() != null && var1.getCause() != var1) {
         var1 = var1.getCause();
      }

      return var1.getClass().getSimpleName() + (var1.getMessage() == null ? "" : ": " + var1.getMessage());
   }

   static {
      ALLOWED = OperatingModeRules.addAllowed(ALLOWED);
   }
}
