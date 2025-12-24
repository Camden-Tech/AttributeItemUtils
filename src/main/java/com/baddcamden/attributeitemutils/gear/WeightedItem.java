package com.baddcamden.attributeitemutils.gear;

import org.bukkit.Material;

/**
 * Represents a material paired with a selection weight for random kit generation.
 *
 * @param material material to produce in a slot
 * @param weight   relative likelihood compared to other entries
 */
public record WeightedItem(Material material, double weight) {
}
