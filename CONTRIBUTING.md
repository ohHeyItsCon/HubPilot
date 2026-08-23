# Contributing

HubPilot is still early enough that solid bug reports and real-world testing are especially useful.

## Reporting a bug

Before opening an issue, check the [FAQ and troubleshooting guide](docs/FAQ.md). It covers the most common problems with server names, ports, provider mappings, startup timeouts, and Velocity addresses.

If the problem is still happening, include:

- HubPilot version
- installed HubPilot components and where they are installed
- Velocity and Paper/Bukkit versions
- Minecraft version
- provider type if the issue involves power control or discovery
- exact steps that reproduce the problem
- the relevant part of the log
- whether the problem still happens with unrelated plugins removed

Remove API keys, tokens, passwords, private URLs, and `secrets.yml` before posting.

Reports from **Pterodactyl and Generic HTTP** setups are especially useful because Crafty is currently the only controller path covered by live beta testing.

## Code changes

The public source/build workflow for 1.0.0 is not finalized yet. Open an issue before starting a large code contribution so the change can be discussed first.

## AI-assisted contributions

AI-assisted contributions are allowed. Contributors are still responsible for understanding what is being submitted, testing the changed path, checking nearby behavior, and making sure the contribution does not include secrets or code that cannot legally be submitted.
