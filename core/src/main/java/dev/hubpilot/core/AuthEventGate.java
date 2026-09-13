package dev.hubpilot.core;

import com.velocitypowered.api.event.connection.PluginMessageEvent;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

final class AuthEventGate {
   private static final Map<Object, Boolean> SEEN = Collections.synchronizedMap(new WeakHashMap<>());

   private AuthEventGate() {
   }

   static void dispatchMain(Object var0) {
      if (var0 instanceof PluginMessageEvent var1) {
         dispatch(new PublicBootstrap.AuthBridge(), var1);
      }
   }

   static void dispatchBridge(Object var0, Object var1) {
      if (var0 instanceof PublicBootstrap.AuthBridge var2) {
         if (var1 instanceof PluginMessageEvent var3) {
            dispatch(var2, var3);
         }
      }
   }

   private static void dispatch(PublicBootstrap.AuthBridge var0, PluginMessageEvent var1) {
      synchronized (SEEN) {
         if (SEEN.put(var1, Boolean.TRUE) != null) {
            return;
         }
      }

      var0.onPluginMessage0(var1);
   }
}
