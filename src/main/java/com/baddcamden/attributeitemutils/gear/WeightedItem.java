package com.baddcamden.attributeitemutils.gear;

import org.bukkit.Material;

/**
 * Immutable pairing of a Bukkit material and its selection weight.
 * @param material concrete Bukkit material that should be chosen.
 * @param weight probability weight used by selection algorithms; higher values increase odds of selection.
 */
public record WeightedItem(Material material, double weight) {
}
