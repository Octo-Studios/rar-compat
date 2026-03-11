package it.hurts.octostudios.rarcompat.items.necklace;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.init.DataComponentRegistry;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

public class CharmOfShrinkingItem extends WearableRelicItem {
    private static final ResourceLocation SCALE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(RARCompat.MODID, "charm_of_shrinking_scale");

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("size")
                                .modes("stabilize", "shrink", "grow")
                                .rankModifier(1, "fall_resistance")
                                .rankModifier(3, "target_escape")
                                .rankModifier(5, "evasion")
                                .stat(AbilityStatTemplate.builder("min_scale")
                                        .thresholdValue(0.125D, 1D)
                                        .initialValue(0.75D, 0.45D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), -0.06D)
                                        .formatValue(value -> MathUtils.round(value, 2))
                                        .build())
                                .stat(AbilityStatTemplate.builder("change_per_second")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(0.04D, 0.12D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 2))
                                        .build())
                                .stat(AbilityStatTemplate.builder("fall_reduction_per_size")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.08D, 0.2D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("target_loss_chance_per_size")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.08D, 0.2D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("target_loss_radius")
                                        .thresholdValue(1D, Double.MAX_VALUE)
                                        .initialValue(4D, 10D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("evasion_per_size")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.08D, 0.2D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide())
            return;

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("size");
        var currentScale = getCurrentScale(stack);

        if (!ability.canPlayerUse(player)) {
            applyScaleModifier(player, currentScale);
            return;
        }

        var changePerSecond = Math.max(0D, ability.getStatData("change_per_second").getValue());
        var stepPerTick = changePerSecond / 20D;
        var targetScale = currentScale;

        switch (ability.getMode()) {
            case "shrink" -> targetScale = clampScale(Math.min(1D, ability.getStatData("min_scale").getValue()));
            case "grow" -> targetScale = 1D;
            default -> {
            }
        }

        var newScale = stepPerTick > 0D ? moveTowards(currentScale, targetScale, stepPerTick) : currentScale;

        newScale = clampScale(newScale);

        setCurrentScale(stack, newScale);
        applyScaleModifier(player, newScale);

        if (!ability.isRankModifierUnlocked("target_escape") || newScale >= 1D || player.tickCount % 20 != 0)
            return;

        var chancePerSize = Math.max(0D, ability.getStatData("target_loss_chance_per_size").getValue());
        var chance = Math.max(0D, Math.min(1D, (1D - newScale) * chancePerSize));
        var radius = Math.max(0D, ability.getStatData("target_loss_radius").getValue());

        if (chance <= 0D || radius <= 0D)
            return;

        var maxDistanceSq = radius * radius;

        for (var mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius), entity -> entity.isAlive() && entity.getTarget() == player)) {
            if (mob.distanceToSqr(player) > maxDistanceSq || player.getRandom().nextDouble() > chance)
                continue;

            mob.setTarget(null);
            mob.getNavigation().stop();
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (stack.getItem() == newStack.getItem())
            return;

        if (slotContext.entity() instanceof Player player && !player.level().isClientSide())
            removeScaleModifier(player);

        setCurrentScale(stack, 1D);
    }

    private void applyScaleModifier(Player player, double scale) {
        var attribute = player.getAttribute(Attributes.SCALE);

        if (attribute == null)
            return;

        var amount = clampScale(scale) - 1D;

        attribute.addOrUpdateTransientModifier(new AttributeModifier(SCALE_MODIFIER_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private void removeScaleModifier(Player player) {
        var attribute = player.getAttribute(Attributes.SCALE);

        if (attribute == null)
            return;

        attribute.removeModifier(new AttributeModifier(SCALE_MODIFIER_ID, 0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private double getCurrentScale(ItemStack stack) {
        return clampScale(stack.getOrDefault(DataComponentRegistry.CHARM_OF_SHRINKING_CURRENT_SCALE.get(), 1D));
    }

    private void setCurrentScale(ItemStack stack, double scale) {
        stack.set(DataComponentRegistry.CHARM_OF_SHRINKING_CURRENT_SCALE.get(), clampScale(scale));
    }

    private static double moveTowards(double current, double target, double maxDelta) {
        if (maxDelta <= 0D)
            return current;

        if (current < target)
            return Math.min(target, current + maxDelta);

        if (current > target)
            return Math.max(target, current - maxDelta);

        return current;
    }

    private static double clampScale(double scale) {
        return Math.max(0.125D, Math.min(10D, scale));
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getAmount() <= 0F)
                return;

            var isFallDamage = event.getSource().is(DamageTypes.FALL);
            var fallReduction = 0D;
            var evasionChance = 0D;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.CHARM_OF_SHRINKING.value())) {
                if (!(stack.getItem() instanceof CharmOfShrinkingItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("size");

                if (!ability.canPlayerUse(player))
                    continue;

                var scale = relic.getCurrentScale(stack);

                if (scale >= 1D)
                    continue;

                var shrinkDelta = 1D - scale;

                if (isFallDamage && ability.isRankModifierUnlocked("fall_resistance")) {
                    var perSize = Math.max(0D, ability.getStatData("fall_reduction_per_size").getValue());
                    var reduction = Math.max(0D, Math.min(1D, shrinkDelta * perSize));

                    fallReduction = Math.max(fallReduction, reduction);
                }

                if (ability.isRankModifierUnlocked("evasion")) {
                    var perSize = Math.max(0D, ability.getStatData("evasion_per_size").getValue());
                    var chance = Math.max(0D, Math.min(1D, shrinkDelta * perSize));

                    evasionChance = Math.max(evasionChance, chance);
                }
            }

            if (isFallDamage && fallReduction > 0D)
                event.setAmount((float) Math.max(0D, event.getAmount() * (1D - fallReduction)));

            if (evasionChance <= 0D || player.getRandom().nextDouble() > evasionChance)
                return;

            event.setAmount(0F);
            event.setCanceled(true);
        }
    }
}
