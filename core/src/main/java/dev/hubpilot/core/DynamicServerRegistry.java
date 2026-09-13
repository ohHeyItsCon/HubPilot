package dev.hubpilot.core;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

final class DynamicServerRegistry {
   private static volatile Object proxy;
   private static volatile Path file;

   private DynamicServerRegistry() {
   }

   static synchronized void restore(Object var0) {
      try {
         proxy = field(var0, "proxy");
         Path var1 = (Path)field(var0, "dataDirectory");
         file = var1.resolve("dynamic-servers.properties");
         if (!Files.exists(file)) {
            return;
         }

         Properties var2 = new Properties();

         try (InputStream var3 = Files.newInputStream(file)) {
            var2.load(var3);
         }

         LinkedHashMap var14 = new LinkedHashMap();

         for (String var5 : var2.stringPropertyNames()) {
            if (var5.startsWith("server.") && var5.endsWith(".host")) {
               String var6 = var5.substring(7, var5.length() - 5);
               String var7 = var2.getProperty("server." + var6 + ".host", "").trim();
               int var8 = parse(var2.getProperty("server." + var6 + ".port"));
               String var9 = var2.getProperty("server." + var6 + ".display", var6);
               String var10 = var2.getProperty("server." + var6 + ".crafty-id", "");
               if (!var7.isBlank() && var8 > 0) {
                  var14.put(var6, new CraftyDiscoverySupport.Endpoint(var6, var9, var7, var8, var10));
               }
            }
         }

         registerAll(var14.values());
      } catch (Throwable var13) {
         System.err.println("[HubPilot] Could not restore dynamically discovered servers: " + root(var13));
      }
   }

   static synchronized void registerAll(Collection<CraftyDiscoverySupport.Endpoint> var0) {
      if (proxy != null && var0 != null) {
         boolean var1 = false;
         Properties var2 = load();

         for (CraftyDiscoverySupport.Endpoint var4 : var0) {
            try {
               register(var4);
               String var5 = "server." + var4.velocityName() + ".";
               var1 |= put(var2, var5 + "host", var4.host());
               var1 |= put(var2, var5 + "port", Integer.toString(var4.port()));
               var1 |= put(var2, var5 + "display", var4.displayName());
               var1 |= put(var2, var5 + "crafty-id", var4.craftyId());
            } catch (Throwable var7) {
               System.err.println("[HubPilot] Could not register discovered server " + var4.displayName() + ": " + root(var7));
            }
         }

         if (var1 && file != null) {
            try {
               Files.createDirectories(file.getParent());

               try (OutputStream var10 = Files.newOutputStream(file)) {
                  var2.store(var10, "HubPilot dynamic Velocity servers discovered from Crafty");
               }
            } catch (Exception var9) {
               System.err.println("[HubPilot] Could not persist dynamically discovered servers: " + var9.getMessage());
            }
         }
      }
   }

   private static void register(CraftyDiscoverySupport.Endpoint var0) throws Exception {
      Method var1 = proxy.getClass().getMethod("getServer", String.class);
      if (!(var1.invoke(proxy, var0.velocityName()) instanceof Optional var3 && var3.isPresent())) {
         ClassLoader var10 = proxy.getClass().getClassLoader();
         Class var4 = Class.forName("com.velocitypowered.api.proxy.server.ServerInfo", true, var10);
         Constructor var5 = null;

         for (Constructor var9 : var4.getConstructors()) {
            if (var9.getParameterCount() == 2
               && var9.getParameterTypes()[0] == String.class
               && var9.getParameterTypes()[1].isAssignableFrom(InetSocketAddress.class)) {
               var5 = var9;
               break;
            }
         }

         if (var5 == null) {
            throw new NoSuchMethodException("ServerInfo(String, address)");
         } else {
            Object var11 = var5.newInstance(var0.velocityName(), new InetSocketAddress(var0.host(), var0.port()));
            Method var12 = proxy.getClass().getMethod("registerServer", var4);
            var12.invoke(proxy, var11);
         }
      }
   }

   private static Object field(Object var0, String var1) throws Exception {
      Field var2 = var0.getClass().getDeclaredField(var1);
      var2.setAccessible(true);
      return var2.get(var0);
   }

   private static Properties load() {
      Properties var0 = new Properties();
      if (file != null && Files.exists(file)) {
         try (InputStream var1 = Files.newInputStream(file)) {
            var0.load(var1);
         } catch (Exception var6) {
         }
      }

      return var0;
   }

   private static boolean put(Properties var0, String var1, String var2) {
      String var3 = var0.getProperty(var1);
      var0.setProperty(var1, var2 == null ? "" : var2);
      return !Objects.equals(var3, var2);
   }

   private static int parse(String var0) {
      try {
         return Integer.parseInt(var0);
      } catch (Exception var2) {
         return -1;
      }
   }

   private static String root(Throwable var0) {
      while (var0.getCause() != null && var0.getCause() != var0) {
         var0 = var0.getCause();
      }

      return var0.getMessage() == null ? var0.getClass().getSimpleName() : var0.getMessage();
   }
}
