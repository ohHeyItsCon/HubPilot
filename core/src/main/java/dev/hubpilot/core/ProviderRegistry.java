package dev.hubpilot.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.slf4j.Logger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

final class ProviderRegistry {
   private static final ProviderRegistry INSTANCE = new ProviderRegistry();
   private final Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
   private volatile Map<String, ProviderRegistry.Definition> definitions = Map.of();
   private volatile Map<String, String> secrets = Map.of();
   private volatile String primary = "always-online";
   private Path dataDir;
   private Logger logger;
   private long providersMtime = -1L;
   private long secretsMtime = -1L;
   private volatile String lastTest = "Not tested";

   static ProviderRegistry get() {
      return INSTANCE;
   }

   static boolean isManaged(String var0) {
      return INSTANCE.managed(var0);
   }

   synchronized void initialize(Path var1, Logger var2) {
      this.dataDir = var1;
      this.logger = var2;
      PublicDefaults.install(var1);
      this.reload();
   }

   synchronized void reload() {
      if (this.dataDir != null) {
         try {
            Map var1 = this.load(this.dataDir.resolve("providers.yml"));
            this.primary = lower(str(var1.get("primary-provider"), "always-online"));
            LinkedHashMap var2 = new LinkedHashMap();
            if (var1.get("providers") instanceof Map<?, ?> var4) {
               for (Entry var6 : var4.entrySet()) {
                  String var7 = lower(String.valueOf(var6.getKey()));
                  if (var6.getValue() instanceof Map var8) {
                     LinkedHashMap<String, String> var21 = new LinkedHashMap<>();
                     var8.forEach((var1x, var2x) -> {
                        if (var2x != null) {
                           var21.put(String.valueOf(var1x), String.valueOf(var2x));
                        }
                     });
                     String var10 = ProviderAliases.canonical(lower(var21.getOrDefault("type", var7)));
                     boolean var11 = Boolean.parseBoolean(var21.getOrDefault("enabled", "true"));
                     String var12 = stripSlash(var21.getOrDefault("base-url", ""));
                     String var13 = var21.getOrDefault("secret", var7 + "-token");
                     boolean var14 = Boolean.parseBoolean(var21.getOrDefault("allow-insecure-tls", "false"));
                     int var15 = intVal((String)var21.get("connect-timeout-seconds"), 10, 1, 120);
                     var2.put(var7, new ProviderRegistry.Definition(var7, var10, var11, var12, var13, var14, var15, Map.copyOf(var21)));
                  }
               }
            }

            var2.putIfAbsent("always-online", new ProviderRegistry.Definition("always-online", "always-on", true, "", "", false, 10, Map.of()));
            this.definitions = Map.copyOf(var2);
            Map var17 = this.load(this.dataDir.resolve("secrets.yml"));
            LinkedHashMap var18 = new LinkedHashMap();
            if (var17.get("secrets") instanceof Map var20) {
               var20.forEach((var1x, var2x) -> var18.put(String.valueOf(var1x), var2x == null ? "" : String.valueOf(var2x)));
            }

            this.secrets = Map.copyOf(var18);
            this.providersMtime = mtime(this.dataDir.resolve("providers.yml"));
            this.secretsMtime = mtime(this.dataDir.resolve("secrets.yml"));
         } catch (Exception var16) {
            this.logWarn("Provider configuration reload failed: {}", var16.getMessage());
         }
      }
   }

   private void reloadIfChanged() {
      if (this.dataDir != null) {
         long var1 = mtime(this.dataDir.resolve("providers.yml"));
         long var3 = mtime(this.dataDir.resolve("secrets.yml"));
         if (var1 != this.providersMtime || var3 != this.secretsMtime) {
            this.reload();
         }
      }
   }

   boolean managed(String var1) {
      this.reloadIfChanged();
      ProviderRegistry.Definition var2 = this.resolve(var1);
      return var2 != null && var2.enabled() && !"always-on".equals(var2.type()) && !"always-online".equals(var2.type());
   }

   boolean known(String var1) {
      this.reloadIfChanged();
      return this.resolve(var1) != null;
   }

   String primaryId() {
      this.reloadIfChanged();
      return this.primary;
   }

   ProviderRegistry.Definition primaryDefinition() {
      this.reloadIfChanged();
      return this.resolve(this.primary);
   }

