package dev.hubpilot.interact;

import dev.hubpilot.hub.HubPilotHubPlugin;
import dev.hubpilot.hub.config.*;
import dev.hubpilot.hub.publicapi.SetupItemManager;
import dev.hubpilot.hub.util.MenuItems;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.mockbukkit.mockbukkit.MockBukkit;
import sun.misc.Unsafe;

public class InteractValidation {
    // MockBukkit does not implement display styling; supply stateful API doubles only
    // for those setters. Entity position, persistence, removal and inventories remain mocked Paper.
    static class LabelDisplay extends org.mockbukkit.mockbukkit.entity.TextDisplayMock {
        String label = "";
        LabelDisplay() { super(MockBukkit.getMock(), UUID.randomUUID()); }
        public void setBillboard(Display.Billboard b) {}
        public void setAlignment(TextDisplay.TextAlignment a) {}
        public void setShadowed(boolean b) {}
        public void setLineWidth(int width) {}
        public String getText() { return label; }
        public void setText(String s) { label = s; }
    }
    static class LabelWorld extends org.mockbukkit.mockbukkit.world.WorldMock {
        @Override public <T extends Entity> T spawn(Location at, Class<T> type, java.util.function.Consumer<? super T> consumer,
            org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason reason, boolean randomize, boolean add) {
            if (type != TextDisplay.class) return super.spawn(at,type,consumer,reason,randomize,add);
            LabelDisplay display = new LabelDisplay(); display.setLocation(at);
            MockBukkit.getMock().registerEntity(display);
            T result = type.cast(display); if (consumer != null) consumer.accept(result); return result;
        }
    }
    static void check(boolean b, String message) { if (!b) throw new AssertionError(message); }
    static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field); f.setAccessible(true); f.set(target, value);
    }
    static <T extends JavaPlugin> T fixture(Class<T> type, String name) throws Exception {
        var mock = MockBukkit.createMockPlugin(name);
        Field f = Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true);
        T instance = type.cast(((Unsafe)f.get(null)).allocateInstance(type));
        for (Field field : JavaPlugin.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            field.setAccessible(true); field.set(instance, field.get(mock));
        }
        return instance;
    }
    public static void main(String[] args) throws Exception {
        var server = MockBukkit.mock();
        try {
            var world = new LabelWorld(); world.setName("world"); server.addWorld(world);
            var other = server.addSimpleWorld("other");
            var hub = fixture(HubPilotHubPlugin.class, "HubPilot");
            hub.getConfig().set("shared-directory", Path.of(args[0]).toAbsolutePath().toString());
            set(hub, "menuConfig", MenuConfig.load(hub));
            var destinations = new DestinationStore(hub, Path.of(args[0]).resolve("destinations.properties"));
            set(destinations, "destinations", List.of(new Destination("survival", "Survival", "", "paper", "GRASS_BLOCK", Destination.TargetType.SERVER, "survival", "survival", true)));
            set(hub, "destinationStore", destinations);
            var plugin = fixture(HubPilotInteractPlugin.class, "HubPilotInteract");
            set(plugin, "hub", hub);
            var store = new InteractionStore(plugin); set(plugin, "store", store);
            var editor = new InteractionEditor(plugin); set(plugin, "editor", editor);
            store.savePortal("test", new Location(world, 4, 64, 2), new Location(world, 0, 68, 0), "survival");
            check(store.portalStyle("test").equals("nether"), "legacy portal default");
            for (String style : InteractionEditor.STYLES) { store.portalStyle("test", style); store.load(); check(store.portalStyle("test").equals(style), "style persistence"); }
            try { store.portalStyle("missing", "end"); throw new AssertionError("missing accepted"); } catch (java.io.IOException expected) {}
            try { store.portalStyle("test", "lava"); throw new AssertionError("invalid accepted"); } catch (java.io.IOException expected) {}
            try { store.savePortal("bad", new Location(world,0,0,0), new Location(other,0,0,0), "survival"); throw new AssertionError("cross-world accepted"); } catch (java.io.IOException expected) {}
            check(store.portals().size() == 1, "cross-world write");
            var p = server.addPlayer(); p.setOp(true);
            p.getInventory().setItem(3, new ItemStack(Material.DIAMOND, 12));
            editor.command(p, new String[]{"items", "all", "on"});
            check(Arrays.stream(p.getInventory().getContents()).filter(Objects::nonNull).count() == 3, "tools not granted");
            editor.command(p, new String[]{"items", "all", "on"});
            check(Arrays.stream(p.getInventory().getContents()).filter(Objects::nonNull).count() == 3, "duplicate tools");
            editor.command(p, new String[]{"items", "all", "off"});
            check(p.getInventory().getItem(3).getAmount() == 12, "ordinary inventory changed");
            check(Arrays.stream(p.getInventory().getContents()).filter(Objects::nonNull).count() == 1, "tools not removed");
            Method ensure = SetupItemManager.class.getDeclaredMethod("ensureAdminReady", HubPilotHubPlugin.class, Player.class); ensure.setAccessible(true); ensure.invoke(null, hub, p);
            check(Arrays.stream(p.getInventory().getContents()).filter(Objects::nonNull).count() == 1, "hidden admin restored");
            editor.command(p, new String[]{"items", "admin", "on"});
            check(Arrays.stream(p.getInventory().getContents()).anyMatch(i -> MenuItems.isAdminOpener(hub,i)), "admin cannot be shown again");
            editor.command(p, new String[]{"items", "all", "off"});
            for (int i=0;i<36;i++) p.getInventory().setItem(i,new ItemStack(Material.STONE,64));
            editor.command(p,new String[]{"items","all","on"});
            for (int i=0;i<36;i++) check(p.getInventory().getItem(i).getType()==Material.STONE && p.getInventory().getItem(i).getAmount()==64, "full inventory overwritten");
            var guest = server.addPlayer(); editor.command(guest,new String[]{"items"});
            check(Arrays.stream(guest.getInventory().getContents()).noneMatch(Objects::nonNull), "unauthorized grant");
            System.out.println("PASS portal persistence/legacy defaults, cross-world rejection, item toggles, permission, deduplication, full inventory, hidden admin reconciliation");

            p.getInventory().clear();
            editor.command(p,new String[]{"portal","create","brush_portal","survival"});
            p.getInventory().setHeldItemSlot(0);
            check(editor.holding(p),"brush not tagged");
            var first = world.getBlockAt(20,64,20); var second = world.getBlockAt(24,68,20);
            editor.block(new org.bukkit.event.player.PlayerInteractEvent(p,org.bukkit.event.block.Action.LEFT_CLICK_BLOCK,p.getInventory().getItemInMainHand(),first,org.bukkit.block.BlockFace.UP,EquipmentSlot.HAND));
            editor.block(new org.bukkit.event.player.PlayerInteractEvent(p,org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK,p.getInventory().getItemInMainHand(),second,org.bukkit.block.BlockFace.UP,EquipmentSlot.HAND));
            Method open = InteractionEditor.class.getDeclaredMethod("open",Player.class,boolean.class,int.class); open.setAccessible(true); open.invoke(editor,p,false,0);
            var click = new org.bukkit.event.inventory.InventoryClickEvent(p.getOpenInventory(),org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,30,org.bukkit.event.inventory.ClickType.LEFT,org.bukkit.event.inventory.InventoryAction.PICKUP_ALL);
            editor.menu(click);
            check(click.isCancelled(),"editor items could be taken");
            check(store.portals().stream().anyMatch(portal -> portal.name().equals("brush_portal") && portal.minX()==20 && portal.maxX()==24 && portal.destination().equals("survival")),"brush selection/save failed");
            store.deletePortal("brush_portal");
            System.out.println("PASS brush corner selection and editor inventory Save portal workflow");

            var sign = world.getBlockAt(8,64,8); sign.setType(Material.OAK_SIGN);
            world.loadChunk(0,0);
            var entity = world.spawn(new Location(world,10,64,10), ArmorStand.class);
            store.bindSign(InteractionStore.signKey(sign),"survival"); store.bindEntity(entity.getUniqueId(),"survival");
            plugin.getConfig().set("portal-particles",false);
            var labels = new DestinationLabels(plugin); labels.refresh();
            var displays = world.getEntitiesByClass(TextDisplay.class);
            check(displays.size()==3,"label count " + displays.size());
            check(displays.stream().anyMatch(d -> Math.abs(d.getLocation().getX()-2.5)<0.01 && Math.abs(d.getLocation().getY()-65.5)<0.01 && Math.abs(d.getLocation().getZ()-1.5)<0.01),"portal label not centered");
            labels.refresh(); check(world.getEntitiesByClass(TextDisplay.class).size()==3,"duplicate labels");
            store.labelOffset("portal.test",0.5); labels.refresh();
            check(world.getEntitiesByClass(TextDisplay.class).stream().anyMatch(d -> Math.abs(d.getLocation().getX()-2.5)<0.01 && Math.abs(d.getLocation().getY()-66)<0.01),"label height");
            entity.teleport(new Location(world,11,64,11)); labels.refresh();
            check(world.getEntitiesByClass(TextDisplay.class).stream().anyMatch(d -> Math.abs(d.getLocation().getX()-11)<0.01),"entity label follow");
            store.unbind("sign."+InteractionStore.signKey(sign)); store.deletePortal("test"); labels.refresh();
            check(world.getEntitiesByClass(TextDisplay.class).size()==1,"orphan labels");
            labels.close(); check(world.getEntitiesByClass(TextDisplay.class).isEmpty(),"disable cleanup");
            System.out.println("PASS TextDisplay labels: all binding types, centered portal placement, movement, offsets, deduplication, removal and cleanup");
        } finally { MockBukkit.unmock(); }
    }
}
