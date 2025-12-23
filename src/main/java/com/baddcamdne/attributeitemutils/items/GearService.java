package com.baddcamdne.attributeitemutils.items;

import com.baddcamdne.attributeitemutils.gear.GearConfigLoader;
import com.baddcamdne.attributeitemutils.gear.GearSlot;
import com.baddcamdne.attributeitemutils.gear.KitConfig;
import com.baddcamdne.attributeitemutils.gear.WeightedItem;
import com.baddcamdne.attributeitemutils.util.BellCurveSelector;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class GearService {
    private final GearConfigLoader loader;
    private final AttributeService attributeService;
    private final BellCurveSelector selector = new BellCurveSelector();
    public GearService(GearConfigLoader loader, AttributeService attributeService) {
        this.loader = loader;
        this.attributeService = attributeService;
    }

    public Optional<KitConfig> getKit(String name) {
        return loader.getKit(name);
    }

    public void applyKit(LivingEntity entity, KitConfig kit) {
        Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
        for (GearSlot slot : GearSlot.values()) {
            WeightedItem selection = selector.select(kit.items().getOrDefault(slot, java.util.List.of()), kit.targetWeight(), kit.steepness(), kit.range());
            Material material = selection == null ? Material.AIR : selection.material();
            ItemStack stack = material == Material.AIR ? null : new ItemStack(material);
            if (stack != null) {
                stack = attributeService.applyAttributes(stack, () -> 0);
                stack = attributeService.applyEnchants(stack, () -> 0);
            }
            switch (slot) {
                case HELMET -> equipment.put(EquipmentSlot.HEAD, stack);
                case CHESTPLATE -> equipment.put(EquipmentSlot.CHEST, stack);
                case LEGGINGS -> equipment.put(EquipmentSlot.LEGS, stack);
                case BOOTS -> equipment.put(EquipmentSlot.FEET, stack);
                case HAND -> equipment.put(EquipmentSlot.HAND, stack);
                case OFF_HAND -> equipment.put(EquipmentSlot.OFF_HAND, stack);
            }
        }
        equipment.forEach((slot, item) -> entity.getEquipment().setItem(slot, item));
    }
}