   String primaryType() {
      ProviderRegistry.Definition var1 = this.primaryDefinition();
      return var1 == null ? "unconfigured" : var1.type();
   }

   boolean primaryConfigured() {
      ProviderRegistry.Definition var1 = this.primaryDefinition();
      if (var1 == null || !var1.enabled()) {
         return false;
      } else if ("always-on".equals(var1.type()) || "always-online".equals(var1.type())) {
         return true;
      } else if (var1.baseUrl().isBlank()) {
         return false;
      } else {
         return "generic-http".equals(var1.type()) ? !var1.values().getOrDefault("start-url", "").isBlank() : !this.secret(var1).isBlank();
      }
   }

   String lastTest() {
      return this.lastTest;
   }

   List<ProviderRegistry.Definition> definitions() {
      this.reloadIfChanged();
      return new ArrayList<>(this.definitions.values());
   }

   CompletableFuture<ProviderRegistry.Result> action(ManagedServer var1, String var2) {
      return CompletableFuture.supplyAsync(() -> this.doAction(var1, var2));
   }

   private ProviderRegistry.Result doAction(ManagedServer var1, String var2) {
      try {
         this.reloadIfChanged();
         ProviderRegistry.Definition var3 = this.resolve(var1.provider());
         if (var3 == null) {
            return ProviderRegistry.Result.fail(0, "Unknown provider: " + var1.provider());
         } else if (!var3.enabled()) {
            return ProviderRegistry.Result.fail(0, "Provider is disabled: " + var3.id());
         } else if (!"always-on".equals(var3.type()) && !"always-online".equals(var3.type())) {
            String var4 = var1.providerServerId() == null ? "" : var1.providerServerId().trim();
            if (var4.isBlank()) {
               return ProviderRegistry.Result.fail(0, "Server has no provider-server-id for " + var3.id() + ".");
            } else {
               String var5 = var3.type();

               return switch (var5) {
                  case "crafty" -> this.crafty(var3, var4, var2);
                  case "pterodactyl" -> this.pterodactyl(var3, var4, var2);
                  case "generic-http" -> this.generic(var3, var4, var2);
                  default -> ProviderRegistry.Result.fail(0, "Unsupported provider type: " + var3.type());
               };
            }
         } else {
            return ProviderRegistry.Result.fail(0, "Server is configured as always-online.");
         }
      } catch (Throwable var7) {
         return ProviderRegistry.Result.fail(0, rootMessage(var7));
      }
   }

   ProviderRegistry.Result test(String var1) {
      try {
         this.reloadIfChanged();
         ProviderRegistry.Definition var2 = this.resolve(var1 != null && !var1.isBlank() ? var1 : this.primary);
         if (var2 == null) {
            return this.remember(ProviderRegistry.Result.fail(0, "Unknown provider."));
         } else if (!var2.enabled()) {
            return this.remember(ProviderRegistry.Result.fail(0, "Provider is disabled."));
         } else if (!"always-on".equals(var2.type()) && !"always-online".equals(var2.type())) {
            String var3 = this.secret(var2);
            if (("crafty".equals(var2.type()) || "pterodactyl".equals(var2.type())) && var3.isBlank()) {
               return this.remember(ProviderRegistry.Result.fail(0, "No API key is configured."));
            } else {
               Builder var4;
               if ("crafty".equals(var2.type())) {
                  var4 = this.request(var2, URI.create(var2.baseUrl() + "/api/v2/servers")).GET();
               } else if ("pterodactyl".equals(var2.type())) {
                  var4 = this.request(var2, URI.create(var2.baseUrl() + "/api/client")).GET();
               } else {
                  if (!"generic-http".equals(var2.type())) {
                     return this.remember(ProviderRegistry.Result.fail(0, "Unsupported provider type: " + var2.type()));
                  }

                  String var5 = var2.values().getOrDefault("status-url", "");
                  if (var5.isBlank()) {
                     return this.remember(ProviderRegistry.Result.ok(204));
                  }

                  var5 = var5.replace("{server}", var2.values().getOrDefault("test-server-id", "test"));
                  var4 = this.request(var2, URI.create(var5))
                     .method(var2.values().getOrDefault("status-method", "GET").toUpperCase(Locale.ROOT), BodyPublishers.noBody());
               }

               return this.remember(this.send(var2, var4.build()));
            }
         } else {
            return this.remember(ProviderRegistry.Result.ok(204));
         }
      } catch (Throwable var6) {
         return this.remember(ProviderRegistry.Result.fail(0, rootMessage(var6)));
      }
   }

