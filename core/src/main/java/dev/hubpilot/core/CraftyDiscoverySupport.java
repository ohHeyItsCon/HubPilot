package dev.hubpilot.core;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.yaml.snakeyaml.Yaml;

final class CraftyDiscoverySupport {
   private static volatile Map<String, String> byCanonicalName = Map.of();
   private static volatile Set<String> liveIds = Set.of();

   private CraftyDiscoverySupport() {
   }

   static List<String> filter(HubPilotConfig var0, List<String> var1) {
      ProviderRegistry var2 = ProviderRegistry.get();
      ProviderRegistry.Definition var3 = var2.primaryDefinition();
      if (var3 != null && var3.enabled() && "crafty".equalsIgnoreCase(var3.type())) {
         CraftyDiscoverySupport.Inventory var4 = fetch(var2, var3);
         DynamicServerRegistry.registerAll(var4.endpoints);
         byCanonicalName = var4.byName;
         liveIds = var4.ids;
         Map var5 = var0.snapshot().servers();
         ArrayList var6 = new ArrayList();

         for (CraftyDiscoverySupport.Endpoint var8 : var4.endpoints) {
            var6.add(var8.velocityName);
         }

         for (String var12 : var1) {
            if (var12 != null && !var12.isBlank()) {
               ManagedServer var9 = findManaged(var5, var12);
               if (var9 != null) {
                  if (!"crafty".equalsIgnoreCase(var9.provider())) {
                     if (!containsIgnoreCase(var6, var12)) {
                        var6.add(var12);
                     }
                  } else {
                     String var10 = clean(var9.providerServerId());
                     if (!var10.isEmpty() && var4.ids.contains(var10.toLowerCase(Locale.ROOT))) {
                        if (!containsIgnoreCase(var6, var12)) {
                           var6.add(var12);
                        }
                     } else if (var4.byName.containsKey(canonical(var12)) && !containsIgnoreCase(var6, var12)) {
                        var6.add(var12);
                     }
                  }
               } else if (var4.byName.containsKey(canonical(var12)) && !containsIgnoreCase(var6, var12)) {
                  var6.add(var12);
               }
            }
         }

         return var6;
      } else {
         return var1;
      }
   }

   static Optional<String> resolveUuid(Collection<String> var0) {
      try {
         ProviderRegistry var1 = ProviderRegistry.get();
         ProviderRegistry.Definition var2 = var1.primaryDefinition();
         if (var2 == null || !var2.enabled() || !"crafty".equalsIgnoreCase(var2.type())) {
            return Optional.empty();
         }

         CraftyDiscoverySupport.Inventory var3 = fetch(var1, var2);
         byCanonicalName = var3.byName;
         liveIds = var3.ids;

         for (String var5 : var0) {
            String var6 = clean(var5);
            if (!var6.isEmpty()) {
               if (var3.ids.contains(var6.toLowerCase(Locale.ROOT))) {
                  return Optional.of(var6);
               }

               String var7 = var3.byName.get(canonical(var6));
               if (var7 != null && !var7.isBlank()) {
                  return Optional.of(var7);
               }
            }
         }
      } catch (Throwable var8) {
      }

      return Optional.empty();
   }

   private static CraftyDiscoverySupport.Inventory fetch(ProviderRegistry var0, ProviderRegistry.Definition var1) {
      try {
         String var2 = var1.baseUrl();
         if (var2 != null && !var2.isBlank()) {
            while (var2.endsWith("/")) {
               var2 = var2.substring(0, var2.length() - 1);
            }

            URI var23 = URI.create(var2 + "/api/v2/servers");
            Method var24 = ProviderRegistry.class.getDeclaredMethod("request", ProviderRegistry.Definition.class, URI.class);
            var24.setAccessible(true);
            HttpRequest var5 = ((Builder)var24.invoke(var0, var1, var23)).GET().build();
            CraftyDiscoverySupport.Response var6;
            if (var1.insecure() && "https".equalsIgnoreCase(var23.getScheme())) {
               var6 = sendTrustedLan(var5, var1.timeout());
            } else {
               Method var7 = ProviderRegistry.class.getDeclaredMethod("client", ProviderRegistry.Definition.class);
               var7.setAccessible(true);
               HttpClient var8 = (HttpClient)var7.invoke(var0, var1);
               HttpResponse var9 = var8.send(var5, BodyHandlers.ofString());
               var6 = new CraftyDiscoverySupport.Response(var9.statusCode(), (String)var9.body());
            }

            if (var6.status >= 200 && var6.status < 300) {
               if (!(new Yaml().load(var6.body) instanceof Map var26)) {
                  throw new IllegalStateException("Crafty server inventory returned invalid JSON");
               } else if (!(var26.get("data") instanceof Collection var10)) {
                  throw new IllegalStateException("Crafty server inventory did not contain a server list");
               } else {
                  LinkedHashMap var11 = new LinkedHashMap();
                  LinkedHashSet var12 = new LinkedHashSet();
                  ArrayList var13 = new ArrayList();

                  for (Object var15 : var10) {
                     if (var15 instanceof Map var16) {
                        String var17 = first(var16, "server_id", "server_uuid", "id");
                        String var18 = first(var16, "server_name", "name");
                        String var19 = first(var16, "server_ip", "ip", "host");
                        int var20 = integer(var16, "server_port", "port");
                        if (var19.isBlank()
                           || var19.equals("0.0.0.0")
                           || var19.equals("::")
                           || var19.equalsIgnoreCase("localhost")
                           || var19.equals("127.0.0.1")) {
                           var19 = var23.getHost();
                        }

                        if (!var17.isBlank()) {
                           var12.add(var17.toLowerCase(Locale.ROOT));
                        }

                        if (!var17.isBlank() && !var18.isBlank()) {
                           var11.putIfAbsent(canonical(var18), var17);
                        }

                        if (!var17.isBlank() && !var18.isBlank() && !var19.isBlank() && var20 > 0 && var20 <= 65535) {
                           var13.add(new CraftyDiscoverySupport.Endpoint(velocityName(var18), var18, var19, var20, var17));
                        }
                     }
                  }

                  return new CraftyDiscoverySupport.Inventory(Collections.unmodifiableMap(var11), Collections.unmodifiableSet(var12), List.copyOf(var13));
               }
            } else {
               throw new IllegalStateException("Crafty server inventory returned HTTP " + var6.status);
            }
         } else {
            throw new IllegalStateException("Crafty base URL is not configured");
         }
      } catch (RuntimeException var21) {
         throw var21;
      } catch (Throwable var22) {
         Throwable var3 = var22;

         while (var3.getCause() != null && var3.getCause() != var3) {
            var3 = var3.getCause();
         }

         String var4 = var3.getMessage();
         throw new IllegalStateException("Crafty discovery verification failed: " + (var4 == null ? var3.getClass().getSimpleName() : var4), var3);
      }
   }

