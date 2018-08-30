package tw.mayortw.cannon;

import tw.mayortw.cannon.util.BukkitManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.EntityType;
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

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot;

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
    private NPC npc;
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
        Vector dir = player.getLocation().getDirection();

        Bukkit.getOnlinePlayers().forEach(p -> p.hidePlayer(player));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 1, false, false));
        player.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "劍為射擊、鞋子為離開");
        player.setAllowFlight(true);
        player.setFlying(true);

        BukkitManager.sendPacket(player, new PacketPlayOutHeldItemSlot(0));

        PlayerInventory inv = player.getInventory();

        npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, player.getName());
        npc.spawn(player.getLocation());
        npc.setProtected(false);
        npc.data().set("Target", player.getUniqueId());
        npc.getTrait(Equipment.class).set(EquipmentSlot.HELMET, inv.getHelmet());
        npc.getTrait(Equipment.class).set(EquipmentSlot.CHESTPLATE, inv.getChestplate());
        npc.getTrait(Equipment.class).set(EquipmentSlot.LEGGINGS, inv.getLeggings());
        npc.getTrait(Equipment.class).set(EquipmentSlot.BOOTS, inv.getBoots());
        npc.getTrait(Equipment.class).set(EquipmentSlot.HAND, inv.getItemInMainHand());
        npc.getTrait(Equipment.class).set(EquipmentSlot.OFF_HAND, inv.getItemInOffHand());
        ((Player)npc.getEntity()).setMaxHealth(player.getMaxHealth());
        ((Player)npc.getEntity()).setHealth(player.getHealth());

        this.inv.clear();
        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            if(item == null || item.getType().equals(Material.AIR)) {
                continue;
            }
            this.inv.put(i, item);
            inv.setItem(i, null);
        }
        inv.setItem(0, fireItem);
        inv.setItem(8, exitItem);

        player.teleport(Structure.getPlayerPos(pos, dir)
                .setDirection(dir));

        updateStructure();
    }

    @SuppressWarnings("deprecation")
    public void deactivate(boolean isDie) {
        if(player == null) return;
        player.setAllowFlight(false);
        player.setFlying(false);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        Bukkit.getOnlinePlayers().forEach(p -> p.showPlayer(player));

        class DestroyNPC implements Runnable {
            private NPC npc;
            DestroyNPC(NPC npc) {
                this.npc = npc;
            }
            @Override
            public void run() {
                npc.destroy();
            }
        }

        Location npcPos = npc.getStoredLocation();

        if(isDie) {
            // Drop items
            this.inv.forEach((k, v) -> {
                npcPos.getWorld().dropItem(npcPos, v);
            });

            npc.getTrait(Equipment.class).set(EquipmentSlot.HELMET, null);
            npc.getTrait(Equipment.class).set(EquipmentSlot.CHESTPLATE, null);
            npc.getTrait(Equipment.class).set(EquipmentSlot.LEGGINGS, null);
            npc.getTrait(Equipment.class).set(EquipmentSlot.BOOTS, null);
            npc.getTrait(Equipment.class).set(EquipmentSlot.HAND, null);
            npc.getTrait(Equipment.class).set(EquipmentSlot.OFF_HAND, null);
            BukkitManager.broadcastEntityEffect(npc.getEntity(), 3);
            Bukkit.getScheduler().runTaskLater(CannonPlugin.plugin, new DestroyNPC(npc), 20);
        } else {
            PlayerInventory inv = player.getInventory();
            for (int i = 0; i < 36; i++) {
                inv.setItem(i, null);
            }
            this.inv.forEach((k, v) -> {
                inv.setItem(k, v);
            });
            this.inv.clear();

            player.teleport(npcPos);
            npc.destroy();
        }
        npc = null;

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
            fireball.setYield(3);
            fireball.setIsIncendiary(false);

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
