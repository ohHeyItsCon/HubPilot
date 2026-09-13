import java.lang.reflect.Field;
import org.bukkit.Material;

public final class ChainCompatibility {
    public static void main(String[] args) throws Exception {
        boolean missing = false;
        try {
            Class<?> menu = Class.forName("dev.hubpilot.hub.adminui.TelemetryMenuBuilder");
            Field icons = menu.getDeclaredField("ICONS");
            icons.setAccessible(true);
            Material icon = ((Material[]) icons.get(null))[17];
            String expected = args[1];
            if (!icon.name().equals(expected)) throw new AssertionError(icon + " != " + expected);
            System.out.println("PASS telemetry Link icon: " + icon);
        } catch (NoSuchFieldError error) {
            if (!error.getMessage().contains("CHAIN")) throw error;
            missing = true;
            System.out.println("REPRODUCED baseline CHAIN linkage failure: " + error);
        }
        if (missing != Boolean.parseBoolean(args[0])) throw new AssertionError("Unexpected linkage outcome");
    }
}
