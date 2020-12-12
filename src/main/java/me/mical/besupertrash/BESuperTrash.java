package me.mical.besupertrash;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

public final class BESuperTrash extends JavaPlugin implements CommandExecutor, Listener {

    private static List<Location> locs = new ArrayList<>();
    private static ItemStack stick;
    private static boolean debug;
    private static boolean emptyDel;
    private static boolean enableTick;
    private static int tick;
    private static BESuperTrash plugin;
    private static final HashMap<Player, Location> dataMap = new HashMap<>();

    public void init() {
        saveDefaultConfig();
        if (Objects.isNull(getConfig().getItemStack("Stick"))) {
            ItemStack item = new ItemStack(Material.WOODEN_SWORD, 1);
            item.getItemMeta().setDisplayName(ChatColor.translateAlternateColorCodes('&', "&7[&9BESuperTrash&7] &a选择棒"));
            getConfig().set("Stick", item);
            saveConfig();
        }
        stick = getConfig().getItemStack("Stick");
        debug = getConfig().getBoolean("Debug");
        emptyDel = getConfig().getBoolean("EmptyDel");
        enableTick = getConfig().getBoolean("EnableTick");
        tick = getConfig().getInt("Tick");
        if (debug) {
            send(null, "已开启调试模式");
        }
        if (!locs.isEmpty()) {
            locs = new ArrayList<>();
        }
        File folder = new File(getDataFolder(), "data");
        File[] files = folder.listFiles(pathname -> pathname.getName().endsWith(".yml"));
        if (files != null && files.length != 0) {
            for (File file : files) {
                YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
                boolean check = data.getKeys(false).contains("Location");
                if (check) {
                    boolean isLoc = data.isLocation("Location");
                    if (!isLoc) {
                        send(null, "&c无法加载数据文件 &f{0}", file.getName().replace(".yml", ""));
                    } else {
                        locs.add(data.getLocation("Location"));
                        if (debug) {
                            send(null, "成功加载数据文件 &f{0}", file.getName().replace(".yml", ""));
                        }
                    }
                } else {
                    send(null, "&c无法加载数据文件 &f{0} &7(&c数据文件不完整&7)", file.getName().replace(".yml", ""));
                }
            }
            send(null, "已成功加载 &c{0} &7个数据文件", files.length);
        } else {
            send(null, "数据文件夹中没有数据可供加载.");
        }
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;
        long start = System.currentTimeMillis();
        init();
        send(null, "已成功加载插件. (&f{0}Ms&7)", System.currentTimeMillis() - start);
    }

    public void initliaze(String name, Location location, CommandSender cs) throws IOException {
        File folder = new File(getDataFolder(), "data");
        File dataFile = new File(folder, name + ".yml");
        if (!dataFile.exists()) {
            if (dataFile.createNewFile()) {
                YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
                data.set("Location", location);
                data.save(dataFile);
                locs.add(location);
                send(null, "已为垃圾箱 (&c{0}&7, 位于&c{1}&7) 创建数据档案.", name, location);
                send(cs, "已成功创建垃圾箱 &c{0}&7, 位于&c{1}", name, location);
            } else {
                send(cs, "无法创建数据文件, 因此创建垃圾箱失败.");
            }
        } else {
            send(cs, "已存在!");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player pl;
        if (sender instanceof Player) {
            pl = (Player) sender;
        } else {
            pl = null;
        }
        if (command.getName().equalsIgnoreCase("besupertrash")) {
            if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                sender.sendMessage("/besupertrash create <name> 创建一个垃圾箱");
                sender.sendMessage("/besupertrash reload 重载配置文件及数据文件");
                sender.sendMessage("/besupertrash give 获取创建箱子要用的选择棒");
            }
            switch (args[0].toLowerCase()) {
                case "reload":
                    init();
                    send(sender, "已成功初始化插件配置及数据文件.");
                    break;
                case "give":
                    if (pl != null) {
                        if (args.length >= 2) {
                            try {
                                int amount = Integer.parseInt(args[1]);
                                giveStick(pl, amount);
                            } catch (NumberFormatException e) {
                                send(pl, "非法参数");
                            }
                        } else {
                            send(pl, "参数不足");
                        }
                    } else {
                        send(null, "你不是玩家");
                    }
                    break;
                case "create":
                    if (pl != null) {
                        if (!dataMap.containsKey(pl)) {
                            send(pl, "请先用选择棒选择箱子再创建.");
                        } else {
                            Block block = dataMap.get(pl).getBlock();
                            if (block.getType().toString().contains("SHULKER_BOX") || block.getType().toString().equalsIgnoreCase("CHEST")) {
                                try {
                                    initliaze(args[1], block.getLocation(), pl);
                                } catch (IOException e) {
                                    send(pl, "无法创建垃圾箱, &c{0}&7, &c{1}" + e.getMessage(), e.getLocalizedMessage());
                                    e.printStackTrace();
                                }
                            } else {
                                send(pl, "你选中的不是容器方块");
                            }
                        }
                    } else {
                        send(null, "你不是玩家");
                    }
            }
        }
        return false;
    }

