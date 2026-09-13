package dev.hubpilot.hub.bridge;

public enum MessageEvent {
   ALREADY_CONNECTED("already-connected", "Already Connected", "&7You are already connected to {server}&7.", "server"),
   CONNECTING("connecting", "Request Sent", "&7Requesting {server}&7...", "server"),
   UNAVAILABLE("unavailable", "Unavailable", "&c{server}&c is unavailable.", "server"),
   AUTOSTART_DISABLED("autostart-disabled", "Autostart Disabled", "&cAutomatic startup is disabled for {server}&c.", "server"),
   STARTING("starting", "Starting", "&eStarting {server}&e...", "server, position, queue_size"),
   ALREADY_STARTING("already-starting", "Already Starting", "&e{server}&e is already starting.", "server, position, queue_size"),
   QUEUE_JOINED("queue-joined", "Queue Joined", "&7You are queue position &f{position}&7 of &f{queue_size}&7.", "server, position, queue_size"),
   ALREADY_QUEUED(
      "already-queued", "Already Queued", "&7You are already queue position &f{position}&7 of &f{queue_size}&7 for {server}&7.", "server, position, queue_size"
   ),
   QUEUE_POSITION(
      "queue-position",
      "Queue Position Changed",
      "&7You are now queue position &f{position}&7 of &f{queue_size}&7 for {server}&7.",
      "server, position, queue_size"
   ),
   READY("ready", "Server Ready", "&a{server}&a is ready.", "server"),
   COUNTDOWN("countdown", "Countdown", "&eJoining {server}&e in &f{seconds}&e...", "server, seconds"),
   JOINING("joining", "Joining", "&7Joining {server}&7...", "server"),
   CONNECTION_FAILED("connection-failed", "Connection Failed", "&cCould not connect to {server}&c.", "server"),
   RETRYING("retrying", "Retrying", "&eConnection failed. Retrying in {delay}s ({attempt}/{max})...", "server, delay, attempt, max"),
   REQUEST_CANCELLED("request-cancelled", "Request Cancelled", "&eYour pending HubPilot request was cancelled.", "server"),
   NO_PENDING_REQUEST("no-pending-request", "No Pending Request", "&7You do not have a pending HubPilot request.", "server"),
   START_FAILED("start-failed", "Start Failed", "&c{server}&c failed to start: {error}", "server, error"),
   MAINTENANCE("maintenance", "Maintenance", "&c{server}&c is currently in maintenance mode.", "server"),
   WRONG_VERSION("wrong-version", "Wrong Version", "&c{server}&c requires Minecraft {required}. Your client supports {current}.", "server, required, current"),
   MISSING_PERMISSION("missing-permission", "Missing Permission", "&cYou do not have permission to join {server}&c.", "server");

   private final String id;
   private final String label;
   private final String defaultText;
   private final String placeholders;

   private MessageEvent(String nullxx, String nullxxx, String nullxxxx, String nullxxxxx) {
      this.id = nullxx;
      this.label = nullxxx;
      this.defaultText = nullxxxx;
      this.placeholders = nullxxxxx;
   }

   public String id() {
      return this.id;
   }

   public String label() {
      return this.label;
   }

   public String defaultText() {
      return this.defaultText;
   }

   public String placeholders() {
      return this.placeholders;
   }
}
