package dev.hubpilot.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;

final class PublicDefaults {
   static final String CONFIG = "# ============================================================\n#                       HUBPILOT CORE\n# ============================================================\n#\n# Global settings shared by HubPilot Core.\n# This file is owner-editable. HubPilot will not rewrite it just\n# to store runtime state.\n#\n# After editing, use /hp reload once setup is complete.\n#\n\n# Shared HubPilot data folder used by the proxy/hub when they are\n# on the same host or share a mounted volume.\n# Plugin messaging is used for authorization, so this path is NOT\n# required to be shared just to use staff/owner permissions.\nshared-directory: shared\n\n# Exact Velocity server name of the hub.\nhub-server: hub\n\n# Seconds between status refreshes.\n# Allowed: 1-60\nstatus-refresh-seconds: 5\n\n# Seconds between config reload checks.\n# Allowed: 1-60\nconfig-reload-seconds: 3\n\n# When true, direct attempts to join an offline managed backend are\n# routed through HubPilot lifecycle handling.\n# Allowed: true, false\nintercept-direct-server-requests: true\n\n# Backends allowed to send trusted HubPilot control/request traffic.\n# The configured hub should normally be present here.\ntrusted-request-servers:\n  - hub\n\nmessages-file: messages/en_US.yml\n\nmodules:\n  lifecycle: true\n  statistics: true\n  idle-shutdown: true\n";
   static final String DEFAULTS = "# ============================================================\n#                 HUBPILOT SERVER DEFAULTS\n# ============================================================\n# Values inherited by managed servers unless their server file\n# overrides the same setting.\n\nstartup:\n  # Provider id from providers.yml.\n  # \"always-online\" means HubPilot will never try to power it on/off.\n  provider: always-online\n  expected-seconds: 60\n  timeout-seconds: 120\n  ping-timeout-seconds: 3\n  stop-after-failure: true\n  stop-when-queue-empty: true\n\nconnection:\n  retry-count: 3\n  retry-delay-seconds: 15\n\nidle-shutdown:\n  # 0 disables automatic idle shutdown.\n  minutes: 30\n\ncompatibility:\n  strict-version: false\n\ncountdown:\n  duration-seconds: 5\n  sound: minecraft:block.note_block.pling\n  volume: 1.0\n  # Available: rising, flat\n  pitch-style: rising\n  message: \"Joining <server> in <seconds>...\"\n  announce-every-second: false\n";
   static final String PERMISSIONS = "# ============================================================\n#                  HUBPILOT STAFF PERMISSIONS\n# ============================================================\n#\n# HubPilot has an internal role system so a proxy permission plugin\n# is optional. Permission plugins are still supported: HubPilot Hub\n# reports Bukkit/Paper permissions to Core securely.\n#\n# OP FALLBACK\n# -----------\n# Bukkit/Paper operators receive this role unless they have an\n# explicit HubPilot staff assignment.\n#\n# Available: none, helper, moderator, admin\n# Owner can NEVER be granted automatically from OP status.\nop-default-role: admin\n\nroles:\n  helper:\n    priority: 10\n    permissions:\n      - hubpilot.status.view\n\n  moderator:\n    priority: 20\n    inherits: helper\n    permissions:\n      - hubpilot.server.request\n      - hubpilot.maintenance.toggle\n      - hubpilot.debug.request\n\n  admin:\n    priority: 30\n    inherits: moderator\n    permissions:\n      - hubpilot.admin\n      - hubpilot.servers.*\n      - hubpilot.navigator.*\n      - hubpilot.interact.*\n      - hubpilot.staff.view\n      - hubpilot.staff.manage\n      - hubpilot.reload\n\n# Owner is implicit and always receives hubpilot.*.\n# Explicit owners/staff are stored in staff.yml.\n";
   static final String PROVIDERS = "# ============================================================\n#                   HUBPILOT SERVER PROVIDERS\n# ============================================================\n#\n# Providers control power actions for managed servers. A Velocity\n# network may use more than one provider at the same time.\n#\n# Built-in types in 1.0.2:\n#   always-on     - HubPilot does not control server power\n#   crafty        - Crafty Controller v2 API\n#   pterodactyl   - Pterodactyl Client API\n#   generic-http  - Custom/paid host HTTP endpoints\n#\n# AMP, Multicraft, and paid hosts can be connected through\n# generic-http when they expose suitable API/webhook endpoints.\n# Provider adapters can be expanded without changing server files.\n#\n# Secrets are referenced by name and stored in secrets.yml or an\n# environment variable. Never paste secrets into this file.\n\nprimary-provider: always-online\n\nproviders:\n  always-online:\n    type: always-on\n    enabled: true\n\n  crafty:\n    type: crafty\n    enabled: false\n    base-url: \"https://panel.example.com\"\n    secret: crafty-token\n    allow-insecure-tls: false\n    connect-timeout-seconds: 10\n\n  pterodactyl:\n    type: pterodactyl\n    enabled: false\n    base-url: \"https://panel.example.com\"\n    secret: pterodactyl-token\n    connect-timeout-seconds: 10\n\n  generic-http:\n    type: generic-http\n    enabled: false\n    # {server} is replaced with startup.provider-server-id.\n    start-method: POST\n    start-url: \"https://host.example/api/servers/{server}/start\"\n    stop-method: POST\n    stop-url: \"https://host.example/api/servers/{server}/stop\"\n    status-method: GET\n    status-url: \"https://host.example/api/servers/{server}/status\"\n    secret: generic-http-token\n    # Available auth types: none, bearer, x-api-key, basic\n    auth-type: bearer\n    connect-timeout-seconds: 10\n";
   static final String SECRETS = "# ============================================================\n#                     HUBPILOT SECRETS\n# ============================================================\n# This file can contain API credentials. Do NOT upload it in bug\n# reports or publish it with your server files.\n#\n# A value may reference an environment variable instead:\n#   crafty-token: \"${HUBPILOT_CRAFTY_TOKEN}\"\n#\n# API keys entered through the HubPilot setup GUI are written here.\n\nsecrets:\n  crafty-token: \"\"\n  pterodactyl-token: \"\"\n  generic-http-token: \"\"\n";
   static final String STAFF = "# ============================================================\n#                       HUBPILOT STAFF\n# ============================================================\n# Runtime-managed by /hp claimowner, /hp staff, and the Staff GUI.\n# UUIDs are authoritative; usernames are kept for readability.\n#\n# To recover an installation with no accessible owner:\n#   1. Stop the proxy/hub.\n#   2. Remove the owner entries below.\n#   3. Restart and use /hp claimowner as an OP on the hub.\n\nowners: []\nstaff: []\n";
   static final String SETUP = "# HubPilot first-run state. Normally managed by /hp setup.\nsetup-version: 1\ncomplete: false\nprovider-skipped: false\n";
   static final String LEGACY_CRAFTY_COMPAT = "# Compatibility file read by HubPilot's legacy 3.x configuration loader.\n# New public installs should configure providers in ../providers.yml.\nenabled: false\nbase-url: https://localhost\n# Kept blank intentionally. API secrets belong in ../secrets.yml.\ntoken-file: /dev/null\nallow-insecure-tls: false\nconnect-timeout-seconds: 10\n";

