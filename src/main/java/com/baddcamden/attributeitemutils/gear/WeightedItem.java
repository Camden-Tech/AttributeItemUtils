package com.baddcamden.attributeitemutils.gear;

import org.bukkit.Material;

/**
 * Represents a material with an associated selection weight used when randomly constructing gear
 * kits.
 *
 * @param material Bukkit material that can appear in a gear slot
 * @param weight   relative chance the material should be chosen within its slot
 */
public record WeightedItem(Material material, double weight) {
}
