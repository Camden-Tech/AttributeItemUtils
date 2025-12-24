package com.baddcamden.attributeitemutils.gear;

import org.bukkit.Material;

/**
 * Represents a weighted material entry used for random kit selection.
 * @param material the Bukkit material to include in a generated kit slot
 * @param weight the probability weight applied during selection; larger values increase likelihood
 */
public record WeightedItem(Material material, double weight) {
}
