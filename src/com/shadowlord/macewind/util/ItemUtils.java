package com.shadowlord.macewind.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemUtils {
    private ItemUtils() {
    }

    public static boolean isMace(ItemStack item) {
        if (item == null) {
            return false;
        }
        if (item.getType() != Material.DIAMOND_AXE) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) {
            return false;
        }
        return ChatColor.stripColor((String)meta.getDisplayName()).equalsIgnoreCase("Mace");
    }

    public static boolean isWindCharge(ItemStack item) {
        boolean typeOk;
        if (item == null) {
            return false;
        }
        Material mat = item.getType();
        Material desired = Material.getMaterial((String)"FIRE_CHARGE");
        Material fallback = Material.getMaterial((String)"FIREBALL");
        if (desired != null) {
            typeOk = mat == desired;
        } else if (fallback != null) {
            typeOk = mat == fallback;
        } else {
            try {
                typeOk = mat == Material.FIREBALL;
            }
            catch (Exception | NoSuchFieldError ex) {
                return false;
            }
        }
        if (!typeOk) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) {
            return false;
        }
        return ChatColor.stripColor((String)meta.getDisplayName()).equalsIgnoreCase("Wind Charge");
    }

    public static ItemStack makeMace() {
        ItemStack i = new ItemStack(Material.DIAMOND_AXE, 1);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(ChatColor.RESET + "Mace");
        i.setItemMeta(m);
        return i;
    }

    public static ItemStack makeWindCharge() {
        Material mat = Material.getMaterial((String)"FIRE_CHARGE");
        if (mat == null) {
            mat = Material.getMaterial((String)"FIREBALL");
        }
        if (mat == null) {
            try {
                mat = Material.FIREBALL;
            }
            catch (Exception | NoSuchFieldError ex) {
                mat = null;
            }
        }
        if (mat == null) {
            mat = Material.FIREBALL;
        }
        ItemStack i = new ItemStack(mat, 1);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(ChatColor.RESET + "Wind Charge");
        i.setItemMeta(m);
        return i;
    }
}

