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

    /** Responsible for applying configured gear kits to entities. */
    private GearService gearService;

    /** Shared attribute facade provided by AttributeUtilitiesPlugin. */
    private AttributeFacade attributeFacade;

    /** Reloadable loader for the Gear.yml configuration. */
    private GearConfigLoader gearConfigLoader;

    /** In-memory representation of AttributePool.yml. */
    private AttributePoolConfig attributePoolConfig;

    /** In-memory representation of EnchantmentPool.yml. */
    private EnchantmentPoolConfig enchantmentPoolConfig;

    /** Service applying attribute bonuses and lore to items. */
    private AttributeService attributeService;

    /** Service applying enchantment selections to items. */
    private EnchantmentService enchantmentService;

    /** Lore configuration applied when attributes are added to items. */
    private AttributeLoreConfig attributeLoreConfig;

    /** Reader for attribute configuration blocks in config.yml. */
    private AttributeConfigSource attributeConfigSource;

    /** Reader for enchantment configuration blocks in config.yml. */
    private EnchantmentConfigSource enchantmentConfigSource;

    /** Reader for drop chance configuration blocks in config.yml. */
    private DropChanceConfigSource dropChanceConfigSource;

    /** Registry for external hooks that can influence drop chances. */
    private final EntityChanceHooks chanceHooks = new EntityChanceHooks();

    @Override
    public void onEnable() {
        // Bootstrap configuration and dependent services before exposing commands.
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
        //VAGUE/IMPROVEMENT NEEDED Re-registers definitions on every reload; unclear whether AttributeFacade deduplicates.
        for (AttributeBonus attributeBonus : poolConfig.attributes()) {
            Attribute attribute = attributeBonus.attribute();
            AttributeDefinition definition = new AttributeDefinition(attribute, AttributeModifier.Operation.MULTIPLY_SCALAR_1, 1.0);
            attributeFacade.registerDefinition(definition);
            attributeFacade.registerBaseline(new AttributeBaseline(attribute, 0.0));
        }
    }

    public boolean applyKit(LivingEntity entity, String kitName) {
        // Attempt to apply a kit and log if it could not be found for the given name.
        Optional<KitConfig> kit = gearService.getKit(kitName);
        if (kit.isEmpty()) {
            getLogger().warning("Unknown kit: " + kitName);
            return false;
        }
        gearService.applyKit(entity, kit.get());
        return true;
    }

    public void reloadPluginConfigs() {
        // Reload every known configuration file and rebuild dependent services.
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
