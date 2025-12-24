package com.baddcamdne.attributeitemutils.items;

import com.baddcamdne.attributeitemutils.config.AttributeAffixConfig;
import com.baddcamdne.attributeitemutils.config.AttributeConfig;
import com.baddcamdne.attributeutils.AttributeFacade;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AttributeServiceSlotTest {
    @Test
    void appliesAttributesUsingProvidedSlot() {
        AttributeFacade facade = mock(AttributeFacade.class);
        Random random = new StubRandom(new int[]{0}, new double[]{0.0, 1.0});
        AttributeService service = new AttributeService(facade, new AttributeAffixConfig(Map.of(), Map.of()), random);

        AttributeConfig config = new AttributeConfig(0.6, 0.0, 0.05, 1.0);
        ItemStack stack = new ItemStack(Material.DIAMOND_SWORD);

        service.applyAttributes(stack, config, EquipmentSlot.OFF_HAND, 0);

        verify(facade).refresh(stack, EquipmentSlot.OFF_HAND);
        verify(facade).mutate(stack, Attribute.GENERIC_MAX_HEALTH, 0.05, EquipmentSlot.OFF_HAND);
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
