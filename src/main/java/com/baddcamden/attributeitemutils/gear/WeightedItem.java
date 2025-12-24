package com.baddcamden.attributeitemutils.gear;

import org.bukkit.Material;

/**
 * Immutable pairing of a material choice with its selection weight within a kit slot.
 */
public record WeightedItem(Material material, double weight) {
}
