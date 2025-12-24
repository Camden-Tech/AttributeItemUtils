package com.baddcamden.attributeitemutils.commands;

import com.baddcamden.attributeitemutils.AttributeItemUtilsPlugin;
import com.baddcamden.attributeitemutils.gear.GearConfigLoader;
import com.baddcamden.attributeitemutils.gear.GearSlot;
import com.baddcamden.attributeitemutils.gear.KitConfig;
import com.baddcamden.attributeitemutils.gear.WeightedItem;
import com.baddcamden.attributeitemutils.items.GearService;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SpawnKitCommand implements CommandExecutor, TabCompleter {

    private final GearService gearService;
    private final GearConfigLoader gearConfigLoader;

    public SpawnKitCommand(AttributeItemUtilsPlugin plugin) {
        this.gearService = plugin.getGearService();
        this.gearConfigLoader = plugin.getGearConfigLoader();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("attributeitemutils.test.spawn")) {
            sender.sendMessage("You do not have permission to run this command.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can run this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("Usage: /" + label + " <entityType> <kit> [targetWeight] [steepness] [range]");
            return true;
        }

        EntityType type;
        try {
            type = EntityType.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException ex) {
            sender.sendMessage("Unknown entity type: " + args[0]);
            return true;
        }

        if (!type.isAlive()) {
            sender.sendMessage("Entity type must be a living entity.");
            return true;
        }

        String kitName = args[1];
        Optional<KitConfig> kitOpt = gearConfigLoader.getKit(kitName);
        if (kitOpt.isEmpty()) {
            sender.sendMessage("Unknown kit: " + kitName);
            return true;
        }

        KitConfig kit = kitOpt.get();
        double targetWeight = kit.targetWeight();
        double steepness = kit.steepness();
        double range = kit.range();

        if (args.length > 2) {
            try {
                targetWeight = Double.parseDouble(args[2]);
                if (args.length > 3) {
                    steepness = Double.parseDouble(args[3]);
                }
                if (args.length > 4) {
                    range = Double.parseDouble(args[4]);
                }
            } catch (NumberFormatException ex) {
                sender.sendMessage("Invalid number format in weight parameters.");
                return true;
            }
        }

        Map<GearSlot, List<WeightedItem>> itemsCopy = new EnumMap<>(GearSlot.class);
        kit.items().forEach((slot, list) -> itemsCopy.put(slot, new ArrayList<>(list)));
        KitConfig configuredKit = new KitConfig(kit.name(), targetWeight, steepness, range, itemsCopy);

        Location spawnLocation = player.getLocation();
        if (spawnLocation.getWorld() == null) {
            sender.sendMessage("Unable to determine your world to spawn the entity.");
            return true;
        }

        Entity spawned = spawnLocation.getWorld().spawnEntity(spawnLocation, type);
        if (!(spawned instanceof LivingEntity living)) {
            sender.sendMessage("Spawned entity is not a living entity; cannot apply kit.");
            return true;
        }

        gearService.applyKit(living, configuredKit);
        sender.sendMessage("Spawned " + type.name().toLowerCase() + " with kit " + kitName);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        switch (args.length) {
            case 1 -> {
                return Arrays.stream(EntityType.values())
                        .filter(EntityType::isAlive)
                        .map(type -> type.name().toLowerCase())
                        .filter(name -> name.startsWith(args[0].toLowerCase()))
                        .toList();
            }
            case 2 -> {
                return gearConfigLoader.getKits().keySet().stream()
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList();
            }
            case 3 -> {
                KitConfig kit = gearConfigLoader.getKit(args[1]).orElse(null);
                return kit == null ? List.of() : List.of(String.valueOf(kit.targetWeight()));
            }
            case 4 -> {
                KitConfig kit = gearConfigLoader.getKit(args[1]).orElse(null);
                return kit == null ? List.of() : List.of(String.valueOf(kit.steepness()));
            }
            case 5 -> {
                KitConfig kit = gearConfigLoader.getKit(args[1]).orElse(null);
                return kit == null ? List.of() : List.of(String.valueOf(kit.range()));
            }
            default -> {
                return List.of();
            }
        }
    }
}
