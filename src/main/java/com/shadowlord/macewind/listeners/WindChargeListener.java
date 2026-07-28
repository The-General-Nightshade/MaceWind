package com.shadowlord.macewind.listeners;

import com.shadowlord.macewind.MaceWindPlugin;
import com.shadowlord.macewind.util.ItemUtils;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Button;
import org.bukkit.material.Lever;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Openable;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

public class WindChargeListener implements Listener {
    private final MaceWindPlugin plugin;
    private static final String WIND_META = "MaceWind_WindCharge";
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, double[]> windChargeLaunchData = new ConcurrentHashMap<>();
    private static final long LAUNCH_DATA_EXPIRY_MS = 30000L;

    public WindChargeListener(MaceWindPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {
        // Accept both main hand and offhand quick-right uses
        EquipmentSlot hand = event.getHand();
        if (hand != EquipmentSlot.HAND && hand != EquipmentSlot.OFF_HAND) return;
        if (!event.getAction().toString().contains("RIGHT")) return;

        Player player = event.getPlayer();
        if (player == null) return;

        // Read the correct hand item
        ItemStack inHand = null;
        try {
            if (hand == EquipmentSlot.HAND) {
                if (player.getInventory() != null) inHand = player.getInventory().getItemInMainHand();
            } else {
                if (player.getInventory() != null) inHand = player.getInventory().getItemInOffHand();
            }
        } catch (Throwable ignored) {}

        if (!ItemUtils.isWindCharge(inHand)) return;

        long cooldownMs = this.plugin.getConfig().getLong("windcharge.cooldown-ms", 500L);
        long now = System.currentTimeMillis();
        Long lastUse = this.cooldowns.get(player.getUniqueId());
        if (lastUse != null && now - lastUse < cooldownMs) {
            event.setCancelled(true);
            return;
        }
        this.cooldowns.put(player.getUniqueId(), now);

        World w = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        double speed = this.plugin.getConfig().getDouble("windcharge.projectile-speed", 1.5);
        Location spawnLoc = eye.clone().add(dir.clone().multiply(1.0));

        // Spawn projectile defensively
        Projectile proj = null;
        try {
            SmallFireball fb = (SmallFireball) w.spawn(spawnLoc, SmallFireball.class);
            fb.setShooter((ProjectileSource) player);
            fb.setYield(0.0f);
            fb.setIsIncendiary(false);
            fb.setVelocity(dir.clone().multiply(speed));
            fb.setMetadata(WIND_META, new FixedMetadataValue((Plugin) this.plugin, true));
            proj = fb;
        } catch (Throwable t) {
            try {
                Projectile p = w.spawn(spawnLoc, Projectile.class);
                p.setVelocity(dir.clone().multiply(speed));
                p.setMetadata(WIND_META, new FixedMetadataValue((Plugin) this.plugin, true));
                proj = p;
            } catch (Throwable ignored) {}
        }

        // Consume item from the correct hand and ensure NBT persists on remaining stack
        if (this.plugin.getConfig().getBoolean("windcharge.consume-item", true)) {
            if (inHand != null) {
                if (inHand.getAmount() > 1) {
                    inHand.setAmount(inHand.getAmount() - 1);
                    // reapply wind tag defensively to the remaining stack
                    ItemStack ensured = ItemUtils.ensureWindTag(inHand);
                    if (hand == EquipmentSlot.HAND) {
                        player.getInventory().setItemInMainHand(ensured);
                    } else {
                        player.getInventory().setItemInOffHand(ensured);
                    }
                } else {
                    if (hand == EquipmentSlot.HAND) {
                        player.getInventory().setItemInMainHand(null);
                    } else {
                        player.getInventory().setItemInOffHand(null);
                    }
                }
            }
        }

        if (this.plugin.getConfig().getBoolean("windcharge.feedback.enable-sound", true)) {
            try {
                String snd = this.plugin.getConfig().getString("windcharge.feedback.throw-sound", "ENTITY_SNOWBALL_THROW");
                w.playSound(player.getLocation(), Sound.valueOf(snd), 1.0f, 1.2f);
            } catch (Throwable ignored) {}
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile proj = event.getEntity();
        if (proj == null || !proj.hasMetadata(WIND_META)) return;

        Location center = proj.getLocation();
        World world = center.getWorld();
        Player shooter = proj.getShooter() instanceof Player ? (Player) proj.getShooter() : null;

        spawnWindBurst(world, center);

        if (this.plugin.getConfig().getBoolean("windcharge.feedback.enable-sound", true)) {
            try {
                String snd = this.plugin.getConfig().getString("windcharge.feedback.impact-sound", "ENTITY_ENDERDRAGON_FLAP");
                world.playSound(center, Sound.valueOf(snd), 1.0f, 1.5f);
            } catch (Throwable ignored) {}
        }

        if (this.plugin.getConfig().getBoolean("windcharge.block-interactions", true)) {
            toggleNearbyBlocks(center);
        }

        double radius = this.plugin.getConfig().getDouble("windcharge.radius", 2.5);
        double knockback = this.plugin.getConfig().getDouble("windcharge.knockback-strength", 1.0);
        double upwardBase = this.plugin.getConfig().getDouble("windcharge.upward-base", 0.5);
        double directDmg = this.plugin.getConfig().getDouble("windcharge.direct-damage", 1.0);
        boolean selfKB = this.plugin.getConfig().getBoolean("windcharge.self-knockback", true);
        double selfUpBoost = this.plugin.getConfig().getDouble("windcharge.self-upward-boost", 1.1);
        double maxUpVel = this.plugin.getConfig().getDouble("windcharge.max-upward-velocity", 1.2);

        Collection<Entity> nearby = world.getNearbyEntities(center, radius, radius, radius);
        for (Entity ent : nearby) {
            if (ent.equals(proj) || !(ent instanceof LivingEntity)) continue;
            LivingEntity living = (LivingEntity) ent;
            boolean isSelf = shooter != null && ent.equals(shooter);
            double dist = ent.getLocation().distance(center);
            if (isSelf && !selfKB) continue;
            if (dist > radius) continue;

            double falloff = Math.max(0.0, (radius - dist) / radius);
            if (isSelf) {
                Vector up = new Vector(0.0, selfUpBoost * Math.max(falloff, 0.4), 0.0);
                if (up.getY() > maxUpVel) up.setY(maxUpVel);
                ent.setVelocity(up);
                if (shooter != null) {
                    this.windChargeLaunchData.put(shooter.getUniqueId(), new double[]{shooter.getLocation().getY(), System.currentTimeMillis()});
                }
            } else {
                Vector dir;
                if (dist < 0.5) {
                    dir = new Vector(0, 1, 0);
                } else {
                    dir = ent.getLocation().toVector().subtract(center.toVector()).normalize();
                    dir.setY(dir.getY() + upwardBase);
                    dir.normalize();
                }
                dir.multiply(knockback * falloff);
                if (dir.getY() > maxUpVel) dir.setY(maxUpVel);
                ent.setVelocity(dir);
                if (dist < 1.5) {
                    try {
                        living.damage(directDmg);
                    } catch (Throwable ignored) {}
                }
            }
            living.setFireTicks(0);
        }

        proj.remove();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        double[] data = this.windChargeLaunchData.remove(player.getUniqueId());
        if (data == null) return;

        double launchY = data[0];
        double launchTime = data[1];
        if (System.currentTimeMillis() - (long) launchTime > LAUNCH_DATA_EXPIRY_MS) return;

        double landingY = player.getLocation().getY();
        if (landingY >= launchY - 0.5) {
            event.setCancelled(true);
        } else {
            double reducedDamage = Math.max(0.0, launchY - landingY - 3.0);
            if (reducedDamage <= 0.0) {
                event.setCancelled(true);
            } else {
                event.setDamage(reducedDamage);
            }
        }
    }

    private void toggleNearbyBlocks(Location center) {
        double blockRadius = this.plugin.getConfig().getDouble("windcharge.block-interact-radius", 1.5);
        int r = (int) Math.ceil(blockRadius);
        for (int dx = -r; dx <= r; ++dx) {
            for (int dy = -r; dy <= r; ++dy) {
                for (int dz = -r; dz <= r; ++dz) {
                    try {
                        Block block = center.getWorld().getBlockAt(center.getBlockX() + dx, center.getBlockY() + dy, center.getBlockZ() + dz);
                        String typeName = block.getType().name().toUpperCase();
                        BlockState state;
                        if (typeName.contains("DOOR") || typeName.contains("GATE")) {
                            state = block.getState();
                            if (!(state.getData() instanceof Openable)) continue;
                            Openable o = (Openable) state.getData();
                            o.setOpen(!o.isOpen());
                            state.setData((MaterialData) o);
                            state.update(true, true);
                            continue;
                        }
                        if (typeName.equals("LEVER")) {
                            state = block.getState();
                            if (!(state.getData() instanceof Lever)) continue;
                            Lever lever = (Lever) state.getData();
                            lever.setPowered(!lever.isPowered());
                            state.setData((MaterialData) lever);
                            state.update(true, true);
                            continue;
                        }
                        state = block.getState();
                        if (!typeName.contains("BUTTON") || !(state.getData() instanceof Button)) continue;
                        Button btn = (Button) state.getData();
                        btn.setPowered(true);
                        state.setData((MaterialData) btn);
                        state.update(true, true);
                        Block b = block;
                        this.plugin.getServer().getScheduler().runTaskLater((Plugin) this.plugin, () -> {
                            try {
                                BlockState s = b.getState();
                                if (s.getData() instanceof Button) {
                                    Button bt = (Button) s.getData();
                                    bt.setPowered(false);
                                    s.setData((MaterialData) bt);
                                    s.update(true, true);
                                }
                            } catch (Throwable ignored) {}
                        }, typeName.contains("WOOD") ? 30L : 20L);
                    } catch (Throwable ignored) {}
                }
            }
        }
    }

    private void spawnWindBurst(World world, Location center) {
        if (!this.plugin.getConfig().getBoolean("windcharge.feedback.enable-effect", true)) return;
        Effect effect;
        try {
            effect = Effect.valueOf(this.plugin.getConfig().getString("windcharge.feedback.effect", "SMOKE"));
        } catch (Throwable e) {
            effect = Effect.SMOKE;
        }
        int effectData = this.plugin.getConfig().getInt("windcharge.feedback.effect-data", 4);
        for (int i = 0; i < 4; ++i) {
            try {
                world.playEffect(center.clone().add(0.0, 0.25 * i, 0.0), effect, effectData);
            } catch (Throwable ignored) {}
        }
        int samples = 8;
        double burstRadius = 1.0;
        for (int i = 0; i < samples; ++i) {
            double angle = Math.PI * 2 * i / samples;
            try {
                world.playEffect(center.clone().add(Math.cos(angle) * burstRadius, 0.15, Math.sin(angle) * burstRadius), effect, effectData);
            } catch (Throwable ignored) {}
        }
    }
}
