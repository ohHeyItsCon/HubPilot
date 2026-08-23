# Permissions and Roles

HubPilot has its own role system. A Velocity permission plugin is not required for HubPilot ownership or staff roles.

## LuckPerms compatibility

HubPilot is compatible with [LuckPerms](https://github.com/LuckPerms/LuckPerms) on the Paper/Bukkit hub.

HubPilot Hub uses normal Bukkit/Paper permission checks, so LuckPerms can grant HubPilot permission nodes to players or groups the same way it does for other plugins.

LuckPerms is optional. HubPilot's built-in Owner/Admin/Moderator/Helper system still works without it.

HubPilot Owner is different from a permission group. Owner is stored by HubPilot and must be claimed or managed through HubPilot itself.

## Owner

Owner is the highest HubPilot role.

- stored explicitly by UUID
- receives `hubpilot.*`
- cannot be created by the OP fallback

Use `/hp claimowner` on an unowned installation to set the first Owner.

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
