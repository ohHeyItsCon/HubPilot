import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.config.MenuConfig;
import dev.hubpilot.hub.publicapi.*;
import dev.hubpilot.hub.util.MenuItems;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import sun.misc.Unsafe;

public class HubToggleValidation {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    static void request(Player player) throws Exception {
        var bytes = new ByteArrayOutputStream(); var output = new DataOutputStream(bytes);
        output.writeUTF("OPEN"); output.writeUTF("GUI:adminitem");
        check(UnifiedGuiBridge.handle("hubpilot:control",player,bytes.toByteArray()),"Core-to-Hub request rejected");
    }
    public static void main(String[] args) throws Exception {
        try { Class.forName("dev.hubpilot.interact.HubPilotInteractPlugin"); throw new AssertionError("Interact must be absent from this test"); }
        catch(ClassNotFoundException expected) {}
        var server=MockBukkit.mock();
        try {
            var mock=MockBukkit.createMockPlugin("HubPilot");
            Field unsafe=Unsafe.class.getDeclaredField("theUnsafe"); unsafe.setAccessible(true);
            var hub=(HubPilotHubPlugin)((Unsafe)unsafe.get(null)).allocateInstance(HubPilotHubPlugin.class);
            for(Field f:JavaPlugin.class.getDeclaredFields()) { if(Modifier.isStatic(f.getModifiers())) continue; f.setAccessible(true); f.set(hub,f.get(mock)); }
            Field config=HubPilotHubPlugin.class.getDeclaredField("menuConfig"); config.setAccessible(true); config.set(hub,MenuConfig.load(hub));
            UnifiedGuiBridge.attach(hub);
            var player=server.addPlayer(); player.setOp(true);
            player.getInventory().setItem(5,new ItemStack(Material.DIAMOND,7));
            request(player);
            check(Arrays.stream(player.getInventory().getContents()).anyMatch(i->MenuItems.isAdminOpener(hub,i)),"admin not shown");
            request(player);
            check(Arrays.stream(player.getInventory().getContents()).noneMatch(i->MenuItems.isAdminOpener(hub,i)),"admin not hidden");
            Method ensure=SetupItemManager.class.getDeclaredMethod("ensureAdminReady",HubPilotHubPlugin.class,Player.class); ensure.setAccessible(true); ensure.invoke(null,hub,player);
            check(Arrays.stream(player.getInventory().getContents()).noneMatch(i->MenuItems.isAdminOpener(hub,i)),"hidden preference ignored");
            check(player.getInventory().getItem(5).getAmount()==7,"ordinary item changed");
            request(player); request(player);
            for(int i=0;i<36;i++) player.getInventory().setItem(i,new ItemStack(Material.STONE,64));
            request(player);
            for(int i=0;i<36;i++) check(player.getInventory().getItem(i).getType()==Material.STONE && player.getInventory().getItem(i).getAmount()==64,"full inventory changed");
            var guest=server.addPlayer(); request(guest);
            check(Arrays.stream(guest.getInventory().getContents()).noneMatch(Objects::nonNull),"guest received admin item");
            System.out.println("PASS Hub alone: Core message -> Hub command -> show/hide; persistent hiding, permissions and full inventory safety; Interact absent");
        } finally { MockBukkit.unmock(); }
    }
}
