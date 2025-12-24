package com.baddcamdne.attributeitemutils;

import com.baddcamdne.attributeitemutils.config.AttributeBonus;
import com.baddcamdne.attributeitemutils.config.AttributeConfigSource;
import com.baddcamdne.attributeitemutils.config.AttributeAffixConfig;
import com.baddcamdne.attributeitemutils.config.AttributePoolConfig;
import com.baddcamdne.attributeitemutils.config.DropChanceConfigSource;
import com.baddcamdne.attributeitemutils.config.EnchantmentConfigSource;
import com.baddcamdne.attributeitemutils.config.EnchantmentPoolConfig;
import com.baddcamdne.attributeitemutils.gear.GearConfigLoader;
import com.baddcamdne.attributeitemutils.gear.KitConfig;
import com.baddcamdne.attributeitemutils.items.AttributeService;
import com.baddcamdne.attributeitemutils.items.EnchantmentService;
import com.baddcamdne.attributeitemutils.items.GearService;
import com.baddcamdne.attributeitemutils.hooks.EntityChanceHook;
import com.baddcamdne.attributeitemutils.hooks.EntityChanceHooks;
import com.baddcamdne.attributeitemutils.commands.ApplyAttributesCommand;
import com.baddcamdne.attributeitemutils.commands.ApplyEnchantsCommand;
import com.baddcamdne.attributeitemutils.commands.ApplyKitCommand;
import com.baddcamdne.attributeitemutils.commands.SpawnKitCommand;
import com.baddcamdne.attributeutils.AttributeBaseline;
import com.baddcamdne.attributeutils.AttributeDefinition;
import com.baddcamdne.attributeutils.AttributeFacade;
import com.baddcamdne.attributeutils.AttributeUtilitiesPlugin;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Optional;

public class AttributeItemUtilsPlugin extends JavaPlugin {

    private GearService gearService;
    private AttributeFacade attributeFacade;
    private GearConfigLoader gearConfigLoader;
    private AttributePoolConfig attributePoolConfig;
    private EnchantmentPoolConfig enchantmentPoolConfig;
    private AttributeService attributeService;
    private EnchantmentService enchantmentService;
    private AttributeConfigSource attributeConfigSource;
    private EnchantmentConfigSource enchantmentConfigSource;
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

        registerAttributeUtilities(attributePoolConfig);
        attributeService = new AttributeService(attributeFacade, attributeAffixConfig, attributePoolConfig);
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
