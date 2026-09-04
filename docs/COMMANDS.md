# Commands

## Core / Hub

| Command | What it does |
| --- | --- |
| `/hp` | Shows HubPilot help |
| `/hubpilot` | Alias for `/hp` |
| `/hp gui` | Opens the HubPilot admin GUI |
| `/hp adminitem` | Gives or repairs the HubPilot admin item |
| `/hp claimowner` | Claims an installation with no Owner |
| `/hp setup` | Opens first-time/provider setup |
| `/hp staff` | Opens staff management |
| `/hp providers` | Shows provider information and management |
| `/hp request <server>` | Requests a managed server |
| `/hp discover` | Lists backend candidates HubPilot can add |
| `/hp discover add <server>` | Adds a discovered backend to HubPilot and the Navigator |
| `/hp reload` | Reloads owner-editable config after setup |
| `/hub` | Sends the player back to the configured hub |
| `/lobby` | Alternate command for returning to the hub |

## Interact

`/hpi` is the main Interact command. `/hubpilotinteract` is the long alias.

```text
/hpi <bind|portal|npc|list|reload|cancel>
```

## Link

`/hpl` is the main Link command. `/hubpilotlink` is the long alias.

Link runs on the hub, so its command is mostly for plugin information and connection/status checks.

If a command is not behaving as expected, check the [FAQ](FAQ.md).

## Interact editing items and portal styles

- `/hpi items [all|admin|interact] [on|off|toggle]` toggles editing items.
- `/hpi tool` toggles the Interact brush.
- `/hpi portal create <name> <destination>` starts a brush selection.
- `/hpi portal type <name> <nether|end|water|invisible>` sets the particle style.
- Sneak + right-click the brush to choose a destination and editing mode.
- Portal mode uses left/right-click for the two corners; save from the brush menu.
- Label modes move floating destination names up or down by 0.25 blocks.

The hidden admin item stays hidden across automatic reconciliation and player reconnects. Showing items requires inventory space. Full instructions are in the 1.0.2 release notes.
