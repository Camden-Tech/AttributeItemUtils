package com.baddcamden.attributeitemutils.gear;

import org.bukkit.Material;

/**
 * Represents an item that can be chosen with a weighted probability.
 *
 * @param material the Bukkit material represented by this entry
 * @param weight   the relative weight used when selecting this entry
 */
public record WeightedItem(Material material, double weight) {
}
