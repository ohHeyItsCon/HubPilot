package dev.hubpilot.hub.publicapi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

public final class ServerControlUi {
   private static volatile Object plugin;
   private static volatile boolean attached;
   private static Object listenerProxy;
   private static Object executorProxy;

   private ServerControlUi() {
   }

   public static synchronized void attach(Object var0) {
      plugin = var0;
      if (!attached && var0 != null) {
         try {
            ClassLoader var1 = var0.getClass().getClassLoader();
            Class var2 = Class.forName("org.bukkit.event.Listener", true, var1);
            Class var3 = Class.forName("org.bukkit.plugin.EventExecutor", true, var1);
            Class var4 = Class.forName("org.bukkit.event.inventory.InventoryClickEvent", true, var1);
            Class var5 = Class.forName("org.bukkit.event.EventPriority", true, var1);
            InvocationHandler var6 = (var0x, var1x, var2x) -> objectMethod(var0x, var1x, var2x, "HubPilotServerControlListener");
            InvocationHandler var7 = (var0x, var1x, var2x) -> {
               if (var1x.getName().equals("execute") && var2x != null && var2x.length >= 2) {
                  handle(var2x[1]);
                  return null;
               } else {
                  return objectMethod(var0x, var1x, var2x, "HubPilotServerControlExecutor");
               }
            };
            listenerProxy = Proxy.newProxyInstance(var1, new Class[]{var2}, var6);
            executorProxy = Proxy.newProxyInstance(var1, new Class[]{var3}, var7);
            Object var8 = call(var0, "getServer");
            Object var9 = call(var8, "getPluginManager");
            Enum var10 = Enum.valueOf(var5, "NORMAL");
            Method var11 = null;

            for (Method var15 : var9.getClass().getMethods()) {
               if (var15.getName().equals("registerEvent") && var15.getParameterCount() == 5) {
                  var11 = var15;
                  break;
               }
            }

            if (var11 == null) {
               throw new NoSuchMethodException("PluginManager.registerEvent/5");
            }

            var11.setAccessible(true);
            var11.invoke(var9, var4, listenerProxy, var10, executorProxy, var0);
            attached = true;
         } catch (Throwable var16) {
            warn("Could not attach Stop Server UI listener: " + root(var16));
         }
      }
   }

   public static void decorate(Object var0) {
      if (var0 != null) {
         try {
            Object var1 = call(var0, "getHolder");
            if (var1 == null || !var1.getClass().getName().equals("dev.hubpilot.hub.gui.AdminEntryHolder")) {
               return;
            }

            String var2 = String.valueOf(call(var1, "destinationId"));
            boolean var3 = running(var2);
            Object var4 = named(
               var3 ? "REDSTONE_BLOCK" : "GRAY_DYE",
               var3 ? "&c&lStop Server" : "&7&lStop Server",
               var3
                  ? List.of("&7Safely stop this managed server", "&cShift-click to confirm", "&7Blocked while players are connected")
                  : List.of("&7Server is not currently reported online")
            );
            call(var0, "setItem", 30, var4);
         } catch (Throwable var5) {
            warn("Could not decorate server editor with Stop Server: " + root(var5));
         }
      }
   }

   private static void handle(Object var0) {
      try {
         if (!(call(var0, "getRawSlot") instanceof Number var2 && var2.intValue() == 30)) {
            return;
         }

         Object var3 = call(var0, "getView");
         Object var4 = call(var3, "getTopInventory");
         Object var5 = call(var4, "getHolder");
         if (var5 == null || !var5.getClass().getName().equals("dev.hubpilot.hub.gui.AdminEntryHolder")) {
            return;
         }

         call(var0, "setCancelled", true);
         Object var6 = call(var0, "getClickedInventory");
         if (var6 != var4) {
            return;
         }

         Object var7 = call(var0, "getWhoClicked");
         if (!canAdmin(var7)) {
            message(var7, "You do not have HubPilot Admin access.");
            return;
         }

         String var8 = String.valueOf(call(var5, "destinationId"));
         if (!running(var8)) {
            message(var7, "HubPilot reports this server is not currently running.");
            return;
         }

         if (!Boolean.TRUE.equals(call(var0, "isShiftClick"))) {
            message(var7, "Shift-click Stop Server to confirm.");
            return;
         }

         if (!ServerControlBridge.stop(var7, ServerControlTarget.resolve(plugin, var8))) {
            message(var7, "Could not send the stop request to HubPilot Core.");
         }
      } catch (Throwable var9) {
         warn("Stop Server click failed: " + root(var9));
      }
   }

   private static boolean canAdmin(Object var0) {
      try {
         Class var1 = Class.forName("dev.hubpilot.hub.publicapi.PublicHubBootstrap");
         Method var2 = var1.getDeclaredMethod("canAdmin", Object.class);
         var2.setAccessible(true);
         return Boolean.TRUE.equals(var2.invoke(null, var0));
      } catch (Throwable var3) {
         return false;
      }
   }

