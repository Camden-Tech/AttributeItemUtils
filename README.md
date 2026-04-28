# AttributeItemUtils

AttributeItemUtils is a companion plugin for [AttributeUtils](https://www.spigotmc.org/resources/attributeutils.108853/). It builds fully attributed equipment from YAML kits, applies lore/affixes, and exposes hooks that let other plugins override drop, attribute-roll, and enchantment chances at runtime.

## Requirements
- Minecraft / Spigot 1.21+
- AttributeUtils installed and enabled (hard dependency)

## Installation
1. Drop the AttributeItemUtils JAR into your server's `plugins/` folder alongside AttributeUtils.
2. Start the server to generate default configs:
   - `config.yml` (base attribute/enchant/drop chances)
   - `Gear.yml` (kit definitions and weighting)
   - `AttributeUffixes.yml` (name prefixes/suffixes)
   - `AttributeLore.yml` (lore templates)
   - `EnchantmentPool.yml` (allowed enchantments and weight tuning)
3. Edit the generated YAML files to match your loot/gear goals, then `/reload` or restart.

## Core features
- **Bell-curve weighted kits:** Each kit in `Gear.yml` defines weighted material pools per slot plus `target-weight`, `steepness`, and `range` values that control how often high-weight entries are picked. Items are built through `GearService` using these curves, so common pieces drop more frequently while rare pieces remain possible.
- **Attribute-first item generation:** Every built item rolls AttributeUtils definitions with per-slot trigger criteria, applies configurable modifier operations, and then reapplies persistent attributes so entities immediately benefit from their gear.
- **Dynamic affixes and lore:** Rolled attributes are normalized through `AttributeAffixConfig` and formatted by `AttributeLoreFormatter`, giving items themed prefixes/suffixes and lore that explain the active stats.
- **Configurable enchantment pool:** Optional enchantments are pulled from `EnchantmentPool.yml` using a tunable base chance and bonus level logic.
- **Runtime chance overrides:** Third-party plugins can register `EntityChanceHook` implementations to override drop, attribute, or enchant chances for specific mobs without touching YAML.

## Commands
- `/aiukit <kit> [player] [targetWeight] [steepness] [range]`
  - Applies a configured kit to the sender (or specified player) and optionally overrides bell-curve parameters for quick tuning.
- `/aiutestspawn <entityType> <kit> [targetWeight] [steepness] [range]`
  - Spawns the requested entity with the given kit applied, honoring optional weighting overrides.

Both commands require the matching `attributeitemutils.test.*` permissions (OP by default) and tab-complete kit names and weight defaults for fast testing.

## Configuration quickstart
```yaml
# Gear.yml
kits:
  bandit:
    target-weight: 5.0
    steepness: 1.2
    range: 4.0
    helmet:
      - LEATHER_HELMET:5
      - CHAINMAIL_HELMET:2
    hand:
      - IRON_SWORD:4
      - STONE_SWORD:8
    attribute-operations:
      example.multiplier_attribute: MULTIPLY
```
- Drop chances default to `drops.chance` in `config.yml`, but `EntityChanceHook` can override per-entity.
- Attribute rolls use `attributes.base-chance` and `attributes.bonus-percent` until capped at `attributes.max-chance`.
- Enchant rolls use `enchants.base-chance`, respecting `enchants.max-chance` and `enchants.level-bonus` when picking levels.
- API consumers can call `AttributeChanceConfig#chance(nightsPassed)` / `EnchantChanceConfig#chance(nightsPassed)` to scale chance by configurable `night-bonus-multiplier` values, or use the overloads that accept a manual multiplier.

## Hooking into AttributeItemUtils
Other plugins can change runtime behavior by registering hooks or invoking exposed services.

```java
public final class MyChanceHook implements EntityChanceHook {
    @Override
    public Optional<Double> dropChanceFor(EntityType type) {
        return type == EntityType.ZOMBIE ? Optional.of(0.25d) : Optional.empty();
    }
}

public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        AttributeItemUtilsPlugin aiu = getPlugin(AttributeItemUtilsPlugin.class);
        aiu.registerChanceHook(new MyChanceHook());
    }
}
```

### Useful entry points for integrations
- `AttributeItemUtilsPlugin`
  - `applyKit(LivingEntity entity, String kitName)`: Applies a named kit to an entity and returns whether it existed.
  - `reloadPluginConfigs(AttributeUtilitiesPlugin attributeUtils)`: Reloads all YAML configs and rebuilds services; call after editing files programmatically.
  - `getGearService()`: Returns the `GearService` used by commands and integrations to build gear.
  - `getGearConfigLoader()`: Exposes the `GearConfigLoader` so you can inspect available kits or refresh them.
  - `registerChanceHook(EntityChanceHook hook)` / `getChanceHooks()`: Registers or retrieves the `EntityChanceHooks` registry for overriding chances.
- `GearService`
  - `applyKit(LivingEntity entity, KitConfig kit)`: Applies a preloaded kit directly (including attribute/enchant rolls and drop chances).
  - `Optional<KitConfig> getKit(String name)`: Fetches a kit definition by name for validation or reuse.
- `GearConfigLoader`
  - `void reload()`: Re-reads `Gear.yml` from disk, merging defaults before parsing entries.
  - `Optional<KitConfig> getKit(String name)` / `Map<String, KitConfig> getKits()`: Access parsed kit definitions.
- `EntityChanceHook`
  - `dropChanceFor(EntityType type)`, `attributeChanceFor(EntityType type)`, `enchantChanceFor(EntityType type)`: Override per-entity probabilities when registered with the plugin.

These APIs let hook plugins change loot odds dynamically (e.g., boosting boss drops), inspect kit contents, or apply gear on demand without reimplementing AttributeUtils glue.
