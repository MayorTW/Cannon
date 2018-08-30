package tw.mayortw.cannon;

import tw.mayortw.cannon.util.BukkitManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Fireball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import net.minecraft.server.v1_12_R1.PacketPlayOutHeldItemSlot;

import java.util.HashMap;
import java.util.Map;

public class Cannon implements ConfigurationSerializable {

	public static ItemStack fireItem = new ItemStack(Material.DIAMOND_SWORD);
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
    private long lastFired = 0;

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

    @SuppressWarnings("deprecation")
    public void activate(Player player) {
        if(this.player != null)
            return;

        this.player = player;
        Bukkit.getOnlinePlayers().forEach(p -> p.hidePlayer(player));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 1, false, false));
        player.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "劍為射擊、鞋子為離開");
        player.setAllowFlight(true);
        player.setFlying(true);
        Vector dir = player.getLocation().getDirection();
        player.teleport(Structure.getPlayerPos(pos, dir)
                .setDirection(dir));
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

    @SuppressWarnings("deprecation")
    public void deactivate() {
        if(player == null) return;
        player.setAllowFlight(false);
        player.setFlying(false);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        Bukkit.getOnlinePlayers().forEach(p -> p.showPlayer(player));
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, null);
        }
        this.inv.forEach((K, V) -> {
            inv.setItem(K, V);
        });
        this.inv.clear();

        Structure.clearBlocks(pos, lastDir);
        player = null;
        lastDir = null;
    }

    public void fire() {
        long now = System.currentTimeMillis();
        if(now - lastFired > Structure.getCooldown() * 1000) {
            Location from = Structure.getFirePos(pos, lastDir);
            Location to = player.getTargetBlock(null, 150).getLocation();
            Vector toward = to.distanceSquared(from) > 9 ?
                to.toVector().subtract(from.toVector()).normalize() :
                lastDir;
            Fireball fireball = player.getWorld().spawn(
                    Structure.getFirePos(pos, lastDir), LargeFireball.class,
                    f -> f.setDirection(toward));
            fireball.setShooter(player);

            lastFired = now;
        }
    }

    public void updateStructure() {
        Vector dir = player.getLocation().getDirection();

        // Move player
        Location to = Structure.getPlayerPos(pos, dir).setDirection(dir);
        double dist = to.distanceSquared(player.getLocation());
        if(dist > 0) {
            Vector toward = to.toVector().subtract(player.getLocation().toVector()).normalize().multiply(Math.min(dist, .5));
            if(Math.abs(toward.angle(lastDir)) < .3)
                player.teleport(to);
            else
                player.setVelocity(toward);
        } else {
            player.setVelocity(new Vector());
        }

        // Update structure
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
