# Permissions and Roles

HubPilot has its own role system, so you do not need a Velocity permission plugin just to manage HubPilot staff.

## LuckPerms

HubPilot works with [LuckPerms](https://github.com/LuckPerms/LuckPerms) on the Paper/Bukkit hub.

Hub uses normal Bukkit/Paper permission checks, so LuckPerms can grant HubPilot nodes to players and groups just like it does for other plugins.

LuckPerms is optional. HubPilot's built-in Owner, Admin, Moderator, and Helper roles still work without it.

HubPilot Owner is separate from a permission group. Owner is stored by HubPilot and has to be claimed or changed through HubPilot itself.

## Owner

Owner is the highest HubPilot role.

- stored explicitly by UUID
- receives `hubpilot.*`
- cannot be created by the OP fallback

Use `/hp claimowner` on an unowned install to set the first Owner.

## Admin

Default Admin permissions include:

- `hubpilot.admin`
- `hubpilot.servers.*`
- `hubpilot.navigator.*`
- `hubpilot.interact.*`
- `hubpilot.staff.view`
- `hubpilot.staff.manage`
- `hubpilot.reload`

## Moderator

Moderator inherits Helper and also includes:

- `hubpilot.server.request`
- `hubpilot.maintenance.toggle`
- `hubpilot.debug.request`

## Helper

Helper includes:

- `hubpilot.status.view`

## OP fallback

Default:

```yaml
op-default-role: admin
```

Available values:

- `none`
- `helper`
- `moderator`
- `admin`

OP is only a fallback staff role. It does not grant HubPilot Owner.

## Hub-side permission nodes

Hub declares:

- `hubpilot.admin`
- `hubpilot.claimowner`
- `hubpilot.role.admin`
- `hubpilot.role.moderator`
- `hubpilot.role.helper`
- `hubpilot.navigator`

Interact declares:

- `hubpilot.interact.admin`
- `hubpilot.interact.edit`

These nodes can be granted through LuckPerms on the hub.
