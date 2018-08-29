package tw.mayortw.cannon;

import tw.mayortw.cannon.util.BukkitManager;

import org.bukkit.ChatColor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import net.minecraft.server.v1_12_R1.PacketPlayOutHeldItemSlot;

import java.util.HashMap;
import java.util.Map;

public class Cannon implements ConfigurationSerializable {

	public static ItemStack fireItem = new ItemStack(Material.FLINT_AND_STEEL);
	public static ItemStack exitItem = new ItemStack(Material.DIAMOND_BOOTS);

	static {
		ItemMeta itemMeta = fireItem.getItemMeta();
		itemMeta.setDisplayName(ChatColor.RED.toString() + ChatColor.BOLD + "射擊");
		fireItem.setItemMeta(itemMeta);
		itemMeta = exitItem.getItemMeta();
		itemMeta.setDisplayName(ChatColor.RED.toString() + ChatColor.BOLD + "離開");
		exitItem.setItemMeta(itemMeta);
	}

    private Location pos;
    private Player player;
    private Map<Integer, ItemStack> inv = new HashMap<>();
    private Vector lastDir;

    public Cannon(Location pos) {
        this.pos = pos;
    }

    public void setLocation(Location pos) {
        this.pos = pos;
    }

    public Location getLocation() {
        return pos.clone();
    }

    public Player getPlayer() {
        return player;
    }

    public void activate(Player player) {
        if(this.player != null) {
            if(this.player != player)
                player.sendMessage(ChatColor.RED.toString() + ChatColor.BOLD + "該砲台正在被 " + this.player.getName() + " 使用");
            return;
        }

        this.player = player;
		player.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "打火機為射擊、鞋子為離開");
		player.setAllowFlight(true);
		player.setFlying(true);
        player.teleport(Structure.getPlayerPos(pos));
		BukkitManager.sendPacket(player, new PacketPlayOutHeldItemSlot(0));
        PlayerInventory inv = player.getInventory();
        this.inv.clear();
        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().equals(Material.AIR)) {
                continue;
            }
            this.inv.put(i, item);
            inv.setItem(i, null);
        }
        inv.setItem(0, fireItem);
        inv.setItem(8, exitItem);

        updateStructure();
    }

    public void deactivate() {
		player.setAllowFlight(false);
		player.setFlying(false);
		PlayerInventory inv = player.getInventory();
		for (int i = 0; i < 36; i++) {
			inv.setItem(i, null);
		}
		this.inv.forEach((K, V) -> {
			inv.setItem(K, V);
		});
		this.inv.clear();

        Structure.clearBlocks(pos, lastDir);
        this.player = null;
        lastDir = null;
    }

    public void fire() {
    }

    public void updateStructure() {
        Vector dir = player.getEyeLocation().getDirection();

        Structure.clearBlocks(pos, lastDir);
        Structure.setBlocks(pos, dir);

        lastDir = dir;
    }

    // Serialization

    public static Cannon deserialize(Map<String, Object> data) {
        return new Cannon(Location.deserialize(data));
    }

    @Override
    public Map<String, Object> serialize() {
        return pos.serialize();
    }

    // For in List.contains

    @Override
    public boolean equals(Object o) {
        if(o instanceof Location)
            return this.pos == null ? pos == null : this.pos.equals(o);
        else if(o instanceof Player)
            return this.player == null ? player == null : this.player.equals(o);
        else
            return super.equals(o);
    }
}
