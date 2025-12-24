package com.baddcamden.attributeitemutils.gear;

import java.util.List;
import java.util.Map;

/**
 * Immutable view of a gear kit parsed from configuration.
 * @param name unique kit identifier from the config file.
 * @param targetWeight desired weight value the bell-curve selector aims toward.
 * @param steepness bell curve steepness parameter controlling preference strength near the target weight.
 * @param range maximum distance from the target weight where items still receive a non-zero normalized score.
 * @param items mapping of gear slots to weighted material options available for the kit.
 */
public record KitConfig(String name,
                        double targetWeight,
                        double steepness,
                        double range,
                        Map<GearSlot, List<WeightedItem>> items) {
}
