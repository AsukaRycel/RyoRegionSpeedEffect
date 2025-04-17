package Ryo;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Ryo extends JavaPlugin implements Listener {
    private Map<String, Region> regions;  // 存储多个区域
    private Map<String, Set<Player>> playersInRegion;  // 记录每个区域内的玩家

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        Bukkit.getPluginManager().registerEvents(this, this);

        regions = new HashMap<>();  // 初始化区域缓存
        playersInRegion = new HashMap<>();  // 初始化玩家记录

        // 加载配置区域
        loadRegions(config);

        getLogger().info("RyoRegionSpeedEffect 插件已加载");
        getLogger().info("已加载 " + regions.size() + " 个区域");
        getLogger().info("作者：Ryo QQ：125512156");
    }

    @Override
    public void onDisable() {
        getLogger().info("RyoRegionSpeedEffect 插件已卸载");
        getLogger().info("作者：Ryo QQ：125512156");
    }

    // 加载区域数据
    private void loadRegions(FileConfiguration config) {
        for (String regionName : config.getConfigurationSection("regions").getKeys(false)) {
            Region region = new Region(config.getConfigurationSection("regions." + regionName));
            regions.put(regionName, region);
            playersInRegion.put(regionName, new HashSet<>());  // 为每个区域初始化玩家记录
        }
    }

    // 判断玩家是否在区域内
    private boolean isInRegion(double x, double y, double z, String worldName, Region region) {
        return worldName.equals(region.world) &&
                x >= region.minX && x <= region.maxX &&
                y >= region.minY && y <= region.maxY &&
                z >= region.minZ && z <= region.maxZ;
    }

    private void applySpeedEffect(Player player, Region region) {
        // 应用加速效果
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, region.amplifier, false, false));
    }

    // 区域数据存储类
    private static class Region {
        private final String world;
        private final double minX, maxX, minY, maxY, minZ, maxZ;
        private final int amplifier;

        public Region(org.bukkit.configuration.ConfigurationSection section) {
            this.world = section.getString("world");
            this.minX = section.getDouble("minX");
            this.maxX = section.getDouble("maxX");
            this.minY = section.getDouble("minY");
            this.maxY = section.getDouble("maxY");
            this.minZ = section.getDouble("minZ");
            this.maxZ = section.getDouble("maxZ");
            this.amplifier = section.getInt("amplifier");
        }
    }

    // 处理命令
    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("RyoRegionSpeedEffect")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("RyoRegionSpeedEffect.reload")) {
                    reloadConfig();
                    loadRegions(getConfig());  // 重新加载区域配置
                    sender.sendMessage("区域配置已重新加载！");
                    sender.sendMessage("已加载 " + regions.size() + " 个区域");
                    return true;
                } else {
                    sender.sendMessage("你没有权限执行此命令！");
                    return false;
                }
            } else {
                sender.sendMessage("使用方法: /RyoRegionSpeedEffect reload");
                return false;
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // 检查 getTo() 是否为 null
        if (event.getTo() == null) return;

        // 其他代码逻辑...
        // 玩家没有实际移动，不处理
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();

        // 遍历所有区域，检查玩家是否进入某个区域
        for (Map.Entry<String, Region> entry : regions.entrySet()) {
            String regionName = entry.getKey();
            Region region = entry.getValue();
            Set<Player> playersInThisRegion = playersInRegion.get(regionName);

            boolean isInRegion = isInRegion(event.getTo().getX(), event.getTo().getY(), event.getTo().getZ(), player.getWorld().getName(), region);

            // 如果玩家进入区域且未曾进入过区域，应用加速效果
            if (isInRegion && !playersInThisRegion.contains(player)) {
                applySpeedEffect(player, region);
                playersInThisRegion.add(player);  // 记录玩家已经进入过区域
            }

            // 如果玩家离开区域，移除加速效果并从记录中删除
            if (!isInRegion && playersInThisRegion.contains(player)) {
                player.removePotionEffect(PotionEffectType.SPEED);
                playersInThisRegion.remove(player);  // 从记录中移除
            }
        }
    }
}
