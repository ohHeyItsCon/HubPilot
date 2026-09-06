# Configuration

HubPilot creates its config files on first start. Check files for private details before sharing them. **Keep `secrets.yml` private.**

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

The examples below show packaged defaults. A server can override inherited values.

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

HubPilot can route players to an Always-On provider server while it is online, but it will not start or stop the process. A direct power action is rejected instead of pretending it worked.

Built-in provider ID: `always-online`  
Provider type: `always-on`

If an offline server should start when somebody requests it, give that server a power provider instead.

## Always-On server option

Added in 1.0.1, **Always-On server** is a lifecycle setting and is separate from the Always-On provider.

A server can use Crafty, Pterodactyl, Generic HTTP, or another managed provider and still have Always-On server enabled. When it is on, HubPilot keeps provider startup available but disables automatic shutdown for:

- idle time
- failed player requests
- an empty startup/request queue

Manual Stop Server still works.

The setting is available in Hub Automation Settings and can be set globally or overridden per server. It is stored in the synchronized `hubpilot.properties` file as:

```properties
global.always-on-server=false
server.<server-id>.always-on-server=true
```

The default is `false`, so upgrading from 1.0.0 does not change existing lifecycle behavior until the option is enabled.

## Configured hub in the 1.0.2 prerelease

The September 3 Core repair disables idle, failed-request, and queue-empty shutdown for the server identified by `hub-server`. It overrides defaults and saved menu settings at startup and after a successful reload. The menu may still show the saved values, but Core won't use them to automatically stop the configured hub. Manual Stop Server remains available.

See [Known Bugs](../KNOWN_BUGS.md) for the repaired build and update steps. Live testing is still pending.

## Join and queue messages

HubPilot 1.0.2 adds an in-game editor for messages used during server requests, startup queues, countdowns, retries, and transfers.

Open **Automation Settings**, then select **Join & Queue Messages**. Global settings are inherited by every server unless that server has its own override.

Controls:

- left-click: show or hide the event
- right-click: edit the template in chat
- middle-click: preview the template
- shift-right-click: reset the global default or clear a server override

Templates use `&` color codes. `{server}` uses the same bold white server-name format as the Navigator item. Use `{server_plain}` when formatting should be supplied entirely by the template.

Other event-specific placeholders include `{position}`, `{queue_size}`, `{seconds}`, `{attempt}`, `{max}`, `{delay}`, `{required}`, `{current}`, and `{error}`.

Fresh installations use event-based entries in `messages/en_US.yml`:

```yaml
events:
  queue-position:
    enabled: true
    text: "&7You are now queue position &f{position}&7 of &f{queue_size}&7 for {server}&7."
```

Existing flat entries remain valid:

```yaml
starting: "Starting <server>... You are queued."
```

Hub saves in-game edits in `hubpilot.properties` and sends them to Core through `hubpilot:settings`. That channel accepts the defined message settings, not provider secrets or unrelated fields.

## Startup and retry timing

Large modded servers can take a lot longer to become connectable than a small Paper server. If the controller starts the process but HubPilot times out before Minecraft begins listening, increase the startup or ping timeout values for that server.

The [FAQ](FAQ.md) covers the common startup and connection problems.

## Version compatibility

`strict-version: false` leaves normal proxy compatibility in place, including [ViaVersion](https://github.com/ViaVersion/ViaVersion) when it supports that client/backend combination.

Use a strict version rule when a server requires an exact Minecraft version. Modded backends may also need a particular loader or modpack.

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

In the latest 1.0.2 Interact build, `destination-labels: false` disables floating destination names. `/hpi reload` reloads Interact's configuration and bindings.

After editing other owner-editable files, use `/hp reload` or the relevant component's reload command once setup is complete.