    public static void giveStick(Player pl, int amount) {
        ItemStack sc = stick.clone();
        if (amount > 64 || amount < 0) {
            send(pl, "非法数值");
        } else {
            sc.setAmount(amount);
        }
        HashMap<Integer, ItemStack> out = pl.getInventory().addItem(sc);
        send(pl, "&a已成功获得 &c{0} &a个选择棒.", amount);
        if (!out.isEmpty()) {
            send(pl, "你的背包已满, 选择棒将被丢到地上.");
            for (Map.Entry<Integer, ItemStack> entry : out.entrySet()) {
                pl.getWorld().dropItem(pl.getLocation(), entry.getValue());
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Inventory inventory = event.getInventory();
        Location location = inventory.getLocation();
        if (Objects.nonNull(location)) {
            if (locs.contains(location)) {
                if (debug) {
                    send(null, "检测到玩家&c{0}&7打开了位于(&c{1}&7)的垃圾桶", event.getPlayer().getName(), location);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        Location location = inventory.getLocation();
        if (Objects.nonNull(location)) {
            if (locs.contains(location)) {
                if ((inventory.getContents().length == 0 && emptyDel) || (inventory.getContents().length != 0)) {
                    Block block = location.getBlock();
                    Material chestType = block.getType();
                    block.setType(Material.AIR);
                    if (enableTick) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                block.setType(chestType);
                            }
                        }.runTaskLater(this, tick * 20);
                    } else {
                        block.setType(chestType);
                    }
                    locs.remove(location);
                    if (debug) {
                        send(null, "检测到玩家&c{0}&7关闭了位于(&c{1}&7)的垃圾桶, 已自动重新生成.", event.getPlayer().getName(), location);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            if (event.getPlayer().getInventory().getItemInMainHand().isSimilar(stick)) {
                if (Objects.nonNull(event.getClickedBlock())) {
                    event.setCancelled(true);
                    dataMap.put(event.getPlayer(), event.getClickedBlock().getLocation());
                    send(event.getPlayer(), "已成功选择方块({0}&7)", formatLocation(event.getClickedBlock().getLocation()));
                }
            }
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Bukkit.getOnlinePlayers().forEach(player -> {
            Inventory inventory = player.getOpenInventory().getTopInventory();
            Location location = inventory.getLocation();
            if (Objects.nonNull(location)) {
                if (locs.contains(location)) {
                    player.closeInventory();
                    Block block = location.getBlock();
                    Material chestType = block.getType();
                    block.setType(Material.AIR);
                    block.setType(chestType);
                    locs.remove(location);
                    if (debug) {
                        send(null, "检测到玩家&c{0}&7关闭了位于(&c{1}&7)的垃圾桶, 已自动重新生成.", player.getName(), location);
                    }
                }
            }
            if (debug) {
                send(null, "检测到插件正在关闭, 已自动重新生成了所有垃圾箱.");
            }
        });
        send(null, "已成功卸载插件.");
    }

    private static void send(CommandSender sender, String message, Object... args) {
        if ((Objects.nonNull(sender)) && (sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', MessageFormat.format(message, args)));
        } else {
            plugin.getLogger().info(ChatColor.translateAlternateColorCodes('&', MessageFormat.format(message, args)));
        }
    }

    private static String formatLocation(Location location) {
        return MessageFormat.format("&cWorld: {0}, X: {1}, Y:{2}, Z:{3}", location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
