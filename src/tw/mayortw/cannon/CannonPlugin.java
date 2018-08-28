package tw.mayortw.cannon;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

public class CannonPlugin extends JavaPlugin implements Listener {

    private Structure struct;
    private Map<CommandSender, String> spawnSettingPlayers = new HashMap<>();
    private Set<CommandSender> cannonSettingPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        if(getServer().getPluginManager().getPlugin("Citizens") == null || getServer().getPluginManager().getPlugin("Citizens").isEnabled() == false) {
            getLogger().severe("Citizens 2.0 not found or not enabled");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);

        struct = new Structure(this);
    }

    @Override
    public void onDisable() {
        saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(cmd.getName().equalsIgnoreCase("cannon") && args.length > 0) {
            switch(args[0].toLowerCase()) {
                case "setspawn":
                    if(args.length < 2) return false;
                    sender.sendMessage("對 " + args[1] + " 隊出生點的方塊按右鍵");
                    cannonSettingPlayers.remove(sender);
                    spawnSettingPlayers.put(sender, args[1].toLowerCase());
                    break;
                case "removespawn":
                    if(args.length < 2) return false;
                    String key = "spawns." + args[1].replaceAll("\\.", "_");
                    if(getConfig().isSet(key)) {
                        getConfig().set(key, null);
                        sender.sendMessage("已清除 " + args[1] + " 隊出生點");
                    } else {
                        sender.sendMessage("找不到 " + args[1] + " 隊出生點");
                    }
                    break;
                case "clearspawn":
                    getConfig().set("spawns", null);
                    sender.sendMessage("清除所有出生點");
                    break;
                case "addcannon":
                    sender.sendMessage("對新的砲點方塊按右鍵");
                    spawnSettingPlayers.remove(sender);
                    cannonSettingPlayers.add(sender);
                    break;
                case "clearcannon":
                    getConfig().set("cannons", null);
                    sender.sendMessage("清除所有砲點");
                    break;
                case "undocannon":
                    Location removed = undoCannon();
                    if(removed != null) {
                        sender.sendMessage("已移除砲點 x:"
                                + removed.getBlockX() + ", y:" + removed.getBlockY() + ", z:" + removed.getBlockZ());
                    } else {
                        sender.sendMessage("已經沒有砲點了");
                    }
                    break;
                default:
                    return false;
            }

            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent eve) {
        Player player = eve.getPlayer();
        Location pos = eve.getClickedBlock().getLocation();

        if(spawnSettingPlayers.containsKey(player)) {
            String team = spawnSettingPlayers.get(player);
            addSpawn(pos, team);
            player.sendMessage(team + " 隊出生點已設成 x:"
                    + pos.getBlockX() + ", y:" + pos.getBlockY() + ", z:" + pos.getBlockZ());
            spawnSettingPlayers.remove(player);
        } else if(cannonSettingPlayers.contains(player)) {
            addCannon(pos);
            player.sendMessage("x:" + pos.getBlockX() + ", y:" + pos.getBlockY() + ", z:" + pos.getBlockZ() +
                    " 已成為砲點");
            cannonSettingPlayers.remove(player);
        } else {
            struct.setBlocks(pos, player.getEyeLocation().getDirection());
        }
    }

    private void addSpawn(Location pos, String team) {
        ConfigurationSection spawns = getConfig().getConfigurationSection("spawns");
        if(spawns == null) {
            spawns = getConfig().createSection("spawns");
        }
        spawns.set(team.replaceAll("\\.", "_"), pos);
    }

    private void addCannon(Location pos) {
        List<Location> cannons = (List<Location>) getConfig().getList("cannons");
        if(cannons == null) {
            cannons = new ArrayList<>();
            getConfig().set("cannons", cannons);
        }
        cannons.add(pos);
    }

    private Location undoCannon() {
        List<Location> cannons = (List<Location>) getConfig().getList("cannons");
        if(cannons == null) {
            cannons = new ArrayList<>();
            getConfig().set("cannons", cannons);
        }
        return cannons.isEmpty() ? null : cannons.remove(cannons.size() - 1);
    }
}
