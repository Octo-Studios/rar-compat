package it.hurts.octostudios.rarcompat.items.charm;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicAttributeModifier;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootData;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootCollections;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.TooltipData;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.List;

public class CrystalHeartItem extends WearableRelicItem {

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("heart")
                                .stat(StatData.builder("amount")
                                        .initialValue(2D, 6D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.2D)
                                        .formatValue(value -> (int) MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xffea1717)
                                .borderBottom(0xff7d0000)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.ANTHROPOGENIC)
                        .build())
                .build();
    }

    // From EntityUtils.findEquippedCurios() (Relics 1.21.1 by SSKirilSS)
    // This method doesn't exist in 1.20.1 relics version
    private static List<ItemStack> findEquippedCurios(Entity entity, Item item) {
        if (!(entity instanceof Player player))
            return List.of();

        return CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.findCurios(item).stream()
                        .map(SlotResult::stack)
                        .toList())
                .orElse(List.of());
    }

    @Override
    public void wornTick(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player) || !canUseAbility(stack, "heart"))
            return;

        List<ItemStack> equippedHearts = findEquippedCurios(player, ModItems.CRYSTAL_HEART.get());

        if (equippedHearts.isEmpty() || equippedHearts.get(0) != stack)
            return;

        float totalHealth = (float) equippedHearts.stream()
                .filter(s -> s.getItem() instanceof CrystalHeartItem relic && relic.canUseAbility(s, "heart"))
                .mapToDouble(s -> ((CrystalHeartItem) s.getItem()).getAbilityValue(s, "heart", "amount"))
                .sum();

        EntityUtils.applyAttribute(player, stack, Attributes.MAX_HEALTH, totalHealth, AttributeModifier.Operation.ADDITION);
    }

    @Override
    public void onEquip(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player))
            return;

        List<ItemStack> equippedHearts = findEquippedCurios(player, ModItems.CRYSTAL_HEART.get());

        if (equippedHearts.size() > 1)
            EntityUtils.removeAttribute(player, equippedHearts.get(0), Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION);
    }


    @Override
    public void onUnequip(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player))
            return;

        EntityUtils.removeAttribute(player, stack, Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION);

        List<ItemStack> equippedHearts = findEquippedCurios(player, ModItems.CRYSTAL_HEART.get()).stream()
                .filter(s -> s != stack)
                .toList();

        if (!equippedHearts.isEmpty()) {
            float totalHealth = (float) equippedHearts.stream()
                    .filter(s -> s.getItem() instanceof CrystalHeartItem relic && relic.canUseAbility(s, "heart"))
                    .mapToDouble(s -> ((CrystalHeartItem) s.getItem()).getAbilityValue(s, "heart", "amount"))
                    .sum();

            if (totalHealth > 0)
                EntityUtils.applyAttribute(player, equippedHearts.get(0), Attributes.MAX_HEALTH, totalHealth, AttributeModifier.Operation.ADDITION);
        }

        if (player.getHealth() > player.getMaxHealth())
            player.setHealth(player.getMaxHealth());
    }

    @Mod.EventBusSubscriber
    public static class CrystalHeartEvent {
        @SubscribeEvent
        public static void onLivingHealEvent(LivingHealEvent event) {
            if (!(event.getEntity() instanceof Player player))
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(event.getEntity(), ModItems.CRYSTAL_HEART.get());

            if (!(stack.getItem() instanceof CrystalHeartItem relic) || !relic.canUseAbility(stack, "heart"))
                return;

            float maxHealth = player.getMaxHealth();

            if (player.getRandom().nextFloat() <= Math.max(0.1, (maxHealth - player.getHealth()) / maxHealth))
                relic.spreadExperience(player, stack, 1);
        }
    }
}
