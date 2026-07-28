package com.shadowlord.macewind;

import com.shadowlord.macewind.listeners.EnchantBookListener;
import com.shadowlord.macewind.listeners.FireworkBoostListener;
import com.shadowlord.macewind.listeners.MaceListener;
import com.shadowlord.macewind.listeners.WindChargeListener;
import com.shadowlord.macewind.util.EnchantUtils;
import com.shadowlord.macewind.util.ItemUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class MaceWindPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.getServer().getPluginManager().registerEvents((Listener)new MaceListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new WindChargeListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new FireworkBoostListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new EnchantBookListener(this), (Plugin)this);
        this.getLogger().info("MaceWind enabled.");
    }

    @Override
    public void onDisable() {
        this.getLogger().info("MaceWind disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("macewind")) {
            return false;
        }
        if (args.length == 0) {
            this.sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload": {
                return this.handleReload(sender);
            }
            case "givemace": {
                return this.handleGiveMace(sender, args);
            }
            case "givewind": {
                return this.handleGiveWind(sender, args);
            }
            case "givebook": {
                return this.handleGiveBook(sender, args);
            }
            case "enchant": {
                return this.handleEnchant(sender, args);
            }
            case "unenchant": {
                return this.handleUnenchant(sender, args);
            }
            case "enchants": {
                return this.handleListEnchants(sender);
            }
        }
        this.sendHelp(sender);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        ArrayList<String> completions;
        block11: {
            block12: {
                String partial;
                String sub;
                block14: {
                    block13: {
                        block10: {
                            if (!cmd.getName().equalsIgnoreCase("macewind")) {
                                return null;
                            }
                            completions = new ArrayList<String>();
                            if (args.length != 1) break block10;
                            String partial2 = args[0].toLowerCase();
                            for (String sub2 : new String[]{"reload", "givemace", "givewind", "givebook", "enchant", "unenchant", "enchants", "help"}) {
                                if (!sub2.startsWith(partial2)) continue;
                                completions.add(sub2);
                            }
                            break block11;
                        }
                        if (args.length != 2) break block12;
                        sub = args[0].toLowerCase();
                        partial = args[1].toLowerCase();
                        if (!sub.equals("enchant") && !sub.equals("givebook")) break block13;
                        for (String n : EnchantUtils.allNames()) {
                            String t = n.replace(" ", "_");
                            if (!t.toLowerCase().startsWith(partial)) continue;
                            completions.add(t);
                        }
                        break block11;
                    }
                    if (!sub.equals("unenchant")) break block14;
                    for (String n : EnchantUtils.allNames()) {
                        String t = n.replace(" ", "_");
                        if (!t.toLowerCase().startsWith(partial)) continue;
                        completions.add(t);
                    }
                    if (!"all".startsWith(partial)) break block11;
                    completions.add("all");
                    break block11;
                }
                if (!sub.equals("givemace") && !sub.equals("givewind")) break block11;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.getName().toLowerCase().startsWith(partial)) continue;
                    completions.add(p.getName());
                }
                break block11;
            }
            if (args.length == 3 && (args[0].equalsIgnoreCase("enchant") || args[0].equalsIgnoreCase("givebook"))) {
                String ench = EnchantUtils.normaliseName(args[1]);
                int max = ench != null ? EnchantUtils.getMaxLevel(ench) : 5;
                for (int i = 1; i <= max; ++i) {
                    String l = String.valueOf(i);
                    if (!l.startsWith(args[2])) continue;
                    completions.add(l);
                }
            } else if (args.length == 4 && args[0].equalsIgnoreCase("givebook")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.getName().toLowerCase().startsWith(args[3].toLowerCase())) continue;
                    completions.add(p.getName());
                }
            }
        }
        return completions;
    }

    private boolean handleReload(CommandSender sender) {
        if (!this.checkPerm(sender, "macewind.admin")) {
            return true;
        }
        this.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "[MaceWind] Config reloaded.");
        return true;
    }

    private boolean handleGiveMace(CommandSender sender, String[] args) {
        if (!this.checkPerm(sender, "macewind.admin")) {
            return true;
        }
        Player target = this.resolveTarget(sender, args, 1);
        if (target == null) {
            return true;
        }
        // ItemUtils.makeMace() already applies the NBT tag
        target.getInventory().addItem(new ItemStack[]{ItemUtils.makeMace()});
        target.sendMessage(ChatColor.GREEN + "[MaceWind] You received a Mace!");
        if (!target.equals((Object)sender)) {
            sender.sendMessage(ChatColor.GREEN + "[MaceWind] Gave a Mace to " + target.getName() + ".");
        }
        return true;
    }

    private boolean handleGiveWind(CommandSender sender, String[] args) {
        if (!this.checkPerm(sender, "macewind.admin")) {
            return true;
        }
        Player target = this.resolveTarget(sender, args, 1);
        if (target == null) {
            return true;
        }
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[2])));
            }
            catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount.");
                return true;
            }
        }
        ItemStack wc = ItemUtils.makeWindCharge();
        wc.setAmount(amount);
        target.getInventory().addItem(new ItemStack[]{wc});
        target.sendMessage(ChatColor.GREEN + "[MaceWind] You received " + amount + "x Wind Charge!");
        if (!target.equals((Object)sender)) {
            sender.sendMessage(ChatColor.GREEN + "[MaceWind] Gave " + amount + "x Wind Charge to " + target.getName() + ".");
        }
        return true;
    }

    private boolean handleGiveBook(CommandSender sender, String[] args) {
        int level;
        if (!this.checkPerm(sender, "macewind.admin")) {
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /macewind givebook <enchant> <level> [player]");
            sender.sendMessage(ChatColor.GRAY + "Enchants: Density, Breach, Wind_Burst");
            return true;
        }
        String enchantName = EnchantUtils.normaliseName(args[1]);
        if (enchantName == null) {
            sender.sendMessage(ChatColor.RED + "Unknown enchantment: " + args[1]);
            return true;
        }
        try {
            level = Integer.parseInt(args[2]);
        }
        catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid level.");
            return true;
        }
        if (level < 1 || level > EnchantUtils.getMaxLevel(enchantName)) {
            sender.sendMessage(ChatColor.RED + enchantName + " max level is " + EnchantUtils.toRoman(EnchantUtils.getMaxLevel(enchantName)) + ".");
            return true;
        }
        Player target = this.resolveTarget(sender, args, 3);
        if (target == null) {
            return true;
        }
        ItemStack book = EnchantUtils.createEnchantBook(enchantName, level);
        if (book == null) {
            sender.sendMessage(ChatColor.RED + "Failed to create book.");
            return true;
        }
        target.getInventory().addItem(new ItemStack[]{book});
        target.sendMessage(ChatColor.GREEN + "[MaceWind] You received a " + ChatColor.AQUA + enchantName + " " + EnchantUtils.toRoman(level) + ChatColor.GREEN + " enchantment book!");
        if (!target.equals((Object)sender)) {
            sender.sendMessage(ChatColor.GREEN + "[MaceWind] Gave book to " + target.getName() + ".");
        }
        return true;
    }

    private boolean handleEnchant(CommandSender sender, String[] args) {
        int level;
        if (!this.checkPerm(sender, "macewind.admin")) {
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /macewind enchant <enchant> <level>");
            return true;
        }
        Player player = (Player)sender;
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!ItemUtils.isMace(held)) {
            player.sendMessage(ChatColor.RED + "Hold a Mace!");
            return true;
        }
        String enchantName = EnchantUtils.normaliseName(args[1]);
        if (enchantName == null) {
            player.sendMessage(ChatColor.RED + "Unknown enchant: " + args[1]);
            return true;
        }
        try {
            level = Integer.parseInt(args[2]);
        }
        catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid level.");
            return true;
        }
        if (level < 1 || level > EnchantUtils.getMaxLevel(enchantName)) {
            player.sendMessage(ChatColor.RED + "Max level: " + EnchantUtils.toRoman(EnchantUtils.getMaxLevel(enchantName)));
            return true;
        }

        /*
         * EnchantUtils.addEnchantment historically may mutate the ItemStack or replace it.
         * We call it defensively, then reapply the mace NBT tag and set the item back into the player's hand.
         * If you later change EnchantUtils to return the modified ItemStack, prefer capturing and setting that directly.
         */
        boolean applied = EnchantUtils.addEnchantment(held, enchantName, level);
        if (applied) {
            // Reapply tag to whatever is currently in the player's hand (covers both mutation and replacement)
            ItemStack current = player.getInventory().getItemInMainHand();
            ItemStack ensured = ItemUtils.ensureMaceTag(current);
            player.getInventory().setItemInMainHand(ensured);
            player.sendMessage(ChatColor.GREEN + "Applied " + ChatColor.AQUA + enchantName + " " + EnchantUtils.toRoman(level) + ChatColor.GREEN + "!");
        } else {
            player.sendMessage(ChatColor.RED + "Cannot apply \u2014 conflicts with existing enchantment!");
        }
        return true;
    }

    private boolean handleUnenchant(CommandSender sender, String[] args) {
        if (!this.checkPerm(sender, "macewind.admin")) {
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /macewind unenchant <enchant|all>");
            return true;
        }
        Player player = (Player)sender;
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!ItemUtils.isMace(held)) {
            player.sendMessage(ChatColor.RED + "Hold a Mace!");
            return true;
        }
        if (args[1].equalsIgnoreCase("all")) {
            EnchantUtils.clearEnchantments(held);
            // ensure tag persists and set back
            ItemStack current = player.getInventory().getItemInMainHand();
            ItemStack ensured = ItemUtils.ensureMaceTag(current);
            player.getInventory().setItemInMainHand(ensured);
            player.sendMessage(ChatColor.GREEN + "Removed all enchantments.");
        } else {
            String name = EnchantUtils.normaliseName(args[1]);
            if (name == null) {
                player.sendMessage(ChatColor.RED + "Unknown enchant.");
                return true;
            }
            boolean removed = EnchantUtils.removeEnchantment(held, name);
            if (removed) {
                ItemStack current = player.getInventory().getItemInMainHand();
                ItemStack ensured = ItemUtils.ensureMaceTag(current);
                player.getInventory().setItemInMainHand(ensured);
                player.sendMessage(ChatColor.GREEN + "Removed " + name + ".");
            } else {
                player.sendMessage(ChatColor.RED + "Mace doesn't have " + name + ".");
            }
        }
        return true;
    }

    private boolean handleListEnchants(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        Player player = (Player)sender;
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!ItemUtils.isMace(held)) {
            player.sendMessage(ChatColor.RED + "Hold a Mace!");
            return true;
        }
        Map<String, Integer> enchants = EnchantUtils.getEnchantments(held);
        if (enchants.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "This Mace has no enchantments.");
        } else {
            player.sendMessage(ChatColor.GREEN + "--- Mace Enchantments ---");
            for (Map.Entry<String, Integer> e : enchants.entrySet()) {
                player.sendMessage(ChatColor.AQUA + " " + e.getKey() + " " + EnchantUtils.toRoman(e.getValue()));
            }
        }
        return true;
    }

    private boolean checkPerm(CommandSender sender, String perm) {
        if (!sender.hasPermission(perm)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return false;
        }
        return true;
    }

    private Player resolveTarget(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            Player t = Bukkit.getPlayerExact((String)args[index]);
            if (t == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[index]);
            }
            return t;
        }
        if (sender instanceof Player) {
            return (Player)sender;
        }
        sender.sendMessage(ChatColor.RED + "Specify a player from console.");
        return null;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "--- MaceWind Commands ---");
        sender.sendMessage(ChatColor.AQUA + "/mw reload" + ChatColor.GRAY + " - Reload config");
        sender.sendMessage(ChatColor.AQUA + "/mw givemace [player]" + ChatColor.GRAY + " - Give a Mace");
        sender.sendMessage(ChatColor.AQUA + "/mw givewind [player] [amount]" + ChatColor.GRAY + " - Give Wind Charges");
        sender.sendMessage(ChatColor.AQUA + "/mw givebook <enchant> <level> [player]" + ChatColor.GRAY + " - Give enchant book");
        sender.sendMessage(ChatColor.AQUA + "/mw enchant <enchant> <level>" + ChatColor.GRAY + " - Apply to held Mace");
        sender.sendMessage(ChatColor.AQUA + "/mw unenchant <enchant|all>" + ChatColor.GRAY + " - Remove from held Mace");
        sender.sendMessage(ChatColor.AQUA + "/mw enchants" + ChatColor.GRAY + " - List enchants on held Mace");
        sender.sendMessage(ChatColor.GRAY + "Enchants: Density (I-V), Breach (I-IV), Wind_Burst (I-III)");
    }
}
