package dev.hubpilot.hub.adminui;

import dev.hubpilot.hub.HubPilotHubPlugin;
import java.util.Map;
import java.util.WeakHashMap;

public final class HubPilotAdminRuntime {
   private static final Map<HubPilotHubPlugin, AdminSettingsStore> SETTINGS = new WeakHashMap<>();
   private static final Map<HubPilotHubPlugin, NavigatorManager> NAV = new WeakHashMap<>();
   private static final Map<HubPilotHubPlugin, UiPromptManager> PROMPTS = new WeakHashMap<>();

   private HubPilotAdminRuntime() {
   }

   public static synchronized AdminSettingsStore settings(HubPilotHubPlugin var0) {
      return SETTINGS.computeIfAbsent(var0, AdminSettingsStore::new);
   }

   public static synchronized NavigatorManager navigator(HubPilotHubPlugin var0) {
      return NAV.computeIfAbsent(var0, var1 -> new NavigatorManager(var0, settings(var0)));
   }

   public static synchronized UiPromptManager prompts(HubPilotHubPlugin var0) {
      return PROMPTS.computeIfAbsent(var0, UiPromptManager::new);
   }
}
