package com.baddcamdne.attributeitemutils.commands;

import com.baddcamdne.attributeitemutils.AttributeItemUtilsPlugin;
import com.baddcamdne.attributeitemutils.config.AttributeConfig;
import com.baddcamdne.attributeitemutils.items.AttributeService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ApplyAttributesCommand implements CommandExecutor, TabCompleter {

    private final AttributeService attributeService;
    private final AttributeItemUtilsPlugin plugin;

    public ApplyAttributesCommand(AttributeItemUtilsPlugin plugin) {
        this.plugin = plugin;
        this.attributeService = plugin.getAttributeService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("attributeitemutils.test.attributes")) {
            sender.sendMessage("You do not have permission to run this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("Usage: /" + label + " <slot> <nights> [default|baseChance nightlyIncrease bonusPercent maxChance]");
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

        AttributeConfig config;
        if (args.length >= 3 && args[2].equalsIgnoreCase("default")) {
            config = plugin.getAttributeConfigSource().defaultConfig();
        } else {
            if (args.length < 6) {
                sender.sendMessage("Usage: /" + label + " <slot> <nights> [default|baseChance nightlyIncrease bonusPercent maxChance]");
                return true;
            }
            try {
                double baseChance = Double.parseDouble(args[2]);
                double nightlyIncrease = Double.parseDouble(args[3]);
                double bonusPercent = Double.parseDouble(args[4]);
                double maxChance = Double.parseDouble(args[5]);
                config = new AttributeConfig(baseChance, nightlyIncrease, bonusPercent, maxChance);
            } catch (NumberFormatException ex) {
                sender.sendMessage("Invalid number format in attribute config.");
                return true;
            }
        }

        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) {
            sender.sendMessage("Unable to resolve your equipment slots.");
            return true;
        }

        ItemStack item = getItem(equipment, slot);
        if (item == null) {
            sender.sendMessage("No item found in " + slot.name().toLowerCase(Locale.ROOT) + " slot.");
            return true;
        }

        ItemStack updated = attributeService.applyAttributes(item, config, slot, nights);
        setItem(equipment, slot, updated);
        sender.sendMessage("Applied attributes to " + slot.name().toLowerCase(Locale.ROOT) + " using nights=" + nights);
        return true;
    }

    private ItemStack getItem(EntityEquipment equipment, EquipmentSlot slot) {
        return switch (slot) {
            case HAND -> equipment.getItemInMainHand();
            case OFF_HAND -> equipment.getItemInOffHand();
            case HEAD -> equipment.getHelmet();
            case CHEST -> equipment.getChestplate();
            case LEGS -> equipment.getLeggings();
            case FEET -> equipment.getBoots();
        };
    }

    private void setItem(EntityEquipment equipment, EquipmentSlot slot, ItemStack stack) {
        switch (slot) {
            case HAND -> equipment.setItemInMainHand(stack);
            case OFF_HAND -> equipment.setItemInOffHand(stack);
            case HEAD -> equipment.setHelmet(stack);
            case CHEST -> equipment.setChestplate(stack);
            case LEGS -> equipment.setLeggings(stack);
            case FEET -> equipment.setBoots(stack);
        }
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
                List<String> suggestions = new ArrayList<>(List.of("default"));
                suggestions.add(String.valueOf(plugin.getAttributeConfigSource().defaultConfig().baseChance()));
                return suggestions.stream().filter(s -> s.startsWith(args[2])).collect(Collectors.toList());
            }
            case 4 -> {
                return List.of(String.valueOf(plugin.getAttributeConfigSource().defaultConfig().nightlyIncrease()))
                        .stream()
                        .filter(s -> s.startsWith(args[3]))
                        .toList();
            }
            case 5 -> {
                return List.of(String.valueOf(plugin.getAttributeConfigSource().defaultConfig().bonusPercent()))
                        .stream()
                        .filter(s -> s.startsWith(args[4]))
                        .toList();
            }
            case 6 -> {
                return List.of(String.valueOf(plugin.getAttributeConfigSource().defaultConfig().maxChance()))
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
