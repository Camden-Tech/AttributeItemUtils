package com.baddcamdne.attributeitemutils.commands;

import com.baddcamdne.attributeitemutils.AttributeItemUtilsPlugin;
import com.baddcamdne.attributeitemutils.config.EnchantmentConfig;
import com.baddcamdne.attributeitemutils.items.EnchantmentService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ApplyEnchantsCommand implements CommandExecutor, TabCompleter {

    private final EnchantmentService enchantmentService;
    private final AttributeItemUtilsPlugin plugin;

    public ApplyEnchantsCommand(AttributeItemUtilsPlugin plugin) {
        this.plugin = plugin;
        this.enchantmentService = plugin.getEnchantmentService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("attributeitemutils.test.enchants")) {
            sender.sendMessage("You do not have permission to run this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("Usage: /" + label + " <slot> <nights> [default|baseChance nightlyIncrease levelBonus maxChance]");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can run this command.");
            return true;
        }

        EquipmentSlot slot;
        try {
            slot = EquipmentSlot.valueOf(args[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            sender.sendMessage("Unknown equipment slot: " + args[0]);
            return true;
        }

        int nights;
        try {
            nights = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage("Invalid nights value: " + args[1]);
            return true;
        }

        EnchantmentConfig config;
        if (args.length >= 3 && args[2].equalsIgnoreCase("default")) {
            config = plugin.getEnchantmentConfigSource().defaultConfig();
        } else {
            if (args.length < 6) {
                sender.sendMessage("Usage: /" + label + " <slot> <nights> [default|baseChance nightlyIncrease levelBonus maxChance]");
                return true;
            }
            try {
                double baseChance = Double.parseDouble(args[2]);
                double nightlyIncrease = Double.parseDouble(args[3]);
                int levelBonus = Integer.parseInt(args[4]);
                double maxChance = Double.parseDouble(args[5]);
                config = new EnchantmentConfig(baseChance, nightlyIncrease, levelBonus, maxChance);
            } catch (NumberFormatException ex) {
                sender.sendMessage("Invalid number format in enchant config.");
                return true;
            }
        }

        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) {
            sender.sendMessage("Unable to resolve your equipment slots.");
            return true;
        }

        ItemStack item = switch (slot) {
            case HAND -> equipment.getItemInMainHand();
            case OFF_HAND -> equipment.getItemInOffHand();
            case HEAD -> equipment.getHelmet();
            case CHEST -> equipment.getChestplate();
            case LEGS -> equipment.getLeggings();
            case FEET -> equipment.getBoots();
        };

        if (item == null) {
            sender.sendMessage("No item found in " + slot.name().toLowerCase(Locale.ROOT) + " slot.");
            return true;
        }

        ItemStack updated = enchantmentService.applyEnchants(item, config, slot, nights);
        switch (slot) {
            case HAND -> equipment.setItemInMainHand(updated);
            case OFF_HAND -> equipment.setItemInOffHand(updated);
            case HEAD -> equipment.setHelmet(updated);
            case CHEST -> equipment.setChestplate(updated);
            case LEGS -> equipment.setLeggings(updated);
            case FEET -> equipment.setBoots(updated);
        }

        sender.sendMessage("Applied enchants to " + slot.name().toLowerCase(Locale.ROOT) + " using nights=" + nights);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        switch (args.length) {
            case 1 -> {
                return Arrays.stream(EquipmentSlot.values())
                        .map(slot -> slot.name().toLowerCase(Locale.ROOT))
                        .filter(name -> name.startsWith(args[0].toLowerCase(Locale.ROOT)))
                        .toList();
            }
            case 2 -> {
                return List.of("0", "10", "20").stream()
                        .filter(n -> n.startsWith(args[1]))
                        .toList();
            }
            case 3 -> {
                return List.of("default", String.valueOf(plugin.getEnchantmentConfigSource().defaultConfig().baseChance()))
                        .stream()
                        .filter(s -> s.startsWith(args[2]))
                        .toList();
            }
            case 4 -> {
                return List.of(String.valueOf(plugin.getEnchantmentConfigSource().defaultConfig().nightlyIncrease()))
                        .stream()
                        .filter(s -> s.startsWith(args[3]))
                        .toList();
            }
            case 5 -> {
                return List.of(String.valueOf(plugin.getEnchantmentConfigSource().defaultConfig().levelBonus()))
                        .stream()
                        .filter(s -> s.startsWith(args[4]))
                        .toList();
            }
            case 6 -> {
                return List.of(String.valueOf(plugin.getEnchantmentConfigSource().defaultConfig().maxChance()))
                        .stream()
                        .filter(s -> s.startsWith(args[5]))
                        .toList();
            }
            default -> {
                return List.of();
            }
        }
    }
}
