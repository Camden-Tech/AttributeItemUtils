package com.baddcamden.attributeitemutils.config;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;

/**
 * Describes an attribute bonus entry with the targeted attribute and percentage modifier.
 *
 * @param attribute     attribute receiving the bonus
 * @param bonusPercent  percent modifier applied to the attribute (defaults may override zero)
 * @param operation     math operation to use when applying modifiers for the attribute
 * @param baseline      baseline modifier amount inserted before any rolls are applied
 */
public record AttributeBonus(Attribute attribute, double bonusPercent, AttributeModifier.Operation operation, double baseline) {
}
