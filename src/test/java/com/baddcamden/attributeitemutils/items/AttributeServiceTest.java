package com.baddcamden.attributeitemutils.items;

import com.baddcamden.attributeitemutils.config.AttributeAffixConfig;
import com.baddcamden.attributeitemutils.config.AttributeBonus;
import com.baddcamden.attributeitemutils.config.AttributeConfig;
import com.baddcamden.attributeitemutils.config.AttributeLoreConfig;
import com.baddcamden.attributeitemutils.config.AttributePoolConfig;
import com.baddcamden.attributeutils.AttributeFacade;
import org.bukkit.attribute.Attribute;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AttributeServiceTest {

    @Test
    void capsIterationsWhenMaxChanceIsGuaranteed() {
        AttributeFacade facade = mock(AttributeFacade.class);
        AttributeAffixConfig affixConfig = mock(AttributeAffixConfig.class);
        AttributeLoreConfig loreConfig = mock(AttributeLoreConfig.class);
        AttributeBonus armorBonus = new AttributeBonus(Attribute.GENERIC_ARMOR, 0.0);
        AttributePoolConfig poolConfig = new AttributePoolConfig(List.of(armorBonus, armorBonus));
        AttributeConfig config = new AttributeConfig(1.0, 0.0, 0.5, 1.0);
        Random alwaysSuccessful = new Random() {
            @Override
            public double nextDouble() {
                return 0.0d;
            }

            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };

        AttributeService service = new AttributeService(facade, affixConfig, poolConfig, loreConfig, alwaysSuccessful);

        Map<Attribute, Double> bonuses = service.collectAttributeBonuses(config, 0);

        assertThat(bonuses).containsEntry(Attribute.GENERIC_ARMOR, 1.0);
    }
}
