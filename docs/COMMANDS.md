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
