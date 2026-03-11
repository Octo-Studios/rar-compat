package it.hurts.octostudios.rarcompat.items.hands;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsMobEffects;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import top.theillusivec4.curios.api.SlotContext;

public class PocketPistonItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("piston")
                                .rankModifier(1, "repulse")
                                .rankModifier(3, "distance_power")
                                .rankModifier(5, "long_reach_stun")
                                .stat(AbilityStatTemplate.builder("range_bonus")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(0.12D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("empty_hand_knockback")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(0.8D, 2.5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("distance_bonus_per_block")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.02D, 0.07D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("stun_duration")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(0.6D, 2D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide())
            return;

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("piston");

        if (!ability.canPlayerUse(player)) {
            removeRangeBonuses(player, stack);
            return;
        }

        var rangeBonus = Math.max(0D, ability.getStatData("range_bonus").getValue());

        if (rangeBonus <= 0D)
            removeRangeBonuses(player, stack);
        else
            applyRangeBonuses(player, stack, rangeBonus);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (!(slotContext.entity() instanceof Player player) || stack.getItem() == newStack.getItem())
            return;

        removeRangeBonuses(player, stack);
    }

    private void applyRangeBonuses(Player player, ItemStack stack, double amount) {
        if (player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE) != null)
            EntityUtils.resetAttribute(player, stack, Attributes.ENTITY_INTERACTION_RANGE, (float) amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        if (player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE) != null)
            EntityUtils.resetAttribute(player, stack, Attributes.BLOCK_INTERACTION_RANGE, (float) amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private void removeRangeBonuses(Player player, ItemStack stack) {
        if (player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE) != null)
            EntityUtils.removeAttribute(player, stack, Attributes.ENTITY_INTERACTION_RANGE, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        if (player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE) != null)
            EntityUtils.removeAttribute(player, stack, Attributes.BLOCK_INTERACTION_RANGE, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static double distanceToBlock(Player player, BlockPos pos) {
        return player.getEyePosition().distanceTo(pos.getCenter());
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamageDealing(LivingIncomingDamageEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player) || event.getSource().getDirectEntity() != player || player.level().isClientSide() || event.getEntity() == player || event.getAmount() <= 0F)
                return;

            var distance = player.distanceTo(event.getEntity());
            var distanceBonusPerBlock = 0D;
            var emptyHandKnockback = 0D;
            var stunTicks = 0;
            var canApplyLongReachStun = false;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.POCKET_PISTON.value())) {
                if (!(stack.getItem() instanceof PocketPistonItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("piston");

                if (!ability.canPlayerUse(player))
                    continue;

                if (ability.isRankModifierUnlocked("repulse") && player.getMainHandItem().isEmpty())
                    emptyHandKnockback = Math.max(emptyHandKnockback, Math.max(0D, ability.getStatData("empty_hand_knockback").getValue()));

                if (ability.isRankModifierUnlocked("distance_power"))
                    distanceBonusPerBlock = Math.max(distanceBonusPerBlock, Math.max(0D, Math.min(1D, ability.getStatData("distance_bonus_per_block").getValue())));

                if (ability.isRankModifierUnlocked("long_reach_stun")) {
                    var ticks = Math.max(0, (int) Math.round(Math.max(0D, ability.getStatData("stun_duration").getValue()) * 20D));

                    stunTicks = Math.max(stunTicks, ticks);
                    canApplyLongReachStun = true;
                }
            }

            if (distanceBonusPerBlock > 0D)
                event.setAmount((float) (event.getAmount() * (1D + distanceBonusPerBlock * distance)));

            if (emptyHandKnockback > 0D && event.getEntity() instanceof LivingEntity target)
                target.knockback(emptyHandKnockback, player.getX() - target.getX(), player.getZ() - target.getZ());

            if (!canApplyLongReachStun || stunTicks <= 0 || !(event.getEntity() instanceof LivingEntity target))
                return;

            var maxRange = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);

            if (maxRange > 0D && distance >= maxRange * 0.9D)
                target.addEffect(new MobEffectInstance(RelicsMobEffects.STUN, stunTicks, 0, false, true));
        }

        @SubscribeEvent
        public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
            var player = event.getEntity();

            if (player.level().isClientSide() || event.getNewSpeed() <= 0F)
                return;

            var pos = event.getPosition().orElse(null);

            if (pos == null)
                return;

            var distance = distanceToBlock(player, pos);
            var bonusPerBlock = 0D;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.POCKET_PISTON.value())) {
                if (!(stack.getItem() instanceof PocketPistonItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("piston");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("distance_power"))
                    continue;

                bonusPerBlock = Math.max(bonusPerBlock, Math.max(0D, Math.min(1D, ability.getStatData("distance_bonus_per_block").getValue())));
            }

            if (bonusPerBlock > 0D)
                event.setNewSpeed((float) Math.max(0D, event.getNewSpeed() * (1D + bonusPerBlock * distance)));
        }
    }
}