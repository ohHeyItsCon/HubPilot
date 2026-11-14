package dev.hubpilot.core;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

/**
 * Live link to the HubPilot Controller, protocol 1 (the Controller's docs/HUBPILOT-LINK.md).
 *
 * Reports what HubPilot is actually doing: heartbeats and player transfer phases. It never reports
 * "completed" until Velocity has confirmed the backend switch. Every network call runs on one
 * background thread, so a slow or offline Controller can't delay players. Without a paired link
 * this class does nothing.
 */
final class ControllerLink {
   static final int PROTOCOL = 1;
   static final String TOKEN = "hubpilot-controller-link";
   private static final ControllerLink INSTANCE = new ControllerLink();
   private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "HubPilot-ControllerLink");
      t.setDaemon(true);
      return t;
   });
   /** Stable report id per player and destination, so each report updates one transfer. */
   private final Map<String, String> transferIds = new ConcurrentHashMap<>();
   private volatile ScheduledFuture<?> heartbeat;
   private volatile String state = "not paired";
   private Logger logger;

   static ControllerLink get() {
      return INSTANCE;
   }

   void initialize(Logger logger) {
      this.logger = logger;
      this.restartHeartbeat(60);
   }

   void shutdown() {
      this.worker.shutdownNow();
   }

   String state() {
      return this.state;
   }

   boolean paired() {
      return !ProviderRegistry.get().linkToken().isBlank() && this.definition() != null;
   }

   private ProviderRegistry.Definition definition() {
      ProviderRegistry.Definition def = ProviderRegistry.get().controllerDefinition();
      return def == null || !def.enabled() || def.values().getOrDefault("tls-pin", "").isBlank() ? null : def;
   }

   /** Claims a link code from the Controller (Proxy > HubPilot > Start pairing). Needs the provider pairing first. */
   String claim(String code, String coreVersion, String network) throws Exception {
      ProviderRegistry.Definition def = this.definition();
      if (def == null) {
         return "Pair the Controller provider first: /hp controller pair <url>.";
      }
      if (code == null || !code.trim().matches("hpl_[A-Za-z0-9_-]{6,64}")) {
         return "That doesn't look like a link code (they start with hpl_).";
      }
      String body = "{\"code\":\"" + code.trim() + "\",\"protocol\":" + PROTOCOL + ",\"coreVersion\":\"" + json(coreVersion) + "\""
         + (network == null || network.isBlank() ? "" : ",\"network\":\"" + json(network) + "\"") + "}";
      ControllerProvider.Response r = ControllerProvider.request(def.baseUrl(), def.values().get("tls-pin"), def.timeout(), "POST", "/api/v1/hubpilot/link/claim", body, null, null);
      if (!r.ok()) {
         return "The Controller refused the link: " + ControllerProvider.explain(r);
      }
      String token = ControllerProvider.field(r.body(), "token");
      if (token == null || token.isBlank()) {
         return "The Controller did not return a link token.";
      }
      ProviderRegistry.get().saveLinkToken(token);
      this.state = "connected";
      int seconds = intField(r.body(), "heartbeatSeconds", 60);
      this.restartHeartbeat(seconds);
      return "SUCCESS";
   }

   void unlink() {
      ProviderRegistry.get().saveLinkToken("");
      this.state = "not paired";
   }

   // ---------- Transfers ----------

   void report(UUID player, String playerName, String origin, String destination, String phase, String detail) {
      if (player == null || destination == null || !this.paired()) {
         return;
      }
      String key = player + "|" + destination;
      boolean terminal = phase.equals("completed") || phase.equals("cancelled") || phase.equals("failed");
      String id = terminal ? this.transferIds.remove(key) : this.transferIds.computeIfAbsent(key, k -> UUID.randomUUID().toString());
      if (id == null) {
         id = UUID.randomUUID().toString();
      }
      String body = "{\"id\":\"" + id + "\",\"phase\":\"" + phase + "\",\"player\":\"" + json(playerName) + "\",\"origin\":\"" + json(origin)
         + "\",\"destination\":\"" + json(destination) + "\"" + (detail == null || detail.isBlank() ? "" : ",\"detail\":\"" + json(detail) + "\"") + "}";
      this.worker.execute(() -> this.send("/api/v1/hubpilot/link/transfers", body));
   }

   // ---------- Heartbeat ----------

   private synchronized void restartHeartbeat(int seconds) {
      if (this.heartbeat != null) {
         this.heartbeat.cancel(false);
      }
      int every = Math.max(15, Math.min(seconds, 300));
      this.heartbeat = this.worker.scheduleWithFixedDelay(() -> {
         if (this.paired()) {
            this.send("/api/v1/hubpilot/link/heartbeat", "{\"coreVersion\":\"" + json(version()) + "\"}");
         }
      }, 2, every, TimeUnit.SECONDS);
   }

   private void send(String path, String body) {
      ProviderRegistry.Definition def = this.definition();
      String token = ProviderRegistry.get().linkToken();
      if (def == null || token.isBlank()) {
         return;
      }
      try {
         ControllerProvider.Response r = ControllerProvider.request(def.baseUrl(), def.values().get("tls-pin"), def.timeout(), "POST", path, body, token, null);
         if (r.status() == 401) {
            this.state = "re-pair needed (the Controller no longer accepts this link)";
            this.warn("HubPilot Controller link was revoked or expired. Pair again with /hp controller link <code>.");
         } else if (r.ok()) {
            this.state = "connected";
         } else {
            this.state = "error: " + ControllerProvider.explain(r);
         }
      } catch (Exception e) {
         this.state = "unreachable (" + e.getMessage() + ")";
      }
   }

   private void warn(String message) {
      if (this.logger != null) {
         this.logger.warn(message);
      }
   }

   static String version() {
      return HubPilotCorePlugin.VERSION;
   }

   private static int intField(String json, String name, int fallback) {
      java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"" + name + "\"\\s*:\\s*(\\d+)").matcher(json);
      return m.find() ? Integer.parseInt(m.group(1)) : fallback;
   }

   private static String json(String s) {
      if (s == null) return "";
      StringBuilder out = new StringBuilder();
      for (char c : s.toCharArray()) {
         if (c == '"' || c == '\\') out.append('\\').append(c);
         else if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
         else out.append(c);
      }
      return out.length() > 200 ? out.substring(0, 200) : out.toString();
   }
}
