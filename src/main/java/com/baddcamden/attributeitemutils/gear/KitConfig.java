package com.baddcamden.attributeitemutils.gear;

import java.util.List;
import java.util.Map;

/**
 * Immutable configuration describing how to build a kit and how weights should be interpreted when
 * selecting items for each {@link GearSlot}.
 *
 * @param name         identifier of the kit in configuration files
 * @param targetWeight preferred weight to center the bell-curve selection around
 * @param steepness    controls how sharply weights away from the target fall off
 * @param range        allowable linear range for weights before they become fully disfavored
 * @param items        mapping of gear slots to the weighted items that may occupy them
 */
public record KitConfig(String name,
                        double targetWeight,
                        double steepness,
                        double range,
                        Map<GearSlot, List<WeightedItem>> items) {
}
