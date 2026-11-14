package dev.hubpilot.core;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * HubPilot Controller as a power provider.
 *
 * The Controller uses a self-signed certificate, so trust is a SHA-256 pin of its leaf certificate,
 * recorded when the owner pairs and shown to them for comparison. Nothing here ever disables
 * certificate checking. Every power request carries a fresh Idempotency-Key, which the Controller
 * requires, and asks as an automatic (HubPilot-owned) action: the Controller itself refuses to stop
 * hubs and Always-On servers automatically and only acts on servers whose lifecycle is delegated to
 * HubPilot.
 */
final class ControllerProvider {
   static final String TYPE = "hubpilot-controller";
   static final String ID = "hubpilot-controller";
   static final String SECRET = "hubpilot-controller-token";
   private static final Pattern UUID_TEXT = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
   private static final SecureRandom RANDOM = new SecureRandom();
   /** One pairing in progress at a time; the verifier never leaves this process. */
   private static final AtomicReference<Pending> PENDING = new AtomicReference<>();

   private ControllerProvider() {
   }

   record Pending(String baseUrl, String verifier, String pin, long expiresAt) {
   }

   record Server(String id, String name, String role, int port, String state) {
   }

   static boolean isUuid(String value) {
      return value != null && UUID_TEXT.matcher(value.trim()).matches();
   }

   // ---------- Power ----------

   static ProviderRegistry.Result power(ProviderRegistry.Definition def, String secret, String serverId, String action) throws Exception {
      if (!isUuid(serverId)) {
         return ProviderRegistry.Result.fail(0, "HubPilot Controller server IDs are UUIDs. Check startup.provider-server-id.");
      }
      String pin = def.values().getOrDefault("tls-pin", "");
      if (pin.isBlank() || secret.isBlank()) {
         return ProviderRegistry.Result.fail(0, "HubPilot Controller is not paired. Run /hp controller pair <url>.");
      }
      String verb = "start".equalsIgnoreCase(action) ? "start" : "stop";
      String body = "{\"action\":\"" + verb + "\",\"automatic\":true}";
      Response r = request(def.baseUrl(), pin, def.timeout(), "POST", "/api/v1/servers/" + URLEncoder.encode(serverId.trim(), StandardCharsets.UTF_8) + "/power",
         body, secret, "hubpilot-" + UUID.randomUUID());
      return r.ok() ? ProviderRegistry.Result.ok(r.status) : ProviderRegistry.Result.fail(r.status, explain(r));
   }

   static ProviderRegistry.Result test(ProviderRegistry.Definition def, String secret) throws Exception {
      String pin = def.values().getOrDefault("tls-pin", "");
      if (pin.isBlank() || secret.isBlank()) {
         return ProviderRegistry.Result.fail(0, "Not paired. Run /hp controller pair <url>.");
      }
      Response r = request(def.baseUrl(), pin, def.timeout(), "GET", "/api/v1/servers", null, secret, null);
      return r.ok() ? ProviderRegistry.Result.ok(r.status) : ProviderRegistry.Result.fail(r.status, explain(r));
   }

   static List<Server> servers(ProviderRegistry.Definition def, String secret) throws Exception {
      Response r = request(def.baseUrl(), def.values().getOrDefault("tls-pin", ""), def.timeout(), "GET", "/api/v1/servers", null, secret, null);
      if (!r.ok()) {
         throw new IllegalStateException(explain(r));
      }
      return parseServers(r.body);
   }

   // ---------- Pairing (owner-approved, PKCE: see the Controller's docs/API.md) ----------

   /** Step 1: read and pin the Controller's certificate, make a verifier, return the challenge. */
   static String[] beginPairing(String baseUrl, int timeout) throws Exception {
      String url = normalize(baseUrl);
      String pin = fetchPin(url, timeout);
      byte[] raw = new byte[32];
      RANDOM.nextBytes(raw);
      String verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
      String challenge = Base64.getUrlEncoder().withoutPadding()
         .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
      PENDING.set(new Pending(url, verifier, pin, System.currentTimeMillis() + 15 * 60_000L));
      return new String[]{challenge, pin, url};
   }

