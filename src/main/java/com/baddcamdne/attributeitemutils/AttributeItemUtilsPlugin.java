package com.baddcamdne.attributeitemutils;

import com.baddcamdne.attributeitemutils.config.AttributeConfigSource;
import com.baddcamdne.attributeitemutils.config.DropChanceConfigSource;
import com.baddcamdne.attributeitemutils.config.EnchantmentConfigSource;
import com.baddcamdne.attributeitemutils.gear.GearConfigLoader;
import com.baddcamdne.attributeitemutils.gear.KitConfig;
import com.baddcamdne.attributeitemutils.items.AttributeService;
import com.baddcamdne.attributeitemutils.items.EnchantmentService;
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

        AttributeConfigSource attributeConfigSource = AttributeConfigSource.fromConfig(getConfig(), getLogger());
        EnchantmentConfigSource enchantmentConfigSource = EnchantmentConfigSource.fromConfig(getConfig(), getLogger());
        DropChanceConfigSource dropChanceConfigSource = DropChanceConfigSource.fromConfig(getConfig(), getLogger());

        gearService = new GearService(loader, new AttributeService(), new EnchantmentService(), attributeConfigSource, enchantmentConfigSource, dropChanceConfigSource);
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
