package com.baddcamden.attributeitemutils;

import com.baddcamden.attributeitemutils.config.AttributeBonus;
import com.baddcamden.attributeitemutils.config.AttributeAffixConfig;
import com.baddcamden.attributeitemutils.config.AttributeConfigSource;
import com.baddcamden.attributeitemutils.config.AttributeLoreConfig;
import com.baddcamden.attributeitemutils.config.AttributePoolConfig;
import com.baddcamden.attributeitemutils.config.DropChanceConfigSource;
import com.baddcamden.attributeitemutils.config.EnchantmentConfigSource;
import com.baddcamden.attributeitemutils.config.EnchantmentPoolConfig;
import com.baddcamden.attributeitemutils.gear.GearConfigLoader;
import com.baddcamden.attributeitemutils.gear.KitConfig;
import com.baddcamden.attributeitemutils.items.AttributeService;
import com.baddcamden.attributeitemutils.items.EnchantmentService;
import com.baddcamden.attributeitemutils.items.GearService;
import com.baddcamden.attributeitemutils.hooks.EntityChanceHook;
import com.baddcamden.attributeitemutils.hooks.EntityChanceHooks;
import com.baddcamden.attributeitemutils.commands.ApplyAttributesCommand;
import com.baddcamden.attributeitemutils.commands.ApplyEnchantsCommand;
import com.baddcamden.attributeitemutils.commands.ApplyKitCommand;
import com.baddcamden.attributeitemutils.commands.SpawnKitCommand;
import com.baddcamden.attributeutils.AttributeBaseline;
import com.baddcamden.attributeutils.AttributeDefinition;
import com.baddcamden.attributeutils.AttributeFacade;
import com.baddcamden.attributeutils.AttributeUtilitiesPlugin;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Optional;

public class AttributeItemUtilsPlugin extends JavaPlugin {

    // Coordinates kit application and item generation across the plugin.
    private GearService gearService;
    // Delegates attribute modifier math and baseline wiring to downstream services.
    private AttributeFacade attributeFacade;
    // Loads gear definitions and weighted item pools from disk.
    private GearConfigLoader gearConfigLoader;
    // Configures which attributes may roll and how they scale.
    private AttributePoolConfig attributePoolConfig;
    // Configures which enchants may roll and how they scale.
    private EnchantmentPoolConfig enchantmentPoolConfig;
    // Applies attribute rolls and lore to generated items.
    private AttributeService attributeService;
    // Applies enchantment rolls to generated items.
    private EnchantmentService enchantmentService;
    // Builds lore text and separators for attribute descriptions.
    private AttributeLoreConfig attributeLoreConfig;
    // Supplies per-entity attribute configuration overrides.
    private AttributeConfigSource attributeConfigSource;
    // Supplies per-entity enchantment configuration overrides.
    private EnchantmentConfigSource enchantmentConfigSource;
    // Supplies per-entity drop chance configuration overrides.
    private DropChanceConfigSource dropChanceConfigSource;
    private final EntityChanceHooks chanceHooks = new EntityChanceHooks();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveDefaultGearConfig();
        saveDefaultAttributePoolConfig();
        saveDefaultEnchantmentPoolConfig();

