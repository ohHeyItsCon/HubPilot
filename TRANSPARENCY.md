# Project Transparency

HubPilot was built on top of [AutoServer](https://github.com/artificial-720/AutoServer) and then expanded far beyond the original plugin into a larger suite for server control, navigation, management, telemetry, and hub tools.

The HubPilot idea, feature direction, requirements, testing decisions, and release approval come from `ohHeyItsCon`, the project's creator and maintainer.

AI tools have been used heavily for coding, debugging, code inspection, test harnesses, regression checks, and documentation. That is stated here plainly so there is no confusion about how the project was made.

## Testing

Live beta testing was done on a Velocity + Paper network using **Crafty Controller** for server management. Crafty is still the only external controller integration with that level of real-network testing.

Pterodactyl and Generic HTTP are included and have controlled implementation testing, but they have not had the same live beta coverage yet.

Always-On is not a controller. It tells HubPilot to leave power management to another system.

The 1.0.0 release artifacts were checked for archive integrity, internal versioning, main classes, bytecode validity, private data, and release hashes. The exact record is kept in [release/1.0.0/VALIDATION.txt](release/1.0.0/VALIDATION.txt).

Testing notes will be updated as more setups are used in the real world.
