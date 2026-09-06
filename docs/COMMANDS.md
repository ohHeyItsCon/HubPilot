# Commands

## Core / Hub

| Command | What it does |
| --- | --- |
| `/hp` | Shows help |
| `/hubpilot` | Alias for `/hp` |
| `/hp gui` | Opens the admin GUI |
| `/hp adminitem` | Gives or repairs the admin item on 1.0.1; toggles it in the latest 1.0.2 Hub build |
| `/hp claimowner` | Claims an installation with no Owner |
| `/hp setup` | Opens first-time/provider setup |
| `/hp staff` | Opens staff management |
| `/hp providers` | Shows provider information and management |
| `/hp request <server>` | Requests a managed server |
| `/hp cancel` | Cancels a pending request; 1.0.2 adds separate cancellation and no-pending-request messages |
| `/hp discover` | Lists backend candidates |
| `/hp discover add <server>` | Adds a discovered backend to HubPilot and the Navigator |
| `/hp reload` | Reloads owner-editable config after setup |
| `/hub` | Returns to the configured hub |
| `/lobby` | Also returns to the configured hub |

## Interact

The main command is `/hpi`, with `/hubpilotinteract` as its long alias.

```text
/hpi <bind|portal|npc|list|reload|cancel>
```

### Editing tools in the latest 1.0.2 prerelease

| Command | What it does |
| --- | --- |
| `/hpi tool [on|off|toggle]` | Shows, hides, or toggles the brush |
| `/hpi items` | Alias for the brush toggle |
| `/hpi portal create <name> <destination>` | Starts a named portal selection |
| `/hpi portal type <name> <nether|end|water|invisible>` | Sets the particle style |

Sneak and right-click the brush to choose a destination and editing mode. In Portal mode, left- and right-click blocks to select corners, then save from the menu. Label modes move a destination name by 0.25 blocks.

`/hp adminitem` handles Hub's item independently of Interact. Hidden admin items stay hidden after reconnects and respawns. A free inventory slot is needed to bring a missing item back.

See the [1.0.2 notes](../release/1.0.2/RELEASE-NOTES.md) for the full editor instructions.

## Link

Use `/hpl` or `/hubpilotlink` for plugin information and connection/status checks on the hub.

See [Permissions](PERMISSIONS.md) for access rules and the [FAQ](FAQ.md) for troubleshooting.
