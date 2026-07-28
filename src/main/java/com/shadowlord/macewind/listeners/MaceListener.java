package com.shadowlord.macewind.listeners;

import com.shadowlord.macewind.MaceWindPlugin;
import com.shadowlord.macewind.util.EnchantUtils;
import com.shadowlord.macewind.util.ItemUtils;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class MaceListener
implements Listener {
    private final MaceWindPlugin plugin;
    private static final String SMASH_HIT_META = "MaceWind_SmashHit";
    private final Map<UUID, Double> fallStartY = new ConcurrentHashMap<UUID, Double>();
    private final Map<UUID, Double> recordedFallDist = new ConcurrentHashMap<UUID, Double>();
    private final Map<UUID, Long> lastHitTs = new ConcurrentHashMap<UUID, Long>();

    public MaceListener(MaceWindPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if (event.getTo() == null) {
            return;
        }
        double fromY = event.getFrom().getY();
        double toY = event.getTo().getY();
        UUID id = p.getUniqueId();
        if (toY < fromY && !p.isFlying() && !p.isGliding()) {
            this.fallStartY.putIfAbsent(id, fromY);
        }
        if (p.isOnGround() && this.fallStartY.containsKey(id)) {
            double startY = this.fallStartY.remove(id);
            double dist = startY - p.getLocation().getY();
            if (dist < 0.0) {
                dist = 0.0;
            }
            this.recordedFallDist.put(id, dist);
        }
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player p = (Player)event.getEntity();
        if (p.hasMetadata(SMASH_HIT_META)) {
            event.setCancelled(true);
            p.removeMetadata(SMASH_HIT_META, (Plugin)this.plugin);
            return;
        }
        if (p.getInventory() != null && ItemUtils.isMace(p.getInventory().getItemInMainHand()) && this.recordedFallDist.containsKey(p.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        boolean isSmash;
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player attacker = (Player)event.getDamager();
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (!ItemUtils.isMace(weapon)) {
            return;
        }
        UUID attackerId = attacker.getUniqueId();
        long now = System.currentTimeMillis();
        long debounce = this.plugin.getConfig().getLong("mace.hit-debounce-ms", 50L);
        if (this.lastHitTs.containsKey(attackerId) && now - this.lastHitTs.get(attackerId) < debounce) {
            return;
        }
        this.lastHitTs.put(attackerId, now);
        double baseDamage = this.plugin.getConfig().getDouble("mace.base-damage", 7.0);
        Double fallDistance = this.recordedFallDist.remove(attackerId);
        if (fallDistance == null) {
            double vy = attacker.getVelocity().getY();
            fallDistance = vy < 0.0 ? Math.abs(vy) * 2.0 : 0.0;
        }
        double threshold = this.plugin.getConfig().getDouble("mace.fall-threshold", 1.5);
        double bonus = 0.0;
        boolean bl = isSmash = fallDistance > threshold;
        if (isSmash) {
            double effectiveFall = fallDistance - threshold;
            double tier1Rate = this.plugin.getConfig().getDouble("mace.tier1-per-block", 4.0);
            double tier2Rate = this.plugin.getConfig().getDouble("mace.tier2-per-block", 2.0);
            double tier3Rate = this.plugin.getConfig().getDouble("mace.tier3-per-block", 1.0);
            double tier1End = this.plugin.getConfig().getDouble("mace.tier1-blocks", 3.0);
            double tier2End = this.plugin.getConfig().getDouble("mace.tier2-blocks", 8.0);
            int densityLevel = EnchantUtils.getLevel(weapon, "Density");
            double densityPerBlk = this.plugin.getConfig().getDouble("mace.enchants.density-per-block-per-level", 0.5);
            double densityExtra = (double)densityLevel * densityPerBlk;
            double remaining = effectiveFall;
            double t1Blocks = Math.min(remaining, tier1End);
            bonus += t1Blocks * (tier1Rate + densityExtra);
            if ((remaining -= t1Blocks) > 0.0) {
                double t2Blocks = Math.min(remaining, tier2End - tier1End);
                bonus += t2Blocks * (tier2Rate + densityExtra);
                remaining -= t2Blocks;
            }
            if (remaining > 0.0) {
                bonus += remaining * (tier3Rate + densityExtra);
            }
            attacker.setMetadata(SMASH_HIT_META, (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
            this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> attacker.removeMetadata(SMASH_HIT_META, (Plugin)this.plugin), 2L);
        }
        double totalDamage = baseDamage + bonus;
        int breachLevel = EnchantUtils.getLevel(weapon, "Breach");
        if (breachLevel > 0 && event.getEntity() instanceof LivingEntity) {
            double bypassPct = this.plugin.getConfig().getDouble("mace.enchants.breach-percent-per-level", 15.0);
            double totalBypass = Math.min(1.0, bypassPct * (double)breachLevel / 100.0);
            try {
                if (event.isApplicable(EntityDamageEvent.DamageModifier.ARMOR)) {
                    double armorMod = event.getDamage(EntityDamageEvent.DamageModifier.ARMOR);
                    event.setDamage(EntityDamageEvent.DamageModifier.ARMOR, armorMod * (1.0 - totalBypass));
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        event.setDamage(EntityDamageEvent.DamageModifier.BASE, totalDamage);
        Entity target = event.getEntity();
        Location loc = target.getLocation();
        Vector knock = target.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize();
        double hMult = this.plugin.getConfig().getDouble("mace.knockback.horizontal-multiplier", 0.5);
        double hCap = this.plugin.getConfig().getDouble("mace.knockback.horizontal-bonus-cap", 1.2);
        double vBase = this.plugin.getConfig().getDouble("mace.knockback.vertical-base", 0.3);
        double vCap = this.plugin.getConfig().getDouble("mace.knockback.vertical-bonus-cap", 0.5);
        double kbScale = isSmash ? Math.min(hCap, bonus / 8.0) : 0.0;
        knock.setY(vBase + Math.min(vCap, bonus / 8.0));
        knock.multiply(hMult + kbScale);
        target.setVelocity(knock);
        int windBurstLevel = EnchantUtils.getLevel(weapon, "Wind Burst");
        if (windBurstLevel > 0 && isSmash) {
            double wbBase = this.plugin.getConfig().getDouble("mace.enchants.wind-burst-base-launch", 0.6);
            double wbPerLvl = this.plugin.getConfig().getDouble("mace.enchants.wind-burst-per-level", 0.35);
            double launchVel = wbBase + wbPerLvl * (double)windBurstLevel;
            attacker.setVelocity(new Vector(0.0, launchVel, 0.0));
        }
        if (this.plugin.getConfig().getBoolean("mace.feedback.enable-effect", true)) {
            try {
                loc.getWorld().playEffect(loc, Effect.STEP_SOUND, this.plugin.getConfig().getInt("mace.feedback.effect-data", 42));
            }
            catch (Throwable wbBase) {
                // empty catch block
            }
        }
        if (this.plugin.getConfig().getBoolean("mace.feedback.enable-sound", true)) {
            try {
                Sound s = Sound.valueOf((String)this.plugin.getConfig().getString("mace.feedback.sound", "ANVIL_LAND"));
                loc.getWorld().playSound(loc, s, 0.8f, 1.2f);
            }
            catch (Exception ex) {
                try {
                    loc.getWorld().playSound(loc, Sound.valueOf((String)"ANVIL_USE"), 0.8f, 1.2f);
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
        }
        if (bonus > 0.5 && this.plugin.getConfig().getBoolean("mace.feedback.action-message-enabled", true)) {
            String tpl = this.plugin.getConfig().getString("mace.feedback.action-message-template", "Crushing Blow! +{bonus} damage");
            String msg = tpl.replace("{bonus}", String.format("%.1f", bonus));
            attacker.sendMessage(ChatColor.GRAY + msg);
        }
    }
}

