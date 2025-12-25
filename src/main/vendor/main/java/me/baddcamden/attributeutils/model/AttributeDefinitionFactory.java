package me.baddcamden.attributeutils.model;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.function.Consumer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Utility methods for building {@link AttributeDefinition} instances from configuration.
 * The helpers mirror the engine's concepts: each definition has a default baseline,
 * optional dynamic current baseline, and cap configuration that can be overridden via
 * keyed entries. The factory keeps all helpers stateless and focused on configuration
 * parsing to simplify consumption elsewhere in the plugin.
 */
public final class AttributeDefinitionFactory {

    private AttributeDefinitionFactory() {
    }

    /**
     * Builds a set of vanilla attribute definitions using configured defaults and caps.
     *
     * @param config configuration to read values from
     * @return ordered map of attribute id to definition to preserve declaration order
     */
    public static Map<String, AttributeDefinition> vanillaAttributes(FileConfiguration config) {
        Map<String, AttributeDefinition> definitions = new LinkedHashMap<>();

        definitions.put("generic.max_health", cappedAttribute(
                "generic.max_health",
                "Max Health",
                capConfigWithMin(config, "max_health", 100, 0.0001d),
                dynamic(config, "max_health", false),
                defaultBase(config, "max_health", 20)
        ));
        definitions.put("generic.follow_range", cappedAttribute(
                "generic.follow_range",
                "Follow Range",
                capConfig(config, "follow_range", 256),
                dynamic(config, "follow_range", false),
                defaultBase(config, "follow_range", 32)
        ));
        definitions.put("generic.attack_damage", cappedAttribute(
                "generic.attack_damage",
                "Attack Damage",
                capConfig(config, "attack_damage", 100),
                dynamic(config, "attack_damage", true),
                defaultBase(config, "attack_damage", 1)
        ));
        definitions.put("generic.attack_knockback", cappedAttribute(
                "generic.attack_knockback",
                "Attack Knockback",
                capConfig(config, "attack_knockback", 10),
                dynamic(config, "attack_knockback", true),
                defaultBase(config, "attack_knockback", 0)
        ));
        definitions.put("generic.attack_speed", cappedAttribute(
                "generic.attack_speed",
                "Attack Speed",
                capConfig(config, "attack_speed", 40),
                dynamic(config, "attack_speed", true),
                defaultBase(config, "attack_speed", 4)
        ));
        definitions.put("generic.movement_speed", cappedAttribute(
                "generic.movement_speed",
                "Movement Speed",
                capConfig(config, "movement_speed", 1),
                dynamic(config, "movement_speed", false),
                defaultBase(config, "movement_speed", 0.1)
        ));
        definitions.put("generic.flying_speed", cappedAttribute(
                "generic.flying_speed",
                "Flying Speed",
                capConfig(config, "flying_speed", 1),
                dynamic(config, "flying_speed", false),
                defaultBase(config, "flying_speed", 0.4)
        ));
        definitions.put("generic.armor", cappedAttribute(
                "generic.armor",
                "Armor",
                capConfig(config, "armor", 40),
                dynamic(config, "armor", true),
                defaultBase(config, "armor", 0)
        ));
        definitions.put("generic.armor_toughness", cappedAttribute(
                "generic.armor_toughness",
                "Armor Toughness",
                capConfig(config, "armor_toughness", 20),
                dynamic(config, "armor_toughness", true),
                defaultBase(config, "armor_toughness", 0)
        ));
        definitions.put("generic.luck", cappedAttribute(
                "generic.luck",
                "Luck",
                capConfig(config, "luck", 1024),
                dynamic(config, "luck", false),
                defaultBase(config, "luck", 0)
        ));
        definitions.put("generic.knockback_resistance", cappedAttribute(
                "generic.knockback_resistance",
                "Knockback Resistance",
                capConfig(config, "knockback_resistance", 1),
                dynamic(config, "knockback_resistance", true),
                defaultBase(config, "knockback_resistance", 0)
        ));
        definitions.put("generic.block_interaction_range", cappedAttribute(
                "generic.block_interaction_range",
                "Block Interaction Range",
                capConfig(config, "block_interaction_range", 128),
                dynamic(config, "block_interaction_range", false),
                defaultBase(config, "block_interaction_range", 5)
        ));
        definitions.put("generic.entity_interaction_range", cappedAttribute(
                "generic.entity_interaction_range",
                "Entity Interaction Range",
                capConfig(config, "entity_interaction_range", 64),
                dynamic(config, "entity_interaction_range", true),
                defaultBase(config, "entity_interaction_range", 3)
        ));
        definitions.put("generic.block_break_speed", cappedAttribute(
                "generic.block_break_speed",
                "Block Break Speed",
                capConfig(config, "block_break_speed", 1024),
                dynamic(config, "block_break_speed", false),
                defaultBase(config, "block_break_speed", 1)
        ));
        definitions.put("generic.mining_efficiency", cappedAttribute(
                "generic.mining_efficiency",
                "Mining Efficiency",
                capConfig(config, "mining_efficiency", 1024),
                dynamic(config, "mining_efficiency", false),
                defaultBase(config, "mining_efficiency", 1)
        ));
        definitions.put("generic.gravity", cappedAttribute(
                "generic.gravity",
                "Gravity",
                capConfig(config, "gravity", 10),
                dynamic(config, "gravity", false),
                defaultBase(config, "gravity", 1)
        ));
        definitions.put("generic.scale", cappedAttribute(
                "generic.scale",
                "Scale",
                capConfigWithMin(config, "scale", 10, 0.0001d),
                dynamic(config, "scale", false),
                defaultBase(config, "scale", 1)
        ));
        definitions.put("generic.step_height", cappedAttribute(
                "generic.step_height",
                "Step Height",
                capConfig(config, "step_height", 5),
                dynamic(config, "step_height", false),
                defaultBase(config, "step_height", 0.6)
        ));
        definitions.put("generic.safe_fall_distance", cappedAttribute(
                "generic.safe_fall_distance",
                "Safe Fall Distance",
                capConfig(config, "safe_fall_distance", 256),
                dynamic(config, "safe_fall_distance", false),
                defaultBase(config, "safe_fall_distance", 3)
        ));
        definitions.put("generic.fall_damage_multiplier", cappedAttribute(
                "generic.fall_damage_multiplier",
                "Fall Damage Multiplier",
                capConfig(config, "fall_damage_multiplier", 10),
                dynamic(config, "fall_damage_multiplier", false),
                defaultBase(config, "fall_damage_multiplier", 1)
        ));
        definitions.put("generic.jump_strength", cappedAttribute(
                "generic.jump_strength",
                "Jump Strength",
                capConfig(config, "jump_strength", 5),
                dynamic(config, "jump_strength", false),
                defaultBase(config, "jump_strength", 1)
        ));
        definitions.put("generic.sneaking_speed", cappedAttribute(
                "generic.sneaking_speed",
                "Sneaking Speed",
                capConfig(config, "sneaking_speed", 4),
                dynamic(config, "sneaking_speed", false),
                defaultBase(config, "sneaking_speed", 1)
        ));
        definitions.put("generic.movement_efficiency", cappedAttribute(
                "generic.movement_efficiency",
                "Movement Efficiency",
                capConfig(config, "movement_efficiency", 4),
                dynamic(config, "movement_efficiency", false),
                defaultBase(config, "movement_efficiency", 1)
        ));
        definitions.put("generic.water_movement_efficiency", cappedAttribute(
                "generic.water_movement_efficiency",
                "Water Movement Efficiency",
                capConfig(config, "water_movement_efficiency", 4),
                dynamic(config, "water_movement_efficiency", false),
                defaultBase(config, "water_movement_efficiency", 1)
        ));
        definitions.put("generic.explosion_knockback_resistance", cappedAttribute(
                "generic.explosion_knockback_resistance",
                "Explosion Knockback Resistance",
                capConfig(config, "explosion_knockback_resistance", 1),
                true,
                defaultBase(config, "explosion_knockback_resistance", 0)
        ));
        return definitions;
    }

