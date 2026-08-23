# Configuration

HubPilot creates its configuration files on first start. Normal settings can be shared for troubleshooting after they have been checked, but **`secrets.yml` must stay private**.

## Core files

- `config.yml` - Core-wide settings
- `defaults.yml` - defaults inherited by managed servers
- `permissions.yml` - HubPilot roles and OP fallback
- `providers.yml` - provider definitions and primary provider
- `secrets.yml` - provider credentials; keep this private
- `staff.yml` - explicit Owner/staff entries
- `setup.yml` - first-run/setup state
- `servers/` - managed-server definitions
- `messages/` - message files
- `backups/` - HubPilot backups

A legacy `providers/crafty.yml` compatibility file can still be created for older 3.x config loading. New installs should use `providers.yml` and `secrets.yml`.

## Core defaults

Some packaged defaults are:

```yaml
shared-directory: shared
hub-server: hub
status-refresh-seconds: 5
config-reload-seconds: 3
intercept-direct-server-requests: true
trusted-request-servers:
  - hub
modules:
  lifecycle: true
  statistics: true
  idle-shutdown: true
```

## Managed-server defaults

```yaml
startup:
  provider: always-online
  expected-seconds: 60
  timeout-seconds: 120
  ping-timeout-seconds: 3
  stop-after-failure: true
  stop-when-queue-empty: true

connection:
  retry-count: 3
  retry-delay-seconds: 15

idle-shutdown:
  minutes: 30

compatibility:
  strict-version: false

countdown:
  duration-seconds: 5
  sound: minecraft:block.note_block.pling
  volume: 1.0
  pitch-style: rising
  message: "Joining <server> in <seconds>..."
  announce-every-second: false
```

### `provider: always-online`

`always-online` means HubPilot does not manage that server's power.

Use it when the server is kept running by a host, Docker, systemd, another panel, or another process manager. The hub is a good example because it normally stays online while larger backend servers are allowed to shut down.

HubPilot can route players to an Always-On server while it is online, but it will not start or stop the process. A direct power action is rejected instead of returning a false success.

Built-in provider ID: `always-online`  
Provider type: `always-on`

If an offline server should start when a player requests it, configure a power provider instead.

## Startup and retry timing

Large modded servers may take much longer to become connectable than a small Paper server. If the controller starts the process but HubPilot times out before Minecraft begins listening, increase the startup or ping timeout values for that server.

The [FAQ](FAQ.md) covers the most common startup and connection problems.

## Version compatibility

`strict-version: false` allows normal proxy compatibility behavior, including [ViaVersion](https://github.com/ViaVersion/ViaVersion) when it is installed and supports the client/backend combination.

Set a strict version rule for servers that should require a specific Minecraft version. This is especially useful for modded servers where the correct loader or modpack also matters.

## Hub config

Hub config includes:

- shared directory
- Navigator title and inventory size
- filler material
- Navigator item name, lore, and custom model data
- admin item name and lore
- optional legacy/manual destination entries

Navigator inventory sizes can be 9, 18, 27, 36, 45, or 54 slots.

## Interact config

Packaged defaults include:

```yaml
portal-particles: true
particle-period-ticks: 20
portal-cooldown-ms: 3000
```

After changing owner-editable files, use `/hp reload` or the component-specific reload command after first-time setup is complete.