        attributeFacade = AttributeUtilitiesPlugin.getInstance().getAttributeFacade();
        gearConfigLoader = new GearConfigLoader(this);
        reloadPluginConfigs();
        registerCommands();
        getLogger().info("AttributeItemUtils enabled");
    }

    private void registerAttributeUtilities(AttributePoolConfig poolConfig) {
        for (AttributeBonus attributeBonus : poolConfig.attributes()) {
            Attribute attribute = attributeBonus.attribute();
            AttributeDefinition definition = new AttributeDefinition(attribute, AttributeModifier.Operation.MULTIPLY_SCALAR_1, 1.0);
            attributeFacade.registerDefinition(definition);
            attributeFacade.registerBaseline(new AttributeBaseline(attribute, 0.0));
        }
    }

    public boolean applyKit(LivingEntity entity, String kitName) {
        Optional<KitConfig> kit = gearService.getKit(kitName);
        if (kit.isEmpty()) {
            getLogger().warning("Unknown kit: " + kitName);
            return false;
        }
        gearService.applyKit(entity, kit.get());
        return true;
    }

    public void reloadPluginConfigs() {
        saveDefaultConfig();
        saveDefaultGearConfig();
        saveDefaultAttributePoolConfig();
        saveDefaultEnchantmentPoolConfig();
        reloadConfig();
        gearConfigLoader.reload();

        attributePoolConfig = AttributePoolConfig.load(this, getLogger());
        enchantmentPoolConfig = EnchantmentPoolConfig.load(this, getLogger());
        attributeConfigSource = AttributeConfigSource.fromConfig(getConfig());
        enchantmentConfigSource = EnchantmentConfigSource.fromConfig(getConfig());
        dropChanceConfigSource = DropChanceConfigSource.fromConfig(getConfig());
        AttributeAffixConfig attributeAffixConfig = AttributeAffixConfig.load(this);
        attributeLoreConfig = AttributeLoreConfig.load(this, getLogger());

        registerAttributeUtilities(attributePoolConfig);
        attributeService = new AttributeService(attributeFacade, attributeAffixConfig, attributePoolConfig, attributeLoreConfig);
        enchantmentService = new EnchantmentService(attributeFacade, enchantmentPoolConfig);
        gearService = new GearService(gearConfigLoader, attributeService, enchantmentService, attributeConfigSource, enchantmentConfigSource, dropChanceConfigSource, chanceHooks);
    }

    private void saveDefaultGearConfig() {
        File gearFile = new File(getDataFolder(), "Gear.yml");
        if (!gearFile.exists()) {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            saveResource("Gear.yml", false);
        }
    }

    private void saveDefaultAttributePoolConfig() {
        File poolFile = new File(getDataFolder(), "AttributePool.yml");
        if (!poolFile.exists()) {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            saveResource("AttributePool.yml", false);
        }
    }

    private void saveDefaultEnchantmentPoolConfig() {
        File poolFile = new File(getDataFolder(), "EnchantmentPool.yml");
        if (!poolFile.exists()) {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            saveResource("EnchantmentPool.yml", false);
        }
    }

    public EntityChanceHooks getChanceHooks() {
        return chanceHooks;
    }

    public void registerChanceHook(EntityChanceHook hook) {
        chanceHooks.register(hook);
    }

    public GearService getGearService() {
        return gearService;
    }

    public GearConfigLoader getGearConfigLoader() {
        return gearConfigLoader;
    }

    public AttributeService getAttributeService() {
        return attributeService;
    }

    public EnchantmentService getEnchantmentService() {
        return enchantmentService;
    }

    public AttributeConfigSource getAttributeConfigSource() {
        return attributeConfigSource;
    }

    public EnchantmentConfigSource getEnchantmentConfigSource() {
        return enchantmentConfigSource;
    }

    public DropChanceConfigSource getDropChanceConfigSource() {
        return dropChanceConfigSource;
    }

    private void registerCommands() {
        if (getCommand("aiukit") != null) {
            ApplyKitCommand command = new ApplyKitCommand(this);
            getCommand("aiukit").setExecutor(command);
            getCommand("aiukit").setTabCompleter(command);
        }
        if (getCommand("aiuattributes") != null) {
            ApplyAttributesCommand command = new ApplyAttributesCommand(this);
            getCommand("aiuattributes").setExecutor(command);
            getCommand("aiuattributes").setTabCompleter(command);
        }
        if (getCommand("aiuenchants") != null) {
            ApplyEnchantsCommand command = new ApplyEnchantsCommand(this);
            getCommand("aiuenchants").setExecutor(command);
            getCommand("aiuenchants").setTabCompleter(command);
        }
        if (getCommand("aiutestspawn") != null) {
            SpawnKitCommand command = new SpawnKitCommand(this);
            getCommand("aiutestspawn").setExecutor(command);
            getCommand("aiutestspawn").setTabCompleter(command);
        }
    }
}
