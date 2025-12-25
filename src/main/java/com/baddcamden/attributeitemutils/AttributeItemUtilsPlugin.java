package com.baddcamden.attributeitemutils;

import com.baddcamden.attributeitemutils.config.DropChanceConfigSource;
import com.baddcamden.attributeitemutils.gear.GearConfigLoader;
import com.baddcamden.attributeitemutils.gear.KitConfig;
import com.baddcamden.attributeitemutils.items.GearService;
import com.baddcamden.attributeitemutils.hooks.EntityChanceHook;
import com.baddcamden.attributeitemutils.hooks.EntityChanceHooks;
import com.baddcamden.attributeitemutils.commands.ApplyKitCommand;
import com.baddcamden.attributeitemutils.commands.SpawnKitCommand;
import me.baddcamden.attributeutils.AttributeUtilitiesPlugin;
import me.baddcamden.attributeutils.api.AttributeFacade;
import me.baddcamden.attributeutils.handler.entity.EntityAttributeHandler;
import me.baddcamden.attributeutils.handler.item.ItemAttributeHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Optional;

public class AttributeItemUtilsPlugin extends JavaPlugin {

    // Coordinates kit application and item generation across the plugin.
    private GearService gearService;
    // Delegates attribute modifier math and baseline wiring to downstream services.
    private AttributeFacade attributeFacade;
    private ItemAttributeHandler itemAttributeHandler;
    private EntityAttributeHandler entityAttributeHandler;
    // Loads gear definitions and weighted item pools from disk.
    private GearConfigLoader gearConfigLoader;
    // Supplies per-entity drop chance configuration overrides.
    private DropChanceConfigSource dropChanceConfigSource;
    private final EntityChanceHooks chanceHooks = new EntityChanceHooks();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveDefaultGearConfig();

        AttributeUtilitiesPlugin attributeUtils = resolveAttributeUtils();
        if (attributeUtils == null) {
            getLogger().severe("AttributeUtils dependency is missing or failed to load. Disabling AttributeItemUtils.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        attributeFacade = attributeUtils.getAttributeFacade();
        itemAttributeHandler = attributeUtils.getItemAttributeHandler();
        entityAttributeHandler = attributeUtils.getEntityAttributeHandler();
        gearConfigLoader = new GearConfigLoader(this);
        reloadPluginConfigs(attributeUtils);
        registerCommands();
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

    public void reloadPluginConfigs(AttributeUtilitiesPlugin attributeUtils) {
        saveDefaultConfig();
        saveDefaultGearConfig();
        reloadConfig();
        gearConfigLoader.reload();

        dropChanceConfigSource = DropChanceConfigSource.fromConfig(getConfig());
        gearService = new GearService(gearConfigLoader, dropChanceConfigSource, chanceHooks, attributeUtils, attributeFacade, itemAttributeHandler, entityAttributeHandler, getLogger());
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

    private AttributeUtilitiesPlugin resolveAttributeUtils() {
        var plugin = getServer().getPluginManager().getPlugin("AttributeUtils");
        if (plugin instanceof AttributeUtilitiesPlugin attributeUtils) {
            return attributeUtils;
        }

        return null;
    }

    private void registerCommands() {
        if (getCommand("aiukit") != null) {
            ApplyKitCommand command = new ApplyKitCommand(this);
            getCommand("aiukit").setExecutor(command);
            getCommand("aiukit").setTabCompleter(command);
        }
        if (getCommand("aiutestspawn") != null) {
            SpawnKitCommand command = new SpawnKitCommand(this);
            getCommand("aiutestspawn").setExecutor(command);
            getCommand("aiutestspawn").setTabCompleter(command);
        }
    }
}
