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
    private static net.minecraft.server.v1_12_R1.ItemStack asNms(org.bukkit.inventory.ItemStack item) {
        return org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack.asNMSCopy(item);
    }

    private static org.bukkit.inventory.ItemStack asBukkit(net.minecraft.server.v1_12_R1.ItemStack nms) {
        return org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack.asBukkitCopy(nms);
    }

    private static net.minecraft.server.v1_12_R1.NBTTagCompound getOrCreateTag(org.bukkit.inventory.ItemStack item) {
        if (item == null) return null;
        net.minecraft.server.v1_12_R1.ItemStack nms = asNms(item);
        if (nms == null) return null;
        if (!nms.hasTag()) nms.setTag(new net.minecraft.server.v1_12_R1.NBTTagCompound());
        return nms.getTag();
    }

    private static org.bukkit.inventory.ItemStack applyBooleanTag(org.bukkit.inventory.ItemStack item, String key) {
        if (item == null) return null;
        net.minecraft.server.v1_12_R1.ItemStack nms = asNms(item);
        if (nms == null) return item;
        if (!nms.hasTag()) nms.setTag(new net.minecraft.server.v1_12_R1.NBTTagCompound());
        nms.getTag().setBoolean(key, true);
        return asBukkit(nms);
    }

    private static boolean hasBooleanTag(org.bukkit.inventory.ItemStack item, String key) {
        if (item == null) return false;
        net.minecraft.server.v1_12_R1.ItemStack nms = asNms(item);
        if (nms == null || !nms.hasTag()) return false;
        net.minecraft.server.v1_12_R1.NBTTagCompound tag = nms.getTag();
        return tag.hasKey(key) && tag.getBoolean(key);
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
