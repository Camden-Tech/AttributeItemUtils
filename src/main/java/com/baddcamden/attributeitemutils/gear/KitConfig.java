package com.baddcamden.attributeitemutils.gear;

import java.util.List;
import java.util.Map;

/**
 * Describes a gear kit configuration and the weighted items available for each slot.
 *
 * @param name         identifier for the kit in configuration files
 * @param targetWeight peak weight used by the bell-curve selector when sampling items
 * @param steepness    bell-curve steepness factor that controls distribution spread
 * @param range        bell-curve range used to limit candidate weights
 * @param items        weighted material selections grouped by gear slot
 */
public record KitConfig(String name,
                        double targetWeight,
                        double steepness,
                        double range,
                        Map<GearSlot, List<WeightedItem>> items) {

    //VAGUE/IMPROVEMENT NEEDED Document expected value ranges for targetWeight, steepness, and range in Gear.yml to guide pack creators.
}
