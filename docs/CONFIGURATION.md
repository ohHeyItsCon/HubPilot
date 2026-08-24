# Configuration

HubPilot creates its config files on first start. Most of them are fine to share after checking them, but **`secrets.yml` must stay private**.

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

A legacy `providers/crafty.yml` file may still be created when loading older 3.x configs. New installs should use `providers.yml` and `secrets.yml`.

## Core defaults

Some packaged defaults:

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

`always-online` means HubPilot does not control that server's power.

Use it when a host, Docker, systemd, another panel, or another process manager keeps the server running. The hub is a common example because it normally stays online while larger backends are allowed to shut down.

HubPilot can route players to an Always-On server while it is online, but it will not start or stop the process. A direct power action is rejected instead of pretending it worked.

Built-in provider ID: `always-online`  
Provider type: `always-on`

If an offline server should start when somebody requests it, give that server a power provider instead.

## Startup and retry timing

Large modded servers can take a lot longer to become connectable than a small Paper server. If the controller starts the process but HubPilot times out before Minecraft begins listening, increase the startup or ping timeout values for that server.

The [FAQ](FAQ.md) covers the common startup and connection problems.

## Version compatibility

`strict-version: false` leaves normal proxy compatibility in place, including [ViaVersion](https://github.com/ViaVersion/ViaVersion) when it supports that client/backend combination.

Use a strict version rule for servers that really need an exact Minecraft version. Modded servers are the obvious example because the loader or modpack may matter too.

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

After changing owner-editable files, use `/hp reload` or the component-specific reload command once first-time setup is complete.
