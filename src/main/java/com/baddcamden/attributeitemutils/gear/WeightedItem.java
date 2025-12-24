package com.baddcamden.attributeitemutils.gear;

import org.bukkit.Material;

/**
 * Simple value object pairing a material with a selection weight.
 */
public record WeightedItem(Material material, double weight) {
}
