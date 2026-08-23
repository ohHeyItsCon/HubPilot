# Repository Notes

Maintenance notes for the public HubPilot repository.

## Repository description

```text
Minecraft server navigation, on-demand server control, telemetry, and hub tools for Velocity + Paper/Bukkit networks.
```

## Suggested topics

```text
minecraft
minecraft-plugin
velocity
paper
bukkit
server-management
minecraft-server
crafty-controller
pterodactyl
viaversion
java
```

## GitHub features

Recommended:

- Issues
- Releases
- Private vulnerability reporting, if available
- Discussions if community use grows enough to need them

Default branch: `main`

## Release files

Keep normal repository history focused on source, docs, and configuration. Release JARs belong on the GitHub Release page instead of `main`.

For 1.0.0:

1. finish the remaining live tests;
2. build all four artifacts with internal version `1.0.0`;
3. validate those exact JARs;
4. record SHA-256 hashes;
5. add the project license;
6. tag `v1.0.0`;
7. attach the four JARs to the release.
