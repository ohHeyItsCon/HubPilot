package dev.hubpilot.interact;




import java.io.IOException;
import java.util.*;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

final class InteractionEditor implements Listener {
    enum Mode { INSPECT, BIND, PORTAL, UNBIND, LABEL_UP, LABEL_DOWN }
    static final List<String> STYLES = List.of("nether", "end", "water", "invisible");
    static final class Session {
        Mode mode = Mode.INSPECT;
        String destination, selected, portalName;
        String style = "nether";
        Location first, second;
    }
    static final class Menu implements InventoryHolder {
        Inventory inventory;
        final boolean destinations;
        final int page;
        final Map<Integer, String> choices = new HashMap<>();
        Menu(boolean destinations, int page) { this.destinations = destinations; this.page = page; }
        public Inventory getInventory() { return inventory; }
    }
    private final HubPilotInteractPlugin plugin;
    private final NamespacedKey toolKey, shownKey;
    private final Map<UUID, Session> sessions = new HashMap<>();
    InteractionEditor(HubPilotInteractPlugin plugin) {
        this.plugin = plugin;
        toolKey = new NamespacedKey(plugin, "interaction_tool");
        shownKey = new NamespacedKey(plugin, "tool_shown");

    }
    private Session session(Player p) { return sessions.computeIfAbsent(p.getUniqueId(), id -> new Session()); }
    private boolean tool(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(toolKey, PersistentDataType.BYTE);
    }
    boolean holding(Player p) { return tool(p.getInventory().getItemInMainHand()); }
    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta(); meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(Arrays.stream(lore).map(s -> ChatColor.translateAlternateColorCodes('&', s)).toList());
        item.setItemMeta(meta); return item;
    }
    private ItemStack createTool() {
        ItemStack item = item(Material.BRUSH, "&b&lHubPilot Interact", "&7Sneak + right-click: editor menu", "&7Click targets to use the selected mode", "&7Portal: left/right-click its two corners", "&7/hpi tool to put the brush away");
        ItemMeta meta = item.getItemMeta(); meta.getPersistentDataContainer().set(toolKey, PersistentDataType.BYTE, (byte)1); item.setItemMeta(meta); return item;
    }
    private boolean has(Player p) {
        for (ItemStack item : p.getInventory().getContents()) if (tool(item)) return true;
        return tool(p.getItemOnCursor());
    }
    private void remove(Player p) {
        PlayerInventory inventory = p.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) if (tool(inventory.getItem(i))) inventory.setItem(i, null);
        if (tool(p.getItemOnCursor())) p.setItemOnCursor(null);
    }
    private boolean show(Player p) {
        if (has(p)) return true;
        if (p.getInventory().firstEmpty() < 0) { p.sendMessage("§eMake one inventory slot available first."); return false; }
        return p.getInventory().addItem(createTool()).isEmpty();
    }
    private void toggle(Player p, String action) {
        boolean visible = action.equals("on") || (action.equals("toggle") && !has(p));
        if (!visible) {
            p.getPersistentDataContainer().remove(shownKey); remove(p);
            p.sendMessage("§aInteract brush hidden. Use /hpi tool to bring it back.");
        } else if (show(p)) {
            p.getPersistentDataContainer().set(shownKey, PersistentDataType.BYTE, (byte)1);
            p.sendMessage("§aInteract brush shown. Sneak + right-click to edit.");
        }
    }
    boolean command(CommandSender sender, String[] args) {
        if (args == null || args.length == 0) return false;
        boolean ours = Set.of("items", "tool", "reload").contains(args[0].toLowerCase(Locale.ROOT))
            || args[0].equalsIgnoreCase("portal") && args.length > 1 && Set.of("type", "create").contains(args[1].toLowerCase(Locale.ROOT));
        if (!ours) return false;
        if (!PublicInteractGate.canManage(sender)) { sender.sendMessage("§cYou do not have permission."); return true; }
        try {
            if (args[0].equalsIgnoreCase("reload")) {
                plugin.reloadConfig(); plugin.store().load(); sessions.clear();
                sender.sendMessage("§aHubPilot Interact configuration and bindings reloaded."); return true;
            }
            if (args[0].equalsIgnoreCase("portal") && args[1].equalsIgnoreCase("type")) {
                if (args.length != 4) { sender.sendMessage("§e/hpi portal type <name> <nether|end|water|invisible>"); return true; }
                plugin.store().portalStyle(args[2], args[3].toLowerCase(Locale.ROOT));
                sender.sendMessage("§aPortal style saved."); return true;
            }
            if (!(sender instanceof Player p)) { sender.sendMessage("§eUse this command in-game."); return true; }
            if (args[0].equalsIgnoreCase("portal")) {
                if (args.length != 4 || !args[2].matches("[a-zA-Z0-9_-]{1,48}") || !plugin.validDestination(args[3])) {
                    p.sendMessage("§e/hpi portal create <name> <destination> (use a unique name)"); return true;
                }
                Session s = session(p); s.mode = Mode.PORTAL; s.portalName = args[2].toLowerCase(Locale.ROOT); s.destination = args[3]; s.first = null; s.second = null;
                show(p); p.sendMessage("§aLeft/right-click two blocks with the brush, then sneak + right-click and Save portal."); return true;
            }
            String action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "toggle";
            if (args.length > 2 || !List.of("on", "off", "toggle").contains(action)) {
                p.sendMessage("§e/hpi tool [on|off|toggle]. Use /hp adminitem for Hub's admin item."); return true;
            }
            toggle(p, action);
        } catch (IOException e) { sender.sendMessage("§cCould not save: " + e.getMessage()); }
        return true;
    }
    List<String> complete(CommandSender sender, String[] args) {
        if (!PublicInteractGate.canManage(sender)) return List.of();
        List<String> choices;
        if (args.length == 1) choices = List.of("items", "tool", "bind", "portal", "npc", "list", "reload", "cancel");
        else if ((args[0].equalsIgnoreCase("items") || args[0].equalsIgnoreCase("tool")) && args.length == 2) choices = List.of("on", "off", "toggle");
        else if (args[0].equalsIgnoreCase("portal") && args.length == 2) choices = List.of("create", "type", "pos1", "pos2", "save", "delete");
        else if (args[0].equalsIgnoreCase("portal") && args.length == 4 && args[1].equalsIgnoreCase("type")) choices = STYLES;
        else return null;
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return choices.stream().filter(s -> s.startsWith(prefix)).toList();
    }
    private void open(Player p, boolean destinations, int page) {
        Menu menu = new Menu(destinations, page);
        menu.inventory = Bukkit.createInventory(menu, 54, destinations ? "Choose destination" : "HubPilot Interact Editor");
        if (destinations) {
            var all = new ArrayList<>(plugin.hub().getDestinationStore().all());
            for (int i = 0; i < 45 && page * 45 + i < all.size(); i++) {
                var d = all.get(page * 45 + i); menu.choices.put(i, d.id());
                menu.inventory.setItem(i, item(Material.ENDER_PEARL, "&f" + d.label(), "&7" + d.id(), "&eClick to select"));
            }
            if (page > 0) menu.inventory.setItem(45, item(Material.ARROW, "&ePrevious"));
            if ((page + 1) * 45 < all.size()) menu.inventory.setItem(53, item(Material.ARROW, "&eNext"));
        } else {
            Session s = session(p);
            for (Mode mode : Mode.values()) menu.inventory.setItem(10 + mode.ordinal(), item(Material.BRUSH, "&b" + mode.name().replace('_', ' '), s.mode == mode ? "&aSelected" : "&eClick to select"));
            menu.inventory.setItem(22, item(Material.ENDER_PEARL, "&eDestination: " + (s.destination == null ? "choose one" : s.destination)));
            menu.inventory.setItem(28, item(Material.ENDER_EYE, "&dPortal style: " + s.style, "&7Click to cycle", "&7Applies to selected or next portal"));
            menu.inventory.setItem(30, item(Material.LIME_DYE, "&aSave new portal", "&7Two corners and a destination required"));
            menu.inventory.setItem(32, item(Material.PAPER, "&fSelected: " + (s.selected == null ? "none" : s.selected), "&7Inspect a sign, entity, or portal first", "&7Label modes adjust height by 0.25 blocks"));
            menu.inventory.setItem(49, item(Material.BARRIER, "&ePut Interact brush away"));
        }
        p.openInventory(menu.inventory);
    }
    @EventHandler public void menu(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Menu menu)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player p) || !PublicInteractGate.canManage(p)) return;
        int slot = event.getRawSlot(); if (slot < 0 || slot >= 54) return;
        Session s = session(p);
        try {
            if (menu.destinations) {
                if (menu.choices.containsKey(slot)) { s.destination = menu.choices.get(slot); open(p, false, 0); }
                else if (slot == 45 && menu.page > 0) open(p, true, menu.page - 1);
                else if (slot == 53 && menu.inventory.getItem(53) != null) open(p, true, menu.page + 1);
            } else if (slot >= 10 && slot < 10 + Mode.values().length) { s.mode = Mode.values()[slot - 10]; p.closeInventory(); p.sendMessage("§aMode: " + s.mode); }
            else if (slot == 22) open(p, true, 0);
            else if (slot == 28) {
                s.style = STYLES.get((STYLES.indexOf(s.style) + 1) % STYLES.size());
                if (s.selected != null && s.selected.startsWith("portal.")) plugin.store().portalStyle(s.selected.substring(7), s.style);
                open(p, false, 0);
            } else if (slot == 30) {
                if (s.first == null || s.second == null || s.destination == null || !plugin.validDestination(s.destination)) { p.sendMessage("§eChoose a destination and select both portal corners first."); return; }
                String name = s.portalName == null ? "portal_" + UUID.randomUUID().toString().substring(0, 8) : s.portalName;
                if (plugin.store().portals().stream().anyMatch(portal -> portal.name().equals(name))) { p.sendMessage("§eThat portal name already exists. Choose a different name."); return; }
                plugin.store().savePortal(name, s.first, s.second, s.destination);
                plugin.store().portalStyle(name, s.style);
                s.selected = "portal." + name; s.first = null; s.second = null; s.portalName = null; s.mode = Mode.INSPECT;
                p.closeInventory(); p.sendMessage("§aPortal " + name + " saved.");
            } else if (slot == 49) { p.closeInventory(); toggle(p, "off"); }
        } catch (IOException e) { p.sendMessage("§cCould not save: " + e.getMessage()); }
    }
    @EventHandler public void drag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.LOWEST) public void block(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !holding(event.getPlayer())) return;
        event.setCancelled(true);
        Player p = event.getPlayer(); if (!PublicInteractGate.canManage(p)) return;
        if (event.getAction() == Action.PHYSICAL) return;
        if (p.isSneaking() || event.getClickedBlock() == null) { open(p, false, 0); return; }
        Session s = session(p);
        if (s.mode == Mode.PORTAL) {
            Location location = event.getClickedBlock().getLocation();
            if (event.getAction() == Action.LEFT_CLICK_BLOCK) s.first = location; else s.second = location;
            p.sendMessage("§aPortal corner " + (event.getAction() == Action.LEFT_CLICK_BLOCK ? "1" : "2") + " selected. Sneak + right-click to save."); return;
        }
        if (event.getClickedBlock().getState() instanceof Sign) target(p, "sign." + InteractionStore.signKey(event.getClickedBlock()));
        else {
            for (var portal : plugin.store().portals()) if (portal.contains(p.getLocation()) || portal.contains(event.getClickedBlock().getLocation())) { target(p, "portal." + portal.name()); return; }
            p.sendMessage("§eClick a sign or entity, or stand inside a portal to inspect it.");
        }
    }
    @EventHandler(priority = EventPriority.LOWEST) public void entity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !holding(event.getPlayer())) return;
        event.setCancelled(true);
        if (!PublicInteractGate.canManage(event.getPlayer())) return;
        if (event.getPlayer().isSneaking()) { open(event.getPlayer(), false, 0); return; }
        if (event.getRightClicked() instanceof Player || event.getRightClicked() instanceof Display) return;
        target(event.getPlayer(), "entity." + event.getRightClicked().getUniqueId());
    }
    @EventHandler(priority = EventPriority.LOWEST) public void damage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p && holding(p)) event.setCancelled(true);
    }
    private void target(Player p, String key) {
        Session s = session(p); s.selected = key;
        try {
            switch (s.mode) {
                case BIND -> {
                    if (s.destination == null || !plugin.validDestination(s.destination)) { p.sendMessage("§eChoose a destination in the editor first."); return; }
                    if (key.startsWith("entity.")) plugin.store().bindEntity(UUID.fromString(key.substring(7)), s.destination);
                    else if (key.startsWith("sign.")) plugin.store().bindSign(key.substring(5), s.destination);
                    else { p.sendMessage("§eUse portal creation to choose its destination."); return; }
                    p.sendMessage("§aLinked to " + s.destination + ".");
                }
                case UNBIND -> {
                    if (key.startsWith("portal.")) plugin.store().deletePortal(key.substring(7)); else plugin.store().unbind(key);
                    p.sendMessage("§aInteraction removed; world blocks and entities are unchanged.");
                }
                case LABEL_UP, LABEL_DOWN -> { plugin.store().labelOffset(key, plugin.store().labelOffset(key) + (s.mode == Mode.LABEL_UP ? 0.25 : -0.25)); p.sendMessage("§aLabel height adjusted."); }
                default -> { if (key.startsWith("portal.")) s.style = plugin.store().portalStyle(key.substring(7)); p.sendMessage("§bSelected " + key + ". Sneak + right-click for options."); }
            }
        } catch (IOException e) { p.sendMessage("§cCould not save: " + e.getMessage()); }
    }
    @EventHandler public void quit(PlayerQuitEvent event) { sessions.remove(event.getPlayer().getUniqueId()); }
    @EventHandler public void join(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline() && PublicInteractGate.canManage(p) && p.getPersistentDataContainer().has(shownKey, PersistentDataType.BYTE)) show(p);
        }, 20);
    }
}
