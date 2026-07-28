package com.shadowlord.macewind.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemUtils {
    private ItemUtils() {}

    private static final String TAG_MACE = "shadowlord_mace";
    private static final String TAG_WIND = "shadowlord_windcharge";

    // --- NMS helpers (v1_12_R1) ---
    private static Object toNmsItem(org.bukkit.inventory.ItemStack item) {
        if (item == null) return null;
        try {
            Class<?> craftItemStack = Class.forName("org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack");
            java.lang.reflect.Method asNmsCopy = craftItemStack.getMethod("asNMSCopy", org.bukkit.inventory.ItemStack.class);
            return asNmsCopy.invoke(null, item);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static org.bukkit.inventory.ItemStack toBukkitItem(Object nmsItem) {
        if (nmsItem == null) return null;
        try {
            Class<?> craftItemStack = Class.forName("org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack");
            java.lang.reflect.Method asBukkitCopy = craftItemStack.getMethod("asBukkitCopy", nmsItem.getClass());
            Object bukkit = asBukkitCopy.invoke(null, nmsItem);
            if (bukkit instanceof org.bukkit.inventory.ItemStack) {
                return (org.bukkit.inventory.ItemStack) bukkit;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object getOrCreateTag(org.bukkit.inventory.ItemStack item) {
        try {
            Object nms = toNmsItem(item);
            if (nms == null) return null;
            Class<?> nmsClass = nms.getClass();
            java.lang.reflect.Method hasTag = nmsClass.getMethod("hasTag");
            Boolean has = (Boolean) hasTag.invoke(nms);
            Class<?> nbtClass = Class.forName("net.minecraft.server.v1_12_R1.NBTTagCompound");
            if (!has) {
                java.lang.reflect.Method setTag = nmsClass.getMethod("setTag", nbtClass);
                Object tag = nbtClass.getConstructor().newInstance();
                setTag.invoke(nms, tag);
                return tag;
            }
            java.lang.reflect.Method getTag = nmsClass.getMethod("getTag");
            return getTag.invoke(nms);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static org.bukkit.inventory.ItemStack applyBooleanTag(org.bukkit.inventory.ItemStack item, String key) {
        if (item == null) return null;
        try {
            Object nms = toNmsItem(item);
            if (nms == null) return item;
            Object tag = getOrCreateTag(item);
            if (tag == null) return item;
            java.lang.reflect.Method setBoolean = tag.getClass().getMethod("setBoolean", String.class, boolean.class);
            setBoolean.invoke(tag, key, true);
            return toBukkitItem(nms);
        } catch (Throwable ignored) {
            return item;
        }
    }

    private static boolean hasBooleanTag(org.bukkit.inventory.ItemStack item, String key) {
        if (item == null) return false;
        try {
            Object nms = toNmsItem(item);
            if (nms == null) return false;
            Class<?> nmsClass = nms.getClass();
            java.lang.reflect.Method hasTag = nmsClass.getMethod("hasTag");
            Boolean has = (Boolean) hasTag.invoke(nms);
            if (!has) return false;
            java.lang.reflect.Method getTag = nmsClass.getMethod("getTag");
            Object tag = getTag.invoke(nms);
            if (tag == null) return false;
            java.lang.reflect.Method hasKey = tag.getClass().getMethod("hasKey", String.class);
            java.lang.reflect.Method getBoolean = tag.getClass().getMethod("getBoolean", String.class);
            Boolean exists = (Boolean) hasKey.invoke(tag, key);
            return exists != null && exists && (Boolean) getBoolean.invoke(tag, key);
        } catch (Throwable ignored) {
            return false;
        }
    }

    // --- Public helpers for other classes to use ---
    public static org.bukkit.inventory.ItemStack ensureMaceTag(org.bukkit.inventory.ItemStack item) {
        return applyBooleanTag(item, TAG_MACE);
    }

    public static org.bukkit.inventory.ItemStack ensureWindTag(org.bukkit.inventory.ItemStack item) {
        return applyBooleanTag(item, TAG_WIND);
    }

    // --- Detection ---
    public static boolean isMace(ItemStack item) {
        if (item == null) return false;
        if (item.getType() != Material.DIAMOND_AXE) return false;
        return hasBooleanTag(item, TAG_MACE);
    }

    public static boolean isWindCharge(ItemStack item) {
        if (item == null) return false;

        Material mat = item.getType();
        Material desired = Material.getMaterial("FIRE_CHARGE");
        Material fallback = Material.getMaterial("FIREBALL");

        boolean typeOk;
        if (desired != null) {
            typeOk = mat == desired;
        } else if (fallback != null) {
            typeOk = mat == fallback;
        } else {
            try {
                typeOk = mat == Material.FIREBALL;
            } catch (Exception | NoSuchFieldError ex) {
                return false;
            }
        }

        if (!typeOk) return false;
        return hasBooleanTag(item, TAG_WIND);
    }

    // --- Creation ---
    public static ItemStack makeMace() {
        ItemStack i = new ItemStack(Material.DIAMOND_AXE, 1);
        ItemMeta m = i.getItemMeta();
        if (m != null) {
            m.setDisplayName(ChatColor.RESET + "Mace");
            i.setItemMeta(m);
        }
        return applyBooleanTag(i, TAG_MACE);
    }

    public static ItemStack makeWindCharge() {
        Material mat = Material.getMaterial("FIRE_CHARGE");
        if (mat == null) mat = Material.getMaterial("FIREBALL");
        if (mat == null) {
            try {
                mat = Material.FIREBALL;
            } catch (Exception | NoSuchFieldError ex) {
                mat = null;
            }
        }
        if (mat == null) mat = Material.FIREBALL;

        ItemStack i = new ItemStack(mat, 1);
        ItemMeta m = i.getItemMeta();
        if (m != null) {
            m.setDisplayName(ChatColor.RESET + "Wind Charge");
            i.setItemMeta(m);
        }
        return applyBooleanTag(i, TAG_WIND);
    }
}
