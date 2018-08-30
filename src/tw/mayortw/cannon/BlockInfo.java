package tw.mayortw.cannon;

import tw.mayortw.cannon.util.LocationManager;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class BlockInfo implements ConfigurationSerializable {

    private Location pos;
    private Material type;
    private byte blockData;

    @SuppressWarnings("deprecation")
    public void setBlock(Location to) {
        if(pos != null && type != null) {
            Block block = LocationManager.getGlobalPos(to, pos).getBlock();
            block.setType(type);

            BlockState state = block.getState();
            state.getData().setData(blockData);
            state.update();
        }
    }

    public void clearBlock(Location to) {
        if(pos != null) {
            LocationManager.getGlobalPos(to, pos).getBlock().setType(Material.AIR);
        }
    }

    /*
    public void hideFromPlayer(Location to, Player player) {
        if(pos != null) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(CannonPlugin.plugin , () -> player.sendBlockChange(LocationManager.getGlobalPos(to, pos), Material.AIR, (byte) 0), 0);
        }
    }
    */

    private BlockInfo(Location pos, Material type, byte blockData) {
        this.pos = pos;
        this.type = type;
        this.blockData = blockData;
    }

    // Serialization

    public static BlockInfo deserialize(Map<String, Object> data) {
        Location pos = (Location) data.get("pos");
        Material type = Material.getMaterial((String) data.get("type"));

        if(pos == null || type == null) {
            Bukkit.getLogger().severe("Cannot deserialize BlockInfo");
            return null;
        }

        Object blockData = data.get("data");
        return new BlockInfo(pos, type,
                blockData instanceof Number ? ((Number) blockData).byteValue() : 0);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("pos", pos);
        data.put("type", type);
        data.put("data", blockData);
        return data;
    }
}
