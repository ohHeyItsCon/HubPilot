package dev.hubpilot.core;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;

final class HubPilotCommands {
   private final Object plugin;
   private final ProxyServer proxy;
   private final Logger logger;
   private final HubPilotConfig config;
   private final LifecycleManager lifecycle;
   private final CraftyProvider crafty;
   private final StatusStore status;
   private volatile Object statusChannel;
   private volatile Object statusEventHandler;

   HubPilotCommands(Object var1, ProxyServer var2, Logger var3, HubPilotConfig var4, LifecycleManager var5, CraftyProvider var6, StatusStore var7) {
      this.plugin = var1;
      this.proxy = var2;
      this.logger = var3;
      this.config = var4;
      this.lifecycle = var5;
      this.crafty = var6;
      this.status = var7;
   }

   void register() {
      Object var1 = invokeNoArgs(this.proxy, "getCommandManager");
      this.register(var1, "hub", this.commandProxy(new HubPilotCommands.HubCommand()));
      this.register(var1, "lobby", this.commandProxy(new HubPilotCommands.HubCommand()));
      this.register(var1, "hubpilot", this.commandProxy(new HubPilotCommands.AdminCommand()), "hp");

      try {
         this.registerStatusBridge();
      } catch (Throwable var3) {
         this.warn("Status bridge could not be enabled: " + unwrap(var3).getMessage());
      }
   }

   private Object commandProxy(HubPilotCommands.CommandLogic var1) {
      try {
         ClassLoader var2 = this.plugin.getClass().getClassLoader();
         if (var2 == null) {
            var2 = Thread.currentThread().getContextClassLoader();
         }

         Class var3 = Class.forName("com.velocitypowered.api.command.SimpleCommand", true, var2);
         InvocationHandler var4 = (var1x, var2x, var3x) -> {
            String var4x = var2x.getName();
            if (var2x.getDeclaringClass() == Object.class) {
               return switch (var4x) {
                  case "toString" -> "HubPilotDynamicCommand[" + var1.getClass().getSimpleName() + "]";
                  case "hashCode" -> System.identityHashCode(var1x);
                  case "equals" -> var1x == (var3x != null && var3x.length != 0 ? var3x[0] : null);
                  default -> null;
               };
            } else {
               Object var5x = var3x != null && var3x.length != 0 ? var3x[0] : null;
               HubPilotCommands.InvocationView var6 = HubPilotCommands.InvocationView.of(var5x);

               return switch (var4x) {
                  case "execute" -> {
                     var1.execute(var6);
                     yield null;
                  }
                  case "hasPermission" -> var1.hasPermission(var6);
                  case "suggest" -> var1.suggest(var6);
                  case "suggestAsync" -> CompletableFuture.completedFuture(var1.suggest(var6));
                  default -> defaultValue(var2x.getReturnType());
               };
            }
         };
         return Proxy.newProxyInstance(var2, new Class[]{var3}, var4);
      } catch (ReflectiveOperationException var5) {
         throw new IllegalStateException("Could not create a command compatible with this Velocity build", var5);
      }
   }

   private void register(Object var1, String var2, Object var3, String... var4) {
      try {
         for (Method var8 : var1.getClass().getMethods()) {
            Class[] var9 = var8.getParameterTypes();
            if (var8.getName().equals("register") && var9.length == 3 && var9[0] == String.class && var9[2].isArray() && var9[1].isInstance(var3)) {
               HubPilotReflect.invokeAccessible(var8, var1, new Object[]{var2, var3, var4});
               return;
            }
         }

         Method var16 = findMethod(var1.getClass(), "metaBuilder", 1);
         if (var16 == null) {
            throw new NoSuchMethodException("metaBuilder");
         } else {
            Object var17 = HubPilotReflect.invokeAccessible(var16, var1, new Object[]{var2});
            Method var18 = findArrayMethod(var17.getClass(), "aliases", String.class);
            if (var18 != null) {
               var17 = HubPilotReflect.invokeAccessible(var18, var17, new Object[]{var4});
            }

            Method var19 = findCompatibleMethod(var17.getClass(), "plugin", this.plugin);
            if (var19 != null) {
               var17 = HubPilotReflect.invokeAccessible(var19, var17, new Object[]{this.plugin});
            }

            Object var20 = HubPilotReflect.invokeAccessible(Objects.requireNonNull(findMethod(var17.getClass(), "build", 0)), var17, new Object[0]);

            for (Method var13 : var1.getClass().getMethods()) {
               if (var13.getName().equals("register") && var13.getParameterCount() == 2) {
                  Class[] var14 = var13.getParameterTypes();
                  if (var14[0].isInstance(var20) && var14[1].isInstance(var3)) {
                     HubPilotReflect.invokeAccessible(var13, var1, new Object[]{var20, var3});
                     return;
                  }
               }
            }

            throw new NoSuchMethodException("No compatible register method");
         }
      } catch (ReflectiveOperationException var15) {
         throw new IllegalStateException("Could not register /" + var2 + " on this Velocity build", unwrap(var15));
      }
   }

