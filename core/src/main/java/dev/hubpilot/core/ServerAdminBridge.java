package dev.hubpilot.core;

import java.io.DataInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class ServerAdminBridge {
   private ServerAdminBridge() {
   }

   static String handleUnknown(Object var0, UUID var1, String var2, DataInputStream var3) {
      if (!"SERVER_STOP".equalsIgnoreCase(var2)) {
         return "Unknown HubPilot setup action: " + var2;
      } else {
         try {
            String var4 = clean(var3.readUTF());
            if (var4.isEmpty()) {
               return "Stop failed: missing server id.";
            } else {
               Object var5 = invokeStatic("dev.hubpilot.core.AccessManager", "get");
               Object var6 = invoke(var5, "can", var1, "hubpilot.servers.stop");
               if (!Boolean.TRUE.equals(var6)) {
                  return "You do not have permission to stop servers.";
               } else {
                  Object var7 = staticField("dev.hubpilot.core.PublicBootstrap", "config");
                  if (var7 == null) {
                     return "Stop failed: HubPilot configuration is unavailable.";
                  } else {
                     Object var8 = invoke(var7, "snapshot");
                     if (invoke(var8, "servers") instanceof Map var10) {
                        Object var11 = findManaged(var10, var4);
                        if (var11 == null) {
                           return "Stop failed: managed server '" + var4 + "' was not found.";
                        } else {
                           String var12 = str(invoke(var11, "velocityServer"));
                           String var13 = str(invoke(var11, "label"));
                           if (var13.isBlank()) {
                              var13 = var4;
                           }

                           String var14 = str(invoke(var8, "hubServer"));
                           if (!var14.isBlank() && var14.equalsIgnoreCase(var12)) {
                              return "Refusing to stop the Hub server from the Hub.";
                           } else {
                              int var15 = connectedPlayers(var12);
                              if (var15 == -2) {
                                 return "Stop failed: Velocity target '" + var12 + "' is not registered.";
                              } else if (var15 < 0) {
                                 return "Stop failed: could not safely verify connected players for " + var13 + ".";
                              } else if (var15 > 0) {
                                 return "Refusing to stop " + var13 + " because " + var15 + " player" + (var15 == 1 ? " is" : "s are") + " connected.";
                              } else {
                                 String var16 = str(invoke(var11, "provider"));
                                 if (!var16.isBlank() && !var16.equalsIgnoreCase("always-on") && !var16.equalsIgnoreCase("always-online")) {
                                    Object var17 = invokeStatic("dev.hubpilot.core.ProviderRegistry", "get");
                                    Object var18 = invoke(var17, "action", var11, "stop");
                                    return var18 == null
                                       ? "Stop failed: provider did not accept the request."
                                       : "SUCCESS: Stop request sent for " + var13 + ".";
                                 } else {
                                    return "Stop failed: " + var13 + " has no controllable provider.";
                                 }
                              }
                           }
                        }
                     } else {
                        return "Stop failed: managed server list is unavailable.";
                     }
                  }
               }
            }
         } catch (Throwable var19) {
            return "Stop failed: " + root(var19);
         }
      }
   }

   private static int connectedPlayers(String var0) {
      if (var0 != null && !var0.isBlank()) {
         try {
            Object var1 = staticField("dev.hubpilot.core.PublicBootstrap", "proxy");
            if (var1 == null) {
               return -1;
            } else {
               Object var2 = invoke(var1, "getServer", var0);
               Object var3 = var2 instanceof Optional var4 ? var4.orElse(null) : var2;
               if (var3 == null) {
                  return -2;
               } else {
                  return invoke(var3, "getPlayersConnected") instanceof Collection var5 ? var5.size() : 0;
               }
            }
         } catch (Throwable var6) {
            return -1;
         }
      } else {
         return -1;
      }
   }

   private static Object findManaged(Map<?, ?> var0, String var1) throws Exception {
      Object var2 = var0.get(var1);
      if (var2 != null) {
         return var2;
      } else {
         for (Object var4 : var0.values()) {
            if (var4 != null) {
               String var5 = str(invoke(var4, "id"));
               String var6 = str(invoke(var4, "velocityServer"));
               String var7 = str(invoke(var4, "label"));
               if (var1.equalsIgnoreCase(var5) || var1.equalsIgnoreCase(var6) || var1.equalsIgnoreCase(var7)) {
                  return var4;
               }
            }
         }

         return null;
      }
   }

   private static Object staticField(String var0, String var1) throws Exception {
      Class var2 = Class.forName(var0);
      Field var3 = var2.getDeclaredField(var1);
      var3.setAccessible(true);
      return var3.get(null);
   }

   private static Object invokeStatic(String var0, String var1, Object... var2) throws Exception {
      return invokeOn(Class.forName(var0), null, var1, var2);
   }

   private static Object invoke(Object var0, String var1, Object... var2) throws Exception {
      if (var0 == null) {
         throw new IllegalStateException("Target for " + var1 + " is null");
      } else {
         return invokeOn(var0.getClass(), var0, var1, var2);
      }
   }

   private static Object invokeOn(Class<?> var0, Object var1, String var2, Object... var3) throws Exception {
      Method var4 = null;

      for (Class var5 = var0; var5 != null && var4 == null; var5 = var5.getSuperclass()) {
         for (Method var9 : var5.getDeclaredMethods()) {
            if (var9.getName().equals(var2) && var9.getParameterCount() == var3.length && compatible(var9.getParameterTypes(), var3)) {
               var4 = var9;
               break;
            }
         }
      }

      if (var4 == null) {
         for (Method var13 : var0.getMethods()) {
            if (var13.getName().equals(var2) && var13.getParameterCount() == var3.length && compatible(var13.getParameterTypes(), var3)) {
               var4 = var13;
               break;
            }
         }
      }

      if (var4 == null) {
         throw new NoSuchMethodException(var0.getName() + "." + var2 + "/" + var3.length);
      } else {
         var4.setAccessible(true);
         return var4.invoke(var1, var3);
      }
   }

   private static boolean compatible(Class<?>[] var0, Object[] var1) {
      for (int var2 = 0; var2 < var0.length; var2++) {
         if (var1[var2] == null) {
            if (var0[var2].isPrimitive()) {
               return false;
            }
         } else {
            Class var3 = box(var0[var2]);
            if (!var3.isAssignableFrom(var1[var2].getClass())) {
               return false;
            }
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

   private static String str(Object var0) {
      return var0 == null ? "" : String.valueOf(var0);
   }

   private static String clean(String var0) {
      return var0 == null ? "" : var0.trim();
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
