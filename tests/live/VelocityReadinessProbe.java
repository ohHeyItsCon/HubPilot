package dev.hubpilot.readiness;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import java.lang.reflect.*;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Exercises real Core provider transport and real Velocity pings, not player admission. */
public final class VelocityReadinessProbe {
    private final ProxyServer proxy;
    private final Logger logger;
    @Inject public VelocityReadinessProbe(ProxyServer proxy, Logger logger) { this.proxy=proxy; this.logger=logger; }
    private static Object call(Object object, String name, Class<?>[] types, Object... args) throws Exception {
        Class<?> type=object instanceof Class<?> c ? c : object.getClass();
        Method method=type.getDeclaredMethod(name,types);method.setAccessible(true);
        return method.invoke(object instanceof Class<?> ? null : object,args);
    }
    private static void result(Object result) throws Exception {
        if (!Boolean.TRUE.equals(call(result,"success",new Class<?>[0]))) throw new AssertionError("Provider operation rejected");
    }
    @Subscribe public void initialize(ProxyInitializeEvent event) {
        proxy.getScheduler().buildTask(this, () -> {
            Object registry=null,game=null;
            boolean validated=false;
            try {
                if (!Boolean.getBoolean("hubpilot.readiness.lab")) throw new IllegalStateException("Explicit isolated-lab opt-in required");
                Object core=proxy.getPluginManager().getPlugin("hubpilot-core").orElseThrow().getInstance().orElseThrow();
                Field field=core.getClass().getDeclaredField("config");field.setAccessible(true);Object config=field.get(core);
                Object snapshot=call(config,"snapshot",new Class<?>[0]);
                game=((Map<?,?>)call(snapshot,"servers",new Class<?>[0])).get("game");
                if (game==null) throw new AssertionError("Missing isolated game definition");
                Class<?> type=Class.forName("dev.hubpilot.core.ProviderRegistry",true,core.getClass().getClassLoader());
                registry=call(type,"get",new Class<?>[0]);
                Object definition=call(registry,"primaryDefinition",new Class<?>[0]);
                if (!"https://127.0.0.1:8443".equals(call(definition,"baseUrl",new Class<?>[0]))) throw new IllegalStateException("Not the loopback fixture provider");
                if (!"crafty".equals(call(game,"provider",new Class<?>[0]))) throw new IllegalStateException("Not the fixture provider mapping");
                String expectedId=System.getProperty("hubpilot.readiness.serverId", "");
                if (expectedId.isEmpty() || !expectedId.equals(call(game,"providerServerId",new Class<?>[0]))) throw new IllegalStateException("Fixture server identity mismatch");
                if (!proxy.getServer("game").orElseThrow().getServerInfo().getAddress().getAddress().isLoopbackAddress()) throw new IllegalStateException("Not a loopback backend");
                validated=true;
                result(call(registry,"test",new Class<?>[]{String.class},"crafty"));
                logger.info("READINESS_PASS real Core Crafty test HTTP");
                result(((CompletableFuture<?>)call(registry,"action",new Class<?>[]{game.getClass(),String.class},game,"start")).get(30,TimeUnit.SECONDS));
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(150);boolean ready=false;
                while (System.nanoTime()<deadline) {
                    try { proxy.getServer("game").orElseThrow().ping().get(3,TimeUnit.SECONDS);ready=true;break; }
                    catch (Exception notReady) { Thread.sleep(1000); }
                }
                if (!ready) throw new AssertionError("Provider accepted start but Minecraft status never became reachable");
                logger.info("READINESS_PASS real Crafty start -> Paper -> Velocity Minecraft status ping");
                Thread.sleep(5000); // Let the independent Paper menu probe finish before provider cleanup.
            } catch (Throwable error) { logger.error("READINESS_FAIL {}: {}",error.getClass().getName(),error.getMessage()); }
            finally {
                if (validated && registry!=null && game!=null) try {
                    result(((CompletableFuture<?>)call(registry,"action",new Class<?>[]{game.getClass(),String.class},game,"stop")).get(30,TimeUnit.SECONDS));
                    logger.info("READINESS_PASS real Core Crafty stop HTTP (process exit checked separately)");
                } catch (Throwable error) { logger.error("READINESS_FAIL stop: {}",error.getClass().getName()); }
            }
        }).delay(Duration.ofSeconds(10)).schedule();
    }
}