   private static CraftyDiscoverySupport.Response sendTrustedLan(HttpRequest var0, int var1) throws Exception {
      HttpURLConnection var2 = (HttpURLConnection)var0.uri().toURL().openConnection();
      if (var2 instanceof HttpsURLConnection var3) {
         TrustManager[] var4 = new TrustManager[]{new X509TrustManager() {
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
         SSLContext var5 = SSLContext.getInstance("TLS");
         var5.init(null, var4, new SecureRandom());
         var3.setSSLSocketFactory(var5.getSocketFactory());
         var3.setHostnameVerifier((var0x, var1x) -> true);
         var3.setInstanceFollowRedirects(false);
         int var6 = Math.toIntExact(Math.min(2147483647L, Math.max(1L, (long)var1) * 1000L));
         var3.setConnectTimeout(var6);
         var3.setReadTimeout(var6);
         var3.setRequestMethod("GET");
         var0.headers().map().forEach((var1x, var2x) -> {
            boolean var3x = true;

            for (String var5x : var2x) {
               if (var3x) {
                  var3.setRequestProperty(var1x, var5x);
                  var3x = false;
               } else {
                  var3.addRequestProperty(var1x, var5x);
               }
            }
         });
         int var7 = var3.getResponseCode();
         InputStream var8 = var7 >= 400 ? var3.getErrorStream() : var3.getInputStream();
         String var9 = var8 == null ? "" : new String(var8.readAllBytes(), StandardCharsets.UTF_8);
         if (var8 != null) {
            var8.close();
         }

         var3.disconnect();
         return new CraftyDiscoverySupport.Response(var7, var9);
      } else {
         throw new IllegalStateException("Insecure TLS requested for a non-HTTPS Crafty URL");
      }
   }

   private static ManagedServer findManaged(Map<String, ManagedServer> var0, String var1) {
      ManagedServer var2 = (ManagedServer)var0.get(var1);
      if (var2 != null) {
         return var2;
      } else {
         for (ManagedServer var4 : var0.values()) {
            if (var4 != null && (eq(var1, var4.id()) || eq(var1, var4.velocityServer()) || eq(var1, var4.label()))) {
               return var4;
            }
         }

         return null;
      }
   }

   private static String first(Map<?, ?> var0, String... var1) {
      for (String var5 : var1) {
         Object var6 = var0.get(var5);
         if (var6 != null) {
            String var7 = String.valueOf(var6).trim();
            if (!var7.isEmpty() && !"null".equalsIgnoreCase(var7)) {
               return var7;
            }
         }
      }

      return "";
   }

   private static int integer(Map<?, ?> var0, String... var1) {
      String var2 = first(var0, var1);

      try {
         return Integer.parseInt(var2);
      } catch (NumberFormatException var4) {
         return -1;
      }
   }

   private static boolean containsIgnoreCase(Collection<String> var0, String var1) {
      for (String var3 : var0) {
         if (var3.equalsIgnoreCase(var1)) {
            return true;
         }
      }

      return false;
   }

   private static String velocityName(String var0) {
      String var1 = clean(var0).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "-").replaceAll("^-+|-+$", "");
      return var1.isBlank() ? "server" : var1;
   }

   private static boolean eq(String var0, String var1) {
      return var0 != null && var1 != null && var0.equalsIgnoreCase(var1);
   }

   private static String clean(String var0) {
      return var0 == null ? "" : var0.trim();
   }

   private static String canonical(String var0) {
      return clean(var0).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
   }

   record Endpoint(String velocityName, String displayName, String host, int port, String craftyId) {
   }

   private record Inventory(Map<String, String> byName, Set<String> ids, List<CraftyDiscoverySupport.Endpoint> endpoints) {
   }

   private record Response(int status, String body) {
   }
}
