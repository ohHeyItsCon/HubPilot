# Project Transparency

HubPilot was created based on the [AutoServer](https://github.com/artificial-720/AutoServer) plugin and expanded into a larger suite for server control, navigation, management, telemetry, and hub tools.

The HubPilot concept, feature direction, requirements, testing decisions, and release approval belong to `ohHeyItsCon`, the project's creator and maintainer.

AI tools have been used heavily during development. Their use includes coding, debugging, code inspection, test harnesses, regression checks, and documentation. The development process is documented here so there is no confusion about how the project has been built.

## Beta testing

Live beta testing has been done on a Velocity + Paper network using **Crafty Controller** for server management. Crafty is currently the only external controller integration with that level of real-network testing.

Pterodactyl and Generic HTTP provider support are also included. Both have controlled implementation testing, but not the same live beta coverage yet.

Always-On is not a controller integration. It simply tells HubPilot to leave server power management to another system.

Testing and support notes will be updated as more setups are tested.
