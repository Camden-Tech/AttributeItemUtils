package com.baddcamden.attributeitemutils.items;

import com.baddcamden.attributeitemutils.config.AttributeAffixConfig;
import com.baddcamden.attributeitemutils.config.AttributeBonus;
import com.baddcamden.attributeitemutils.config.AttributeConfig;
import com.baddcamden.attributeitemutils.config.AttributeLoreConfig;
import com.baddcamden.attributeitemutils.config.AttributePoolConfig;
import com.baddcamden.attributeutils.AttributeDefinition;
import com.baddcamden.attributeutils.AttributeFacade;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(Lifecycle.PER_CLASS)
class AttributeServiceIntegrationTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void retainsSwordDefaultsWhenApplyingAttributes() {
        AttributeFacade attributeFacade = new AttributeFacade();
        attributeFacade.registerDefinition(new AttributeDefinition(Attribute.GENERIC_ATTACK_DAMAGE, AttributeModifier.Operation.ADD_NUMBER, Double.NaN));

        AttributeService attributeService = new AttributeService(
                attributeFacade,
                emptyAffixConfig(),
                new AttributePoolConfig(List.of(new AttributeBonus(Attribute.GENERIC_ATTACK_DAMAGE, 0.1, AttributeModifier.Operation.ADD_NUMBER, 0.0))),
                emptyLoreConfig(),
                new FixedRandom()
        );

        ItemStack stack = new ItemStack(Material.NETHERITE_SWORD);
        EquipmentSlot slot = EquipmentSlot.HAND;

        Set<ModifierSignature> defaultModifiers = vanillaDefaults(Material.NETHERITE_SWORD, slot);
        assertThat(defaultModifiers).isNotEmpty();

        attributeService.applyAttributes(stack, guaranteedRolls(), slot, 0);

        Multimap<Attribute, AttributeModifier> applied = modifiersForSlot(stack, slot);
        Set<ModifierSignature> resulting = signatures(applied);

        assertThat(resulting)
                .as("Sword retains all vanilla modifiers after applyAttributes")
                .containsAll(defaultModifiers);

        long pluginModifiers = applied.entries().stream()
                .map(Map.Entry::getValue)
                .filter(modifier -> Objects.requireNonNullElse(modifier.getName(), "").startsWith("attributeitemutils:"))
                .count();