   private void connectToHub(Player var1) {
      String var2 = this.config.snapshot().hubServer();
      Optional var3 = optional(invoke(this.proxy, "getServer", var2));
      if (var3.isEmpty()) {
         send(var1, "The configured hub server '" + var2 + "' is not registered on Velocity.");
      } else {
         Optional var4 = optional(invokeNoArgs(var1, "getCurrentServer"));
         if (var4.isPresent()) {
            Object var5 = invokeNoArgs(var4.get(), "getServerInfo");
            Object var6 = invokeNoArgs(var5, "getName");
            if (var6 != null && var2.equalsIgnoreCase(String.valueOf(var6))) {
               send(var1, "You are already in the hub.");
               return;
            }
         }

         Object var9 = invokeCompatible(var1, "createConnectionRequest", var3.get());
         Method var10 = findMethod(var9.getClass(), "connectWithIndication", 0);
         if (var10 != null) {
            try {
               HubPilotReflect.invokeAccessible(var10, var9, new Object[0]);
               return;
            } catch (ReflectiveOperationException var8) {
            }
         }

         invokeNoArgs(var9, "fireAndForget");
      }
   }

   private void registerStatusBridge() throws Exception {
      ClassLoader var1 = this.plugin.getClass().getClassLoader();
      if (var1 == null) {
         var1 = Thread.currentThread().getContextClassLoader();
      }

      Class var2 = Class.forName("com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier", true, var1);
      Method var3 = var2.getMethod("create", String.class, String.class);
      this.statusChannel = HubPilotReflect.invokeAccessible(var3, null, new Object[]{"hubpilot", "status"});
      Object var4 = invokeNoArgs(this.proxy, "getChannelRegistrar");
      Method var5 = null;

      for (Method var9 : var4.getClass().getMethods()) {
         if (var9.getName().equals("register") && var9.getParameterCount() == 1) {
            Class var10 = var9.getParameterTypes()[0];
            if (var10.isArray() && var10.getComponentType().isInstance(this.statusChannel)) {
               var5 = var9;
               break;
            }
         }
      }

      if (var5 == null) {
         throw new NoSuchMethodException("ChannelRegistrar.register(ChannelIdentifier...)");
      } else {
         Object var17 = Array.newInstance(var5.getParameterTypes()[0].getComponentType(), 1);
         Array.set(var17, 0, this.statusChannel);
         HubPilotReflect.invokeAccessible(var5, var4, new Object[]{var17});
         Class var18 = Class.forName("com.velocitypowered.api.event.connection.PluginMessageEvent", true, var1);
         Class var19 = Class.forName("com.velocitypowered.api.event.EventHandler", true, var1);
         InvocationHandler var20 = (var1x, var2x, var3x) -> {
            if (var2x.getDeclaringClass() == Object.class) {
               return switch (var2x.getName()) {
                  case "toString" -> "HubPilotStatusBridge";
                  case "hashCode" -> System.identityHashCode(var1x);
                  case "equals" -> var1x == (var3x == null || var3x.length == 0 ? null : var3x[0]);
                  default -> null;
               };
            }
            if (var2x.getName().equals("execute") && var3x != null && var3x.length > 0) {
               this.handleStatusSyncEvent(var3x[0]);
               return null;
            }
            return defaultValue(var2x.getReturnType());
         };
         this.statusEventHandler = Proxy.newProxyInstance(var1, new Class[]{var19}, var20);
         Object var21 = invokeNoArgs(this.proxy, "getEventManager");
         Method var11 = null;

         for (Method var15 : var21.getClass().getMethods()) {
            if (var15.getName().equals("register") && var15.getParameterCount() == 3) {
               Class[] var16 = var15.getParameterTypes();
               if (var16[1] == Class.class && var16[2].isInstance(this.statusEventHandler)) {
                  var11 = var15;
                  break;
               }
            }
         }

         if (var11 == null) {
            throw new NoSuchMethodException("EventManager.register(plugin, Class, EventHandler)");
         } else {
            HubPilotReflect.invokeAccessible(var11, var21, new Object[]{this.plugin, var18, this.statusEventHandler});
            this.warn("HubPilot live status bridge enabled on hubpilot:status");
         }
      }
   }

