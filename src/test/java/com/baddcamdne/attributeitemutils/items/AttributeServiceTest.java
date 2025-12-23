package com.baddcamdne.attributeitemutils.items;

import com.baddcamdne.attributeitemutils.config.AttributeConfig;
import org.bukkit.attribute.Attribute;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
