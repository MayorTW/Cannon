package tw.mayortw.cannon.util;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_12_R1.CraftServer;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftTNTPrimed;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.util.Vector;
import com.mojang.authlib.GameProfile;

import net.minecraft.server.v1_12_R1.EntityPlayer;
import net.minecraft.server.v1_12_R1.EntityTNTPrimed;
import net.minecraft.server.v1_12_R1.EntityTippedArrow;
import net.minecraft.server.v1_12_R1.Explosion;
import net.minecraft.server.v1_12_R1.MinecraftServer;
import net.minecraft.server.v1_12_R1.Packet;
import net.minecraft.server.v1_12_R1.SoundCategory;
import net.minecraft.server.v1_12_R1.SoundEffects;
import net.minecraft.server.v1_12_R1.World;
import net.minecraft.server.v1_12_R1.WorldServer;
import net.minecraft.server.v1_12_R1.DamageSource;
import net.minecraft.server.v1_12_R1.Entity;
import net.minecraft.server.v1_12_R1.EntityArrow.PickupStatus;

public class BukkitManager {

	public static Map<UUID, Entity> entitys = new ConcurrentHashMap<UUID, Entity>();

	public static DamageSource createExplosionDamageSource(Player player) {
		EntityPlayer ep = getNMSPlayer(player);
		EntityTNTPrimed etnt = new EntityTNTPrimed(ep.world, 0, 0, 0, ep);
		Explosion explosion = new Explosion(null, etnt, 0, 0, 0, 0, false, false);
//		CraftEventFactory.entityDamage = etnt;
		return DamageSource.explosion(explosion);
	}

	public static UUID getPlayer(String name) {
		MinecraftServer minectaft = ((CraftServer) Bukkit.getServer()).getServer();
		GameProfile gp = minectaft.getUserCache().getProfile(name);
		if (gp == null) {
			return null;
		}
		return gp.getId();
	}
	
	public static WorldServer getNMSWorld(org.bukkit.World world){
		CraftWorld craftWorld = (CraftWorld) world;
		return craftWorld.getHandle();
	}

	public static EntityPlayer getNMSPlayer(Player player) {
		CraftPlayer craftPlayer = (CraftPlayer) player;
		return craftPlayer.getHandle();
	}

	public static void sendPacket(Player player, Packet<?> packet) {
		getNMSPlayer(player).playerConnection.sendPacket(packet);
	}

	public static void sendAllPacket(Packet<?> packet) {
		Bukkit.getOnlinePlayers().forEach(player -> sendPacket(player, packet));
	}

	public static Player getDamager(org.bukkit.entity.Entity entity) {
		Player attacker = null;
		if (entity instanceof Player) {
			attacker = (Player) entity;
		} else if (entity instanceof Projectile) {
			LivingEntity shooter = (LivingEntity) ((Projectile) entity).getShooter();
			if (shooter.getType().equals(EntityType.PLAYER)) {
				attacker = (Player) shooter;
			}
		} else if (entity instanceof CraftTNTPrimed) {
			CraftTNTPrimed craftTNTPrimed = (CraftTNTPrimed) entity;
			if (craftTNTPrimed.getSource().getType().equals(EntityType.PLAYER)) {
				attacker = (Player) craftTNTPrimed.getSource();
			}
		}
		return attacker;
	}

	public static void broadcastEntityEffect(org.bukkit.entity.Entity entity, int id) {
		((CraftWorld) entity.getWorld()).getHandle().broadcastEntityEffect(((CraftEntity) entity).getHandle(),
				(byte) id);
	}

	public static String getPlayerName(UUID uuid) {
		MinecraftServer minectaft = ((CraftServer) Bukkit.getServer()).getServer();
		GameProfile gp = minectaft.getUserCache().a(uuid);
		if(gp == null){
			return "Unknow";
		}
		return gp.getName();
	}

	public static void ShootBow(Player player, Location loc, float speed, double Precision, boolean Gravity) {
		EntityPlayer ep = ((CraftPlayer) player).getHandle();
		CraftWorld cw = (CraftWorld) loc.getWorld();
		World world = cw.getHandle();
		EntityTippedArrow eta = new EntityTippedArrow(world, ep);
		eta.glowing = true;
		eta.setNoGravity(Gravity);
		eta.fromPlayer = PickupStatus.DISALLOWED;
		eta.setPositionRotation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
		Vector velocity = loc.getDirection();
		eta.shoot(velocity.getX(), velocity.getY(), velocity.getZ(), speed, (float) Precision);
		cw.addEntity(eta, SpawnReason.CUSTOM);
	}

	public static void ShootBow(Player player, float speed, double Precision, boolean Gravity) {
		EntityPlayer ep = ((CraftPlayer) player).getHandle();
		CraftWorld cw = (CraftWorld) player.getWorld();
		World world = cw.getHandle();
		EntityTippedArrow eta = new EntityTippedArrow(world, ep);
		entitys.put(eta.getUniqueID(), eta);
		eta.a(ep, ep.pitch, ep.yaw, 0.0f, speed, (float) Precision);
		eta.glowing = true;
		eta.setNoGravity(Gravity);
		eta.fromPlayer = PickupStatus.DISALLOWED;
		cw.addEntity(eta, SpawnReason.CUSTOM);
		world.a(null, ep.locX, ep.locY, ep.locZ, SoundEffects.w, SoundCategory.PLAYERS, 1.0F,
				1.0F / (new Random().nextFloat() * 0.4F + 1.2F) + speed * 0.5F);
	}

}
