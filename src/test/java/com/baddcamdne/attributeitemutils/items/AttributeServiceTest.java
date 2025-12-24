package com.baddcamdne.attributeitemutils.items;

import com.baddcamdne.attributeitemutils.config.AttributeAffixConfig;
import com.baddcamdne.attributeitemutils.config.AttributeBonus;
import com.baddcamdne.attributeitemutils.config.AttributeConfig;
import com.baddcamdne.attributeitemutils.config.AttributePoolConfig;
import com.baddcamdne.attributeutils.AttributeFacade;
import com.baddcamdne.attributeutils.AttributeDefinition;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttributeServiceTest {

    @Test
    void collectAttributeBonusesAggregatesDuplicates() {
        AttributeConfig attributeConfig = new AttributeConfig(1.0, 0.0, 0.05, 1.0);
        StubRandom random = new StubRandom(new int[]{0, 0}, new double[]{0.0, 1.0});
        AttributeService service = new AttributeService(new AttributeFacade(), emptyAffixes(), singleAttributePool(), random);

        Map<Attribute, Double> bonuses = service.collectAttributeBonuses(attributeConfig, 0);

        assertEquals(1, bonuses.size());
        assertEquals(0.10, bonuses.get(Attribute.GENERIC_MAX_HEALTH), 0.00001);
    }

    @Test
    void collectAttributeBonusesReturnsEmptyWhenFirstRollFails() {
        AttributeConfig attributeConfig = new AttributeConfig(0.0, 0.0, 0.05, 1.0);
        StubRandom random = new StubRandom(new int[]{}, new double[]{1.0});
        AttributeService service = new AttributeService(new AttributeFacade(), emptyAffixes(), singleAttributePool(), random);

        Map<Attribute, Double> bonuses = service.collectAttributeBonuses(attributeConfig, 0);

        assertTrue(bonuses.isEmpty());
    }

    @Test
    void resolveAmountHandlesSpecialCases() {
        AttributeConfig attributeConfig = new AttributeConfig(1.0, 0.0, 0.05, 1.0);
        AttributeService service = new AttributeService(new AttributeFacade(), emptyAffixes(), singleAttributePool(), new StubRandom(new int[]{0}, new double[]{1.0}));

        double scaleAmount = service.resolveAmount(Attribute.GENERIC_SCALE, 0.05);
        assertEquals(Math.cbrt(1.05) - 1, scaleAmount, 1.0e-9);

        double fallDamageAmount = service.resolveAmount(Attribute.GENERIC_FALL_DAMAGE_MULTIPLIER, 0.05);
        assertEquals(-0.05, fallDamageAmount, 1.0e-9);

        double normalAmount = service.resolveAmount(Attribute.GENERIC_ATTACK_DAMAGE, 0.05);
        assertEquals(0.05, normalAmount, 1.0e-9);
    }

    @Test
    void poolExposesAllConfiguredAttributes() {
        List<Attribute> expected = List.of(
                Attribute.GENERIC_ATTACK_DAMAGE,
                Attribute.GENERIC_ATTACK_SPEED
        );

        AttributePoolConfig pool = new AttributePoolConfig(List.of(
                new AttributeBonus(Attribute.GENERIC_ATTACK_DAMAGE, 0.05),
                new AttributeBonus(Attribute.GENERIC_ATTACK_SPEED, 0.10)
        ));

        List<Attribute> configured = pool.attributes().stream().map(AttributeBonus::attribute).toList();

        assertIterableEquals(expected, configured);
    }

    @Test
    void decoratesNameWhenAllModifiersMatchConfig() {
        AttributeFacade facade = facadeWithDefinitions(Attribute.GENERIC_ATTACK_DAMAGE, Attribute.GENERIC_ATTACK_SPEED);
        AttributeAffixConfig affixConfig = new AttributeAffixConfig(
                Map.of(Set.of(Attribute.GENERIC_ATTACK_DAMAGE, Attribute.GENERIC_ATTACK_SPEED), "Relentless "),
                Map.of(Set.of(Attribute.GENERIC_ATTACK_DAMAGE, Attribute.GENERIC_ATTACK_SPEED), " of Fury")
        );
        AttributePoolConfig pool = new AttributePoolConfig(List.of(
                new AttributeBonus(Attribute.GENERIC_ATTACK_DAMAGE, 0.05),
                new AttributeBonus(Attribute.GENERIC_ATTACK_SPEED, 0.05)
        ));
        AttributeService service = new AttributeService(facade, affixConfig, pool, new StubRandom(new int[]{0, 1}, new double[]{0.0, 0.0, 1.0}));

        AttributeConfig config = new AttributeConfig(1.0, 0.0, 0.05, 1.0);
        ItemStack stack = new ItemStack(Material.DIAMOND_SWORD);

        ItemStack result = service.applyAttributes(stack, config, EquipmentSlot.HAND, 0);

        assertEquals("Relentless DIAMOND_SWORD of Fury", result.getItemMeta().getDisplayName());
    }

    @Test
    void leavesNameUnchangedWhenModifiersDoNotMatchConfig() {
        AttributeFacade facade = facadeWithDefinitions(Attribute.GENERIC_ATTACK_DAMAGE, Attribute.GENERIC_ATTACK_SPEED);
        AttributeAffixConfig affixConfig = new AttributeAffixConfig(
                Map.of(Set.of(Attribute.GENERIC_ATTACK_DAMAGE, Attribute.GENERIC_ATTACK_SPEED), "Relentless "),
                Map.of(Set.of(Attribute.GENERIC_ATTACK_DAMAGE, Attribute.GENERIC_ATTACK_SPEED), " of Fury")
        );
        AttributePoolConfig pool = new AttributePoolConfig(List.of(new AttributeBonus(Attribute.GENERIC_ATTACK_DAMAGE, 0.05)));
        AttributeService service = new AttributeService(facade, affixConfig, pool, new StubRandom(new int[]{0}, new double[]{0.0, 1.0}));

        AttributeConfig config = new AttributeConfig(1.0, 0.0, 0.05, 1.0);
        ItemStack stack = new ItemStack(Material.DIAMOND_SWORD);

        ItemStack result = service.applyAttributes(stack, config, EquipmentSlot.HAND, 0);

        assertFalse(result.getItemMeta().hasDisplayName());
    }

    private AttributeAffixConfig emptyAffixes() {
        return new AttributeAffixConfig(Map.of(), Map.of());
    }

    private AttributePoolConfig singleAttributePool() {
        return new AttributePoolConfig(List.of(new AttributeBonus(Attribute.GENERIC_MAX_HEALTH, 0.05)));
    }

    private AttributeFacade facadeWithDefinitions(Attribute... attributes) {
        AttributeFacade facade = new AttributeFacade();
        for (Attribute attribute : attributes) {
            facade.registerDefinition(new AttributeDefinition(attribute, AttributeModifier.Operation.MULTIPLY_SCALAR_1, 1.0));
        }
        return facade;
    }

    private static final class StubRandom extends java.util.Random {
        private final int[] ints;
        private final double[] doubles;
        private int intIndex;
        private int doubleIndex;

        private StubRandom(int[] ints, double[] doubles) {
            this.ints = ints;
            this.doubles = doubles;
        }

        @Override
        public int nextInt(int bound) {
            if (intIndex >= ints.length) {
                return 0;
            }
            return ints[intIndex++] % bound;
        }

        @Override
        public double nextDouble() {
            if (doubleIndex >= doubles.length) {
                return 1.0;
            }
            return doubles[doubleIndex++];
        }
    }
}