   private PublicDefaults() {
   }

   static void install(Path var0) {
      try {
         Files.createDirectories(var0);
         Files.createDirectories(var0.resolve("providers"));
         Files.createDirectories(var0.resolve("servers"));
         Files.createDirectories(var0.resolve("messages"));
         Files.createDirectories(var0.resolve("backups"));
         writeIfMissing(
            var0.resolve("config.yml"),
            "# ============================================================\n#                       HUBPILOT CORE\n# ============================================================\n#\n# Global settings shared by HubPilot Core.\n# This file is owner-editable. HubPilot will not rewrite it just\n# to store runtime state.\n#\n# After editing, use /hp reload once setup is complete.\n#\n\n# Shared HubPilot data folder used by the proxy/hub when they are\n# on the same host or share a mounted volume.\n# Plugin messaging is used for authorization, so this path is NOT\n# required to be shared just to use staff/owner permissions.\nshared-directory: shared\n\n# Exact Velocity server name of the hub.\nhub-server: hub\n\n# Seconds between status refreshes.\n# Allowed: 1-60\nstatus-refresh-seconds: 5\n\n# Seconds between config reload checks.\n# Allowed: 1-60\nconfig-reload-seconds: 3\n\n# When true, direct attempts to join an offline managed backend are\n# routed through HubPilot lifecycle handling.\n# Allowed: true, false\nintercept-direct-server-requests: true\n\n# Backends allowed to send trusted HubPilot control/request traffic.\n# The configured hub should normally be present here.\ntrusted-request-servers:\n  - hub\n\nmessages-file: messages/en_US.yml\n\nmodules:\n  lifecycle: true\n  statistics: true\n  idle-shutdown: true\n"
         );
         writeIfMissing(
            var0.resolve("defaults.yml"),
            "# ============================================================\n#                 HUBPILOT SERVER DEFAULTS\n# ============================================================\n# Values inherited by managed servers unless their server file\n# overrides the same setting.\n\nstartup:\n  # Provider id from providers.yml.\n  # \"always-online\" means HubPilot will never try to power it on/off.\n  provider: always-online\n  expected-seconds: 60\n  timeout-seconds: 120\n  ping-timeout-seconds: 3\n  stop-after-failure: true\n  stop-when-queue-empty: true\n\nconnection:\n  retry-count: 3\n  retry-delay-seconds: 15\n\nidle-shutdown:\n  # 0 disables automatic idle shutdown.\n  minutes: 30\n\ncompatibility:\n  strict-version: false\n\ncountdown:\n  duration-seconds: 5\n  sound: minecraft:block.note_block.pling\n  volume: 1.0\n  # Available: rising, flat\n  pitch-style: rising\n  message: \"Joining <server> in <seconds>...\"\n  announce-every-second: false\n"
         );
         writeIfMissing(
            var0.resolve("permissions.yml"),
            "# ============================================================\n#                  HUBPILOT STAFF PERMISSIONS\n# ============================================================\n#\n# HubPilot has an internal role system so a proxy permission plugin\n# is optional. Permission plugins are still supported: HubPilot Hub\n# reports Bukkit/Paper permissions to Core securely.\n#\n# OP FALLBACK\n# -----------\n# Bukkit/Paper operators receive this role unless they have an\n# explicit HubPilot staff assignment.\n#\n# Available: none, helper, moderator, admin\n# Owner can NEVER be granted automatically from OP status.\nop-default-role: admin\n\nroles:\n  helper:\n    priority: 10\n    permissions:\n      - hubpilot.status.view\n\n  moderator:\n    priority: 20\n    inherits: helper\n    permissions:\n      - hubpilot.server.request\n      - hubpilot.maintenance.toggle\n      - hubpilot.debug.request\n\n  admin:\n    priority: 30\n    inherits: moderator\n    permissions:\n      - hubpilot.admin\n      - hubpilot.servers.*\n      - hubpilot.navigator.*\n      - hubpilot.interact.*\n      - hubpilot.staff.view\n      - hubpilot.staff.manage\n      - hubpilot.reload\n\n# Owner is implicit and always receives hubpilot.*.\n# Explicit owners/staff are stored in staff.yml.\n"
         );
         writeIfMissing(
            var0.resolve("providers.yml"),
            "# ============================================================\n#                   HUBPILOT SERVER PROVIDERS\n# ============================================================\n#\n# Providers control power actions for managed servers. A Velocity\n# network may use more than one provider at the same time.\n#\n# Built-in types in 1.0.2:\n#   always-on     - HubPilot does not control server power\n#   crafty        - Crafty Controller v2 API\n#   pterodactyl   - Pterodactyl Client API\n#   generic-http  - Custom/paid host HTTP endpoints\n#\n# AMP, Multicraft, and paid hosts can be connected through\n# generic-http when they expose suitable API/webhook endpoints.\n# Provider adapters can be expanded without changing server files.\n#\n# Secrets are referenced by name and stored in secrets.yml or an\n# environment variable. Never paste secrets into this file.\n\nprimary-provider: always-online\n\nproviders:\n  always-online:\n    type: always-on\n    enabled: true\n\n  crafty:\n    type: crafty\n    enabled: false\n    base-url: \"https://panel.example.com\"\n    secret: crafty-token\n    allow-insecure-tls: false\n    connect-timeout-seconds: 10\n\n  pterodactyl:\n    type: pterodactyl\n    enabled: false\n    base-url: \"https://panel.example.com\"\n    secret: pterodactyl-token\n    connect-timeout-seconds: 10\n\n  generic-http:\n    type: generic-http\n    enabled: false\n    # {server} is replaced with startup.provider-server-id.\n    start-method: POST\n    start-url: \"https://host.example/api/servers/{server}/start\"\n    stop-method: POST\n    stop-url: \"https://host.example/api/servers/{server}/stop\"\n    status-method: GET\n    status-url: \"https://host.example/api/servers/{server}/status\"\n    secret: generic-http-token\n    # Available auth types: none, bearer, x-api-key, basic\n    auth-type: bearer\n    connect-timeout-seconds: 10\n"
         );
         writeIfMissing(
            var0.resolve("secrets.yml"),
            "# ============================================================\n#                     HUBPILOT SECRETS\n# ============================================================\n# This file can contain API credentials. Do NOT upload it in bug\n# reports or publish it with your server files.\n#\n# A value may reference an environment variable instead:\n#   crafty-token: \"${HUBPILOT_CRAFTY_TOKEN}\"\n#\n# API keys entered through the HubPilot setup GUI are written here.\n\nsecrets:\n  crafty-token: \"\"\n  pterodactyl-token: \"\"\n  generic-http-token: \"\"\n"
         );
         writeIfMissing(
            var0.resolve("staff.yml"),
            "# ============================================================\n#                       HUBPILOT STAFF\n# ============================================================\n# Runtime-managed by /hp claimowner, /hp staff, and the Staff GUI.\n# UUIDs are authoritative; usernames are kept for readability.\n#\n# To recover an installation with no accessible owner:\n#   1. Stop the proxy/hub.\n#   2. Remove the owner entries below.\n#   3. Restart and use /hp claimowner as an OP on the hub.\n\nowners: []\nstaff: []\n"
         );
         writeIfMissing(
            var0.resolve("setup.yml"),
            "# HubPilot first-run state. Normally managed by /hp setup.\nsetup-version: 1\ncomplete: false\nprovider-skipped: false\n"
         );
         writeIfMissing(
            var0.resolve("providers/crafty.yml"),
            "# Compatibility file read by HubPilot's legacy 3.x configuration loader.\n# New public installs should configure providers in ../providers.yml.\nenabled: false\nbase-url: https://localhost\n# Kept blank intentionally. API secrets belong in ../secrets.yml.\ntoken-file: /dev/null\nallow-insecure-tls: false\nconnect-timeout-seconds: 10\n"
         );
         harden(var0.resolve("secrets.yml"));
      } catch (Exception var2) {
      }
   }

   private static void writeIfMissing(Path var0, String var1) throws IOException {
      if (!Files.exists(var0)) {
         Files.writeString(var0, var1, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
      }
   }

   private static void harden(Path var0) {
      try {
         if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            Files.setPosixFilePermissions(var0, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
         }
      } catch (Exception var2) {
      }
   }
}