        assertThat(pluginModifiers).isEqualTo(1);
    }

    @Test
    void retainsArmorDefaultsWhenApplyingAttributes() {
        AttributeFacade attributeFacade = new AttributeFacade();
        attributeFacade.registerDefinition(new AttributeDefinition(Attribute.GENERIC_ARMOR, AttributeModifier.Operation.ADD_NUMBER, Double.NaN));

        AttributeService attributeService = new AttributeService(
                attributeFacade,
                emptyAffixConfig(),
                new AttributePoolConfig(List.of(new AttributeBonus(Attribute.GENERIC_ARMOR, 0.15, AttributeModifier.Operation.ADD_NUMBER, 0.0))),
                emptyLoreConfig(),
                new FixedRandom()
        );

        ItemStack stack = new ItemStack(Material.NETHERITE_CHESTPLATE);
        EquipmentSlot slot = EquipmentSlot.CHEST;

        Set<ModifierSignature> defaultModifiers = vanillaDefaults(Material.NETHERITE_CHESTPLATE, slot);
        assertThat(defaultModifiers).isNotEmpty();

        attributeService.applyAttributes(stack, guaranteedRolls(), slot, 0);

        Multimap<Attribute, AttributeModifier> applied = modifiersForSlot(stack, slot);
        Set<ModifierSignature> resulting = signatures(applied);

        assertThat(resulting)
                .as("Chestplate retains all vanilla modifiers after applyAttributes")
                .containsAll(defaultModifiers);

        long pluginModifiers = applied.entries().stream()
                .map(Map.Entry::getValue)
                .filter(modifier -> Objects.requireNonNullElse(modifier.getName(), "").startsWith("attributeitemutils:"))
                .count();

        assertThat(pluginModifiers).isEqualTo(1);
    }

    @Test
    void restoresVanillaWhenOnlyPluginModifiersExist() {
        AttributeFacade attributeFacade = new AttributeFacade();
        attributeFacade.registerDefinition(new AttributeDefinition(Attribute.GENERIC_ATTACK_DAMAGE, AttributeModifier.Operation.ADD_NUMBER, Double.NaN));
        attributeFacade.registerDefinition(new AttributeDefinition(Attribute.GENERIC_ATTACK_SPEED, AttributeModifier.Operation.ADD_NUMBER, Double.NaN));

        AttributeService attributeService = new AttributeService(
                attributeFacade,
                emptyAffixConfig(),
                new AttributePoolConfig(List.of(
                        new AttributeBonus(Attribute.GENERIC_ATTACK_DAMAGE, 0.1, AttributeModifier.Operation.ADD_NUMBER, 0.0),
                        new AttributeBonus(Attribute.GENERIC_ATTACK_SPEED, 0.1, AttributeModifier.Operation.ADD_NUMBER, 0.0)
                )),
                emptyLoreConfig(),
                new FixedRandom()
        );

        ItemStack stack = new ItemStack(Material.NETHERITE_SWORD);
        EquipmentSlot slot = EquipmentSlot.HAND;

        // Strip vanilla modifiers and replace them with legacy plugin-authored ones to mimic migrated items.
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            Multimap<Attribute, AttributeModifier> modifiers = meta.getAttributeModifiers();
            if (modifiers != null) {
                modifiers.forEach(meta::removeAttributeModifier);
            }
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(
                    java.util.UUID.randomUUID(),
                    "attributeutils:attack_damage",
                    1.0,
                    AttributeModifier.Operation.ADD_NUMBER,
                    slot));
            stack.setItemMeta(meta);
        }

        Set<ModifierSignature> defaultModifiers = vanillaDefaults(Material.NETHERITE_SWORD, slot);
        attributeService.applyAttributes(stack, guaranteedRolls(), slot, 0);

        Multimap<Attribute, AttributeModifier> applied = modifiersForSlot(stack, slot);
        Set<ModifierSignature> resulting = signatures(applied);

        assertThat(resulting)
                .as("Vanilla sword defaults are restored even when legacy plugin modifiers existed")
                .containsAll(defaultModifiers);
    }

    private AttributeLoreConfig emptyLoreConfig() {
        return new AttributeLoreConfig(Map.of(), "{name}", "{amount}", "", "", "", "", Map.of());
    }

    private AttributeAffixConfig emptyAffixConfig() {
        return new AttributeAffixConfig(List.of(), List.of(), "");
    }

    private AttributeConfig guaranteedRolls() {
        return new AttributeConfig(1.0, 0.0, 0.2, 1.0);
    }

    private Set<ModifierSignature> vanillaDefaults(Material material, EquipmentSlot slot) {
        ItemStack vanilla = new ItemStack(material);
        Multimap<Attribute, AttributeModifier> defaults = modifiersForSlot(vanilla, slot);
        return signatures(defaults);
    }

    private Multimap<Attribute, AttributeModifier> modifiersForSlot(ItemStack stack, EquipmentSlot slot) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return ImmutableMultimap.of();
        }
        Multimap<Attribute, AttributeModifier> bySlot = meta.getAttributeModifiers(slot);
        if (bySlot != null) {
            return bySlot;
        }
        Multimap<Attribute, AttributeModifier> all = meta.getAttributeModifiers();
        return all == null ? ImmutableMultimap.of() : all;
    }

    private Set<ModifierSignature> signatures(Multimap<Attribute, AttributeModifier> modifiers) {
        return modifiers.entries().stream()
                .map(entry -> new ModifierSignature(entry.getKey(), entry.getValue()))
                .collect(Collectors.toSet());
    }

    private record ModifierSignature(Attribute attribute, double amount, AttributeModifier.Operation operation, EquipmentSlot slot, String name) {
        ModifierSignature(Attribute attribute, AttributeModifier modifier) {
            this(attribute, modifier.getAmount(), modifier.getOperation(), modifier.getSlot(), modifier.getName());
        }
    }

    private static final class FixedRandom extends Random {
        @Override
        public double nextDouble() {
            return 0.0;
        }

        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }
}