   /** Step 2: exchange the owner's one-time code for a revocable service credential. */
   static String[] finishPairing(String code, int timeout) throws Exception {
      Pending p = PENDING.get();
      if (p == null || System.currentTimeMillis() > p.expiresAt()) {
         PENDING.set(null);
         throw new IllegalStateException("No pairing in progress (or it expired). Run /hp controller pair <url> again.");
      }
      if (code == null || !code.trim().matches("[A-Za-z0-9_-]{8,128}")) {
         throw new IllegalArgumentException("That doesn't look like a pairing code.");
      }
      String body = "{\"code\":\"" + code.trim() + "\",\"verifier\":\"" + p.verifier() + "\"}";
      Response r = request(p.baseUrl(), p.pin(), timeout, "POST", "/api/v1/pairing/exchange", body, null, null);
      if (!r.ok()) {
         throw new IllegalStateException(explain(r));
      }
      String token = field(r.body, "token");
      if (token == null || token.isBlank()) {
         throw new IllegalStateException("The Controller did not return a credential.");
      }
      PENDING.set(null);
      return new String[]{p.baseUrl(), p.pin(), token, field(r.body, "id")};
   }

   // ---------- HTTP over a pinned certificate ----------

   record Response(int status, String body) {
      boolean ok() {
         return status >= 200 && status < 300;
      }
   }

   static Response request(String baseUrl, String pin, int timeout, String method, String path, String body, String bearer, String idempotencyKey) throws Exception {
      URI uri = URI.create(normalize(baseUrl) + path);
      HttpsURLConnection c = (HttpsURLConnection) uri.toURL().openConnection();
      c.setSSLSocketFactory(pinnedContext(pin).getSocketFactory());
      // The pin identifies the Controller; a self-signed LAN certificate rarely names the address used.
      c.setHostnameVerifier((host, session) -> true);
      c.setInstanceFollowRedirects(false);
      int ms = Math.max(1, timeout) * 1000;
      c.setConnectTimeout(ms);
      c.setReadTimeout(ms);
      c.setRequestMethod(method);
      c.setRequestProperty("Accept", "application/json");
      c.setRequestProperty("User-Agent", "HubPilot/" + version());
      if (bearer != null) {
         c.setRequestProperty("Authorization", "Bearer " + bearer);
      }
      if (idempotencyKey != null) {
         c.setRequestProperty("Idempotency-Key", idempotencyKey);
      }
      if (body != null) {
         byte[] data = body.getBytes(StandardCharsets.UTF_8);
         c.setDoOutput(true);
         c.setRequestProperty("Content-Type", "application/json");
         c.setFixedLengthStreamingMode(data.length);
         try (OutputStream out = c.getOutputStream()) {
            out.write(data);
         }
      }
      int status = c.getResponseCode();
      InputStream in = status >= 400 ? c.getErrorStream() : c.getInputStream();
      String text = in == null ? "" : read(in, 1 << 20);
      c.disconnect();
      return new Response(status, text);
   }

   static String fetchPin(String baseUrl, int timeout) throws Exception {
      AtomicReference<String> seen = new AtomicReference<>();
      TrustManager capture = new X509TrustManager() {
         @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
         @Override public void checkClientTrusted(X509Certificate[] chain, String type) { }
         @Override public void checkServerTrusted(X509Certificate[] chain, String type) throws CertificateException {
            if (chain == null || chain.length == 0) throw new CertificateException("No certificate presented.");
            seen.set(fingerprint(chain[0]));
            // Fail on purpose: nothing is sent to an unpinned peer.
            throw new CertificateException("captured");
         }
      };
      SSLContext ctx = SSLContext.getInstance("TLS");
      ctx.init(null, new TrustManager[]{capture}, RANDOM);
      HttpsURLConnection c = (HttpsURLConnection) URI.create(normalize(baseUrl) + "/").toURL().openConnection();
      c.setSSLSocketFactory(ctx.getSocketFactory());
      c.setHostnameVerifier((h, s) -> true);
      c.setConnectTimeout(Math.max(1, timeout) * 1000);
      c.setReadTimeout(Math.max(1, timeout) * 1000);
      try {
         c.getResponseCode();
      } catch (Exception ignored) {
         // Expected: the capture manager aborts the handshake after reading the certificate.
      } finally {
         c.disconnect();
      }
      if (seen.get() == null) {
         throw new IllegalStateException("Couldn't reach the Controller over HTTPS at " + normalize(baseUrl) + ".");
      }
      return seen.get();
   }

