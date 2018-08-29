package tw.mayortw.cannon;

import tw.mayortw.cannon.util.LocationManager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class BlockInfo implements ConfigurationSerializable {

    private Location pos;
    private Material type;

    public void setBlock(Location to) {
        if(pos != null && type != null) {
            LocationManager.getGlobalPos(to, pos).getBlock().setType(type);
        }
    }

    public void clearBlock(Location to) {
        if(pos != null) {
            LocationManager.getGlobalPos(to, pos).getBlock().setType(Material.AIR);
        }
    }

    private BlockInfo(Location pos, Material type) {
        this.pos = pos;
        this.type = type;
        if(pos == null)
            Bukkit.getLogger().warning("Block position is null");
        if(type == null)
            Bukkit.getLogger().warning("Block type is null");
    }

    // Serialization

    public static BlockInfo deserialize(Map<String, Object> data) {
        return new BlockInfo((Location) data.get("pos"), Material.getMaterial((String) data.get("type")));
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("pos", pos);
        data.put("type", type);
        return data;
    }
}
