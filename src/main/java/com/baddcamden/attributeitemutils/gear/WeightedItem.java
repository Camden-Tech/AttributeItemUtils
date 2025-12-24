package com.baddcamden.attributeitemutils.gear;

import org.bukkit.Material;

/**
 * Immutable pairing of a Bukkit material with a selection weight used when randomly choosing gear
 * pieces. Higher weights bias selection utilities toward the associated material.
 *
 * @param material item type to generate
 * @param weight   relative chance of the item being selected compared to peers in the same slot
 */
public record WeightedItem(Material material, double weight) {
}
