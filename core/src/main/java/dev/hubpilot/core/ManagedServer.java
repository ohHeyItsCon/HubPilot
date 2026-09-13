package dev.hubpilot.core;

import java.util.List;

public record ManagedServer(
   String id,
   String displayName,
   String velocityServer,
   String provider,
   String providerServerId,
   String requiredVersion,
   boolean strictVersion,
   String loader,
   String permission,
   boolean maintenance,
   boolean autoStartEnabled,
   int expectedStartupSeconds,
   int startupTimeoutSeconds,
   int pingTimeoutSeconds,
   int retryCount,
   int retryDelaySeconds,
   boolean stopAfterFailure,
   boolean stopWhenQueueEmpty,
   int idleShutdownMinutes,
   int countdownSeconds,
   String countdownSound,
   float countdownVolume,
   String pitchStyle,
   String countdownMessage,
   boolean announceEverySecond,
   List<String> tags
) {
   public String label() {
      return this.displayName != null && !this.displayName.isBlank() ? this.displayName : this.id;
   }
}
