package com.shadowlord.macewind.util;

import com.shadowlord.macewind.util.ItemUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class EnchantUtils {
    public static final String DENSITY = "Density";
    public static final String BREACH = "Breach";
    public static final String WIND_BURST = "Wind Burst";
    public static final int DENSITY_MAX = 5;
    public static final int BREACH_MAX = 4;
    public static final int WIND_BURST_MAX = 3;
    private static final String BOOK_MARKER = ChatColor.DARK_GRAY + "Mace Enchantment";
    private static final String[] ROMAN = new String[]{"", "I", "II", "III", "IV", "V"};

    private EnchantUtils() {
    }

    public static String toRoman(int level) {
        if (level < 1) {
            return "";
        }
        if (level <= 5) {
            return ROMAN[level];
        }
        return String.valueOf(level);
    }

    private static int fromRoman(String s) {
        if (s == null || s.trim().isEmpty()) {
            return 1;
        }
        s = s.trim().toUpperCase();
        for (int i = 1; i < ROMAN.length; ++i) {
            if (!ROMAN[i].equals(s)) continue;
            return i;
        }
        try {
            return Integer.parseInt(s);
        }
        catch (NumberFormatException e) {
            return 0;
        }
    }

    public static Map<String, Integer> getEnchantments(ItemStack item) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<String, Integer>();
        if (item == null || !item.hasItemMeta()) {
            return result;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return result;
        }
        block0: for (String line : meta.getLore()) {
            String plain = ChatColor.stripColor((String)line).trim();
            for (String name : new String[]{WIND_BURST, DENSITY, BREACH}) {
                if (!plain.toLowerCase().startsWith(name.toLowerCase())) continue;
                String remainder = plain.substring(name.length()).trim();
                int level = remainder.isEmpty() ? 1 : EnchantUtils.fromRoman(remainder);
                if (level <= 0) continue block0;
                result.put(name, level);
                continue block0;
            }
        }
        return result;
    }

    public static int getLevel(ItemStack item, String enchantName) {
        return EnchantUtils.getEnchantments(item).getOrDefault(enchantName, 0);
    }

    public static boolean addEnchantment(ItemStack item, String enchantName, int level) {
        if (item == null || !ItemUtils.isMace(item)) {
            return false;
        }
        if ((enchantName = EnchantUtils.normaliseName(enchantName)) == null) {
            return false;
        }
        if (level < 1 || level > EnchantUtils.getMaxLevel(enchantName)) {
            return false;
        }
        Map<String, Integer> existing = EnchantUtils.getEnchantments(item);
        if (enchantName.equals(DENSITY) && existing.containsKey(BREACH)) {
            return false;
        }
        if (enchantName.equals(BREACH) && existing.containsKey(DENSITY)) {
            return false;
        }
        existing.put(enchantName, level);
        EnchantUtils.writeLore(item, existing);
        return true;
    }

    public static boolean removeEnchantment(ItemStack item, String enchantName) {
        if (item == null) {
            return false;
        }
        if ((enchantName = EnchantUtils.normaliseName(enchantName)) == null) {
            return false;
        }
        Map<String, Integer> existing = EnchantUtils.getEnchantments(item);
        if (!existing.containsKey(enchantName)) {
            return false;
        }
        existing.remove(enchantName);
        EnchantUtils.writeLore(item, existing);
        return true;
    }

    public static void clearEnchantments(ItemStack item) {
        if (item == null) {
            return;
        }
        EnchantUtils.writeLore(item, new LinkedHashMap<String, Integer>());
    }

    public static ItemStack createEnchantBook(String enchantName, int level) {
        if ((enchantName = EnchantUtils.normaliseName(enchantName)) == null) {
            return null;
        }
        if (level < 1 || level > EnchantUtils.getMaxLevel(enchantName)) {
            return null;
        }
        ItemStack book = new ItemStack(Material.BOOK, 1);
        ItemMeta meta = book.getItemMeta();
        if (meta == null) {
            return null;
        }
        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + enchantName + " " + EnchantUtils.toRoman(level));
        ArrayList<String> lore = new ArrayList<String>();
        lore.add(BOOK_MARKER);
        lore.add("");
        lore.add(ChatColor.GRAY + EnchantUtils.getDescription(enchantName, level));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click on a Mace to apply");
        meta.setLore(lore);
        book.setItemMeta(meta);
        return book;
    }

    public static boolean isEnchantBook(ItemStack item) {
        if (item == null || item.getType() != Material.BOOK) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return false;
        }
        for (String line : meta.getLore()) {
            if (!line.equals(BOOK_MARKER)) continue;
            return true;
        }
        return false;
    }

    public static Object[] parseEnchantBook(ItemStack item) {
        if (!EnchantUtils.isEnchantBook(item)) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return null;
        }
        String name = ChatColor.stripColor((String)meta.getDisplayName()).trim();
        for (String ench : new String[]{WIND_BURST, DENSITY, BREACH}) {
            int level;
            if (!name.toLowerCase().startsWith(ench.toLowerCase())) continue;
            String remainder = name.substring(ench.length()).trim();
            int n = level = remainder.isEmpty() ? 1 : EnchantUtils.fromRoman(remainder);
            if (level <= 0) continue;
            return new Object[]{ench, level};
        }
        return null;
    }

    private static String getDescription(String enchantName, int level) {
        if (DENSITY.equals(enchantName)) {
            return "+" + String.format("%.1f", 0.5 * (double)level) + " smash damage per block fallen";
        }
        if (BREACH.equals(enchantName)) {
            return "Bypasses " + 15 * level + "% of target armor";
        }
        if (WIND_BURST.equals(enchantName)) {
            return "Launches you upward on smash hit (Lvl " + level + ")";
        }
        return "";
    }

    private static void writeLore(ItemStack item, Map<String, Integer> enchants) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        ArrayList<String> newLore = new ArrayList<String>();
        if (meta.hasLore() && meta.getLore() != null) {
            for (String line : meta.getLore()) {
                if (EnchantUtils.isEnchantLine(line)) continue;
                newLore.add(line);
            }
        }
        int idx = 0;
        for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
            newLore.add(idx++, ChatColor.GRAY + entry.getKey() + " " + EnchantUtils.toRoman(entry.getValue()));
        }
        meta.setLore(newLore.isEmpty() ? null : newLore);
        item.setItemMeta(meta);
    }

    private static boolean isEnchantLine(String line) {
        String plain = ChatColor.stripColor((String)line).trim().toLowerCase();
        return plain.startsWith("density") || plain.startsWith("breach") || plain.startsWith("wind burst");
    }

    public static String normaliseName(String input) {
        if (input == null) {
            return null;
        }
        String lower = input.trim().toLowerCase().replace("_", " ");
        if (lower.equals("density")) {
            return DENSITY;
        }
        if (lower.equals("breach")) {
            return BREACH;
        }
        if (lower.equals("wind burst") || lower.equals("windburst")) {
            return WIND_BURST;
        }
        return null;
    }

    public static int getMaxLevel(String name) {
        if (DENSITY.equals(name)) {
            return 5;
        }
        if (BREACH.equals(name)) {
            return 4;
        }
        if (WIND_BURST.equals(name)) {
            return 3;
        }
        return 0;
    }

    public static String[] allNames() {
        return new String[]{DENSITY, BREACH, WIND_BURST};
    }
}

