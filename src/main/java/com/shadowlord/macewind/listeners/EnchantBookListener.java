package com.shadowlord.macewind.listeners;

import com.shadowlord.macewind.MaceWindPlugin;
import com.shadowlord.macewind.util.EnchantUtils;
import com.shadowlord.macewind.util.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class EnchantBookListener implements Listener {
    private final MaceWindPlugin plugin;

    public EnchantBookListener(MaceWindPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity who = event.getWhoClicked();
        if (!(who instanceof Player)) {
            return;
        }
        Player player = (Player) who;

        ItemStack cursor = event.getCursor();
        ItemStack clicked = event.getCurrentItem();

        // Defensive null checks
        if (cursor == null || clicked == null) {
            return;
        }

        // Only handle when cursor is an enchant book and clicked is a mace
        if (!EnchantUtils.isEnchantBook(cursor)) {
            return;
        }
        if (!ItemUtils.isMace(clicked)) {
            return;
        }

        // Parse book data
        Object[] bookData = EnchantUtils.parseEnchantBook(cursor);
        if (bookData == null) {
            return;
        }
        String enchantName = (String) bookData[0];
        int level = (Integer) bookData[1];

        // We are handling this click — cancel default behavior
        event.setCancelled(true);

        boolean success = EnchantUtils.addEnchantment(clicked, enchantName, level);
        if (success) {
            // Consume one book from the cursor
            if (cursor.getAmount() > 1) {
                cursor.setAmount(cursor.getAmount() - 1);
                event.setCursor(cursor);
            } else {
                event.setCursor(null);
            }

            // Reapply mace NBT tag to the clicked item and write it back to the inventory
            ItemStack ensured = ItemUtils.ensureMaceTag(clicked);

            // Try to put the ensured stack back into the exact clicked slot
            try {
                Inventory clickedInv = event.getClickedInventory();
                int slot = event.getSlot();
                if (clickedInv != null) {
                    clickedInv.setItem(slot, ensured);
                } else {
                    // Fallback: if clicked inventory is null, set player's held item if it matches
                    player.getInventory().setItemInMainHand(ensured);
                }
            } catch (Throwable t) {
                // Last-resort: set player's main hand
                player.getInventory().setItemInMainHand(ensured);
            }

            player.sendMessage(ChatColor.GREEN + "Applied " + ChatColor.AQUA + enchantName + " " + EnchantUtils.toRoman(level) + ChatColor.GREEN + " to your Mace!");
            try {
                player.playSound(player.getLocation(), Sound.valueOf("BLOCK_ENCHANTMENT_TABLE_USE"), 1.0f, 1.0f);
            } catch (Throwable t1) {
                try {
                    player.playSound(player.getLocation(), Sound.valueOf("LEVEL_UP"), 0.8f, 1.2f);
                } catch (Throwable ignored) {}
            }
        } else {
            String reason = EnchantUtils.normaliseName(enchantName) == null
                    ? "Unknown enchantment."
                    : (level < 1 || level > EnchantUtils.getMaxLevel(enchantName)
                        ? "Invalid level for " + enchantName + " (max " + EnchantUtils.toRoman(EnchantUtils.getMaxLevel(enchantName)) + ")."
                        : "Conflicts with an existing enchantment! (Density and Breach cannot coexist)");
            player.sendMessage(ChatColor.RED + "Cannot apply: " + reason);
        }
    }
}