    /**
     * Registers capped attributes found in configuration with the provided consumer. Each entry is
     * converted into a {@link CapConfig} that respects per-key overrides so callers can map
     * override keys (such as player identifiers) to distinct maxima.
     *
     * @param consumer destination for generated definitions
     * @param caps configuration section containing caps by key
     */
    public static void registerConfigCaps(Consumer<AttributeDefinition> consumer, ConfigurationSection caps) {
        registerConfigCaps(consumer, caps, Set.of());
    }

    /**
     * Registers capped attributes found in configuration while skipping known keys.
     *
     * @param consumer destination for generated definitions
     * @param caps configuration section containing caps by key
     * @param skipKeys normalized keys to ignore when creating definitions
     */
    public static void registerConfigCaps(Consumer<AttributeDefinition> consumer, ConfigurationSection caps, Set<String> skipKeys) {
        if (caps == null) {
            return;
        }

        for (String key : caps.getKeys(false)) {
            String normalizedKey = normalizeKey(key);
            if (skipKeys.contains(normalizedKey)) {
                continue;
            }
            ConfigurationSection capSection = caps.getConfigurationSection(key);
            double capValue = capSection == null ? caps.getDouble(key) : capSection.getDouble("max", caps.getDouble(key));
            // VAGUE/IMPROVEMENT NEEDED deciding precedence when both section and scalar exist is implicit; clarify desired priority
            consumer.accept(cappedAttribute(normalizedKey, humanize(normalizedKey), capValue));
        }
    }

    public static AttributeDefinition cappedAttribute(String id, String displayName, double capValue) {
        return cappedAttribute(id, displayName, capValue, false);
    }

