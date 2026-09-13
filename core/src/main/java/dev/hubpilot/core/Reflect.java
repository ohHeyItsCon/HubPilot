package dev.hubpilot.core;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class Reflect {
   private Reflect() {
   }

   static Object call(Object var0, String var1, Object... var2) {
      if (var0 == null) {
         return null;
      } else {
         try {
            Method var3 = find(var0.getClass(), var1, var2);
            if (var3 == null) {
               throw new NoSuchMethodException(var0.getClass().getName() + "." + var1);
            } else {
               try {
                  if (!var3.canAccess(var0)) {
                     var3.trySetAccessible();
                  }
               } catch (RuntimeException var6) {
               }

               return var3.invoke(var0, var2);
            }
         } catch (InvocationTargetException var7) {
            Throwable var4 = var7.getCause();
            if (var4 instanceof RuntimeException var5) {
               throw var5;
            } else {
               throw new IllegalStateException((Throwable)(var4 == null ? var7 : var4));
            }
         } catch (Throwable var8) {
            throw new IllegalStateException(var8);
         }
      }
   }

   static Object callStatic(Class<?> var0, String var1, Object... var2) {
      try {
         Method var3 = find(var0, var1, var2);
         if (var3 == null) {
            throw new NoSuchMethodException(var0.getName() + "." + var1);
         } else {
            try {
               if (!var3.canAccess(null)) {
                  var3.trySetAccessible();
               }
            } catch (RuntimeException var6) {
            }

            return var3.invoke(null, var2);
         }
      } catch (InvocationTargetException var7) {
         Throwable var4 = var7.getCause();
         if (var4 instanceof RuntimeException var5) {
            throw var5;
         } else {
            throw new IllegalStateException((Throwable)(var4 == null ? var7 : var4));
         }
      } catch (Throwable var8) {
         throw new IllegalStateException(var8);
      }
   }

   static Method find(Class<?> var0, String var1, Object... var2) {
      LinkedHashSet<Class<?>> var3 = new LinkedHashSet<>();
      collectInterfaces(var0, var3);

      for (Class var5 : var3) {
         Method var6 = compatiblePublic(var5.getMethods(), var1, var2);
         if (var6 != null) {
            return var6;
         }
      }

      for (Method var7 : var0.getMethods()) {
         if (Modifier.isPublic(var7.getDeclaringClass().getModifiers()) && compatible(var7, var1, var2)) {
            return var7;
         }
      }

      for (Class var13 : hierarchy(var0)) {
         for (Method var9 : var13.getDeclaredMethods()) {
            if (compatible(var9, var1, var2)) {
               return var9;
            }
         }
      }

      return null;
   }

   private static Method compatiblePublic(Method[] var0, String var1, Object[] var2) {
      for (Method var6 : var0) {
         if (compatible(var6, var1, var2)) {
            return var6;
         }
      }

      return null;
   }

   private static boolean compatible(Method var0, String var1, Object[] var2) {
      if (var0.getName().equals(var1) && var0.getParameterCount() == var2.length) {
         Class[] var3 = var0.getParameterTypes();

         for (int var4 = 0; var4 < var3.length; var4++) {
            if (var2[var4] == null) {
               if (var3[var4].isPrimitive()) {
                  return false;
               }
            } else if (!box(var3[var4]).isAssignableFrom(var2[var4].getClass())) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static void collectInterfaces(Class<?> var0, Set<Class<?>> var1) {
      for (Class var2 = var0; var2 != null; var2 = var2.getSuperclass()) {
         for (Class var6 : var2.getInterfaces()) {
            collectInterface(var6, var1);
         }
      }
   }

   private static void collectInterface(Class<?> var0, Set<Class<?>> var1) {
      if (var1.add(var0)) {
         for (Class var5 : var0.getInterfaces()) {
            collectInterface(var5, var1);
         }
      }
   }

   static Field field(Class<?> var0, String var1) {
      for (Class var3 : hierarchy(var0)) {
         try {
            Field var4 = var3.getDeclaredField(var1);

            try {
               var4.trySetAccessible();
            } catch (RuntimeException var6) {
            }

            return var4;
         } catch (NoSuchFieldException var7) {
         }
      }

      return null;
   }

   static Object get(Object var0, String var1) {
      try {
         Field var2 = field(var0.getClass(), var1);
         return var2 == null ? null : var2.get(var0);
      } catch (IllegalAccessException var3) {
         throw new IllegalStateException(var3);
      }
   }

   private static List<Class<?>> hierarchy(Class<?> var0) {
      ArrayList var1 = new ArrayList();

      for (Class var2 = var0; var2 != null; var2 = var2.getSuperclass()) {
         var1.add(var2);
      }

      return var1;
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
         return var0 == char.class ? Character.class : Void.class;
      }
   }
}
