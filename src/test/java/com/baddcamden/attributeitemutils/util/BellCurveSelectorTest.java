package com.baddcamden.attributeitemutils.util;

import com.baddcamden.attributeitemutils.gear.WeightedItem;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BellCurveSelectorTest {

    @Test
    void clampsZeroSteepnessAndRange() {
        BellCurveSelector selector = new BellCurveSelector(new Random(1L));
        List<WeightedItem> options = List.of(new WeightedItem(Material.DIAMOND, 5.0));

        WeightedItem selected = selector.select(options, 5.0, 0, 0);

        assertNotNull(selected);
        assertEquals(options.get(0), selected);
    }

    @Test
    void fallsBackToUniformSelectionWhenWeightsZero() {
        BellCurveSelector selector = new BellCurveSelector(new Random(2L));
        List<WeightedItem> options = List.of(
                new WeightedItem(Material.STONE, 0.0),
                new WeightedItem(Material.DIRT, 10.0),
                new WeightedItem(Material.GRASS_BLOCK, 20.0)
        );

        WeightedItem selected = selector.select(options, 1000.0, -5.0, -1.0);

        assertNotNull(selected);
        assertEquals(options.get(0), selected);
    }
}
