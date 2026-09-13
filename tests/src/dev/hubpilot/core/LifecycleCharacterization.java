package dev.hubpilot.core;

import com.sun.net.httpserver.HttpServer;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.scheduler.Scheduler;
import com.velocitypowered.api.scheduler.ScheduledTask;
import java.lang.reflect.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

/** Executes unchanged production lifecycle/config/provider classes with controlled platform boundaries. */
public final class LifecycleCharacterization {
    interface Checked { void run() throws Exception; }
    static final Logger LOG = NOPLogger.NOP_LOGGER;
    static int passed;

    public static void main(String[] args) throws Exception {
        test("shared-start-dedup-and-cancel-preserves-other-demand", () -> {
            try (Lab l = new Lab()) {
                Guest a = l.guest(), b = l.guest();
                l.lifecycle.request(a.player, "game", "test");
                l.lifecycle.request(b.player, "game", "test");
                l.lifecycle.request(a.player, "game", "test");
                l.accepted("game");
                check(l.starts.get() == 1 && l.lifecycle.queueSize("game") == 2, "one shared start and two distinct UUIDs");
                l.lifecycle.cancel(a.player);
                check(l.lifecycle.queueSize("game") == 1 && l.stops.get() == 0, "first cancellation preserves second requester");
                l.lifecycle.cancel(b.player);
                await(() -> l.stops.get() == 1);
                check(l.lifecycle.queueSize("game") == 0, "last cancellation removes queue");
            }
        });
        test("concurrent-shared-start-coalesces-eight-requesters", () -> {
            try (Lab l = new Lab()) {
                List<Guest> guests = new ArrayList<>();
                for (int i = 0; i < 8; i++) guests.add(l.guest());
                CountDownLatch ready = new CountDownLatch(8), release = new CountDownLatch(1);
                ExecutorService pool = Executors.newFixedThreadPool(8);
                try {
                    List<Future<?>> jobs = new ArrayList<>();
                    for (Guest guest : guests) jobs.add(pool.submit(() -> {
                        ready.countDown();
                        try { check(release.await(5, TimeUnit.SECONDS), "barrier released"); }
                        catch (InterruptedException e) { throw new RuntimeException(e); }
                        l.lifecycle.request(guest.player, "game", "concurrent-test");
                    }));
                    check(ready.await(5, TimeUnit.SECONDS), "all requesters ready"); release.countDown();
                    for (Future<?> job : jobs) job.get(5, TimeUnit.SECONDS);
                    l.accepted("game");
                    check(l.starts.get() == 1 && l.lifecycle.queueSize("game") == 8, "one provider start for eight simultaneous requests");
                } finally { release.countDown(); pool.shutdownNow(); pool.awaitTermination(5, TimeUnit.SECONDS); }
            }
        });
        test("control-dispatch-bytecode-true-success-path", () -> {
            try (Lab l = new Lab()) {
                Guest guest = l.guest();
                var channel = com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier.from("hubpilot:control");
                for (String opcode : List.of("AUTH", "COMPONENT", "ACTION", "STATE")) {
                    var bytes = new java.io.ByteArrayOutputStream();
                    try (var stream = new java.io.DataOutputStream(bytes)) { stream.writeUTF(opcode); }
                    var event = new com.velocitypowered.api.event.connection.PluginMessageEvent(guest.player, guest.player, channel, bytes.toByteArray());
                    check(PublicControlGate.dispatch(event) == !opcode.equals("STATE"), "recognized control dispatch reports true even when inner handler rejects untrusted client");
                }
                check(!PublicControlGate.dispatch(null) && !PublicControlGate.dispatch(new Object()), "unsupported input rejected");
            }
        });
        test("provider-auth-reconstructed-switches-match-baseline", () -> {
            try (Lab l = new Lab()) {
                Files.writeString(l.dir.resolve("secrets.yml"), "secrets:\n  fixture-key: fixture-only\n"); ProviderRegistry.get().reload();
                for (String type : List.of("crafty", "pterodactyl", "generic-http")) {
                    var definition = new ProviderRegistry.Definition("test", type, true, "http://127.0.0.1", "fixture-key", false, 10, Map.of());
                    var request = ((java.net.http.HttpRequest.Builder)invoke(ProviderRegistry.get(), "request", definition, java.net.URI.create("http://127.0.0.1/fixture"))).GET().build();
                    check(request.headers().firstValue("Authorization").orElse("").equals(type.equals("generic-http") ? "" : "Bearer fixture-only"), "provider default authentication");
                }
                for (String auth : List.of("bearer", "basic", "x-api-key", "none")) {
                    var definition = new ProviderRegistry.Definition("test", "generic-http", true, "http://127.0.0.1", "fixture-key", false, 10, Map.of("auth-type",auth));
                    var request = ((java.net.http.HttpRequest.Builder)invoke(ProviderRegistry.get(), "request", definition, java.net.URI.create("http://127.0.0.1/fixture"))).GET().build();
                    String expected = switch(auth) { case "bearer" -> "Bearer fixture-only"; case "basic" -> "Basic Zml4dHVyZS1vbmx5"; default -> ""; };
                    check(request.headers().firstValue("Authorization").orElse("").equals(expected), "explicit authentication mode");
                    check(request.headers().firstValue("X-API-Key").orElse("").equals(auth.equals("x-api-key") ? "fixture-only" : ""), "API-key header mode");
                }
            }
        });
        test("existing-behavior-cancel-before-ping-does-not-invalidate-callback", () -> {
            try (Lab l = new Lab()) {
                Guest a = l.guest();
                l.backends.get("game").ping = new CompletableFuture<>();
                l.lifecycle.request(a.player, "game", "test");
                l.lifecycle.cancel(a.player);
                l.backends.get("game").ping.completeExceptionally(new IllegalStateException("offline"));
                l.accepted("game");
                check(l.lifecycle.queueSize("game") == 1, "late ping callback requeues cancelled UUID");
            }
        });
        test("online-join-bypasses-countdown-and-start", () -> {
            try (Lab l = new Lab()) {
                Guest a = l.guest(); l.online("game");
                l.lifecycle.request(a.player, "game", "test");
                check(a.targets.equals(List.of("game")) && l.starts.get() == 0 && l.lifecycle.queueSize("game") == 0, "direct online connection");
            }
        });
        test("countdown-fanout-does-not-serialize-admission", () -> {
            try (Lab l = new Lab()) {
                Guest a = l.guest(), b = l.guest();
                l.lifecycle.request(a.player, "game", "test"); l.lifecycle.request(b.player, "game", "test");
                l.accepted("game"); l.online("game");
                Object session = l.session("game");
                long now = System.currentTimeMillis();
                invoke(l.lifecycle, "tickSession", session, now);
                check(a.targets.isEmpty() && b.targets.isEmpty(), "countdown not skipped for queued starts");
                invoke(l.lifecycle, "tickSession", session, now + 6000);
                check(a.targets.size() == 1 && b.targets.size() == 1 && l.lifecycle.queueSize("game") == 0, "all waiting players attempted together");
            }
        });
        test("existing-behavior-delayed-retry-survives-cancel-and-new-destination", () -> {
            try (Lab l = new Lab()) {
                Guest a = l.guest(); l.online("game"); l.online("other");
                l.lifecycle.request(a.player, "game", "test");
                a.connections.get(0).completeExceptionally(new IllegalStateException("join failed"));
                check(l.tasks.size() == 1, "connection retry scheduled");
                l.lifecycle.cancel(a.player);
                l.lifecycle.request(a.player, "other", "test");
                a.current = l.backends.get("other");
                a.connections.get(1).complete(result(true));
                l.lifecycle.onPostConnect(new ServerPostConnectEvent(a.player, null));
                l.tasks.remove(0).run();
                check(a.targets.equals(List.of("game", "other", "game")), "old retry reconnects to captured destination");
            }
        });
        test("existing-behavior-retry-does-not-revalidate-current-maintenance", () -> {
            try (Lab l = new Lab()) {
                Guest a = l.guest(); l.online("game");
                l.lifecycle.request(a.player, "game", "test");
                a.connections.get(0).completeExceptionally(new IllegalStateException("join failed"));
                l.server("game", "game", true, 1, true); l.config.reload();
                check(l.config.snapshot().servers().get("game").maintenance(), "new config has maintenance");
                l.tasks.remove(0).run();
                check(a.targets.size() == 2, "captured retry still attempts connection");
                Guest b = l.guest(); l.lifecycle.request(b.player, "game", "test");
                check(b.targets.isEmpty(), "fresh request is denied by maintenance");
            }
        });
        test("existing-behavior-failed-online-join-can-stop-competing-startup-demand", () -> {
            try (Lab l = new Lab()) {
                l.server("game", "game", false, 0, true); l.config.reload();
                Guest a = l.guest(), b = l.guest(); l.online("game");
                l.lifecycle.request(a.player, "game", "test");
                l.status.setState("game", StatusStore.State.OFFLINE);
                l.lifecycle.request(b.player, "game", "test"); l.accepted("game");
                a.connections.get(0).completeExceptionally(new IllegalStateException("join failed"));
                await(() -> l.stops.get() == 1);
                check(l.lifecycle.queueSize("game") == 1, "provider stop dispatched while another requester is queued");
            }
        });
        test("configured-hub-final-guard-rechecks-current-mapping", () -> {
            try (Lab l = new Lab()) {
                ManagedServer stale = l.config.snapshot().servers().get("game");
                l.server("game", "hub", false, 1, true); l.config.reload();
                ManagedServer current = l.config.snapshot().servers().get("game");
                check(!current.stopAfterFailure() && !current.stopWhenQueueEmpty() && current.idleShutdownMinutes() == 0, "effective hub flags repaired");
                invoke(l.lifecycle, "stopServer", stale, "stale cleanup test");
                check(l.stops.get() == 0 && l.status.get("game").state() != StatusStore.State.STOPPING, "current hub mapping blocks stale automatic stop");
            }
        });
        test("connected-player-final-stop-guard", () -> {
            try (Lab l = new Lab()) {
                l.backends.get("game").players.add(l.guest().player);
                invoke(l.lifecycle, "stopServer", l.config.snapshot().servers().get("game"), "test");
                check(l.stops.get() == 0 && l.status.get("game").state() != StatusStore.State.STOPPING, "connected player blocks stop");
            }
        });
        test("idle-guard-preserves-queued-and-pending-demand", () -> {
            try (Lab l = new Lab()) {
                Guest a = l.guest(); l.lifecycle.request(a.player, "game", "test"); l.accepted("game"); l.online("game");
                invoke(l.lifecycle, "tickIdleShutdown", 1L); invoke(l.lifecycle, "tickIdleShutdown", 120001L);
                check(l.stops.get() == 0, "queue blocks idle stop");
                l.online("other"); Guest b = l.guest(); l.lifecycle.request(b.player, "other", "test");
                invoke(l.lifecycle, "tickIdleShutdown", 240001L);
                check(l.stops.get() == 0, "pending transfer blocks idle stop");
            }
        });
        test("ordinary-idle-backend-can-stop", () -> {
            try (Lab l = new Lab()) {
                l.online("game"); invoke(l.lifecycle, "tickIdleShutdown", 1L); invoke(l.lifecycle, "tickIdleShutdown", 120001L);
                await(() -> l.stops.get() == 1);
            }
        });
        test("existing-behavior-empty-queue-stop-does-not-cancel-provider-start", () -> {
            try (Lab l = new Lab()) {
                l.holdStart = new CountDownLatch(1);
                Guest a = l.guest(); l.lifecycle.request(a.player, "game", "test"); await(() -> l.starts.get() == 1);
                l.lifecycle.cancel(a.player); await(() -> l.stops.get() == 1);
                l.holdStart.countDown(); await(() -> l.completedStarts.get() == 1);
                check(l.lifecycle.queueSize("game") == 0, "start completes after demand has been removed");
            }
        });
        test("existing-behavior-one-UUID-can-queue-multiple-backends", () -> {
            try (Lab l = new Lab()) {
                Guest a = l.guest(); l.lifecycle.request(a.player, "game", "test"); l.lifecycle.request(a.player, "other", "test");
                l.accepted("game"); l.accepted("other");
                check(l.starts.get() == 2 && l.lifecycle.queueSize("game") == 1 && l.lifecycle.queueSize("other") == 1, "no global per-player admission ledger");
            }
        });
        test("existing-behavior-stale-nonhub-definition-bypasses-new-always-on-flags", () -> {
            try (Lab l = new Lab()) {
                ManagedServer stale = l.config.snapshot().servers().get("game");
                Files.writeString(l.dir.resolve("shared/hubpilot.properties"), "server.game.always-on-server=true\n"); l.config.reload();
                ManagedServer current = l.config.snapshot().servers().get("game");
                check(!current.stopAfterFailure() && current.idleShutdownMinutes() == 0, "current Always-On effective settings enforced");
                invoke(l.lifecycle, "stopServer", stale, "stale nonhub cleanup"); await(() -> l.stops.get() == 1);
            }
        });
        System.out.println("TOTAL PASS " + passed);
    }

