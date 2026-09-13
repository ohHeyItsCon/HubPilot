package dev.hubpilot.core;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class PublicControlGate {
   private PublicControlGate() {
   }

   static boolean dispatch(Object var0) {
      if (var0 == null) {
         return false;
      } else {
         try {
            Class var1 = Class.forName("com.velocitypowered.api.event.connection.PluginMessageEvent");
            if (!var1.isInstance(var0)) {
               return false;
            } else {
               Object var2 = var1.getMethod("getIdentifier").invoke(var0);
               if (var2 == null) {
                  return false;
               } else {
                  Class var3 = Class.forName("com.velocitypowered.api.proxy.messages.ChannelIdentifier");
                  String var4 = String.valueOf(var3.getMethod("getId").invoke(var2));
                  if (!"hubpilot:control".equalsIgnoreCase(var4)) {
                     return false;
                  } else {
                     byte[] var5 = (byte[])var1.getMethod("getData").invoke(var0);
                     if (var5 != null && var5.length != 0) {
                        DataInputStream var7 = new DataInputStream(new ByteArrayInputStream(var5));

                        String var6;
                        try {
                           var6 = var7.readUTF();
                        } catch (Throwable var11) {
                           try {
                              var7.close();
                           } catch (Throwable var10) {
                              var11.addSuppressed(var10);
                           }

                           throw var11;
                        }

                        var7.close();
                        if (!"AUTH".equals(var6) && !"COMPONENT".equals(var6) && !"ACTION".equals(var6)) {
                           return false;
                        } else {
                           Method var14 = DirectAuthHandler.class.getDeclaredMethod("dispatch", var1);
                           var14.setAccessible(true);
                           var14.invoke(null, var0);
                           return true;
                        }
                     } else {
                        return false;
                     }
                  }
               }
            }
         } catch (InvocationTargetException var12) {
            return false;
         } catch (Throwable var13) {
            return false;
         }
      }
   }
}
