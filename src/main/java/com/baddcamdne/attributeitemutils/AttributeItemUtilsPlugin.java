package com.baddcamdne.attributeitemutils;

import com.baddcamdne.attributeitemutils.config.AttributeConfig;
import com.baddcamdne.attributeitemutils.config.EnchantmentConfig;
import com.baddcamdne.attributeitemutils.gear.GearConfigLoader;
import com.baddcamdne.attributeitemutils.gear.KitConfig;
import com.baddcamdne.attributeitemutils.items.AttributeService;
import com.baddcamdne.attributeitemutils.items.GearService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public class AttributeItemUtilsPlugin extends JavaPlugin {

    private GearService gearService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        GearConfigLoader loader = new GearConfigLoader(this);
        loader.reload();

        AttributeConfig attributeConfig = AttributeConfig.fromConfig(getConfig());
        EnchantmentConfig enchantmentConfig = EnchantmentConfig.fromConfig(getConfig());

        gearService = new GearService(loader, new AttributeService(attributeConfig, enchantmentConfig));
        getLogger().info("AttributeItemUtils enabled");
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
}
