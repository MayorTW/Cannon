/*
 * Structure.java
 */

package tw.mayortw.cannon;

import tw.mayortw.cannon.util.LocationManager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Loads info about cannon structure
// includes block position, player position, cannon damage, cannon cooldown
public class Structure {

    public static final String FILE_PATH = "cannon.yml";
    private static final float SNAP_TO = (float) Math.PI / 4; // 8 snaps around the whole circle

    private static Map<String, List<BlockInfo>> structs = new HashMap<>();
    private static Location playerPos;
    private static Location firePos;
    private static int cooldown;
    private static int damage;

    public static void init(Plugin plugin) {

        // Load the file
        YamlConfiguration data = null;
        File file = new File(plugin.getDataFolder(), FILE_PATH);

        try {
            data = YamlConfiguration.loadConfiguration(file);
        } catch(IllegalArgumentException e) {} // Only thrown if file is null

        // Load datas
        playerPos = (Location) data.get("playerPos");
        firePos = (Location) data.get("firePos");
        cooldown = data.getInt("cooldown");
        damage = data.getInt("damage");

        // Load blocks
        for(String type : new String[] {"straight", "angled"}) {
            @SuppressWarnings("unchecked")
            List<BlockInfo> blockInfo = (List<BlockInfo>) data.getList("blocks_" + type);
            // Straight blocks
            List<BlockInfo> blocks = new ArrayList<>();
            if(blockInfo != null) {
                for(BlockInfo block : blockInfo) {
                    if(block != null)
                        blocks.add(block);
                }
            }
            structs.put(type, blocks);
        }
    }

    public static void setBlocks(Location pos, float angle/*, Player hideFrom*/) {
        angle = snappedAngle(angle);
        List<BlockInfo> blocks = structs.get(angle % (SNAP_TO * 2) == 0 ? "straight" : "angled");
        for(BlockInfo block : blocks) {
            Location to = pos.clone().add(.5, 0, .5).setDirection(LocationManager.toVector(angle));
            block.setBlock(to);
            /*
            if(hideFrom != null)
                block.hideFromPlayer(to, hideFrom);
            */
        }
    }

    public static void clearBlocks(Location pos, float angle) {
        angle = snappedAngle(angle);
        List<BlockInfo> blocks = structs.get(angle % SNAP_TO * 2 == 0 ? "straight" : "angled");
        for(BlockInfo block : blocks) {
            block.clearBlock(pos.clone().add(.5, 0, .5).setDirection(LocationManager.toVector(angle)));
        }
    }

    // Setters and getters
    public static Location getPlayerPos(Location pos, float angle) {
        angle = snappedAngle(angle);
        return LocationManager.getGlobalPos(pos.clone().add(.5, 0, .5).setDirection(LocationManager.toVector(angle)), playerPos.clone());
    }

    public static Location getFirePos(Location pos, float angle) {
        angle = snappedAngle(angle);
        return LocationManager.getGlobalPos(pos.clone().add(.5, .5, .5).setDirection(LocationManager.toVector(angle)), firePos.clone());
    }

    public static int getCooldown() {
        return cooldown;
    }

    public static int getDamage() {
        return damage;
    }

    // Return angle thats snapped to multiples of SNAP_TO
    private static float snappedAngle(float angle) {
        return (float) Math.round(angle / SNAP_TO) * SNAP_TO;
    }
}
