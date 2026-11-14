package dev.hubpilot.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.slf4j.helpers.NOPLogger;

/** Core side of the link test: provider pairing, link claim, heartbeat and a transfer walk. Arg: workdir. */
public final class ControllerLinkLiveTest {
   static Path w(Path d, String n, int tries) throws Exception {
      Path p = d.resolve(n);
      for (int i = 0; i < tries && !Files.exists(p); i++) Thread.sleep(200);
      return p;
   }
   public static void main(String[] a) throws Exception {
      Path dir = Path.of(a[0]);
      String url = Files.readString(w(dir, "url", 150)).trim();
      Path data = Files.createTempDirectory("hubpilot-core");
      ProviderRegistry.get().initialize(data, NOPLogger.NOP_LOGGER);
      String[] begin = ControllerProvider.beginPairing(url, 10);
      Files.writeString(dir.resolve("challenge"), begin[0]);
      String[] done = ControllerProvider.finishPairing(Files.readString(w(dir, "code", 150)).trim(), 10);
      ProviderRegistry.get().saveControllerPairing(done[0], done[1], done[2]);
      System.out.println("provider paired; primary provider now " + ProviderRegistry.get().primaryId());
      ControllerLink.get().initialize(NOPLogger.NOP_LOGGER);
      String claim = ControllerLink.get().claim(Files.readString(w(dir, "linkcode", 150)).trim(), "1.1.0-beta.1", "test network");
      System.out.println("claim: " + claim + " | link state " + ControllerLink.get().state());
      UUID player = UUID.randomUUID();
      for (String phase : new String[]{"queued", "starting_destination", "waiting_for_readiness", "countdown", "switching", "completed"}) {
         ControllerLink.get().report(player, "Steve", "hub", "survival", phase, phase.equals("queued") ? "Queue position 1 of 1" : "");
         Thread.sleep(150);
      }
      UUID other = UUID.randomUUID();
      ControllerLink.get().report(other, "Alex", "hub", "skyblock", "starting_destination", "");
      ControllerLink.get().report(other, "Alex", "hub", "skyblock", "failed", "startup timed out after 120 seconds");
      Thread.sleep(3000);
      System.out.println("link state after heartbeat: " + ControllerLink.get().state());
      String providersYml = Files.readString(data.resolve("providers.yml"));
      String secretsYml = Files.readString(data.resolve("secrets.yml"));
      System.out.println("providers.yml has pin: " + providersYml.contains("tls-pin: ") + " | secrets.yml has link token: " + secretsYml.contains("hubpilot-controller-link: \"hpl_"));
      Files.writeString(dir.resolve("reported"), "1");
      w(dir, "done", 100);
      ControllerLink.get().shutdown();
   }
}
