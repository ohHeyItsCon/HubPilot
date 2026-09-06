# Known Bugs

## Hub discovery and automatic shutdown in 1.0.1

**Fix available in the September 3 Core build of the 1.0.2 prerelease. Live testing is still pending.**

In 1.0.1, `/hp discover` could include the configured hub. Once imported as a managed backend, it could inherit automatic shutdown settings meant for game servers.

The original September 1 Core download excluded the hub from discovery but didn't repair hubs already imported. The later September 3 build covers those too.

### Installing the fix

Replace Core and restart Velocity. If `hub-server` already identifies the right hub, no manual Always-On toggle or configuration deletion is needed.

An existing Hub 1.0.2 can stay installed for this repair. If you're coming from 1.0.1, update Hub to 1.0.2 as well. Use the supplied SHA-256 list to identify the repaired Core; both downloads are named 1.0.2.

Core disables idle, failed-request, and queue-empty shutdown for the configured hub. This overrides YAML defaults and saved menu settings at startup and after a successful reload, including requests still holding older settings. Discovery also excludes common equivalent hub names.

The repair keeps the managed entry, saved settings and comments, destinations, provider mappings, and startup preferences. Other servers aren't removed or changed. If you choose a different hub later, the former hub uses its saved backend settings again. **Manual Stop Server still works.**

### Staying on stable 1.0.1

- Don't import the hub as a normal backend through `/hp discover`.
- If it's already managed, enable **Always-On server** or disable its automatic shutdown settings.
- Check that it isn't inheriting a global idle timeout.

Guided hub selection during setup and a separate multi-hub setup path are still planned. See the [roadmap](docs/ROADMAP.md).
