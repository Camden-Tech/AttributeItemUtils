package com.baddcamden.attributeitemutils.config;

import org.bukkit.attribute.Attribute;

/**
 * Describes an attribute bonus entry with the targeted attribute and percentage modifier.
 *
 * @param attribute     attribute receiving the bonus
 * @param bonusPercent  percent modifier applied to the attribute (defaults may override zero)
 */
public record AttributeBonus(Attribute attribute, double bonusPercent) {
}
