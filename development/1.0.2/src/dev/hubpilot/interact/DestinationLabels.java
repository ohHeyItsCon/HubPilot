package dev.hubpilot.interact;

import java.util.*;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

final class DestinationLabels {
    private final HubPilotInteractPlugin plugin;
    private final Map<String, TextDisplay> displays = new HashMap<>();
    private final NamespacedKey marker;
    private BukkitTask task;
    private long elapsed;
    DestinationLabels(HubPilotInteractPlugin plugin) {
        this.plugin = plugin;
        marker = new NamespacedKey(plugin, "destination_label");
    }
    void start() {
        // Only loaded entities: never force-load a chunk to show a label.
        for (World world : Bukkit.getWorlds()) for (TextDisplay display : world.getEntitiesByClass(TextDisplay.class)) {
            if (display.getPersistentDataContainer().has(marker, PersistentDataType.STRING)) display.remove();
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, 1, 10);
    }
    void close() {
        if (task != null) task.cancel();
        displays.values().forEach(Entity::remove);
        displays.clear();
    }
    private boolean loaded(Location l) {
        return l.getWorld() != null && l.getWorld().isChunkLoaded(l.getBlockX() >> 4, l.getBlockZ() >> 4);
    }
    void refresh() {
        elapsed += 10;
        long period = Math.max(10, plugin.getConfig().getLong("particle-period-ticks", 20));
        boolean drawParticles = elapsed >= period;
        if (drawParticles) elapsed = 0;
        Set<String> wanted = new HashSet<>();
        if (plugin.getConfig().getBoolean("destination-labels", true)) {
            for (var binding : plugin.store().bindings("entity").entrySet()) {
                try {
                    Entity entity = Bukkit.getEntity(UUID.fromString(binding.getKey()));
                    if (entity == null || !entity.isValid() || entity instanceof TextDisplay) continue;
                    put(wanted, "entity." + binding.getKey(), binding.getValue(), entity.getLocation().add(0, entity.getHeight() + 0.35, 0));
                } catch (IllegalArgumentException ignored) { }
            }
            for (var binding : plugin.store().bindings("sign").entrySet()) {
                try {
                    String[] parts = binding.getKey().split(":");
                    if (parts.length != 4) continue;
                    World world = Bukkit.getWorld(parts[0]);
                    if (world == null) continue;
                    Location block = new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                    if (!loaded(block) || !(block.getBlock().getState() instanceof Sign)) continue;
                    put(wanted, "sign." + binding.getKey(), binding.getValue(), block.add(0.5, 1.4, 0.5));
                } catch (IllegalArgumentException ignored) { }
            }
        }
        for (InteractionStore.Portal portal : plugin.store().portals()) {
            World world = Bukkit.getWorld(portal.world());
            if (world == null) continue;
            Location center = new Location(world, (portal.minX() + (double)portal.maxX() + 1) / 2,
                portal.minY() + 1.5, (portal.minZ() + (double)portal.maxZ() + 1) / 2);
            if (!loaded(center)) continue;
            if (plugin.getConfig().getBoolean("destination-labels", true)) put(wanted, "portal." + portal.name(), portal.destination(), center.clone());
            if (!drawParticles || !plugin.getConfig().getBoolean("portal-particles", true)) continue;
            Particle particle = switch (plugin.store().portalStyle(portal.name())) {
                case "end" -> Particle.END_ROD;
                case "water" -> Particle.SPLASH;
                case "invisible" -> null;
                default -> Particle.PORTAL;
            };
            if (particle != null) {
                // Fixed cost per portal regardless of selection size.
                boolean alongX = portal.maxX() - portal.minX() >= portal.maxZ() - portal.minZ();
                for (int i = 0; i < 20; i++) {
                    double t = (i % 5) / 4.0;
                    int side = i / 5;
                    double across = side == 1 ? 1 : side == 3 ? 0 : t;
                    double height = side == 0 ? 0 : side == 2 ? 1 : t;
                    double x = alongX ? portal.minX() + (portal.maxX() + 1.0 - portal.minX()) * across : center.getX();
                    double z = alongX ? center.getZ() : portal.minZ() + (portal.maxZ() + 1.0 - portal.minZ()) * across;
                    Location edge = new Location(world, x, portal.minY() + (portal.maxY() + 1.0 - portal.minY()) * height, z);
                    if (loaded(edge)) world.spawnParticle(particle, edge, 1, 0, 0.1, 0, 0);
                }
            }
        }
        displays.entrySet().removeIf(entry -> {
            if (wanted.contains(entry.getKey()) && entry.getValue().isValid()) return false;
            entry.getValue().remove(); return true;
        });
    }
    private void put(Set<String> wanted, String key, String destination, Location at) {
        var target = plugin.hub().getDestinationStore().find(destination);
        if (target == null || !loaded(at)) return;
        at.add(0, plugin.store().labelOffset(key), 0);
        if (!Double.isFinite(at.getY())) return;
        wanted.add(key);
        TextDisplay display = displays.get(key);
        if (display == null || !display.isValid()) {
            display = at.getWorld().spawn(at, TextDisplay.class, d -> {
                d.setPersistent(false);
                d.setBillboard(Display.Billboard.CENTER);
                d.setAlignment(TextDisplay.TextAlignment.CENTER);
                d.setShadowed(true);
                d.setInvulnerable(true);
                d.setGravity(false);
                d.setLineWidth(240);
                d.getPersistentDataContainer().set(marker, PersistentDataType.STRING, key);
            });
            displays.put(key, display);
        } else if (!display.getWorld().equals(at.getWorld()) || display.getLocation().distanceSquared(at) > 0.0025) display.teleport(at);
        String text = ChatColor.translateAlternateColorCodes('&', "&f&l" + target.label());
        if (!text.equals(display.getText())) display.setText(text);
    }
}