   private static boolean running(String var0) {
      Object var1 = plugin;
      if (var1 == null) {
         return false;
      } else {
         try {
            Object var2 = call(var1, "getDestinationStore");
            Object var3 = call(var2, "find", var0);
            if (var3 == null) {
               return false;
            } else {
               String var4 = String.valueOf(call(var3, "statusTarget"));
               if (var4.isBlank()) {
                  var4 = String.valueOf(call(var3, "target"));
               }

               Object var5 = call(var1, "getStatusStore");
               Object var6 = call(var5, "get", var4);
               if (var6 == null) {
                  return false;
               } else {
                  String var7 = String.valueOf(call(var6, "state"));
                  return var7.equalsIgnoreCase("ONLINE") || var7.equalsIgnoreCase("STARTING");
               }
            }
         } catch (Throwable var8) {
            return false;
         }
      }
   }

   private static Object named(String var0, String var1, List<String> var2) throws Exception {
      ClassLoader var3 = plugin.getClass().getClassLoader();
      Class var4 = Class.forName("org.bukkit.Material", true, var3);
      Enum var5 = Enum.valueOf(var4, var0);
      Class var6 = Class.forName("dev.hubpilot.hub.util.MenuItems", true, var3);

      for (Method var10 : var6.getMethods()) {
         if (var10.getName().equals("named") && var10.getParameterCount() == 3) {
            return var10.invoke(null, var5, var1, var2);
         }
      }

      throw new NoSuchMethodException("MenuItems.named");
   }

   private static void message(Object var0, String var1) {
      try {
         call(var0, "sendMessage", "[HubPilot] " + var1);
      } catch (Throwable var3) {
      }
   }

   private static void warn(String var0) {
      try {
         Object var1 = plugin;
         if (var1 != null) {
            call(call(var1, "getLogger"), "warning", var0);
         }
      } catch (Throwable var2) {
      }
   }

   private static Object call(Object var0, String var1, Object... var2) throws Exception {
      if (var0 == null) {
         throw new IllegalStateException("Target for " + var1 + " is null");
      } else {
         Method var3 = null;

         for (Class var4 = var0.getClass(); var4 != null && var3 == null; var4 = var4.getSuperclass()) {
            for (Method var8 : var4.getDeclaredMethods()) {
               if (var8.getName().equals(var1) && var8.getParameterCount() == var2.length && compatible(var8.getParameterTypes(), var2)) {
                  var3 = var8;
                  break;
               }
            }
         }

         if (var3 == null) {
            for (Method var12 : var0.getClass().getMethods()) {
               if (var12.getName().equals(var1) && var12.getParameterCount() == var2.length && compatible(var12.getParameterTypes(), var2)) {
                  var3 = var12;
                  break;
               }
            }
         }

         if (var3 == null) {
            throw new NoSuchMethodException(var0.getClass().getName() + "." + var1 + "/" + var2.length);
         } else {
            var3.setAccessible(true);
            return var3.invoke(var0, var2);
         }
      }
   }

   private static boolean compatible(Class<?>[] var0, Object[] var1) {
      for (int var2 = 0; var2 < var0.length; var2++) {
         if (var1[var2] == null) {
            if (var0[var2].isPrimitive()) {
               return false;
            }
         } else if (!box(var0[var2]).isAssignableFrom(var1[var2].getClass())) {
            return false;
         }
      }

      return true;
   }

   private static Class<?> box(Class<?> var0) {
      if (!var0.isPrimitive()) {
         return var0;
      } else if (var0 == boolean.class) {
         return Boolean.class;
      } else if (var0 == byte.class) {
         return Byte.class;
      } else if (var0 == short.class) {
         return Short.class;
      } else if (var0 == int.class) {
         return Integer.class;
      } else if (var0 == long.class) {
         return Long.class;
      } else if (var0 == float.class) {
         return Float.class;
      } else if (var0 == double.class) {
         return Double.class;
      } else {
         return var0 == char.class ? Character.class : var0;
      }
   }

   private static Object objectMethod(Object var0, Method var1, Object[] var2, String var3) {
      String var4 = var1.getName();

      return switch (var4) {
         case "toString" -> var3;
         case "hashCode" -> System.identityHashCode(var0);
         case "equals" -> var0 == (var2 == null ? null : var2[0]);
         default -> null;
      };
   }

   private static String root(Throwable var0) {
      Throwable var1 = var0;

      while (var1.getCause() != null && var1.getCause() != var1) {
         var1 = var1.getCause();
      }

      String var2 = var1.getMessage();
      return var2 != null && !var2.isBlank() ? var2 : var1.getClass().getSimpleName();
   }
}
