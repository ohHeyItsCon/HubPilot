package dev.hubpilot.core;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

final class ProviderHttpSupport {
   private ProviderHttpSupport() {
   }

   static ProviderRegistry.Result send(ProviderRegistry.Definition var0, HttpRequest var1) throws Exception {
      if (var0.insecure() && "https".equalsIgnoreCase(var1.uri().getScheme())) {
         return sendInsecure(var0, var1);
      } else {
         HttpClient var2 = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(var0.timeout())).build();
         HttpResponse var3 = var2.send(var1, BodyHandlers.ofString());
         int var4 = var3.statusCode();
         return var4 >= 200 && var4 < 300 ? ProviderRegistry.Result.ok(var4) : ProviderRegistry.Result.fail(var4, compact((String)var3.body()));
      }
   }

   private static ProviderRegistry.Result sendInsecure(ProviderRegistry.Definition var0, HttpRequest var1) throws Exception {
      HttpURLConnection var2 = (HttpURLConnection)var1.uri().toURL().openConnection();
      if (!(var2 instanceof HttpsURLConnection var3)) {
         throw new IllegalStateException("Insecure TLS requested for a non-HTTPS connection.");
      } else {
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
         int var6 = Math.toIntExact(Math.min(2147483647L, Math.max(1L, (long)var0.timeout()) * 1000L));
         var3.setConnectTimeout(var6);
         var3.setReadTimeout(var6);
         var3.setRequestMethod(var1.method());
         var1.headers()
            .map()
            .forEach(
               (var1x, var2x) -> {
                  String var3x = var1x.toLowerCase(Locale.ROOT);
                  if (!var3x.equals("host")
                     && !var3x.equals("content-length")
                     && !var3x.equals("connection")
                     && !var3x.equals("expect")
                     && !var3x.equals("upgrade")) {
                     boolean var4x = true;

                     for (String var6x : var2x) {
                        if (var4x) {
                           var3.setRequestProperty(var1x, var6x);
                           var4x = false;
                        } else {
                           var3.addRequestProperty(var1x, var6x);
                        }
                     }
                  }
               }
            );
         byte[] var7 = bodyBytes(var1);
         if (var7.length > 0) {
            var3.setDoOutput(true);
            var3.setFixedLengthStreamingMode(var7.length);

            try (OutputStream var8 = var3.getOutputStream()) {
               var8.write(var7);
            }
         }

         int var18 = var3.getResponseCode();
         InputStream var10 = var18 >= 400 ? var3.getErrorStream() : var3.getInputStream();
         String var9;
         if (var10 == null) {
            var9 = "";
         } else {
            InputStream var11 = var10;

            try {
               var9 = new String(var10.readAllBytes(), StandardCharsets.UTF_8);
            } catch (Throwable var16) {
               if (var10 != null) {
                  try {
                     var11.close();
                  } catch (Throwable var14) {
                     var16.addSuppressed(var14);
                  }
               }

               throw var16;
            }

            if (var10 != null) {
               var10.close();
            }
         }

         var3.disconnect();
         return var18 >= 200 && var18 < 300 ? ProviderRegistry.Result.ok(var18) : ProviderRegistry.Result.fail(var18, compact(var9));
      }
   }

   private static byte[] bodyBytes(HttpRequest var0) throws Exception {
      Optional var1 = var0.bodyPublisher();
      if (var1.isEmpty()) {
         return new byte[0];
      } else {
         BodyPublisher var2 = (BodyPublisher)var1.get();
         final CompletableFuture var3 = new CompletableFuture();
         final ByteArrayOutputStream var4 = new ByteArrayOutputStream();
         var2.subscribe(new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription var1) {
               var1.request(Long.MAX_VALUE);
            }

            public void onNext(ByteBuffer var1) {
               byte[] var2x = new byte[var1.remaining()];
               var1.get(var2x);
               var4.writeBytes(var2x);
            }

            @Override
            public void onError(Throwable var1) {
               var3.completeExceptionally(var1);
            }

            @Override
            public void onComplete() {
               var3.complete(var4.toByteArray());
            }
         });
         long var5 = var0.timeout().map(Duration::toSeconds).orElse(30L);
         return (byte[])var3.get(Math.max(1L, var5), TimeUnit.SECONDS);
      }
   }

   private static String compact(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var1 = var0.replaceAll("\\s+", " ").trim();
         return var1.length() > 300 ? var1.substring(0, 300) + "..." : var1;
      } else {
         return "HTTP request failed.";
      }
   }
}