   private void handleStatusSyncEvent(Object var1) {
      try {
         Object var2 = invokeNoArgs(var1, "getIdentifier");
         if (this.statusChannel != null && this.statusChannel.equals(var2)) {
            Object var3 = invokeNoArgs(var1, "getSource");
            if (this.isTrustedStatusSource(var3)) {
               if (invokeNoArgs(var1, "getData") instanceof byte[] var5) {
                  try (DataInputStream var6 = new DataInputStream(new ByteArrayInputStream(var5))) {
                     String var7 = var6.readUTF();
                     if (!"STATUS_SYNC_REQUEST".equals(var7)) {
                        return;
                     }
                  }

                  this.markPluginMessageHandled(var1);
                  HubPilotConfig.Snapshot var13 = this.config.snapshot();

                  for (ManagedServer var8 : var13.servers().values()) {
                     StatusStore.ServerStatus var9 = this.status.get(var8.id());
                     this.sendStatusRow(var3, var8.velocityServer(), var9);
                     if (!var8.id().equalsIgnoreCase(var8.velocityServer())) {
                        this.sendStatusRow(var3, var8.id(), var9);
                     }
                  }

                  this.sendStatusEnd(var3);
               }
            }
         }
      } catch (Throwable var12) {
         this.warn("Could not service status sync request: " + unwrap(var12).getMessage());
      }
   }

   private boolean isTrustedStatusSource(Object var1) {
      try {
         Object var2 = invokeNoArgs(var1, "getServerInfo");
         Object var3 = invokeNoArgs(var2, "getName");
         if (var3 == null) {
            return false;
         } else {
            String var4 = String.valueOf(var3).toLowerCase(Locale.ROOT);
            HubPilotConfig.Snapshot var5 = this.config.snapshot();
            return var4.equals(var5.hubServer().toLowerCase(Locale.ROOT)) || var5.trustedRequestServers().contains(var4);
         }
      } catch (Throwable var6) {
         return false;
      }
   }

   private void sendStatusRow(Object var1, String var2, StatusStore.ServerStatus var3) throws IOException {
      ByteArrayOutputStream var4 = new ByteArrayOutputStream();

      try (DataOutputStream var5 = new DataOutputStream(var4)) {
         var5.writeUTF("STATUS_ROW");
         var5.writeUTF(var2 == null ? "" : var2);
         var5.writeUTF(var3.state().name());
         var5.writeLong(var3.onlineSince());
         var5.writeLong(var3.lastStarted());
         var5.writeLong(var3.lastChecked());
         var5.writeInt(var3.playersOnline());
         var5.writeInt(var3.playersMax());
         var5.writeLong(var3.backendPingMs());
         var5.writeInt(var3.queueSize());
         var5.writeLong(var3.startRequestedAt());
         var5.writeInt(var3.expectedStartupSeconds());
         var5.writeUTF(var3.lastError() == null ? "" : var3.lastError());
      }

      this.sendPluginMessage(var1, var4.toByteArray());
   }

