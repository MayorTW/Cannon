package tw.mayortw.cannon;

import tw.mayortw.cannon.util.BukkitManager;
import tw.mayortw.cannon.util.LocationManager;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import net.citizensnpcs.api.event.NPCDamageByEntityEvent;
import net.citizensnpcs.api.event.NPCDeathEvent;
import net.citizensnpcs.api.npc.NPC;

import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class CannonPlugin extends JavaPlugin implements Listener {

    public static JavaPlugin plugin;

    private Map<CommandSender, String> spawnSettingPlayers = new HashMap<>();
    private Set<CommandSender> cannonSettingPlayers = new HashSet<>();

    private List<Cannon> cannons;
    private ConfigurationSection spawns;

    @Override
    @SuppressWarnings("unchecked")
    public void onEnable() {
        if(getServer().getPluginManager().getPlugin("Citizens") == null || getServer().getPluginManager().getPlugin("Citizens").isEnabled() == false) {
            getLogger().severe("Citizens 2.0 not found or not enabled");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);
        ConfigurationSerialization.registerClass(Cannon.class);
        ConfigurationSerialization.registerClass(BlockInfo.class);
        saveResource(Structure.FILE_PATH, false);

        Structure.init(this);
        CannonPlugin.plugin = this;
        spawns = getConfig().getConfigurationSection("spawns");
        cannons = (List<Cannon>) getConfig().getList("cannons");
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
                    Location removed = undoCannon().getLocation();
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
        Action action = eve.getAction();

        if(action != Action.PHYSICAL) {
            Cannon cannon = cannons.stream().filter(c -> player.equals(c.getPlayer())).findFirst().orElse(null);
            if(cannon != null) {
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if(Cannon.fireItem.equals(itemInHand)) {
                    // Fire cannon
                    cannon.fire();
                    eve.setCancelled(true);
                    return;
                } else if(Cannon.exitItem.equals(itemInHand)) {
                    // Exit cannon
                    cannon.deactivate(false);
                    eve.setCancelled(true);
                    return;
                }
            }
        }

        if(action == Action.RIGHT_CLICK_BLOCK) {
            Location pos = eve.getClickedBlock().getLocation();
            //player.sendBlockChange(player.getLocation(), org.bukkit.Material.BARRIER, (byte) 0);

            if(spawnSettingPlayers.containsKey(player)) {
                String team = spawnSettingPlayers.get(player);
                addSpawn(pos, team);
                player.sendMessage(team + " 隊出生點已設成 " + LocationManager.toString(pos));
                spawnSettingPlayers.remove(player);
                eve.setCancelled(true);
            } else if(cannonSettingPlayers.contains(player)) {
                addCannon(new Cannon(pos));
                player.sendMessage(LocationManager.toString(pos) + " 已成為砲點");
                cannonSettingPlayers.remove(player);
                eve.setCancelled(true);
            } else {
                Cannon cannon = cannons.stream().filter(c -> pos.equals(c.getLocation())).findFirst().orElse(null);
                if(cannon != null) {
                    cannon.activate(player);
                    eve.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent eve) {
        if(cannons != null) {
            Player player = eve.getPlayer();
            Cannon cannon = cannons.stream().filter(c -> player.equals(c.getPlayer())).findFirst().orElse(null);
            if(cannon != null) {
                Location from = eve.getFrom();
                Location to = eve.getTo();
                /*
                if(from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                    player.setAllowFlight(true);
                    player.setFlying(true);
                    eve.setCancelled(true);
                }
                */
                cannon.updateStructure();
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent eve) {
        Player player = eve.getPlayer();
        Cannon cannon = cannons.stream().filter(c -> player.equals(c.getPlayer())).findFirst().orElse(null);
        if(cannon != null) {
            cannon.deactivate(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent eve) {
        Player player = eve.getEntity();
        Cannon cannon = cannons.stream().filter(c -> player.equals(c.getPlayer())).findFirst().orElse(null);
        if(cannon != null) {
            cannon.deactivate(true);
            eve.getDrops().clear();
        }
    }

	// 虛擬NPC操受到傷害轉移到其他玩家身上
    @EventHandler
    public void onNPCDamageByEntity(NPCDamageByEntityEvent eve) {
        if(!eve.getNPC().data().has("Target")) return;

        UUID uuid = (UUID) eve.getNPC().data().get("Target");
        Player target = Bukkit.getPlayer(uuid);

        if(target == null || !target.isOnline()) return;

        Entity attacker = BukkitManager.getDamager(eve.getDamager());
        if(attacker == null) return;

        /*
        if(defenseData.getTeam().equals(attackerData.getTeam())) {
            attackerData.sendMessage(ChatColor.RED.toString() + ChatColor.BOLD + defense.getName() + " 是你的盟友，你無法傷害她");
            return;
        }
        */

        NPC npc = eve.getNPC();
        BukkitManager.broadcastEntityEffect(npc.getEntity(), 2);

        /*
        double damage = eve.getDamage();
        DamageSource damageSource = null;
        if(eve.getDamager() instanceof Player) {
            EntityPlayer attackerEP = BukkitManager.getNMSPlayer((Player) eve.getDamager());
            damageSource = DamageSource.playerAttack(attackerEP);
        } else if(eve.getDamager() instanceof Projectile && eve.getDamager() instanceof CraftArrow) {
            LivingEntity shooter = (LivingEntity) ((Projectile) eve.getDamager()).getShooter();
            if(shooter.getType().equals(EntityType.PLAYER)) {
                EntityArrow attackerEA = ((CraftArrow) eve.getDamager()).getHandle();
                EntityPlayer attackerEP = BukkitManager.getNMSPlayer((Player) shooter);
                damageSource = DamageSource.arrow(attackerEA, attackerEP);
            }
        } else if(eve.getDamager() instanceof CraftTNTPrimed) {
            CraftTNTPrimed craftTNTPrimed = (CraftTNTPrimed) eve.getDamager();
            if(craftTNTPrimed.getSource() != null && craftTNTPrimed.getSource().getType().equals(EntityType.PLAYER)) {
                damageSource = BukkitManager.createExplosionDamageSource((Player) craftTNTPrimed.getSource());
            }
        }
        if(damageSource == null) {
            return;
        }
        EntityPlayer defenseEP = ((CraftPlayer) defense).getHandle();

        class Damage implements Runnable {
            EntityPlayer defenseEP;
            DamageSource damageSource;
            double damage;

            public Damage(EntityPlayer defenseEP, DamageSource damageSource, double damage) {
                super();
                this.defenseEP = defenseEP;
                this.damageSource = damageSource;
                this.damage = damage;
            }

            @Override
            public void run() {
                defenseEP.damageEntity(damageSource, (float) damage);
            }

        }

        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(SpaceWar.War,
                new Damage(defenseEP, damageSource, damage), 0);
                */

        target.damage(eve.getDamage(), attacker);
        eve.setCancelled(true);
    }

	// 虛擬NPC清除噴裝
    @EventHandler
    public void onNPCDeath(NPCDeathEvent eve) {
        eve.getDrops().clear();
    }


    @EventHandler
    public void onEntityDamagedByEntity(EntityDamageByEntityEvent eve) {
        if(!eve.getEntity().getType().equals(EntityType.PLAYER)) {
            return;
        }

        Entity damager = eve.getDamager();
        if(damager.getType() == EntityType.FIREBALL &&
                ((Projectile) damager).getShooter() instanceof Player)
            eve.setDamage(Structure.getDamage());
    }

    private void addSpawn(Location pos, String team) {
        if(spawns == null) {
            spawns = getConfig().createSection("spawns");
        }
        spawns.set(team.replaceAll("\\.", "_"), pos);
    }

    private void addCannon(Cannon cannon) {
        if(cannons == null) {
            cannons = new ArrayList<>();
            getConfig().set("cannons", cannons);
        }
        cannons.add(cannon);
    }

    private Cannon undoCannon() {
        return cannons == null || cannons.isEmpty() ?  null :
            cannons.remove(cannons.size() - 1);
    }
}