    static void test(String name, Checked body) throws Exception { body.run(); passed++; System.out.println("PASS " + name); }
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) throw new AssertionError("Timed out waiting for controlled async callback");
            Thread.sleep(5);
        }
    }
    static Object invoke(Object target, String name, Object... args) throws Exception {
        Method method = Arrays.stream(target.getClass().getDeclaredMethods()).filter(m -> m.getName().equals(name) && m.getParameterCount() == args.length).findFirst().orElseThrow();
        method.setAccessible(true); return method.invoke(target, args);
    }
    static Object field(Object target, String name) throws Exception { Field f = target.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(target); }
    @SuppressWarnings("unchecked") static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p,m,a) -> {
            if (m.getDeclaringClass() == Object.class) return switch(m.getName()) { case "hashCode" -> System.identityHashCode(p); case "equals" -> p == a[0]; default -> type.getSimpleName()+"-fixture"; };
            return handler.invoke(p,m,a == null ? new Object[0] : a);
        });
    }
    static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) { if (type == Optional.class) return Optional.empty(); return null; }
        if (type == boolean.class) return false;
        if (type == long.class) return 0L;
        if (type == double.class) return 0D;
        if (type == float.class) return 0F;
        if (type == void.class) return null;
        return 0;
    }
    static ConnectionRequestBuilder.Result result(boolean success) {
        return proxy(ConnectionRequestBuilder.Result.class, (p,m,a) -> m.getName().equals("isSuccessful") ? success : defaultValue(m.getReturnType()));
    }
    static final class Backend {
        final String name;
        final List<Player> players = new CopyOnWriteArrayList<>();
        CompletableFuture<ServerPing> ping = CompletableFuture.failedFuture(new IllegalStateException("offline"));
        final RegisteredServer server;
        Backend(String name) {
            this.name = name;
            server = proxy(RegisteredServer.class, (p,m,a) -> switch(m.getName()) {
                case "getServerInfo" -> new ServerInfo(name, new InetSocketAddress("127.0.0.1", 25565));
                case "getPlayersConnected" -> players;
                case "ping" -> ping;
                default -> defaultValue(m.getReturnType());
            });
        }
    }
    static final class Guest {
        final UUID id = UUID.randomUUID();
        final List<String> targets = new ArrayList<>();
        final List<CompletableFuture<ConnectionRequestBuilder.Result>> connections = new ArrayList<>();
        Backend current;
        final Player player;
        Guest(Lab lab) {
            player = proxy(Player.class, (p,m,a) -> switch(m.getName()) {
                case "getUniqueId" -> id;
                case "getUsername" -> "fixture";
                case "isActive", "hasPermission" -> true;
                case "getCurrentServer" -> current == null ? Optional.empty() : Optional.of(proxy(ServerConnection.class, (p2,m2,a2) -> switch(m2.getName()) {
                    case "getServer" -> current.server;
                    case "getServerInfo" -> current.server.getServerInfo();
                    case "getPlayer" -> player();
                    default -> defaultValue(m2.getReturnType());
                }));
                case "createConnectionRequest" -> {
                    RegisteredServer server = (RegisteredServer)a[0];
                    yield proxy(ConnectionRequestBuilder.class, (p2,m2,a2) -> {
                        if (m2.getName().equals("connect")) {
                            targets.add(server.getServerInfo().getName());
                            CompletableFuture<ConnectionRequestBuilder.Result> future = new CompletableFuture<>(); connections.add(future); return future;
                        }
                        return defaultValue(m2.getReturnType());
                    });
                }
                default -> defaultValue(m.getReturnType());
            });
        }
        Player player() { return player; }
    }
    static final class Lab implements AutoCloseable {
        final Path dir = Files.createTempDirectory("hubpilot-lifecycle-");
        final AtomicInteger starts = new AtomicInteger(), stops = new AtomicInteger(), completedStarts = new AtomicInteger();
        final HttpServer http;
        final ExecutorService executor = Executors.newCachedThreadPool(r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
        volatile CountDownLatch holdStart;
        final Map<String, Backend> backends = new HashMap<>();
        final Map<UUID, Guest> guests = new HashMap<>();
        final List<Runnable> tasks = new ArrayList<>();
        final HubPilotConfig config;
        final StatusStore status;
        final LifecycleManager lifecycle;
        Lab() throws Exception {
            http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0); http.setExecutor(executor);
            http.createContext("/", exchange -> {
                boolean start = exchange.getRequestURI().getPath().startsWith("/start");
                if (start) starts.incrementAndGet(); else stops.incrementAndGet();
                try {
                    if (start && holdStart != null && !holdStart.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("start latch timeout");
                    exchange.sendResponseHeaders(204, -1);
                    if (start) completedStarts.incrementAndGet();
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                finally { exchange.close(); }
            });
            Files.createDirectories(dir.resolve("servers")); Files.createDirectories(dir.resolve("shared"));
            Files.writeString(dir.resolve("config.yml"), "hub-server: hub\nshared-directory: " + dir.resolve("shared") + "\n");
            Files.writeString(dir.resolve("defaults.yml"), "{}\n");
            Files.createDirectories(dir.resolve("providers")); Files.createDirectories(dir.resolve("messages"));
            Files.writeString(dir.resolve("providers/crafty.yml"), "{}\n");
            Files.writeString(dir.resolve("messages/en_US.yml"), "{}\n");
            Files.writeString(dir.resolve("providers.yml"), "primary-provider: test\nproviders:\n  test:\n    type: generic-http\n    enabled: true\n    start-url: http://127.0.0.1:" + http.getAddress().getPort() + "/start/{server}\n    stop-url: http://127.0.0.1:" + http.getAddress().getPort() + "/stop/{server}\n");
            server("game", "game", false, 1, true); server("other", "other", false, 1, true);
            for (String name : List.of("game", "other", "hub")) backends.put(name, new Backend(name));
            Scheduler scheduler = proxy(Scheduler.class, (p,m,a) -> {
                if (m.getName().equals("buildTask")) {
                    Runnable task = (Runnable)a[1];
                    return proxy(Scheduler.TaskBuilder.class, (p2,m2,a2) -> {
                        if (m2.getName().equals("schedule")) { tasks.add(task); return proxy(ScheduledTask.class, (p3,m3,a3) -> defaultValue(m3.getReturnType())); }
                        return p2;
                    });
                }
                return defaultValue(m.getReturnType());
            });
            ProxyServer proxy = proxy(ProxyServer.class, (p,m,a) -> switch(m.getName()) {
                case "getServer" -> Optional.ofNullable(backends.get(a[0])).map(b -> b.server);
                case "getPlayer" -> Optional.ofNullable(guests.get(a[0])).map(g -> g.player);
                case "getScheduler" -> scheduler;
                case "getAllServers" -> backends.values().stream().map(b -> b.server).toList();
                default -> defaultValue(m.getReturnType());
            });
            config = new HubPilotConfig(dir); config.reload();
            ProviderRegistry.get().initialize(dir, LOG);
            check(ProviderRegistry.isManaged("test"), "loopback provider definition loaded");
            StatsStore stats = new StatsStore(dir, LOG);
            status = new StatusStore(proxy, LOG, stats, dir.resolve("shared").toString());
            lifecycle = new LifecycleManager(new Object(), proxy, LOG, config, new CraftyProvider(LOG, config.snapshot().crafty()), status, stats);
            http.start();
        }
        void server(String id, String target, boolean maintenance, int retries, boolean cleanup) throws Exception {
            Files.writeString(dir.resolve("servers/"+id+".yml"), "id: "+id+"\nvelocity-server: "+target+"\nstartup:\n  provider: test\n  provider-server-id: "+id+"\n  stop-after-failure: "+cleanup+"\n  stop-when-queue-empty: true\nconnection:\n  retry-count: "+retries+"\n  retry-delay-seconds: 1\ncountdown:\n  duration-seconds: 5\n  sound: none\nidle-shutdown:\n  minutes: 1\naccess:\n  maintenance: "+maintenance+"\n");
        }
        Guest guest() { Guest g = new Guest(this); guests.put(g.id,g); return g; }
        void online(String id) { status.setState(id, StatusStore.State.ONLINE); }
        Object session(String id) throws Exception { return ((Map<?,?>)field(lifecycle,"sessions")).get(id); }
        void accepted(String id) throws Exception { await(() -> { try { Object s = session(id); return s != null && Boolean.TRUE.equals(field(s,"startAccepted")); } catch (Exception e) { throw new RuntimeException(e); } }); }
        @Override public void close() throws Exception {
            if (holdStart != null) holdStart.countDown();
            lifecycle.shutdown(); http.stop(0); executor.shutdownNow(); executor.awaitTermination(5,TimeUnit.SECONDS);
            try (var files = Files.walk(dir)) { for (Path p : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(p); }
        }
    }
}