    /**
     * Creates an attribute definition with a static cap and default current/base values equal to
     * the cap.
     */
    public static AttributeDefinition cappedAttribute(String id, String displayName, double capValue, boolean dynamic) {
        return cappedAttribute(id, displayName, capValue, dynamic, capValue);
    }

    /**
     * Creates an attribute definition with a static cap and explicit default values.
     */
    public static AttributeDefinition cappedAttribute(String id, String displayName, double capValue, boolean dynamic, double defaultValue) {
        CapConfig capConfig = new CapConfig(0, capValue, Map.of());
        return cappedAttribute(id, displayName, capConfig, dynamic, defaultValue);
    }

    /**
     * Creates an attribute definition with the provided cap configuration and default values.
     */
    public static AttributeDefinition cappedAttribute(String id,
                                                      String displayName,
                                                      CapConfig capConfig,
                                                      boolean dynamic,
                                                      double defaultValue) {
        return new AttributeDefinition(
                id.toLowerCase(Locale.ROOT),
                displayName,
                dynamic,
                defaultValue,
                defaultValue,
                capConfig,
                MultiplierApplicability.allowAllMultipliers()
        );
    }

    /**
     * Converts an identifier into a user-facing label with spaces and capitalized first letter.
     */
    private static String humanize(String id) {
        String withSpaces = id.replace('_', ' ').trim();
        if (withSpaces.isEmpty()) {
            return id;
        }
        return Character.toUpperCase(withSpaces.charAt(0)) + withSpaces.substring(1);
    }

    private static double defaultBase(FileConfiguration config, String attributeId, double fallback) {
        return defaultValue(config, attributeId, "default-base", fallback);
    }

    private static boolean dynamic(FileConfiguration config, String attributeId, boolean fallback) {
        String configKey = configKey(attributeId);
        ConfigurationSection defaults = config.getConfigurationSection("vanilla-attribute-defaults");
        if (defaults != null) {
            ConfigurationSection entry = defaults.getConfigurationSection(configKey);
            if (entry != null) {
                return entry.getBoolean("dynamic", fallback);
            }
            if (defaults.isSet(configKey)) {
                return defaults.getBoolean(configKey, fallback);
            }
        }

        if (config.isSet(configKey)) {
            return config.getBoolean(configKey, fallback);
        }

        return fallback;
    }

    /**
     * Resolves a default value for a configured attribute by searching nested defaults and
     * falling back to the attribute root or provided fallback.
     */
    private static double defaultValue(FileConfiguration config, String attributeId, String field, double fallback) {
        String configKey = configKey(attributeId);
        ConfigurationSection defaults = config.getConfigurationSection("vanilla-attribute-defaults");
        if (defaults != null) {
            ConfigurationSection entry = defaults.getConfigurationSection(configKey);
            if (entry != null) {
                return entry.getDouble(field, fallback);
            }
            if (defaults.isSet(configKey)) {
                return defaults.getDouble(configKey, fallback);
            }
        }

        if (config.isSet(configKey)) {
            return config.getDouble(configKey, fallback);
        }
        return fallback;
    }

    /**
     * Builds a {@link CapConfig} from the {@code global-attribute-caps} configuration section,
     * preserving override entries. Overrides use normalized keys so different dash/underscore
     * styles map to the same identifier.
     */
    private static CapConfig capConfig(FileConfiguration config, String attributeId, double defaultMax) {
        double min = 0;
        double max = defaultMax;
        Map<String, Double> overrides = new LinkedHashMap<>();
        ConfigurationSection caps = config.getConfigurationSection("global-attribute-caps");
        String configKey = configKey(attributeId);
        if (caps != null) {
            ConfigurationSection capSection = caps.getConfigurationSection(configKey);
            if (capSection != null) {
                min = capSection.getDouble("min", min);
                max = capSection.getDouble("max", max);
                ConfigurationSection overrideSection = capSection.getConfigurationSection("overrides");
                if (overrideSection != null) {
                    for (String key : overrideSection.getKeys(false)) {
                        overrides.put(normalizeKey(key), overrideSection.getDouble(key));
                    }
                }
            } else if (caps.isSet(configKey)) {
                max = caps.getDouble(configKey, max);
            }
        }

        return new CapConfig(min, max, overrides);
    }

    /**
     * Ensures the generated cap configuration respects a minimum allowed value in addition to
     * any configured minimum.
     */
    private static CapConfig capConfigWithMin(FileConfiguration config, String attributeId, double defaultMax, double minimum) {
        CapConfig base = capConfig(config, attributeId, defaultMax);
        double enforcedMin = Math.max(base.globalMin(), minimum);
        return new CapConfig(enforcedMin, base.globalMax(), base.overrideMaxValues());
    }

    private static String configKey(String attributeId) {
        return attributeId.toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static String normalizeKey(String key) {
        return key.toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
