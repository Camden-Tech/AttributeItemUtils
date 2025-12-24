package com.baddcamdne.attributeitemutils;

import com.baddcamdne.attributeitemutils.config.AttributeConfigSource;
import com.baddcamdne.attributeitemutils.config.AttributeAffixConfig;
import com.baddcamdne.attributeitemutils.config.DropChanceConfigSource;
import com.baddcamdne.attributeitemutils.config.EnchantmentConfigSource;
import com.baddcamdne.attributeitemutils.gear.GearConfigLoader;
import com.baddcamdne.attributeitemutils.gear.KitConfig;
import com.baddcamdne.attributeitemutils.items.AttributeService;
import com.baddcamdne.attributeitemutils.items.EnchantmentService;
import com.baddcamdne.attributeitemutils.items.GearService;
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

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveDefaultGearConfig();

        attributeFacade = AttributeUtilitiesPlugin.getInstance().getAttributeFacade();
        registerAttributeUtilities();

        gearConfigLoader = new GearConfigLoader(this);
        reloadPluginConfigs();
        getLogger().info("AttributeItemUtils enabled");
    }

    private void registerAttributeUtilities() {
        for (Attribute attribute : AttributeService.CANDIDATE_ATTRIBUTES) {
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
        reloadConfig();
        gearConfigLoader.reload();

        AttributeConfigSource attributeConfigSource = AttributeConfigSource.fromConfig(getConfig(), getLogger());
        EnchantmentConfigSource enchantmentConfigSource = EnchantmentConfigSource.fromConfig(getConfig(), getLogger());
        DropChanceConfigSource dropChanceConfigSource = DropChanceConfigSource.fromConfig(getConfig(), getLogger());
        AttributeAffixConfig attributeAffixConfig = AttributeAffixConfig.load(this);

        gearService = new GearService(gearConfigLoader, new AttributeService(attributeFacade, attributeAffixConfig.prefixes(), attributeAffixConfig.suffixes()), new EnchantmentService(attributeFacade), attributeConfigSource, enchantmentConfigSource, dropChanceConfigSource);
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
}