   private ProviderRegistry.Result remember(ProviderRegistry.Result var1) {
      this.lastTest = var1.success() ? "Connected (HTTP " + var1.statusCode() + ")" : "Failed: " + var1.error();
      return var1;
   }

   synchronized String configurePrimary(String var1, String var2) {
      this.reloadIfChanged();
      var1 = lower(var1);
      var1 = ProviderAliases.canonical(var1);

      String var3 = switch (var1) {
         case "always-on", "always-online" -> "always-online";
         case "crafty" -> "crafty";
         case "pterodactyl" -> "pterodactyl";
         case "generic-http" -> "generic-http";
         default -> null;
      };
      if (var3 == null) {
         return "Unknown provider type: " + var1;
      } else {
         LinkedHashMap var4 = new LinkedHashMap<>(this.definitions);
         ProviderRegistry.Definition var9 = (ProviderRegistry.Definition)var4.get(var3);
         if (var9 == null) {
            return "Provider template is missing: " + var3;
         } else {
            String var6 = var2 != null && !var2.isBlank() ? stripSlash(var2.trim()) : var9.baseUrl();
            var4.put(
               var3,
               new ProviderRegistry.Definition(
                  var9.id(),
                  var9.type(),
                  true,
                  var6,
                  var9.secretKey(),
                  var9.insecure(),
                  var9.timeout(),
                  merge(var9.values(), Map.of("enabled", "true", "base-url", var6))
               )
            );
            this.primary = var3;
            this.writeProviders(var4, this.primary);
            this.reload();
            return "SUCCESS";
         }
      }
   }

   synchronized String setPrimaryUrl(String var1) {
      ProviderRegistry.Definition var2 = this.primaryDefinition();
      return var2 == null ? "No primary provider is selected." : this.configurePrimary(var2.type(), var1);
   }

   synchronized String setSecretForPrimary(String var1) {
      return ProviderCredentialSupport.apply(this, var1);
   }

   synchronized String setSecret(String var1, String var2) {
      this.reloadIfChanged();
      ProviderRegistry.Definition var3 = this.resolve(var1);
      if (var3 == null) {
         return "Unknown provider: " + var1;
      } else if (var2 != null && !var2.isBlank()) {
         LinkedHashMap var4 = new LinkedHashMap<>(this.secrets);
         var4.put(var3.secretKey(), var2.trim());
         this.writeSecrets(var4);
         this.reload();
         return "SUCCESS";
      } else {
         return "API key cannot be empty.";
      }
   }

   private ProviderRegistry.Result crafty(ProviderRegistry.Definition var1, String var2, String var3) throws Exception {
      String var4 = "start".equalsIgnoreCase(var3) ? "start_server" : "stop_server";
      URI var5 = URI.create(var1.baseUrl() + "/api/v2/servers/" + url(var2) + "/action/" + var4);
      HttpRequest var6 = this.request(var1, var5).POST(BodyPublishers.noBody()).build();
      return this.send(var1, var6);
   }

   private ProviderRegistry.Result pterodactyl(ProviderRegistry.Definition var1, String var2, String var3) throws Exception {
      String var4 = "start".equalsIgnoreCase(var3) ? "start" : "stop";
      URI var5 = URI.create(var1.baseUrl() + "/api/client/servers/" + url(var2) + "/power");
      String var6 = "{\"signal\":\"" + var4 + "\"}";
      HttpRequest var7 = this.request(var1, var5).header("Content-Type", "application/json").POST(BodyPublishers.ofString(var6)).build();
      return this.send(var1, var7);
   }

   private ProviderRegistry.Result generic(ProviderRegistry.Definition var1, String var2, String var3) throws Exception {
      String var4 = "start".equalsIgnoreCase(var3) ? "start" : "stop";
      String var5 = var1.values().getOrDefault(var4 + "-url", "").replace("{server}", url(var2));
      if (var5.isBlank()) {
         return ProviderRegistry.Result.fail(0, "No " + var4 + "-url is configured.");
      } else {
         String var6 = var1.values().getOrDefault(var4 + "-method", "POST").toUpperCase(Locale.ROOT);
         String var7 = var1.values().getOrDefault(var4 + "-body", "").replace("{server}", var2);
         BodyPublisher var8 = var7.isEmpty() ? BodyPublishers.noBody() : BodyPublishers.ofString(var7);
         HttpRequest var9 = this.request(var1, URI.create(var5)).method(var6, var8).build();
         return this.send(var1, var9);
      }
   }

