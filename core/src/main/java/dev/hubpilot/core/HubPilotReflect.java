package dev.hubpilot.core;

import java.lang.reflect.Method;

final class HubPilotReflect {
   private HubPilotReflect() {
   }

   static Object invokeAccessible(Method var0, Object var1, Object[] var2) throws ReflectiveOperationException {
      if (var0 == null) {
         throw new NoSuchMethodException("HubPilot reflection method was null");
      } else {
         try {
            var0.trySetAccessible();
         } catch (RuntimeException var4) {
         }

         return var0.invoke(var1, var2);
      }
   }
}
