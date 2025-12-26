package com.baddcamden.attributeitemutils;

import com.baddcamden.attributeitemutils.config.AttributeChanceConfig;
import com.baddcamden.attributeitemutils.config.AttributeOperationConfig;
import com.baddcamden.attributeitemutils.config.DropChanceConfigSource;
import com.baddcamden.attributeitemutils.config.EnchantChanceConfig;
import com.baddcamden.attributeitemutils.gear.GearConfigLoader;
import com.baddcamden.attributeitemutils.gear.KitConfig;
import com.baddcamden.attributeitemutils.items.AttributeAffixConfig;
import com.baddcamden.attributeitemutils.items.AttributeLoreFormatter;
import com.baddcamden.attributeitemutils.items.EnchantmentPool;
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

/**
 * Primary plugin entry point responsible for bootstrapping configuration, wiring dependencies, and
 * exposing public services/commands for AttributeItemUtils.
 */
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
    private AttributeChanceConfig attributeChanceConfig;
    private AttributeOperationConfig attributeOperationConfig;
    private EnchantChanceConfig enchantChanceConfig;
    private AttributeAffixConfig attributeAffixConfig;
    private AttributeLoreFormatter attributeLoreFormatter;
    private EnchantmentPool enchantmentPool;
    private final EntityChanceHooks chanceHooks = new EntityChanceHooks();

    /**
     * Initializes the plugin, ensuring default resources exist, resolving dependencies, wiring
     * services, and registering commands.
     */
    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveDefaultGearConfig();
        saveDefaultEnchantPool();

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
        attributeAffixConfig = new AttributeAffixConfig(this, getLogger());
        attributeLoreFormatter = new AttributeLoreFormatter(this, getLogger());
        enchantmentPool = new EnchantmentPool(this, getLogger());
        reloadPluginConfigs(attributeUtils);
        registerCommands();
        getLogger().info("AttributeItemUtils enabled");
    }

    /**
     * Attempts to apply the named kit to the provided entity.
     *
     * @param entity  target entity to equip
     * @param kitName configured kit name to look up
     * @return {@code true} when the kit exists and is applied; {@code false} otherwise
     */
    public boolean applyKit(LivingEntity entity, String kitName) {
        Optional<KitConfig> kit = gearService.getKit(kitName);
        if (kit.isEmpty()) {
            getLogger().warning("Unknown kit: " + kitName);
            return false;
        }
        gearService.applyKit(entity, kit.get());
        return true;
    }

    /**
     * Reloads all plugin configuration files, refreshing gear definitions, lore, affixes, attribute
     * operations, and enchantment pools before rebuilding the {@link GearService}.
     *
     * @param attributeUtils resolved AttributeUtils dependency used when constructing services
     */
    public void reloadPluginConfigs(AttributeUtilitiesPlugin attributeUtils) {
        saveDefaultConfig();
        saveDefaultGearConfig();
        saveDefaultAffixConfig();
        saveDefaultLoreConfig();
        saveDefaultEnchantPool();
        reloadConfig();
        gearConfigLoader.reload();
        attributeAffixConfig.reload();
        attributeLoreFormatter.reload();
        if (enchantmentPool == null) {
            enchantmentPool = new EnchantmentPool(this, getLogger());
        }
        enchantmentPool.reload();

        dropChanceConfigSource = DropChanceConfigSource.fromConfig(getConfig());
        attributeChanceConfig = AttributeChanceConfig.fromConfig(getConfig());
        attributeOperationConfig = AttributeOperationConfig.fromConfig(getConfig(), getLogger());
        enchantChanceConfig = EnchantChanceConfig.fromConfig(getConfig());
        gearService = new GearService(gearConfigLoader, dropChanceConfigSource, chanceHooks, attributeUtils, attributeFacade, itemAttributeHandler, entityAttributeHandler, attributeAffixConfig, attributeLoreFormatter, attributeOperationConfig, attributeChanceConfig, enchantChanceConfig, enchantmentPool, getLogger());
    }

    /**
     * Ensures Gear.yml exists on disk by copying the bundled default when missing.
     */
    private void saveDefaultGearConfig() {
        File gearFile = new File(getDataFolder(), "Gear.yml");
        if (!gearFile.exists()) {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            saveResource("Gear.yml", false);
        }
    }

    /**
     * Ensures AttributeUffixes.yml exists on disk by copying the bundled default when missing.
     */
    private void saveDefaultAffixConfig() {
        File affixFile = new File(getDataFolder(), "AttributeUffixes.yml");
        if (!affixFile.exists()) {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            saveResource("AttributeUffixes.yml", false);
        }
    }

    /**
     * Ensures AttributeLore.yml exists on disk by copying the bundled default when missing.
     */
    private void saveDefaultLoreConfig() {
        File loreFile = new File(getDataFolder(), "AttributeLore.yml");
        if (!loreFile.exists()) {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            saveResource("AttributeLore.yml", false);
        }
    }

    /**
     * Ensures EnchantmentPool.yml exists on disk by copying the bundled default when missing.
     */
    private void saveDefaultEnchantPool() {
        File enchantFile = new File(getDataFolder(), "EnchantmentPool.yml");
        if (!enchantFile.exists()) {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            saveResource("EnchantmentPool.yml", false);
        }
    }

    /**
     * Exposes the active chance hook registry for third-party integrations.
     */
    public EntityChanceHooks getChanceHooks() {
        return chanceHooks;
    }

    /**
     * Registers a new {@link EntityChanceHook} for runtime drop/attribute/enchant overrides.
     */
    public void registerChanceHook(EntityChanceHook hook) {
        chanceHooks.register(hook);
    }

    /**
     * Returns the current {@link GearService} instance used by commands and integrations.
     */
    public GearService getGearService() {
        return gearService;
    }

    /**
     * Returns the loader responsible for reading kit definitions from configuration.
     */
    public GearConfigLoader getGearConfigLoader() {
        return gearConfigLoader;
    }

    /**
     * Attempts to resolve the AttributeUtils dependency via plugin name or class-based lookup.
     *
     * @return an enabled AttributeUtilitiesPlugin instance when available; otherwise {@code null}
     */
    private AttributeUtilitiesPlugin resolveAttributeUtils() {
        var pluginManager = getServer().getPluginManager();

        // Prefer a name lookup so we do not rely on classloader equality when plugins are reloaded.
        for (String candidate : new String[]{"AttributeUtils", "AttributeUtilities"}) {
            var plugin = pluginManager.getPlugin(candidate);
            if (plugin instanceof AttributeUtilitiesPlugin attributeUtils) {
                if (!plugin.isEnabled()) {
                    getLogger().severe(candidate + " is installed but not enabled.");
                    return null;
                }
                return attributeUtils;
            }
        }

        // Fallback to Bukkit's class-based lookup in case the dependency was renamed but uses the same API.
        try {
            AttributeUtilitiesPlugin attributeUtils = JavaPlugin.getPlugin(AttributeUtilitiesPlugin.class);
            if (attributeUtils != null && attributeUtils.isEnabled()) {
                return attributeUtils;
            }
        } catch (IllegalStateException | ClassCastException ignored) {
            // Bukkit may throw if the plugin has not finished loading or if a classloader mismatch occurs.
        }

        return null;
    }

    /**
     * Registers the test commands provided by AttributeItemUtils when they are declared.
     */
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