   private ProviderRegistry.Result send(ProviderRegistry.Definition var1, HttpRequest var2) throws Exception {
      return ProviderHttpSupport.send(var1, var2);
   }

   private Builder request(ProviderRegistry.Definition var1, URI var2) {
      Builder var3 = HttpRequest.newBuilder(var2)
         .timeout(Duration.ofSeconds(var1.timeout()))
         .header("Accept", "application/json")
         .header("User-Agent", "HubPilot/1.0.2");
      String var4 = this.secret(var1);
      Map<String, String> var10000 = var1.values();
      String var6 = var1.type();

      String var5 = var10000.getOrDefault("auth-type", switch (var6) {
         case "crafty", "pterodactyl" -> "bearer";
         default -> "none";
      }).toLowerCase(Locale.ROOT);
      if (!var4.isBlank()) {
         switch (var5) {
            case "bearer":
               var3.header("Authorization", "Bearer " + var4);
               break;
            case "x-api-key":
               var3.header("X-API-Key", var4);
               break;
            case "basic":
               var3.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(var4.getBytes(StandardCharsets.UTF_8)));
         }
      }

      for (Entry var10 : var1.values().entrySet()) {
         if (((String)var10.getKey()).startsWith("header.")) {
            var3.header(((String)var10.getKey()).substring(7), this.resolveSecretText((String)var10.getValue()));
         }
      }

