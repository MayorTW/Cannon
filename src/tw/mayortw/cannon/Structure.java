package tw.mayortw.cannon;

import tw.mayortw.cannon.util.LocationManager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.file.YamlConfiguration;
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

public class Structure {

    public static final String FILE_PATH = "cannon.yml";

    private static List<BlockInfo> blocks = new ArrayList<>();
    private static Location playerPos;

    public static void init(Plugin plugin) {

        YamlConfiguration data = null;
        File file = new File(plugin.getDataFolder(), FILE_PATH);

        try {
            data = YamlConfiguration.loadConfiguration(file);
        } catch(IllegalArgumentException e) {}

        playerPos = (Location) data.get("playerPos");

        @SuppressWarnings("unchecked")
        List<BlockInfo> blockInfo = (List<BlockInfo>) data.getList("blocks");
        if(blockInfo != null) {
            for(BlockInfo block : blockInfo) {
                blocks.add(block);
            }
        }
    }

    public static void setBlocks(Location pos, Vector dir) {
        for(BlockInfo block : blocks) {
            block.setBlock(pos.clone().add(.5, 0, .5).setDirection(dir));
        }
    }

    public static void clearBlocks(Location pos, Vector dir) {
        if(dir == null) return;
        for(BlockInfo block : blocks) {
            block.clearBlock(pos.clone().add(.5, 0, .5).setDirection(dir));
        }
    }

    public static Location getPlayerPos(Location pos) {
        return LocationManager.getGlobalPos(pos.clone().add(.5, 0, .5), playerPos.clone());
    }
}
