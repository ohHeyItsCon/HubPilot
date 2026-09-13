/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.block.Block
 */
package dev.hubpilot.interact;

import dev.hubpilot.interact.HubPilotInteractPlugin;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.block.Block;

final class InteractionStore {
    private final HubPilotInteractPlugin plugin;
    private final Path file;
    private final Properties props = new Properties();

    InteractionStore(HubPilotInteractPlugin hubPilotInteractPlugin) {
        this.plugin = hubPilotInteractPlugin;
        this.file = hubPilotInteractPlugin.getDataFolder().toPath().resolve("interactions.properties");
    }

    synchronized void load() {
        this.props.clear();
        if (!Files.exists(this.file, new LinkOption[0])) {
            return;
        }
        try (BufferedReader bufferedReader = Files.newBufferedReader(this.file, StandardCharsets.UTF_8);){
            this.props.load(bufferedReader);
        }
        catch (IOException iOException) {
            this.plugin.getLogger().warning("Could not load interactions: " + iOException.getMessage());
        }
    }

    synchronized void bindEntity(UUID uUID, String string) throws IOException {
        this.props.setProperty("entity." + String.valueOf(uUID), string);
        this.save();
    }

    synchronized String entity(UUID uUID) {
        return this.props.getProperty("entity." + String.valueOf(uUID));
    }

    synchronized void bindSign(String string, String string2) throws IOException {
        this.props.setProperty("sign." + string, string2);
        this.save();
    }

    synchronized String sign(String string) {
        return this.props.getProperty("sign." + string);
    }

    synchronized java.util.Map<String, String> bindings(String kind) {
        java.util.Map<String, String> result = new java.util.LinkedHashMap<>();
        String prefix = kind + ".";
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith(prefix)) result.put(key.substring(prefix.length()), props.getProperty(key));
        }
        return result;
    }

    synchronized void unbind(String key) throws IOException {
        if (!key.startsWith("entity.") && !key.startsWith("sign.")) throw new IOException("Invalid binding.");
        props.remove(key);
        props.remove("label." + key + ".height");
        save();
    }

    synchronized String portalStyle(String name) {
        return props.getProperty("portal." + norm(name) + ".style", "nether");
    }

    synchronized void portalStyle(String name, String style) throws IOException {
        if (!List.of("nether", "end", "water", "invisible").contains(style)) throw new IOException("Unknown portal style.");
        if (portals().stream().noneMatch(p -> p.name().equals(norm(name)))) throw new IOException("Portal not found.");
        props.setProperty("portal." + norm(name) + ".style", style);
        save();
    }

    synchronized double labelOffset(String key) {
        try { return Math.max(-4, Math.min(8, Double.parseDouble(props.getProperty("label." + key + ".height", "0")))); }
        catch (NumberFormatException e) { return 0; }
    }

    synchronized void labelOffset(String key, double value) throws IOException {
        props.setProperty("label." + key + ".height", Double.toString(Math.max(-4, Math.min(8, value))));
        save();
    }

    synchronized void savePortal(String string, Location location, Location location2, String string2) throws IOException {
        String string3 = InteractionStore.norm(string);
        if (location.getWorld() == null || location2.getWorld() == null || !location.getWorld().getName().equals(location2.getWorld().getName())) {
            throw new IOException("Portal points must be in the same world.");
        }
        String string4 = "portal." + string3 + ".";
        this.props.setProperty(string4 + "world", location.getWorld().getName());
        this.props.setProperty(string4 + "minX", String.valueOf(Math.min(location.getBlockX(), location2.getBlockX())));
        this.props.setProperty(string4 + "minY", String.valueOf(Math.min(location.getBlockY(), location2.getBlockY())));
        this.props.setProperty(string4 + "minZ", String.valueOf(Math.min(location.getBlockZ(), location2.getBlockZ())));
        this.props.setProperty(string4 + "maxX", String.valueOf(Math.max(location.getBlockX(), location2.getBlockX())));
        this.props.setProperty(string4 + "maxY", String.valueOf(Math.max(location.getBlockY(), location2.getBlockY())));
        this.props.setProperty(string4 + "maxZ", String.valueOf(Math.max(location.getBlockZ(), location2.getBlockZ())));
        this.props.setProperty(string4 + "destination", string2);
        this.save();
    }

    synchronized boolean deletePortal(String string) throws IOException {
        String string2 = "portal." + InteractionStore.norm(string) + ".";
        boolean bl = false;
        for (String string3 : new ArrayList<String>(this.props.stringPropertyNames())) {
            if (!string3.startsWith(string2)) continue;
            this.props.remove(string3);
            bl = true;
        }
        if (bl) {
            this.save();
        }
        return bl;
    }

    synchronized List<Portal> portals() {
        TreeSet<String> treeSet = new TreeSet<String>();
        for (String object : this.props.stringPropertyNames()) {
            if (!object.startsWith("portal.") || !object.endsWith(".world")) continue;
            treeSet.add(object.substring(7, object.length() - 6));
        }
        ArrayList arrayList = new ArrayList();
        for (String string : treeSet) {
            String string2 = "portal." + string + ".";
            try {
                arrayList.add(new Portal(string, this.props.getProperty(string2 + "world"), this.i(string2 + "minX"), this.i(string2 + "minY"), this.i(string2 + "minZ"), this.i(string2 + "maxX"), this.i(string2 + "maxY"), this.i(string2 + "maxZ"), this.props.getProperty(string2 + "destination", "")));
            }
            catch (RuntimeException runtimeException) {}
        }
        return arrayList;
    }

    synchronized Set<String> entityDestinations() {
        TreeSet<String> treeSet = new TreeSet<String>();
        for (String string : this.props.stringPropertyNames()) {
            if (!string.startsWith("entity.")) continue;
            treeSet.add(this.props.getProperty(string));
        }
        return treeSet;
    }

    synchronized Set<String> signDestinations() {
        TreeSet<String> treeSet = new TreeSet<String>();
        for (String string : this.props.stringPropertyNames()) {
            if (!string.startsWith("sign.")) continue;
            treeSet.add(this.props.getProperty(string));
        }
        return treeSet;
    }

    static String signKey(Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private int i(String string) {
        return Integer.parseInt(this.props.getProperty(string));
    }

    private void save() throws IOException {
        Files.createDirectories(this.file.getParent(), new FileAttribute[0]);
        Path path = this.file.resolveSibling(String.valueOf(this.file.getFileName()) + ".tmp");
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(path, StandardCharsets.UTF_8, new OpenOption[0]);){
            this.props.store(bufferedWriter, "HubPilot Interact bindings");
        }
        try {
            Files.move(path, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (AtomicMoveNotSupportedException atomicMoveNotSupportedException) {
            Files.move(path, this.file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String norm(String string) {
        return string.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
    }

    record Portal(String name, String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, String destination) {
        boolean contains(Location location) {
            return location != null && location.getWorld() != null && this.world.equals(location.getWorld().getName()) && location.getX() >= (double)this.minX && location.getX() <= (double)(this.maxX + 1) && location.getY() >= (double)this.minY && location.getY() <= (double)(this.maxY + 1) && location.getZ() >= (double)this.minZ && location.getZ() <= (double)(this.maxZ + 1);
        }
    }
}