   private void sendStatusEnd(Object var1) throws IOException {
      ByteArrayOutputStream var2 = new ByteArrayOutputStream();

      try (DataOutputStream var3 = new DataOutputStream(var2)) {
         var3.writeUTF("STATUS_SYNC_END");
      }

      this.sendPluginMessage(var1, var2.toByteArray());
   }

   private void sendPluginMessage(Object var1, byte[] var2) {
      Method var3 = null;

      for (Method var7 : var1.getClass().getMethods()) {
         if (var7.getName().equals("sendPluginMessage") && var7.getParameterCount() == 2) {
            Class[] var8 = var7.getParameterTypes();
            if (var8[1] == byte[].class && var8[0].isInstance(this.statusChannel)) {
               var3 = var7;
               break;
            }
         }
      }

      if (var3 == null) {
         throw new IllegalStateException("Backend connection does not expose sendPluginMessage");
      } else {
         try {
            HubPilotReflect.invokeAccessible(var3, var1, new Object[]{this.statusChannel, var2});
         } catch (ReflectiveOperationException var9) {
            throw new IllegalStateException("Could not send status row", unwrap(var9));
         }
      }
   }

   private void markPluginMessageHandled(Object var1) {
      try {
         ClassLoader var2 = this.plugin.getClass().getClassLoader();
         Class var3 = Class.forName("com.velocitypowered.api.event.connection.PluginMessageEvent$ForwardResult", true, var2);
         Object var4 = HubPilotReflect.invokeAccessible(var3.getMethod("handled"), null, new Object[0]);
         Method var5 = findCompatibleMethod(var1.getClass(), "setResult", var4);
         if (var5 != null) {
            HubPilotReflect.invokeAccessible(var5, var1, new Object[]{var4});
         }
      } catch (ReflectiveOperationException var6) {
      }
   }

   private boolean requireAdmin(Object var1) {
      if (isAdmin(var1)) {
         return true;
      } else {
         send(var1, "This subcommand requires hubpilot.admin on the proxy, or must be run from the Velocity console.");
         return false;
      }
   }

   private static boolean isAdmin(Object var0) {
      return PublicCommandLayer.isAdmin(var0);
   }

   private void provider(Object var1, String[] var2) {
      if (var2.length < 3) {
         send(var1, "Usage: /hp provider <server> <crafty|always-online> [crafty-uuid]");
      } else {
         String var3 = var2[2].toLowerCase(Locale.ROOT);
         String var4 = var2.length >= 4 ? var2[3].trim() : "";
         if (!var3.equals("crafty") && !var3.equals("always-online")) {
            send(var1, "Provider must be crafty or always-online.");
         } else if (var3.equals("crafty") && !var4.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            send(var1, "Crafty requires a valid server UUID.");
         } else {
            try {
               this.config.setStartupProvider(var2[1], var3, var4);
               this.config.reload();
               this.crafty.update(this.config.snapshot().crafty());
               this.status.updateSharedDirectory(this.config.snapshot().sharedDirectory());
               send(var1, "Updated " + var2[1] + " to provider=" + var3 + (var4.isBlank() ? "" : ", UUID=" + var4) + ".");
            } catch (Exception var6) {
               this.warn("Could not update provider: " + var6.getMessage());
               send(var1, "Provider update failed: " + var6.getMessage());
            }
         }
      }
   }

   private void repair(Object var1) {
      try {
         int var2 = this.config.repairProviderMappings();
         this.config.reload();
         this.crafty.update(this.config.snapshot().crafty());
         this.status.updateSharedDirectory(this.config.snapshot().sharedDirectory());
         send(var1, "Provider repair finished. Updated " + var2 + " server file(s).");
      } catch (Exception var3) {
         this.warn("Provider repair failed: " + var3.getMessage());
         send(var1, "Provider repair failed: " + var3.getMessage());
      }
   }

   private void reload(Object var1) {
      try {
         this.config.reload();
         this.crafty.update(this.config.snapshot().crafty());
         this.status.updateSharedDirectory(this.config.snapshot().sharedDirectory());
         send(var1, "HubPilot configuration reloaded. Managed servers: " + this.config.snapshot().servers().size());
      } catch (Exception var3) {
         this.warn("HubPilot reload failed: " + var3.getMessage());
         send(var1, "Reload failed; the previous configuration remains active: " + var3.getMessage());
      }
   }

