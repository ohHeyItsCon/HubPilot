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
luckperms
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

Normal repository history stays focused on docs, release metadata, and configuration references. Compiled JARs belong on GitHub Releases rather than being committed to `main`.

For future releases:

1. build the exact final artifacts;
2. validate those artifacts;
3. record SHA-256 hashes;
4. update the changelog/release notes;
5. tag the release;
6. attach the final JARs and validation report to the GitHub Release.
