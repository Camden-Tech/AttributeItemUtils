package com.baddcamden.attributeitemutils.gear;

import java.util.List;
import java.util.Map;

/**
 * Immutable data holder describing a gear kit and its weighting behavior for each equipment slot.
 * Values originate from {@code Gear.yml} and are used by selection utilities to determine which
 * items are eligible for generation.
 *
 * @param name          configuration identifier for the kit
 * @param targetWeight  center point for the bell curve used when selecting items
 * @param steepness     aggressiveness of the curve; higher values favor the target weight more
 * @param range         permissible distance from the targetWeight when generating a random weight
 * @param items         weighted item definitions grouped by {@link GearSlot}
 */
public record KitConfig(String name,
                        double targetWeight,
                        double steepness,
                        double range,
                        Map<GearSlot, List<WeightedItem>> items) {
}
