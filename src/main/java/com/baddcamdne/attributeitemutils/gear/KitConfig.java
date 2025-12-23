package com.baddcamdne.attributeitemutils.gear;

import java.util.List;
import java.util.Map;

public record KitConfig(String name,
                        double targetWeight,
                        double steepness,
                        double range,
                        Map<GearSlot, List<WeightedItem>> items) {
}
