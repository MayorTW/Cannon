/*
 * BukkitManager.java
 *
 * Copied and edited from dianlemel's SpaceWar plugin
 */
package tw.mayortw.cannon.util;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public class LocationManager {

	public static String toString(Location loc) {
		if(loc == null){
			return "None";
		}
		return String.format("[%s: %d, %d, %d]", loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
	}

	public static Vector getVectorBetween(Location to, Location from) {
		Vector dir = new Vector();		
		dir.setX(to.getX() - from.getX());
		dir.setY(to.getY() - from.getY());
		dir.setZ(to.getZ() - from.getZ());	
		return dir;
	}

    // Global position relative to pos 1 on the xz plane
    public static Location getGlobalPos(Location pos1, Location pos2) {
        double x = pos2.getX() * Math.cos(Math.toRadians(pos1.getYaw())) -
            pos2.getZ() * Math.sin(Math.toRadians(pos1.getYaw())) + pos1.getX();
        double z = pos2.getX() * Math.sin(Math.toRadians(pos1.getYaw())) +
            pos2.getZ() * Math.cos(Math.toRadians(pos1.getYaw())) + pos1.getZ();
        double y = pos2.getY() + pos1.getY();

        return new Location(pos1.getWorld(), x, y, z, pos2.getYaw() + pos1.getYaw(), pos2.getPitch());
    }

    // Local position relative to pos 1 on the xz plane
    public static Location getLocalLoc(Location loc1, Location loc2) {
        double x = (loc2.getX() - loc1.getX()) * Math.cos(Math.toRadians(loc1.getYaw())) +
            (loc2.getZ() - loc1.getZ()) * Math.sin(Math.toRadians(loc1.getYaw()));
        double z = -(loc2.getX() - loc1.getX()) * Math.sin(Math.toRadians(loc1.getYaw())) +
            (loc2.getZ() - loc1.getZ()) * Math.cos(Math.toRadians(loc1.getYaw()));
        double y = loc2.getY() - loc1.getY();

        return new Location(loc1.getWorld(), x, y, z, loc2.getYaw() - loc1.getYaw(), loc2.getPitch());
    }
}
