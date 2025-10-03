package it.hurts.octostudios.rarcompat.items.hands;

import artifacts.registry.ModItems;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.LivingDestroyBlockEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

public class PocketPistonItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("discarding")
                                .stat(StatData.builder("interaction")
                                        .initialValue(0.2D, 0.4D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.15D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())
                                .build())
                        .ability(AbilityData.builder("interaction")
                                .requiredLevel(5)
                                .stat(StatData.builder("range")
                                        .initialValue(0.2D, 0.4D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.15D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xffcb9848)
                                .borderBottom(0xff6a6a6a)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(15)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.VILLAGE)
                        .build())
                .build();
    }

    @Override
    public void wornTick(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player) || !canUseAbility(stack, "interaction"))
            return;

        float modifier = (float) getAbilityValue(stack, "interaction", "range");

        EntityUtils.applyAttribute(player, stack, ForgeMod.ENTITY_REACH.get(), modifier, AttributeModifier.Operation.MULTIPLY_TOTAL);
        EntityUtils.applyAttribute(player, stack, ForgeMod.BLOCK_REACH.get(), modifier, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public void onUnequip(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player))
            return;

        EntityUtils.removeAttribute(player, stack, ForgeMod.ENTITY_REACH.get(), AttributeModifier.Operation.MULTIPLY_TOTAL);
        EntityUtils.removeAttribute(player, stack, ForgeMod.BLOCK_REACH.get(), AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Mod.EventBusSubscriber
    public static class PocketPistonEvent {

        @SubscribeEvent
        public static void onContacted(BlockEvent.EntityPlaceEvent event) {
            if (!(event.getEntity() instanceof Player player))
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.POCKET_PISTON.get());

            if (!(stack.getItem() instanceof PocketPistonItem relic) || !relic.canPlayerUseActiveAbility(player, stack, "discarding"))
                return;

            int distance = (int) Math.sqrt(event.getPos().distToCenterSqr(player.getX(), player.getY() + player.getEyeHeight(), player.getZ()));

            if (distance >= 5)
                relic.spreadExperience(player, stack, 1);
        }

        @SubscribeEvent
        public static void onContacted(LivingDestroyBlockEvent event) {
            if (!(event.getEntity() instanceof Player player))
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.POCKET_PISTON.get());

            if (!(stack.getItem() instanceof PocketPistonItem relic) || !relic.canPlayerUseActiveAbility(player, stack, "discarding"))
                return;

            int distance = (int) Math.sqrt(event.getPos().distToCenterSqr(player.getX(), player.getY() + player.getEyeHeight(), player.getZ()));

            if (distance >= 5)
                relic.spreadExperience(player, stack, 1);
        }

        @SubscribeEvent
        public static void onAttacking(AttackEntityEvent event) {
            Player player = event.getEntity();
            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.POCKET_PISTON.get());

            if (!(event.getTarget() instanceof LivingEntity target) || !(stack.getItem() instanceof PocketPistonItem relic)
                    || !relic.canPlayerUseActiveAbility(player, stack, "discarding"))
                return;

            float modifier = (float) relic.getAbilityValue(stack, "discarding", "interaction");

            relic.spreadExperience(player, stack, 1);

            if (player.distanceTo(target) > 3)
                relic.spreadExperience(player, stack, 1);

            Vec3 toEntity = target.position().subtract(player.position()).normalize().scale(modifier);

            target.setDeltaMovement(toEntity.x, toEntity.y / 2, toEntity.z);
        }
    }
}