   private static SSLContext pinnedContext(String pin) throws Exception {
      String expected = pin == null ? "" : pin.replace(":", "").trim().toLowerCase(Locale.ROOT);
      if (!expected.matches("[0-9a-f]{64}")) {
         throw new IllegalStateException("The Controller certificate pin is missing or malformed. Pair again.");
      }
      TrustManager pinned = new X509TrustManager() {
         @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
         @Override public void checkClientTrusted(X509Certificate[] chain, String type) throws CertificateException {
            throw new CertificateException("Client certificates are not used.");
         }
         @Override public void checkServerTrusted(X509Certificate[] chain, String type) throws CertificateException {
            if (chain == null || chain.length == 0 || !expected.equals(fingerprint(chain[0]))) {
               throw new CertificateException("The Controller's certificate changed. If you replaced it, pair again; otherwise something is intercepting the connection.");
            }
         }
      };
      SSLContext ctx = SSLContext.getInstance("TLS");
      ctx.init(null, new TrustManager[]{pinned}, RANDOM);
      return ctx;
   }

   static String fingerprint(X509Certificate cert) throws CertificateException {
      try {
         return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));
      } catch (Exception e) {
         throw new CertificateException(e);
      }
   }

   static String pretty(String pin) {
      StringBuilder out = new StringBuilder();
      for (int i = 0; i < pin.length(); i += 2) {
         if (i > 0) out.append(':');
         out.append(pin, i, Math.min(pin.length(), i + 2));
      }
      return out.toString().toUpperCase(Locale.ROOT);
   }

   // ---------- Small, dependency-free JSON reading (Controller responses are flat and bounded) ----------

   static String explain(Response r) {
      String message = field(r.body, "message");
      String code = field(r.body, "code");
      String text = message != null ? message : r.body.isBlank() ? "HTTP " + r.status : compact(r.body);
      return (code != null ? code + ": " : "") + text;
   }

   static String field(String json, String name) {
      Matcher m = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json == null ? "" : json);
      return m.find() ? m.group(1).replace("\\\"", "\"").replace("\\\\", "\\") : null;
   }

   /** Servers from GET /api/v1/servers: id, name, role, first address port, state. */
   static List<Server> parseServers(String json) {
      List<Server> out = new ArrayList<>();
      int at = json.indexOf("\"servers\"");
      if (at < 0) return out;
      int depth = 0, start = -1;
      for (int i = json.indexOf('[', at); i >= 0 && i < json.length(); i++) {
         char ch = json.charAt(i);
         if (ch == '"') {
            i = skipString(json, i);
         } else if (ch == '{') {
            if (depth++ == 0) start = i;
         } else if (ch == '}') {
            if (--depth == 0 && start >= 0) {
               String obj = json.substring(start, i + 1);
               String id = field(obj, "id");
               if (isUuid(id)) {
                  Matcher port = Pattern.compile("\"port\"\\s*:\\s*(\\d+)").matcher(obj);
                  out.add(new Server(id, nvl(field(obj, "name")), nvl(field(obj, "role")), port.find() ? Integer.parseInt(port.group(1)) : -1, nvl(field(obj, "state"))));
               }
               start = -1;
            }
         } else if (ch == ']' && depth == 0) {
            break;
         }
      }
      return out;
   }

   private static int skipString(String s, int i) {
      for (int j = i + 1; j < s.length(); j++) {
         char c = s.charAt(j);
         if (c == '\\') j++;
         else if (c == '"') return j;
      }
      return s.length();
   }

   private static String nvl(String s) {
      return s == null ? "" : s;
   }

   private static String compact(String s) {
      String t = s.replaceAll("\\s+", " ").trim();
      return t.length() > 200 ? t.substring(0, 200) + "…" : t;
   }

   static String normalize(String url) {
      String u = url == null ? "" : url.trim();
      while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
      if (!u.toLowerCase(Locale.ROOT).startsWith("https://")) {
         throw new IllegalArgumentException("The Controller address must start with https://");
      }
      return u;
   }

   private static String read(InputStream in, int limit) throws Exception {
      try (in) {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         byte[] buf = new byte[8192];
         int n;
         while ((n = in.read(buf)) > 0 && out.size() < limit) out.write(buf, 0, n);
         return out.toString(StandardCharsets.UTF_8);
      }
   }

   private static String version() {
      return HubPilotCorePlugin.VERSION;
   }
}
