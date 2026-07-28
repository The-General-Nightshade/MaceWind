package com.shadowlord.macewind.listeners;

import com.shadowlord.macewind.MaceWindPlugin;
import com.shadowlord.macewind.util.EnchantUtils;
import com.shadowlord.macewind.util.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class EnchantBookListener
implements Listener {
    private final MaceWindPlugin plugin;

    public EnchantBookListener(MaceWindPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getWhoClicked();
        ItemStack cursor = event.getCursor();
        ItemStack clicked = event.getCurrentItem();
        if (!EnchantUtils.isEnchantBook(cursor)) {
            return;
        }
        if (!ItemUtils.isMace(clicked)) {
            return;
        }
        Object[] bookData = EnchantUtils.parseEnchantBook(cursor);
        if (bookData == null) {
            return;
        }
        String enchantName = (String)bookData[0];
        int level = (Integer)bookData[1];
        boolean success = EnchantUtils.addEnchantment(clicked, enchantName, level);
        if (success) {
            if (cursor.getAmount() > 1) {
                cursor.setAmount(cursor.getAmount() - 1);
                event.setCursor(cursor);
            } else {
                event.setCursor(null);
            }
            event.setCurrentItem(clicked);
            player.sendMessage(ChatColor.GREEN + "Applied " + ChatColor.AQUA + enchantName + " " + EnchantUtils.toRoman(level) + ChatColor.GREEN + " to your Mace!");
            try {
                player.playSound(player.getLocation(), Sound.valueOf((String)"BLOCK_ENCHANTMENT_TABLE_USE"), 1.0f, 1.0f);
            }
            catch (Throwable t1) {
                try {
                    player.playSound(player.getLocation(), Sound.valueOf((String)"LEVEL_UP"), 0.8f, 1.2f);
                }
                catch (Throwable throwable) {}
            }
        } else {
            String reason = EnchantUtils.normaliseName(enchantName) == null ? "Unknown enchantment." : (level < 1 || level > EnchantUtils.getMaxLevel(enchantName) ? "Invalid level for " + enchantName + " (max " + EnchantUtils.toRoman(EnchantUtils.getMaxLevel(enchantName)) + ")." : "Conflicts with an existing enchantment! (Density and Breach cannot coexist)");
            player.sendMessage(ChatColor.RED + "Cannot apply: " + reason);
        }
        event.setCancelled(true);
    }
}

