package com.baddcamdne.attributeitemutils.items;

import com.baddcamdne.attributeitemutils.config.AttributeConfig;
import org.bukkit.attribute.Attribute;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttributeServiceTest {

    @Test
    void collectAttributeBonusesAggregatesDuplicates() {
        AttributeConfig attributeConfig = new AttributeConfig(1.0, 0.0, 0.05, 1.0);
        StubRandom random = new StubRandom(new int[]{0, 0}, new double[]{0.0, 1.0});
        AttributeService service = new AttributeService(random);

        Map<Attribute, Double> bonuses = service.collectAttributeBonuses(attributeConfig, 0);

        assertEquals(1, bonuses.size());
        assertEquals(0.10, bonuses.get(Attribute.GENERIC_MAX_HEALTH), 0.00001);
    }

    @Test
    void collectAttributeBonusesReturnsEmptyWhenFirstRollFails() {
        AttributeConfig attributeConfig = new AttributeConfig(0.0, 0.0, 0.05, 1.0);
        StubRandom random = new StubRandom(new int[]{}, new double[]{1.0});
        AttributeService service = new AttributeService(random);

        Map<Attribute, Double> bonuses = service.collectAttributeBonuses(attributeConfig, 0);

        assertTrue(bonuses.isEmpty());
    }

    @Test
    void resolveAmountHandlesSpecialCases() {
        AttributeConfig attributeConfig = new AttributeConfig(1.0, 0.0, 0.05, 1.0);
        AttributeService service = new AttributeService(new StubRandom(new int[]{0}, new double[]{1.0}));

        double scaleAmount = service.resolveAmount(Attribute.GENERIC_SCALE, 0.05);
        assertEquals(Math.cbrt(1.05) - 1, scaleAmount, 1.0e-9);

        double fallDamageAmount = service.resolveAmount(Attribute.GENERIC_FALL_DAMAGE_MULTIPLIER, 0.05);
        assertEquals(-0.05, fallDamageAmount, 1.0e-9);

        double normalAmount = service.resolveAmount(Attribute.GENERIC_ATTACK_DAMAGE, 0.05);
        assertEquals(0.05, normalAmount, 1.0e-9);
    }

    @Test
    void candidatesExposeAllConfiguredAttributes() throws Exception {
        List<Attribute> expected = List.of(
                Attribute.GENERIC_MAX_HEALTH,
                Attribute.GENERIC_ARMOR,
                Attribute.GENERIC_ARMOR_TOUGHNESS,
                Attribute.GENERIC_ATTACK_DAMAGE,
                Attribute.GENERIC_ATTACK_SPEED,
                Attribute.GENERIC_ATTACK_KNOCKBACK,
                Attribute.GENERIC_MOVEMENT_SPEED,
                Attribute.GENERIC_FLYING_SPEED,
                Attribute.GENERIC_KNOCKBACK_RESISTANCE,
                Attribute.GENERIC_LUCK,
                Attribute.GENERIC_FOLLOW_RANGE,
                Attribute.GENERIC_BLOCK_INTERACTION_RANGE,
                Attribute.GENERIC_ENTITY_INTERACTION_RANGE,
                Attribute.GENERIC_MINING_EFFICIENCY,
                Attribute.GENERIC_MAX_ABSORPTION,
                Attribute.GENERIC_STEP_HEIGHT,
                Attribute.GENERIC_SAFE_FALL_DISTANCE,
                Attribute.GENERIC_SCALE,
                Attribute.GENERIC_JUMP_STRENGTH,
                Attribute.GENERIC_GRAVITY,
                Attribute.GENERIC_FALL_DAMAGE_MULTIPLIER
        );

        var candidatesField = AttributeService.class.getDeclaredField("CANDIDATE_ATTRIBUTES");
        var prefixesField = AttributeService.class.getDeclaredField("PREFIXES");
        var suffixesField = AttributeService.class.getDeclaredField("SUFFIXES");

        assertTrue(candidatesField.trySetAccessible());
        assertTrue(prefixesField.trySetAccessible());
        assertTrue(suffixesField.trySetAccessible());

        @SuppressWarnings("unchecked")
        List<Attribute> candidates = (List<Attribute>) candidatesField.get(null);
        @SuppressWarnings("unchecked")
        Map<Attribute, String> prefixes = (Map<Attribute, String>) prefixesField.get(null);
        @SuppressWarnings("unchecked")
        Map<Attribute, String> suffixes = (Map<Attribute, String>) suffixesField.get(null);

        assertIterableEquals(expected, candidates);
        assertTrue(prefixes.keySet().containsAll(expected));
        assertTrue(suffixes.keySet().containsAll(expected));
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