      return var3;
   }

   private HttpClient client(ProviderRegistry.Definition var1) throws Exception {
      java.net.http.HttpClient.Builder var2 = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(var1.timeout()));
      if (var1.insecure()) {
         TrustManager[] var3 = new TrustManager[]{new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
               return new X509Certificate[0];
            }

            @Override
            public void checkClientTrusted(X509Certificate[] var1, String var2x) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] var1, String var2x) {
            }
         }};
         SSLContext var4 = SSLContext.getInstance("TLS");
         var4.init(null, var3, new SecureRandom());
         var2.sslContext(var4);
         SSLParameters var5 = new SSLParameters();
         var5.setEndpointIdentificationAlgorithm("");
         var2.sslParameters(var5);
      }

      return var2.build();
   }

   private ProviderRegistry.Definition resolve(String var1) {
      if (var1 == null || var1.isBlank()) {
         var1 = this.primary;
      }

      String var2 = lower(var1);
      ProviderRegistry.Definition var3 = this.definitions.get(var2);
      if (var3 != null) {
         return var3;
      } else {
         var2 = ProviderAliases.canonical(var2);
         var3 = this.definitions.get(var2);
         if (var3 != null) {
            return var3;
         } else {
            ProviderRegistry.Definition var4 = this.definitions.get(this.primary);
            if (var4 != null && var4.type().equals(var2)) {
               return var4;
            } else {
               for (ProviderRegistry.Definition var6 : this.definitions.values()) {
                  if (var6.type().equals(var2) && var6.enabled()) {
                     return var6;
                  }
               }

               return null;
            }
         }
      }
   }

   private String secret(ProviderRegistry.Definition var1) {
      String var2 = this.secrets.getOrDefault(var1.secretKey(), "");
      return this.resolveSecretText(var2).trim();
   }

   private String resolveSecretText(String var1) {
      if (var1 == null) {
         return "";
      } else {
         String var2 = var1.trim();
         return var2.startsWith("${") && var2.endsWith("}") && var2.length() > 3
            ? System.getenv().getOrDefault(var2.substring(2, var2.length() - 1), "")
            : var2;
      }
   }

   private synchronized void writeProviders(Map<String, ProviderRegistry.Definition> var1, String var2) {
      StringBuilder var3 = new StringBuilder(
         "# ============================================================\n#                   HUBPILOT SERVER PROVIDERS\n# ============================================================\n#\n# Provider definitions are owner-editable. The setup GUI updates this\n# file while keeping a documented layout. A network can use multiple\n# providers at once; individual servers reference a provider id.\n#\n# Built-in provider types:\n#   always-on     - no remote start/stop control\n#   crafty        - Crafty Controller v2 API\n#   pterodactyl   - Pterodactyl Client API\n#   generic-http  - custom API/webhook endpoints\n#\n# Secrets DO NOT belong here. Use secrets.yml or an environment variable.\n#\n# Generic HTTP supports these auth-type values:\n#   none, bearer, x-api-key, basic\n#\n# Generic HTTP URL fields may contain {server}, which HubPilot replaces\n# with the server's startup.provider-server-id.\n#\n"
      );
      var3.append("primary-provider: ").append(var2).append("\n\nproviders:\n");

      for (ProviderRegistry.Definition var5 : var1.values()) {
         var3.append("\n  # ----------------------------------------------------------\n");
         var3.append("  # Provider id: ").append(var5.id()).append("\n");
         var3.append("  # Type: ").append(var5.type()).append("\n");
         var3.append("  # ----------------------------------------------------------\n");
         var3.append("  ").append(var5.id()).append(":\n");
         var3.append("    type: ").append(var5.type()).append("\n");
         var3.append("    # Available: true, false\n    enabled: ").append(var5.enabled()).append("\n");
         if (!var5.baseUrl().isBlank()) {
            var3.append("    # Base panel/API URL. Do not include a trailing slash.\n");
            var3.append("    base-url: \"").append(yamlQuote(var5.baseUrl())).append("\"\n");
         }

         if (!var5.secretKey().isBlank() && !"always-on".equals(var5.type())) {
            var3.append("    # Key name in secrets.yml (not the API key itself).\n");
            var3.append("    secret: ").append(var5.secretKey()).append("\n");
         }

         for (Entry var7 : var5.values().entrySet()) {
            if (!Set.of("type", "enabled", "base-url", "secret").contains(var7.getKey())) {
               if (((String)var7.getKey()).equals("allow-insecure-tls")) {
                  var3.append("    # true disables TLS certificate verification. Keep false unless you knowingly use a trusted self-signed endpoint.\n");
               } else if (((String)var7.getKey()).equals("connect-timeout-seconds")) {
                  var3.append("    # HTTP connect/request timeout in seconds. Recommended: 3-30.\n");
               } else if (((String)var7.getKey()).endsWith("-url")) {
                  var3.append("    # HTTP endpoint. {server} is replaced with provider-server-id.\n");
               } else if (((String)var7.getKey()).endsWith("-method")) {
                  var3.append("    # HTTP method, for example GET, POST, PUT, PATCH, DELETE.\n");
               } else if (((String)var7.getKey()).equals("auth-type")) {
                  var3.append("    # Available: none, bearer, x-api-key, basic\n");
               } else if (((String)var7.getKey()).startsWith("header.")) {
                  var3.append("    # Custom HTTP header. Environment references such as ${NAME} are supported.\n");
               }

               var3.append("    ").append((String)var7.getKey()).append(": ").append(yamlScalar((String)var7.getValue())).append("\n");
            }
         }
      }

      atomic(this.dataDir.resolve("providers.yml"), var3.toString());
   }

   private synchronized void writeSecrets(Map<String, String> var1) {
      StringBuilder var2 = new StringBuilder(
         "# ============================================================\n#                     HUBPILOT SECRETS\n# ============================================================\n# This file contains sensitive credentials. Do NOT upload it in bug\n# reports or publish it with your server configuration.\n#\n# Values may reference environment variables instead of storing a key:\n#   crafty-token: \"${HUBPILOT_CRAFTY_TOKEN}\"\n#\n# API keys entered through /hp setup are written here. HubPilot never\n# provides an in-game command to print a saved secret back out.\n#\nsecrets:\n"
      );
      var1.forEach((var1x, var2x) -> var2.append("  ").append(var1x).append(": \"").append(yamlQuote(var2x)).append("\"\n"));
      atomic(this.dataDir.resolve("secrets.yml"), var2.toString());

      try {
         if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            Files.setPosixFilePermissions(this.dataDir.resolve("secrets.yml"), Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
         }
      } catch (Exception var4) {
      }
   }

   private Map<String, Object> load(Path var1) throws IOException {
      if (!Files.exists(var1)) {
         return Map.of();
      } else {
         Map var5;
         try (BufferedReader var2 = Files.newBufferedReader(var1, StandardCharsets.UTF_8)) {
            if (this.yaml.load(var2) instanceof Map<?, ?> var4) {
               LinkedHashMap var9 = new LinkedHashMap();
               var4.forEach((var1x, var2x) -> var9.put(String.valueOf(var1x), var2x));
               return var9;
            }

            var5 = Map.of();
         }

         return var5;
      }
   }

   private static Map<String, String> merge(Map<String, String> var0, Map<String, String> var1) {
      LinkedHashMap var2 = new LinkedHashMap(var0);
      var2.putAll(var1);
      return Map.copyOf(var2);
   }

   private static String compact(String var0) {
      if (var0 != null && !var0.isBlank()) {
         var0 = var0.replaceAll("\\s+", " ").trim();
         return var0.length() > 300 ? var0.substring(0, 300) + "…" : var0;
      } else {
         return "HTTP request failed.";
      }
   }

   private static String rootMessage(Throwable var0) {
      while (var0.getCause() != null && var0.getCause() != var0) {
         var0 = var0.getCause();
      }

      return var0.getMessage() == null ? var0.getClass().getSimpleName() : var0.getMessage();
   }

   private static String lower(String var0) {
      return var0 == null ? "" : var0.trim().toLowerCase(Locale.ROOT);
   }

   private static String str(Object var0, String var1) {
      return var0 == null ? var1 : String.valueOf(var0);
   }

   private static int intVal(String var0, int var1, int var2, int var3) {
      try {
         int var4 = Integer.parseInt(var0);
         return Math.max(var2, Math.min(var3, var4));
      } catch (Exception var5) {
         return var1;
      }
   }

   private static long mtime(Path var0) {
      try {
         return Files.getLastModifiedTime(var0).toMillis();
      } catch (Exception var2) {
         return -1L;
      }
   }

   private static String stripSlash(String var0) {
      if (var0 == null) {
         return "";
      } else {
         var0 = var0.trim();

         while (var0.endsWith("/")) {
            var0 = var0.substring(0, var0.length() - 1);
         }

         return var0;
      }
   }

   private static String url(String var0) {
      return URLEncoder.encode(var0, StandardCharsets.UTF_8).replace("+", "%20");
   }

   private static String yamlQuote(String var0) {
      return var0 == null ? "" : var0.replace("\\", "\\\\").replace("\"", "\\\"");
   }

   private static String yamlScalar(String var0) {
      if (var0 == null) {
         return "\"\"";
      } else {
         return var0.matches("(?i:true|false|-?\\d+(\\.\\d+)?)") ? var0 : "\"" + yamlQuote(var0) + "\"";
      }
   }

   private static void atomic(Path var0, String var1) {
      try {
         Path var2 = var0.resolveSibling(var0.getFileName() + ".tmp");
         Files.writeString(var2, var1, StandardCharsets.UTF_8);

         try {
            Files.move(var2, var0, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (AtomicMoveNotSupportedException var4) {
            Files.move(var2, var0, StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (Exception var5) {
      }
   }

   private void logWarn(String var1, Object... var2) {
      try {
         if (this.logger != null) {
            this.logger.warn(var1, var2);
         }
      } catch (Throwable var4) {
      }
   }

   synchronized String setPrimaryInsecure(boolean var1) {
      ProviderRegistry.Definition var2 = this.primaryDefinition();
      LinkedHashMap var3 = new LinkedHashMap<>(this.definitions);
      LinkedHashMap var4 = new LinkedHashMap<>(var2.values());
      var4.put("allow-insecure-tls", Boolean.toString(var1));
      var3.put(var2.id(), new ProviderRegistry.Definition(var2.id(), var2.type(), var2.enabled(), var2.baseUrl(), var2.secretKey(), var1, var2.timeout(), var4));
      this.writeProviders(var3, this.primaryId());
      this.reload();
      return "SUCCESS";
   }

   record Definition(String id, String type, boolean enabled, String baseUrl, String secretKey, boolean insecure, int timeout, Map<String, String> values) {
   }

   record Result(boolean success, int statusCode, String error) {
      static ProviderRegistry.Result ok(int var0) {
         return new ProviderRegistry.Result(true, var0, "");
      }

      static ProviderRegistry.Result fail(int var0, String var1) {
         return new ProviderRegistry.Result(false, var0, var1 == null ? "Unknown provider error" : var1);
      }
   }
}
