package com.baddcamden.attributeitemutils.items;

import com.baddcamden.attributeitemutils.config.AttributeAffixConfig;
import com.baddcamden.attributeitemutils.config.AttributeBonus;
import com.baddcamden.attributeitemutils.config.AttributeConfig;
import com.baddcamden.attributeitemutils.config.AttributeLoreConfig;
import com.baddcamden.attributeitemutils.config.AttributePoolConfig;
import com.baddcamden.attributeutils.AttributeDefinition;
import com.baddcamden.attributeutils.AttributeFacade;
import com.google.common.collect.ImmutableList;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

class AttributeServiceNbtDiffTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void reportsNbtDiffBetweenVanillaAndGeneratedItem() {
        AttributeFacade facade = new AttributeFacade();
        facade.registerDefinition(new AttributeDefinition(Attribute.GENERIC_ATTACK_DAMAGE, AttributeModifier.Operation.ADD_NUMBER, Double.NaN));
        facade.registerDefinition(new AttributeDefinition(Attribute.GENERIC_ATTACK_SPEED, AttributeModifier.Operation.ADD_NUMBER, Double.NaN));

        AttributeService service = new AttributeService(
                facade,
                emptyAffixConfig(),
                new AttributePoolConfig(ImmutableList.of(
                        new AttributeBonus(Attribute.GENERIC_ATTACK_DAMAGE, 0.15, AttributeModifier.Operation.ADD_NUMBER, 0.0),
                        new AttributeBonus(Attribute.GENERIC_ATTACK_SPEED, 0.05, AttributeModifier.Operation.ADD_NUMBER, 0.0)
                )),
                emptyLoreConfig(),
                new FixedRandom(),
                Logger.getLogger("AttributeServiceNbtDiffTest")
        );

        ItemStack vanilla = new ItemStack(Material.NETHERITE_SWORD);
        ItemStack generated = vanilla.clone();
        service.applyAttributes(generated, guaranteedRolls(), EquipmentSlot.HAND, 0);

        Map<String, Object> vanillaNbt = normalize(vanilla.serialize());
        Map<String, Object> generatedNbt = normalize(generated.serialize());

        List<DiffEntry> diffs = new ArrayList<>();
        diff("", vanillaNbt, generatedNbt, diffs);
        List<DiffEntry> interesting = diffs.stream()
                .filter(this::isInteresting)
                .toList();

        System.out.println("==== Vanilla item NBT-like structure ====");
        System.out.println(format(vanillaNbt));
        System.out.println("==== Generated item NBT-like structure ====");
        System.out.println(format(generatedNbt));
        System.out.println("==== Focused diff (attributes, hide flags, damage, custom tags) ====");
        interesting.forEach(entry -> System.out.println(entry.path() + " -> " + entry.before() + " | " + entry.after()));
    }

    private AttributeConfig guaranteedRolls() {
        return new AttributeConfig(1.0, 0.0, 0.2, 1.0);
    }

    private AttributeAffixConfig emptyAffixConfig() {
        return new AttributeAffixConfig(List.of(), List.of(), "");
    }

    private AttributeLoreConfig emptyLoreConfig() {
        return new AttributeLoreConfig(Map.of(), "{name}", "{amount}", "", "", "", "", Map.of());
    }

    private boolean isInteresting(DiffEntry entry) {
        String path = entry.path().toLowerCase(Locale.ROOT);
        return path.contains("attributemodifiers") || path.contains("attribute") || path.contains("hideflags") ||
                path.endsWith("/damage") || path.contains("custom");
    }

    private Map<String, Object> normalize(Map<String, Object> raw) {
        return (Map<String, Object>) deepNormalize(raw);
    }

    private Object deepNormalize(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sorted.put(String.valueOf(entry.getKey()), deepNormalize(entry.getValue()));
            }
            return sorted;
        }
        if (value instanceof List<?> list) {
            List<Object> normalized = new ArrayList<>();
            for (Object element : list) {
                normalized.add(deepNormalize(element));
            }
            return normalized;
        }
        return value;
    }

    private void diff(String path, Object before, Object after, List<DiffEntry> out) {
        if (Objects.equals(before, after)) {
            return;
        }

        if (before instanceof Map<?, ?> beforeMap && after instanceof Map<?, ?> afterMap) {
            Set<String> keys = new java.util.LinkedHashSet<>();
            beforeMap.keySet().forEach(key -> keys.add(String.valueOf(key)));
            afterMap.keySet().forEach(key -> keys.add(String.valueOf(key)));
            for (String key : keys) {
                diff(join(path, key), beforeMap.get(key), afterMap.get(key), out);
            }
            return;
        }

        if (before instanceof List<?> beforeList && after instanceof List<?> afterList) {
            int max = Math.max(beforeList.size(), afterList.size());
            for (int i = 0; i < max; i++) {
                Object left = i < beforeList.size() ? beforeList.get(i) : null;
                Object right = i < afterList.size() ? afterList.get(i) : null;
                diff(join(path, String.valueOf(i)), left, right, out);
            }
            return;
        }

        out.add(new DiffEntry(path, before, after));
    }

    private String join(String base, String key) {
        if (base.isEmpty()) {
            return key;
        }
        return base + "/" + key;
    }

    private String format(Object value) {
        StringBuilder builder = new StringBuilder();
        format(value, builder, 0);
        return builder.toString();
    }

    private void format(Object value, StringBuilder builder, int indent) {
        if (value instanceof Map<?, ?> map) {
            builder.append("{\n");
            map.forEach((key, val) -> {
                indent(builder, indent + 2).append(key).append(": ");
                format(val, builder, indent + 2);
            });
            indent(builder, indent).append("}\n");
            return;
        }
        if (value instanceof List<?> list) {
            builder.append("[\n");
            for (Object element : list) {
                indent(builder, indent + 2);
                format(element, builder, indent + 2);
            }
            indent(builder, indent).append("]\n");
            return;
        }
        builder.append(value).append("\n");
    }

    private StringBuilder indent(StringBuilder builder, int indent) {
        return builder.append(" ".repeat(Math.max(0, indent)));
    }

    private record DiffEntry(String path, Object before, Object after) {}

    private static final class FixedRandom extends Random {
        @Override
        public double nextDouble() {
            return 0.0;
        }

        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }
}