   private void doctor(Object var1) {
      HubPilotConfig.Snapshot var2 = this.config.snapshot();
      send(var1, "HubPilot Doctor:");
      send(var1, "- Managed servers: " + var2.servers().size());
      send(var1, "- Shared directory: " + var2.sharedDirectory());
      send(var1, "- Crafty provider: " + (var2.crafty().enabled() ? "enabled" : "disabled"));
      send(var1, "- Crafty token file: " + (Files.isReadable(Path.of(var2.crafty().tokenFile())) ? "readable" : "NOT readable"));

      for (ManagedServer var4 : var2.servers().values()) {
         boolean var5 = optional(invoke(this.proxy, "getServer", var4.velocityServer())).isPresent();
         boolean var6 = !var4.provider().equalsIgnoreCase("crafty") || !var4.providerServerId().isBlank();
         send(
            var1,
            "- "
               + var4.id()
               + ": Velocity="
               + (var5 ? "OK" : "MISSING")
               + ", velocity-name="
               + var4.velocityServer()
               + ", provider="
               + var4.provider()
               + (var6 ? "" : " (missing UUID)")
         );
      }
   }

   private static List<String> filter(List<String> var0, String var1) {
      String var2 = var1.toLowerCase(Locale.ROOT);
      return var0.stream().filter(var1x -> var1x.toLowerCase(Locale.ROOT).startsWith(var2)).sorted().toList();
   }

   private static boolean hasPermission(Object var0, String var1) {
      return invoke(var0, "hasPermission", var1) instanceof Boolean var3 && var3;
   }

   private static void send(Object var0, String var1) {
      if (var0 != null) {
         try {
            Method var8 = var0.getClass().getMethod("sendPlainMessage", String.class);
            HubPilotReflect.invokeAccessible(var8, var0, new Object[]{var1});
         } catch (ReflectiveOperationException var7) {
            try {
               ClassLoader var2 = var0.getClass().getClassLoader();
               Class var3 = Class.forName("net.kyori.adventure.text.Component", true, var2);
               Object var4 = HubPilotReflect.invokeAccessible(var3.getMethod("text", String.class), null, new Object[]{var1});
               Method var5 = findCompatibleMethod(var0.getClass(), "sendMessage", var4);
               if (var5 != null) {
                  HubPilotReflect.invokeAccessible(var5, var0, new Object[]{var4});
               }
            } catch (ReflectiveOperationException var6) {
            }
         }
      }
   }

   private void warn(String var1) {
      try {
         Method var2 = this.logger.getClass().getMethod("warn", String.class);
         HubPilotReflect.invokeAccessible(var2, this.logger, new Object[]{var1});
      } catch (ReflectiveOperationException var3) {
         System.err.println("[HubPilot] " + var1);
      }
   }

   private static Optional<?> optional(Object var0) {
      return var0 instanceof Optional var1 ? var1 : Optional.empty();
   }

   private static Object invokeNoArgs(Object var0, String var1) {
      return invoke(var0, var1);
   }

   private static Object invoke(Object var0, String var1, Object... var2) {
      if (var0 == null) {
         return null;
      } else {
         Method var3 = findCompatibleMethod(var0.getClass(), var1, var2);
         if (var3 == null) {
            throw new IllegalStateException("Missing method " + var0.getClass().getName() + "#" + var1);
         } else {
            try {
               return HubPilotReflect.invokeAccessible(var3, var0, var2);
            } catch (ReflectiveOperationException var5) {
               throw new IllegalStateException("Could not invoke " + var1 + ": " + unwrap(var5).getMessage(), unwrap(var5));
            }
         }
      }
   }

   private static Object invokeCompatible(Object var0, String var1, Object var2) {
      return invoke(var0, var1, var2);
   }

   private static Method findCompatibleMethod(Class<?> var0, String var1, Object... var2) {
      label33:
      for (Method var6 : var0.getMethods()) {
         if (var6.getName().equals(var1) && var6.getParameterCount() == var2.length) {
            Class[] var7 = var6.getParameterTypes();

            for (int var8 = 0; var8 < var7.length; var8++) {
               Object var9 = var2[var8];
               if (var9 != null && !box(var7[var8]).isInstance(var9)) {
                  continue label33;
               }
            }

            return var6;
         }
      }

      return null;
   }

