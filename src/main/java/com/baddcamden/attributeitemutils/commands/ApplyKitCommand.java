package com.baddcamden.attributeitemutils.commands;

import com.baddcamden.attributeitemutils.AttributeItemUtilsPlugin;
import com.baddcamden.attributeitemutils.gear.GearConfigLoader;
import com.baddcamden.attributeitemutils.gear.GearSlot;
import com.baddcamden.attributeitemutils.gear.KitConfig;
import com.baddcamden.attributeitemutils.gear.WeightedItem;
import com.baddcamden.attributeitemutils.items.GearService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ApplyKitCommand implements CommandExecutor, TabCompleter {

    private final GearService gearService;
    private final GearConfigLoader gearConfigLoader;

    /**
     * Creates a command executor that can apply predefined kits to players or other living entities.
     */
    public ApplyKitCommand(AttributeItemUtilsPlugin plugin) {
        this.gearService = plugin.getGearService();
        this.gearConfigLoader = plugin.getGearConfigLoader();
    }

    /**
     * Resolves the desired kit, target entity, and optional weight overrides before applying gear.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("attributeitemutils.test.kit")) {
            sender.sendMessage("You do not have permission to run this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("Usage: /" + label + " <kit> [player] [targetWeight] [steepness] [range]");
            return true;
        }

        String kitName = args[0];
        Optional<KitConfig> kitOpt = gearConfigLoader.getKit(kitName);
        if (kitOpt.isEmpty()) {
            sender.sendMessage("Unknown kit: " + kitName);
            return true;
        }

        int argIndex = 1;
        LivingEntity target = null;
        if (args.length > 1) {
            Player potential = Bukkit.getPlayerExact(args[1]);
            if (potential != null) {
                target = potential;
                argIndex = 2;
            }
        }

        if (target == null) {
            if (sender instanceof Player player) {
                target = player;
            } else {
                sender.sendMessage("You must specify a player to apply the kit to.");
                return true;
            }
        }

        KitConfig kit = kitOpt.get();
        double targetWeight = kit.targetWeight();
        double steepness = kit.steepness();
        double range = kit.range();

        if (args.length > argIndex) {
            try {
                targetWeight = Double.parseDouble(args[argIndex]);
                if (args.length > argIndex + 1) {
                    steepness = Double.parseDouble(args[argIndex + 1]);
                }
                if (args.length > argIndex + 2) {
                    range = Double.parseDouble(args[argIndex + 2]);
                }
            } catch (NumberFormatException ex) {
                sender.sendMessage("Invalid number format in weight parameters.");
                return true;
            }
        }

        Map<GearSlot, List<WeightedItem>> itemsCopy = new EnumMap<>(GearSlot.class);
        kit.items().forEach((slot, list) -> itemsCopy.put(slot, new ArrayList<>(list)));
        KitConfig configuredKit = new KitConfig(kit.name(), targetWeight, steepness, range, itemsCopy);

        gearService.applyKit(target, configuredKit);
        sender.sendMessage("Applied kit " + kitName + " to " + target.getName());
        return true;
    }

    /**
     * Offers tab completion hints for kit names, player targets, and kit weight parameters.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> kits = new ArrayList<>(gearConfigLoader.getKits().keySet());
        switch (args.length) {
            case 1 -> {
                return kits.stream()
                        .filter(k -> k.toLowerCase().startsWith(args[0].toLowerCase()))
                        .toList();
            }
            case 2 -> {
                List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                players.addAll(kits); // allow skipping player and jumping to weights
                return players.stream()
                        .filter(p -> p.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList();
            }
            case 3 -> {
                KitConfig kit = gearConfigLoader.getKit(args[0]).orElse(null);
                return kit == null ? List.of() : List.of(String.valueOf(kit.targetWeight()));
            }
            case 4 -> {
                KitConfig kit = gearConfigLoader.getKit(args[0]).orElse(null);
                return kit == null ? List.of() : List.of(String.valueOf(kit.steepness()));
            }
            case 5 -> {
                KitConfig kit = gearConfigLoader.getKit(args[0]).orElse(null);
                return kit == null ? List.of() : List.of(String.valueOf(kit.range()));
            }
            default -> {
                return List.of();
            }
        }
    }
}
