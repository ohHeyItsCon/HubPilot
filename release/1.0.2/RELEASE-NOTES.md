# HubPilot 1.0.2 — Queue Update

> Development pre-release draft. Features and testing results must be verified against the final packaged build before publication.

HubPilot 1.0.2 focuses on the messages players see while HubPilot starts a server, manages its queue, and completes the connection.

## Planned changes

### Configurable join-flow messages

Server owners will be able to control the messages shown during the complete join flow.

- Show or hide individual message events
- Edit message text, colors, and formatting
- Preview messages from the Admin menu
- Restore individual messages to their defaults
- Use global defaults with optional per-server overrides
- Reload message changes without restarting the network

Planned configurable events include:

- Already connected
- Connecting
- Server unavailable
- Automatic startup disabled
- Starting server
- Already starting
- Added to queue
- Already queued
- Queue position changed
- Server ready
- Countdown
- Joining
- Connection failed
- Retrying
- Request canceled
- No pending request
- Start failed
- Maintenance
- Wrong version
- Missing permission

### Shared server-name formatting

The `{server}` placeholder will use the same display name, color, and formatting shown by the server in the Navigator instead of falling back to an unformatted internal server name.

Message templates will support placeholders appropriate to their event, including:

- `{server}`
- `{position}`
- `{queue_size}`
- `{seconds}`
- `{attempt}`
- `{max_attempts}`
- `{delay}`
- `{required_version}`
- `{current_version}`
- `{error}`

### Queue position and cancellation feedback

- Players will be told their position when joining a startup queue.
- Position updates will only be sent when a player's position changes.
- `/hp cancel` will use configurable success and no-pending-request messages.
- Queue messages will use the same visibility and formatting controls as the rest of the join flow.

### Safe custom templates

Owners will be able to create custom templates for supported HubPilot events. Arbitrary event scripting is outside the scope of 1.0.2; every template must be attached to a known event so HubPilot knows when and where to display it.

## Configuration plan

- `messages/en_US.yml` remains the source for global message defaults.
- Each event stores an enabled state and message template.
- Per-server overrides inherit from the global event unless explicitly changed.
- Existing 1.0.1 flat message entries remain compatible and are migrated without losing custom text.
- Core owns lifecycle, startup, queue, retry, and transfer messages.
- Hub owns immediate Navigator feedback and the in-game message editor.

## Compatibility

- Core and Hub must be updated together for 1.0.2.
- Existing 1.0.1 server, provider, destination, layout, and message files will remain supported.
- Existing Always-On behavior, Crafty discovery, live Navigator telemetry, and Admin layout behavior must remain unchanged.
- Link and Interact compatibility will be confirmed before the pre-release is published.

## Testing required before publication

- Upgrade from an existing customized 1.0.1 `messages/en_US.yml`
- Fresh-install default generation
- Global visibility toggles
- Per-server inheritance and overrides
- Navigator-formatted `{server}` output
- Queue positions with several players
- Position updates after a player leaves or cancels
- Cancellation with and without a pending request
- Hidden messages producing no blank chat lines
- Placeholder replacement and unknown-placeholder handling
- Countdown, retry, failure, and successful transfer flows
- Velocity and Paper restart persistence
- Regression checks for Always-On, discovery, live telemetry, shared layouts, and Admin editing

## Testing status

- Implementation: Not complete
- Packaged validation: Not started
- Live Velocity and Paper testing: Not started
- Release status: Development draft