   private static Method findMethod(Class<?> var0, String var1, int var2) {
      for (Method var6 : var0.getMethods()) {
         if (var6.getName().equals(var1) && var6.getParameterCount() == var2) {
            return var6;
         }
      }

      return null;
   }

   private static Method findArrayMethod(Class<?> var0, String var1, Class<?> var2) {
      for (Method var6 : var0.getMethods()) {
         if (var6.getName().equals(var1) && var6.getParameterCount() == 1) {
            Class var7 = var6.getParameterTypes()[0];
            if (var7.isArray() && var7.getComponentType() == var2) {
               return var6;
            }
         }
      }

      return null;
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

   private static Object defaultValue(Class<?> var0) {
      if (var0 == void.class || !var0.isPrimitive()) {
         return null;
      } else if (var0 == boolean.class) {
         return false;
      } else if (var0 == char.class) {
         return '\u0000';
      } else if (var0 == byte.class) {
         return (byte)0;
      } else if (var0 == short.class) {
         return (short)0;
      } else if (var0 == int.class) {
         return 0;
      } else if (var0 == long.class) {
         return 0L;
      } else if (var0 == float.class) {
         return 0.0F;
      } else {
         return var0 == double.class ? 0.0 : null;
      }
   }

   private static Throwable unwrap(Throwable var0) {
      Throwable var1 = var0;

      while (var1.getCause() != null && var1.getCause() != var1) {
         var1 = var1.getCause();
      }

      return var1;
   }

   private final class AdminCommand implements HubPilotCommands.CommandLogic {
      @Override
      public boolean hasPermission(HubPilotCommands.InvocationView var1) {
         return true;
      }

      @Override
      public void execute(HubPilotCommands.InvocationView var1) {
         if (!PublicGuiRouter.handle(var1.source(), var1.arguments())) {
            if (!DiscoveryCommand.handle(HubPilotCommands.this, var1)) {
               Object var2 = var1.source();
               String[] var3 = var1.arguments();
               if (var3.length == 0) {
                  HubPilotCommands.send(var2, "HubPilot Core 1.0.2 — /hp hub, /hp status <server>, /hp request <server>, /hp discover, /hp doctor, /hp cancel");
                  HubPilotCommands.send(var2, "Admin/console: /hp reload, /hp repair, /hp provider <server> <crafty|always-online> [uuid]");
               } else {
                  String var4 = var3[0].toLowerCase(Locale.ROOT);
                  switch (var4) {
                     case "reload":
                        if (HubPilotCommands.this.requireAdmin(var2)) {
                           HubPilotCommands.this.reload(var2);
                        }
                        break;
                     case "repair":
                        if (HubPilotCommands.this.requireAdmin(var2)) {
                           HubPilotCommands.this.repair(var2);
                        }
                        break;
                     case "provider":
                     case "setprovider":
                        if (HubPilotCommands.this.requireAdmin(var2)) {
                           HubPilotCommands.this.provider(var2, var3);
                        }
                        break;
                     case "doctor":
                        HubPilotCommands.this.doctor(var2);
                        break;
                     case "status":
                        if (var3.length < 2) {
                           HubPilotCommands.send(var2, "Usage: /hp status <server>");
                        } else {
                           HubPilotCommands.send(var2, HubPilotCommands.this.lifecycle.describe(var3[1]));
                        }
                        break;
                     case "request":
                        if (var2 instanceof Player var8) {
                           if (var3.length < 2) {
                              HubPilotCommands.send(var2, "Usage: /hp request <server>");
                           } else {
                              HubPilotCommands.this.lifecycle.request(var8, var3[1], "command");
                           }
                        } else {
                           HubPilotCommands.send(var2, "Only players can request a server.");
                        }
                        break;
                     case "cancel":
                        if (var2 instanceof Player var7) {
                           HubPilotCommands.this.lifecycle.cancel(var7);
                        } else {
                           HubPilotCommands.send(var2, "Only players can cancel a request.");
                        }
                        break;
                     case "hub":
                     case "lobby":
                        if (var2 instanceof Player var6) {
                           HubPilotCommands.this.connectToHub(var6);
                        } else {
                           HubPilotCommands.send(var2, "Only players can return to the hub.");
                        }
                        break;
                     default:
                        HubPilotCommands.send(var2, "Unknown HubPilot subcommand. Try /hp help.");
                  }
               }
            }
         }
      }

      @Override
      public List<String> suggest(HubPilotCommands.InvocationView var1) {
         List var10000 = DiscoveryCommand.suggest(HubPilotCommands.this, var1);
         if (var10000 != null) {
            return var10000;
         } else {
            String[] var2 = var1.arguments();
            if (var2.length <= 1) {
               ArrayList var3 = new ArrayList<>(List.of("hub", "status", "request", "doctor", "cancel"));
               if (HubPilotCommands.isAdmin(var1.source())) {
                  var3.addAll(List.of("reload", "repair", "provider"));
               }

               return HubPilotCommands.filter(var3, var2.length == 0 ? "" : var2[0]);
            } else if (var2.length != 2
               || !var2[0].equalsIgnoreCase("status")
                  && !var2[0].equalsIgnoreCase("request")
                  && !var2[0].equalsIgnoreCase("provider")
                  && !var2[0].equalsIgnoreCase("setprovider")) {
               return var2.length != 3 || !var2[0].equalsIgnoreCase("provider") && !var2[0].equalsIgnoreCase("setprovider")
                  ? List.of()
                  : HubPilotCommands.filter(List.of("crafty", "always-online"), var2[2]);
            } else {
               return HubPilotCommands.filter(new ArrayList<>(HubPilotCommands.this.config.snapshot().servers().keySet()), var2[1]);
            }
         }
      }
   }

   private interface CommandLogic {
      void execute(HubPilotCommands.InvocationView var1);

      default boolean hasPermission(HubPilotCommands.InvocationView var1) {
         return true;
      }

      default List<String> suggest(HubPilotCommands.InvocationView var1) {
         return List.of();
      }
   }

   private final class HubCommand implements HubPilotCommands.CommandLogic {
      public void executeLegacy(HubPilotCommands.InvocationView var1) {
         Object var2 = var1.source();
         if (var2 instanceof Player var3) {
            try {
               HubPilotCommands.this.connectToHub(var3);
            } catch (Throwable var6) {
               Throwable var5 = HubPilotCommands.unwrap(var6);
               HubPilotCommands.this.warn("/hub failed for player: " + var5.getMessage());
               HubPilotCommands.send(var3, "HubPilot could not return you to the hub: " + var5.getMessage());
            }
         } else {
            HubPilotCommands.send(var2, "This command can only be used by a player.");
         }
      }

      @Override
      public void execute(HubPilotCommands.InvocationView var1) {
         if (PublicCommandLayer.allowPlayerCommand(var1.source())) {
            this.executeLegacy(var1);
         }
      }
   }

   private record InvocationView(Object source, String[] arguments) {
      static HubPilotCommands.InvocationView of(Object var0) {
         if (var0 == null) {
            return new HubPilotCommands.InvocationView(null, new String[0]);
         } else {
            Object var1 = HubPilotCommands.invokeNoArgs(var0, "source");
            Object var2 = HubPilotCommands.invokeNoArgs(var0, "arguments");
            if (var2 instanceof String[] var7) {
               return new HubPilotCommands.InvocationView(var1, var7);
            } else if (var2 != null && var2.getClass().isArray()) {
               int var6 = Array.getLength(var2);
               String[] var4 = new String[var6];

               for (int var5 = 0; var5 < var6; var5++) {
                  var4[var5] = String.valueOf(Array.get(var2, var5));
               }

               return new HubPilotCommands.InvocationView(var1, var4);
            } else {
               return var2 instanceof Collection<?> var3
                  ? new HubPilotCommands.InvocationView(var1, var3.stream().map(String::valueOf).toArray(String[]::new))
                  : new HubPilotCommands.InvocationView(var1, new String[0]);
            }
         }
      }
   }
}
