# Contributing

Bug reports and real-world testing are especially useful while HubPilot is still young.

## Reporting a bug

Check the [FAQ](docs/FAQ.md) first. It covers the common stuff like server names, ports, provider mappings, startup timeouts, and bad Velocity addresses.

If the problem is still there, include:

- HubPilot version
- which HubPilot components are installed and where
- Velocity and Paper/Bukkit versions
- Minecraft version
- provider type if the issue involves power control or discovery
- exact steps that reproduce the problem
- the useful part of the log
- whether the problem still happens with unrelated plugins removed

Remove API keys, tokens, passwords, private URLs, and `secrets.yml` before posting anything.

Pterodactyl and Generic HTTP reports are especially helpful because Crafty is still the only controller path with live beta coverage.

## Code changes

Open an issue before starting a large code change. It is easier to agree on the behavior first than to review a big rewrite after the fact.

## AI-assisted contributions

AI-assisted contributions are allowed. The contributor is still responsible for understanding the change, testing it, checking nearby behavior, and making sure nothing private or legally questionable is being submitted.
