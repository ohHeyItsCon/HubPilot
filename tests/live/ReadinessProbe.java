package dev.hubpilot.readiness;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.Inventory;
import java.lang.reflect.InvocationTargetException;

/** Disposable lab probe; never packaged into HubPilot or installed in production. */
public final class ReadinessProbe extends JavaPlugin {
    @Override public void onEnable() {
        getServer().getScheduler().runTaskLater(this, () -> {
            try {
                Plugin hub = getServer().getPluginManager().getPlugin("HubPilot");
                if (hub == null || !hub.isEnabled()) throw new AssertionError("Hub not enabled");
                ClassLoader loader = hub.getClass().getClassLoader();
                Class<?> menu = Class.forName("dev.hubpilot.hub.adminui.TelemetryMenuBuilder", true, loader);
                Inventory inventory = (Inventory) menu.getMethod("buildGlobal", hub.getClass()).invoke(null, hub);
                if (inventory.getSize() != 54 || inventory.getItem(31) == null) throw new AssertionError("Incomplete telemetry inventory");
                for (String name : new String[]{"HubPilotLink", "HubPilotInteract"}) {
                    Plugin component = getServer().getPluginManager().getPlugin(name);
                    if (component == null || !component.isEnabled()) throw new AssertionError(name + " not enabled");
                }
                getLogger().info("READINESS_PASS real Bukkit telemetry inventory and all three Paper components enabled");
            } catch (Throwable error) {
                Throwable cause = error instanceof InvocationTargetException ? error.getCause() : error;
                getLogger().severe("READINESS_FAIL " + cause.getClass().getName() + ": " + cause.getMessage());
            }
        }, 40L);
    }
}
