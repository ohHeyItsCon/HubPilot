package dev.hubpilot.hub.bridge;

public enum SettingKey {
   COUNTDOWN_SECONDS("countdown-seconds", "Countdown duration", "5"),
   COUNTDOWN_SOUND("countdown-sound", "Countdown sound", "minecraft:block.note_block.pling"),
   SOUND_VOLUME("sound-volume", "Sound volume", "1.0"),
   PITCH_STYLE("pitch-style", "Pitch style", "RISING"),
   COUNTDOWN_MESSAGE("countdown-message", "Countdown message", "Joining {server} in {seconds}..."),
   REQUIRED_VERSION("required-version", "Required version", "any"),
   STARTUP_TIMEOUT_SECONDS("startup-timeout-seconds", "Startup timeout", "90"),
   RETRY_COUNT("retry-count", "Retry count", "3"),
   RETRY_DELAY_SECONDS("retry-delay-seconds", "Retry delay", "15"),
   STOP_AFTER_FAILURE("stop-after-failure", "Stop after failed join", "true"),
   IDLE_SHUTDOWN_MINUTES("idle-shutdown-minutes", "Idle shutdown", "30"),
   AUTOSTART_ENABLED("autostart-enabled", "AutoStart", "true"),
   ALWAYS_ON_SERVER("always-on-server", "Always-On server", "false");

   private final String property;
   private final String label;
   private final String defaultValue;

   private SettingKey(String nullxx, String nullxxx, String nullxxxx) {
      this.property = nullxx;
      this.label = nullxxx;
      this.defaultValue = nullxxxx;
   }

   public String property() {
      return this.property;
   }

   public String label() {
      return this.label;
   }

   public String defaultValue() {
      return this.defaultValue;
   }
}
