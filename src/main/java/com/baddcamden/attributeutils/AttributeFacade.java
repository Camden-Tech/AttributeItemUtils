package com.baddcamden.attributeutils;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.google.common.collect.Multimap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AttributeFacade {
    private static final String NAMESPACE = "attributeitemutils";
    private static final NamespacedKey PERSISTED_MODIFIERS_KEY = new NamespacedKey(NAMESPACE, "stored-modifiers");
    private static final NamespacedKey PERSISTED_ATTRIBUTE_KEY = new NamespacedKey(NAMESPACE, "attribute");
    private static final NamespacedKey PERSISTED_AMOUNT_KEY = new NamespacedKey(NAMESPACE, "amount");
    private static final NamespacedKey PERSISTED_SLOT_KEY = new NamespacedKey(NAMESPACE, "slot");
    private static final String GENERIC_SCALE_KEY = "generic.scale";
    private static final String GENERIC_FALL_DAMAGE_MULTIPLIER_KEY = "generic.fall_damage_multiplier";

    private static final Attribute GENERIC_SCALE = findAttribute("GENERIC_SCALE");
    private static final Attribute GENERIC_FALL_DAMAGE_MULTIPLIER = findAttribute("GENERIC_FALL_DAMAGE_MULTIPLIER");

    private final Map<Attribute, AttributeDefinition> definitions = new HashMap<Attribute, AttributeDefinition>();
    private final Map<Attribute, AttributeBaseline> baselines = new HashMap<Attribute, AttributeBaseline>();
    private static final String ATTRIBUTEUTILS_PREFIX = "attributeutils:";
    private Logger logger = Logger.getLogger(AttributeFacade.class.getName());

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void registerDefinition(AttributeDefinition definition) {
        definitions.put(definition.attribute(), definition);
    }

    public void registerBaseline(AttributeBaseline baseline) {
        baselines.put(baseline.attribute(), baseline);
    }

    public double computeAmount(Attribute attribute, double baseAmount) {
        double amount = baseAmount;
        if (isAttributeMatch(attribute, GENERIC_SCALE, GENERIC_SCALE_KEY)) {
            amount = Math.cbrt(1 + baseAmount) - 1;
        } else if (isAttributeMatch(attribute, GENERIC_FALL_DAMAGE_MULTIPLIER, GENERIC_FALL_DAMAGE_MULTIPLIER_KEY)) {
            amount = -baseAmount;
        }

        AttributeDefinition definition = definitions.get(attribute);
        if (definition == null) {
            return amount;
        }
        return definition.applyCap(amount);
    }

    private static Attribute findAttribute(String name) {
        try {
            return Attribute.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean isAttributeMatch(Attribute attribute, Attribute optionalEnum, String key) {
        if (attribute == null) {
            return false;
        }

        if (optionalEnum != null) {
            return attribute == optionalEnum;
        }

        return attribute.getKey().getKey().equalsIgnoreCase(key);
    }

    public void refresh(ItemStack stack, EquipmentSlot slot) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }

        Multimap<Attribute, AttributeModifier> defaults = defaultModifiers(stack, slot);
        Multimap<Attribute, AttributeModifier> modifiers = meta.getAttributeModifiers();
        Multimap<Attribute, AttributeModifier> initialModifiers = modifiers;
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("[ATTR] Refreshing modifiers for " + stack.getType() + " slot=" + slot + " existing=" + summarize(modifiers));
        }
        if (modifiers == null || modifiers.isEmpty()) {
            if (defaults != null) {
                defaults.forEach(meta::addAttributeModifier);
            }
            modifiers = meta.getAttributeModifiers();
        }

        Multimap<Attribute, AttributeModifier> finalModifiers = modifiers;

        // Ensure vanilla baselines are reattached even when only plugin-authored modifiers are present.
        if (defaults != null) {
            defaults.forEach((attribute, modifier) -> {
                if (finalModifiers == null || !hasNonPluginModifier(finalModifiers.get(attribute))) {
                    meta.addAttributeModifier(attribute, modifier);
                }
            });
        }
        baselines.forEach((attribute, baseline) -> {
            AttributeDefinition definition = definitions.get(attribute);
            if (definition != null) {
                meta.addAttributeModifier(attribute, baseline.asModifier(definition, slot));
            }
        });
        applyPersistedModifiers(stack, meta);
        if (defaults == null && (initialModifiers == null || initialModifiers.isEmpty())) {
            logger.warning("[ATTR] No vanilla defaults discovered for " + stack.getType() + " slot=" + slot
                    + "; refresh may strip attack stats. Current modifiers=" + summarize(meta.getAttributeModifiers()));
        } else if (logger.isLoggable(Level.FINE)) {
            logger.fine(() -> "[ATTR] Final modifiers after refresh for " + stack.getType() + " slot=" + slot
                    + " -> " + summarize(meta.getAttributeModifiers()));
        }
        stack.setItemMeta(meta);
    }

    private Multimap<Attribute, AttributeModifier> defaultModifiers(ItemStack stack, EquipmentSlot slot) {
        ItemMeta vanillaMeta = new ItemStack(stack.getType()).getItemMeta();
        if (vanillaMeta != null) {
            Multimap<Attribute, AttributeModifier> defaultForSlot = vanillaMeta.getAttributeModifiers(slot);
            if (defaultForSlot != null && !defaultForSlot.isEmpty()) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("[ATTR] Using vanilla default modifiers for " + stack.getType() + " slot=" + slot
                            + " -> " + summarize(defaultForSlot));
                }
                return defaultForSlot;
            }

            Multimap<Attribute, AttributeModifier> allDefaults = vanillaMeta.getAttributeModifiers();
            if (allDefaults != null && !allDefaults.isEmpty()) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("[ATTR] Using vanilla default modifiers (any slot) for " + stack.getType()
                            + " -> " + summarize(allDefaults));
                }
                return allDefaults;
            }
        }

        // Fall back to any modifiers currently on the stack so we never strip out vanilla values when the
        // Bukkit item factory fails to expose built-in defaults for the material. This mirrors the vendor
        // behavior that always reattaches vanilla baselines before layering plugin modifiers.
        ItemMeta existingMeta = stack.getItemMeta();
        if (existingMeta != null) {
            Multimap<Attribute, AttributeModifier> existing = slot == null
                    ? existingMeta.getAttributeModifiers()
                    : existingMeta.getAttributeModifiers(slot);
            if (existing != null && !existing.isEmpty()) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("[ATTR] Falling back to existing modifiers on stack for " + stack.getType()
                            + " slot=" + slot + " -> " + summarize(existing));
                }
                return existing;
            }
        }

        logger.fine("[ATTR] No default modifiers found for " + stack.getType() + " slot=" + slot);
        return null;
    }

    public void mutate(ItemStack stack, Attribute attribute, double baseAmount, EquipmentSlot slot) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }

        AttributeDefinition definition = definitions.get(attribute);
        if (definition == null) {
            return;
        }

        Multimap<Attribute, AttributeModifier> defaults = defaultModifiers(stack, slot);
        Multimap<Attribute, AttributeModifier> existing = meta.getAttributeModifiers();
        final Multimap<Attribute, AttributeModifier> existingBefore = existing;

        // Ensure vanilla attributes stick around before we apply plugin modifiers, mirroring the merge
        // patterns recommended by the community to avoid NBT replacement wiping AttributeModifiers.
        if (defaults != null) {
            defaults.forEach((vanillaAttribute, modifier) -> {
                Iterable<AttributeModifier> current = existingBefore == null ? null : existingBefore.get(vanillaAttribute);
                if (!hasNonPluginModifier(current)) {
                    meta.addAttributeModifier(vanillaAttribute, modifier);
                }
            });
        }

        // Remove any previously-applied plugin modifiers for the same attribute/slot so the deterministic
        // UUIDs we generate do not collide and cause vanilla modifiers to be dropped.
        existing = meta.getAttributeModifiers();
        if (existing != null) {
            for (AttributeModifier modifier : List.copyOf(existing.get(attribute))) {
                if (isPluginModifier(modifier) && (slot == null || modifier.getSlot() == null || modifier.getSlot() == slot)) {
                    meta.removeAttributeModifier(attribute, modifier);
                }
            }
        }

        persistModifier(meta, attribute, baseAmount, slot);

        AttributeModifier modifier = definition.newModifier(computeAmount(attribute, baseAmount), slot);
        meta.addAttributeModifier(attribute, modifier);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("[ATTR] Applied modifier " + modifier + " to " + stack.getType() + " for attribute " + attribute
                    + " resulting modifiers=" + summarize(meta.getAttributeModifiers()));
        }
        stack.setItemMeta(meta);
    }

    private void persistModifier(ItemMeta meta, Attribute attribute, double baseAmount, EquipmentSlot slot) {
        PersistentDataContainer root = meta.getPersistentDataContainer();
        PersistentDataContainer modifiers = root.get(PERSISTED_MODIFIERS_KEY, PersistentDataType.TAG_CONTAINER);
        if (modifiers == null) {
            modifiers = root.getAdapterContext().newPersistentDataContainer();
        }

        PersistentDataContainer entry = root.getAdapterContext().newPersistentDataContainer();
        entry.set(PERSISTED_ATTRIBUTE_KEY, PersistentDataType.STRING, attribute.name());
        entry.set(PERSISTED_AMOUNT_KEY, PersistentDataType.DOUBLE, baseAmount);
        entry.set(PERSISTED_SLOT_KEY, PersistentDataType.STRING, slotKey(slot));

        NamespacedKey entryKey = new NamespacedKey(NAMESPACE, attributeKey(attribute) + "." + slotKey(slot));
        modifiers.set(entryKey, PersistentDataType.TAG_CONTAINER, entry);
        root.set(PERSISTED_MODIFIERS_KEY, PersistentDataType.TAG_CONTAINER, modifiers);
    }

    private void applyPersistedModifiers(ItemStack stack, ItemMeta meta) {
        PersistentDataContainer root = meta.getPersistentDataContainer();
        PersistentDataContainer modifiers = root.get(PERSISTED_MODIFIERS_KEY, PersistentDataType.TAG_CONTAINER);
        if (modifiers == null || modifiers.isEmpty()) {
            return;
        }

        boolean added = false;
        for (NamespacedKey entryKey : modifiers.getKeys()) {
            PersistentDataContainer entry = modifiers.get(entryKey, PersistentDataType.TAG_CONTAINER);
            if (entry == null) {
                continue;
            }

            String attributeName = entry.get(PERSISTED_ATTRIBUTE_KEY, PersistentDataType.STRING);
            Double baseAmount = entry.get(PERSISTED_AMOUNT_KEY, PersistentDataType.DOUBLE);
            String slotName = entry.get(PERSISTED_SLOT_KEY, PersistentDataType.STRING);
            Attribute attribute = parseAttribute(attributeName);
            EquipmentSlot slot = parseSlot(slotName);

            if (attribute == null || baseAmount == null || slotName == null) {
                continue;
            }

            AttributeDefinition definition = definitions.get(attribute);
            if (definition == null) {
                continue;
            }

            Multimap<Attribute, AttributeModifier> existing = meta.getAttributeModifiers();
            if (existing != null) {
                for (AttributeModifier modifier : List.copyOf(existing.get(attribute))) {
                    if (isPluginModifier(modifier) && (slot == null || modifier.getSlot() == null || modifier.getSlot() == slot)) {
                        meta.removeAttributeModifier(attribute, modifier);
                    }
                }
            }

            AttributeModifier modifier = definition.newModifier(computeAmount(attribute, baseAmount), slot);
            meta.addAttributeModifier(attribute, modifier);
            added = true;
        }

        if (added) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
        }
    }

    private Attribute parseAttribute(String attributeName) {
        if (attributeName == null) {
            return null;
        }
        try {
            return Attribute.valueOf(attributeName);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private EquipmentSlot parseSlot(String slotName) {
        if (slotName == null) {
            return null;
        }
        if (slotName.equals("any")) {
            return null;
        }
        try {
            return EquipmentSlot.valueOf(slotName.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String slotKey(EquipmentSlot slot) {
        return slot == null ? "any" : slot.name().toLowerCase();
    }

    private String attributeKey(Attribute attribute) {
        return attribute == null ? "unknown" : attribute.getKey().getKey().toLowerCase();
    }

    private boolean isPluginModifier(AttributeModifier modifier) {
        String name = modifier.getName();
        if (name == null) {
            return false;
        }
        String normalized = name.toLowerCase();
        return normalized.startsWith("attributeitemutils:") || normalized.startsWith(ATTRIBUTEUTILS_PREFIX);
    }

    private boolean hasNonPluginModifier(Iterable<AttributeModifier> modifiers) {
        if (modifiers == null) {
            return false;
        }
        for (AttributeModifier modifier : modifiers) {
            if (!isPluginModifier(modifier)) {
                return true;
            }
        }
        return false;
    }

    private String summarize(Multimap<Attribute, AttributeModifier> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) {
            return "[]";
        }
        return modifiers.entries().stream()
                .map(entry -> entry.getKey().name() + "=" + entry.getValue().getAmount() + "@" + entry.getValue().getOperation() +
                        " slot=" + entry.getValue().getSlot())
                .collect(java.util.stream.Collectors.joining(", ", "[", "]"));
    }

    public void applyEnchant(ItemStack stack, Enchantment enchantment, int level) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.addEnchant(enchantment, level, true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
    }
}
