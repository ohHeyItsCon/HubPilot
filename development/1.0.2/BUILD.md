# HubPilot 1.0.2 Interact sources

These are the changed classes for the Interact expansion and Hub admin-item visibility.
They supplement the existing 1.0.2 JARs; this is not a complete source distribution of
the other HubPilot components. Original classes were reconstructed from the existing
release implementation where the repository did not contain sources.

Compile `src` and `hub-src` together with Java 21, annotation processing disabled,
against HubPilot-Hub-1.0.2.jar, HubPilot-Interact-1.0.2.jar, Paper API 1.21.10,
Adventure API/key, examination API/string, SLF4J API and BungeeCord chat.
Overlay only `dev/hubpilot/interact` into Interact and
`dev/hubpilot/hub/publicapi/SetupItemManager*.class` into Hub. Include the Interact
resource files. Never package the Paper API, test framework or test classes.

InteractValidation.java executes the packaged JARs using MockBukkit 4.95.0 and Paper
API 1.21.10. It verifies persistence, inventory safety, editing and label lifecycle.
The fixture uses test-only display styling methods because MockBukkit does not
implement them. Live client appearance and complete server startup are not covered.

All filenames and plugin versions remain the standard 1.0.2 names. Core is unchanged
from the hub-protection build with SHA-256
503deebfd9f925017a0fcf675cc9a839f5346480d359e6a9941e79e50411cb9f.
