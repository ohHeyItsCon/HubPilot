package dev.hubpilot.hub.gui;

import dev.hubpilot.hub.status.ServerStatus;

// $VF: synthetic class
class DestinationMenuBuilder$1 {
   static final int[] $SwitchMap$dev$hubpilot$hub$status$ServerStatus$State = new int[ServerStatus.State.values().length];

   static {
      try {
         $SwitchMap$dev$hubpilot$hub$status$ServerStatus$State[ServerStatus.State.ONLINE.ordinal()] = 1;
      } catch (NoSuchFieldError var3) {
      }

      try {
         $SwitchMap$dev$hubpilot$hub$status$ServerStatus$State[ServerStatus.State.STARTING.ordinal()] = 2;
      } catch (NoSuchFieldError var2) {
      }

      try {
         $SwitchMap$dev$hubpilot$hub$status$ServerStatus$State[ServerStatus.State.OFFLINE.ordinal()] = 3;
      } catch (NoSuchFieldError var1) {
      }
   }
}
