package dev.hubpilot.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Exercises ControllerProvider against a real (disposable) HubPilot Controller. Args: url, workdir, serverId. */
public final class ControllerProviderLiveTest {
   static int failures = 0;
   static void check(String name, boolean ok, String detail) {
      System.out.println((ok ? "PASS " : "FAIL ") + name + (detail.isEmpty() ? "" : " :: " + detail));
      if (!ok) failures++;
   }
   public static void main(String[] a) throws Exception {
      String url = a[0]; Path dir = Path.of(a[1]); String sid = a[2];
      String[] begin = ControllerProvider.beginPairing(url, 10);
      Files.writeString(dir.resolve("challenge"), begin[0]);
      Files.writeString(dir.resolve("pin"), begin[1]);
      check("pin is sha256 hex", begin[1].matches("[0-9a-f]{64}"), ControllerProvider.pretty(begin[1]).substring(0, 11) + "…");
      Path codeFile = dir.resolve("code");
      for (int i = 0; i < 100 && !Files.exists(codeFile); i++) Thread.sleep(200);
      try { ControllerProvider.finishPairing("bad!code", 10); check("rejects malformed code", false, ""); } catch (IllegalArgumentException e) { check("rejects malformed code", true, ""); }
      String[] done = ControllerProvider.finishPairing(Files.readString(codeFile).trim(), 10);
      check("exchange returns credential", done[2] != null && done[2].length() > 20, "credential id " + done[3]);
      try { ControllerProvider.finishPairing(Files.readString(codeFile).trim(), 10); check("code is single use", false, ""); } catch (IllegalStateException e) { check("code is single use (no pending pairing)", true, ""); }
      ProviderRegistry.Definition def = new ProviderRegistry.Definition("hubpilot-controller", "hubpilot-controller", true, done[0], "x", false, 10, Map.of("tls-pin", done[1]));
      check("test() connects", ControllerProvider.test(def, done[2]).success(), "");
      List<ControllerProvider.Server> servers = ControllerProvider.servers(def, done[2]);
      check("lists servers", servers.stream().anyMatch(s -> s.id().equals(sid)), servers.size() + " servers");
      ProviderRegistry.Result manual = ControllerProvider.power(def, done[2], sid, "start");
      Files.writeString(dir.resolve("phase1"), manual.statusCode() + " " + manual.error());
      check("start refused while lifecycle is manual", !manual.success() && manual.error().contains("lifecycle_conflict"), manual.statusCode() + " " + manual.error());
      for (int i = 0; i < 100 && !Files.exists(dir.resolve("delegated")); i++) Thread.sleep(200);
      ProviderRegistry.Result start = ControllerProvider.power(def, done[2], sid, "start");
      check("start accepted or refused by the Controller for a runtime reason, not auth/idempotency",
         start.success() || !(start.error().contains("idempotency") || start.error().contains("forbidden") || start.error().contains("lifecycle")), start.statusCode() + " " + start.error());
      ProviderRegistry.Definition wrongPin = new ProviderRegistry.Definition("hubpilot-controller", "hubpilot-controller", true, done[0], "x", false, 10, Map.of("tls-pin", "00".repeat(32)));
      ProviderRegistry.Result pinned;
      try { pinned = ControllerProvider.test(wrongPin, done[2]); } catch (Exception e) { pinned = ProviderRegistry.Result.fail(0, String.valueOf(e.getMessage()) + " " + e.getCause()); }
      check("wrong certificate pin refuses to connect", !pinned.success(), pinned.error().length() > 90 ? pinned.error().substring(0, 90) : pinned.error());
      check("bad server id refused locally", !ControllerProvider.power(def, done[2], "../etc", "start").success(), "");
      ProviderRegistry.Result badToken = ControllerProvider.test(def, "not-a-real-token-123456");
      check("revoked/invalid token is refused", !badToken.success(), badToken.statusCode() + " " + badToken.error());
      Files.writeString(dir.resolve("credential-id"), done[3] == null ? "" : done[3]);
      System.out.println(failures == 0 ? "ALL PASS" : failures + " FAILED");
      System.exit(failures == 0 ? 0 : 1);
   }
}
