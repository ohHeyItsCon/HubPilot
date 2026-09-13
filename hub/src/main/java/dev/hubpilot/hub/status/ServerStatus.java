package dev.hubpilot.hub.status;

public record ServerStatus(ServerStatus.State state, long onlineSince, long lastStarted, long lastChecked, int playersOnline, int playersMax) {
   public static ServerStatus unknown() {
      return new ServerStatus(ServerStatus.State.UNKNOWN, 0L, 0L, 0L, -1, -1);
   }

   public static enum State {
      ONLINE,
      OFFLINE,
      STARTING,
      UNKNOWN;
   }
}
