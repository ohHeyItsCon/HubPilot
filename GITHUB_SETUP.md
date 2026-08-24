# Repository Notes

A few maintenance notes for the public HubPilot repo.

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

Useful repo features:

- Issues
- Releases
- Private vulnerability reporting, if available
- Discussions if the community gets large enough to need them

Default branch: `main`

## Release files

Keep normal repo history focused on docs, release metadata, and configuration references. Compiled JARs belong on the GitHub Release page instead of `main`.

For future releases:

1. build the exact final artifacts;
2. validate those exact files;
3. record SHA-256 hashes;
4. update the changelog and release notes;
5. tag the release;
6. attach the final JARs and validation report to the GitHub Release.
