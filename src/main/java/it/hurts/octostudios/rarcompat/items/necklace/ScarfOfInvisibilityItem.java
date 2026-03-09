package it.hurts.octostudios.rarcompat.items.necklace;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.init.DataComponentRegistry;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsMobEffects;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import top.theillusivec4.curios.api.SlotContext;

public class ScarfOfInvisibilityItem extends WearableRelicItem {
    private static final double STILL_HORIZONTAL_THRESHOLD = 1.0E-4D;
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("invisibility")
                                .rankModifier(1, "strike")
                                .rankModifier(3, "stun")
                                .rankModifier(5, "regeneration")
                                .stat(AbilityStatTemplate.builder("delay")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(2D, 5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("cooldown")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(10D, 6D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), -0.06D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("damage")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(0.1D, 0.25D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("stun")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(0.75D, 2D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 2))
                                        .build())
                                .stat(AbilityStatTemplate.builder("regeneration")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(0.5D, 2D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 2))
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        var entity = slotContext.entity();

        if (entity.level().isClientSide())
            return;

        var ability = this.getRelicData(entity, stack).getAbilitiesData().getAbilityData("invisibility");

        if (!ability.canPlayerUse(entity)) {
            if (isInvisibilityActive(stack))
                entity.removeEffect(RelicsMobEffects.VANISHING);

            setInvisibilityActive(stack, false);
            setStationaryTicks(stack, 0);
            setInvisibilityCooldown(stack, 0);
            setNeedsOutOfSight(stack, false);
            setStrikeTicks(stack, 0);

            return;
        }

        if (getInvisibilityCooldown(stack) > 0)
            addInvisibilityCooldown(stack, -1);

        if (getStrikeTicks(stack) > 0)
            addStrikeTicks(stack, -1);

        if (needsOutOfSight(stack) && !isTrackedByVisibleTargets(entity))
            setNeedsOutOfSight(stack, false);

        if (isInvisibilityActive(stack)) {
            entity.addEffect(new MobEffectInstance(RelicsMobEffects.VANISHING, 10, 0, false, false));

            if (ability.isRankModifierUnlocked("regeneration") && entity.tickCount % 20 == 0) {
                var heal = (float) Math.max(0D, ability.getStatData("regeneration").getValue());

                if (heal > 0F)
                    entity.heal(heal);
            }

            return;
        }

        if (needsOutOfSight(stack)) {
            setStationaryTicks(stack, 0);
            return;
        }

        if (!isStandingStill(entity)) {
            setStationaryTicks(stack, 0);
            return;
        }

        addStationaryTicks(stack, 1);

        if (getInvisibilityCooldown(stack) > 0)
            return;

        if (getStationaryTicks(stack) < getDelayTicks(entity, stack))
            return;

        setInvisibilityActive(stack, true);
        setStationaryTicks(stack, 0);
        entity.addEffect(new MobEffectInstance(RelicsMobEffects.VANISHING, 10, 0, false, false));
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (stack.getItem() == newStack.getItem())
            return;

        var entity = slotContext.entity();

        if (!entity.level().isClientSide() && isInvisibilityActive(stack))
            entity.removeEffect(RelicsMobEffects.VANISHING);

        setInvisibilityActive(stack, false);
        setStationaryTicks(stack, 0);
        setInvisibilityCooldown(stack, 0);
        setNeedsOutOfSight(stack, false);
        setStrikeTicks(stack, 0);
    }

    private int getDelayTicks(LivingEntity entity, ItemStack stack) {
        var ability = this.getRelicData(entity, stack).getAbilitiesData().getAbilityData("invisibility");
        var seconds = Math.max(0D, ability.getStatData("delay").getValue());

        return Math.max(1, (int) Math.round(seconds * 20D));
    }

    private int getConfiguredCooldownTicks(LivingEntity entity, ItemStack stack) {
        return getDelayTicks(entity, stack);
    }

    private static boolean isStandingStill(LivingEntity entity) {
        if (!entity.onGround())
            return false;

        return entity.getDeltaMovement().horizontalDistanceSqr() <= STILL_HORIZONTAL_THRESHOLD;
    }

    private static boolean isTrackedByVisibleTargets(LivingEntity entity) {
        var level = entity.getCommandSenderWorld();

        return level.getEntitiesOfClass(Mob.class, entity.getBoundingBox().inflate(16D)).stream()
                .anyMatch(mob -> mob.getTarget() == entity && mob.hasLineOfSight(entity));
    }

    private int getStationaryTicks(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.SCARF_OF_INVISIBILITY_STATIONARY_TICKS.get(), 0);
    }

    private void setStationaryTicks(ItemStack stack, int ticks) {
        stack.set(DataComponentRegistry.SCARF_OF_INVISIBILITY_STATIONARY_TICKS.get(), Math.max(0, ticks));
    }

    private void addStationaryTicks(ItemStack stack, int ticks) {
        setStationaryTicks(stack, getStationaryTicks(stack) + ticks);
    }

    private int getInvisibilityCooldown(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.SCARF_OF_INVISIBILITY_COOLDOWN.get(), 0);
    }

    private void setInvisibilityCooldown(ItemStack stack, int cooldown) {
        stack.set(DataComponentRegistry.SCARF_OF_INVISIBILITY_COOLDOWN.get(), Math.max(0, cooldown));
    }

    private void addInvisibilityCooldown(ItemStack stack, int ticks) {
        setInvisibilityCooldown(stack, getInvisibilityCooldown(stack) + ticks);
    }

    private boolean isInvisibilityActive(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.SCARF_OF_INVISIBILITY_ACTIVE.get(), false);
    }

    private void setInvisibilityActive(ItemStack stack, boolean value) {
        stack.set(DataComponentRegistry.SCARF_OF_INVISIBILITY_ACTIVE.get(), value);
    }

    private boolean needsOutOfSight(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.SCARF_OF_INVISIBILITY_NEEDS_OUT_OF_SIGHT.get(), false);
    }

    private void setNeedsOutOfSight(ItemStack stack, boolean value) {
        stack.set(DataComponentRegistry.SCARF_OF_INVISIBILITY_NEEDS_OUT_OF_SIGHT.get(), value);
    }

    private int getStrikeTicks(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.SCARF_OF_INVISIBILITY_STRIKE_TICKS.get(), 0);
    }

    private void setStrikeTicks(ItemStack stack, int ticks) {
        stack.set(DataComponentRegistry.SCARF_OF_INVISIBILITY_STRIKE_TICKS.get(), Math.max(0, ticks));
    }

    private void addStrikeTicks(ItemStack stack, int ticks) {
        setStrikeTicks(stack, getStrikeTicks(stack) + ticks);
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        private static void onInteract(LivingEntity entity) {
            onInteract(entity, false);
        }

        private static void onInteract(LivingEntity entity, boolean prepareStrike) {
            if (entity.getCommandSenderWorld().isClientSide())
                return;

            for (var stack : EntityUtils.findEquippedCurios(entity, ModItems.SCARF_OF_INVISIBILITY.value())) {
                var relic = (ScarfOfInvisibilityItem) stack.getItem();
                var ability = relic.getRelicData(entity, stack).getAbilitiesData().getAbilityData("invisibility");

                if (!ability.canPlayerUse(entity) || !relic.isInvisibilityActive(stack))
                    continue;

                if (prepareStrike && (ability.isRankModifierUnlocked("strike") || ability.isRankModifierUnlocked("stun")))
                    relic.setStrikeTicks(stack, 2);

                relic.setInvisibilityActive(stack, false);
                relic.setStationaryTicks(stack, 0);
                relic.setNeedsOutOfSight(stack, true);
                relic.setInvisibilityCooldown(stack, relic.getConfiguredCooldownTicks(entity, stack));

                entity.removeEffect(RelicsMobEffects.VANISHING);
            }
        }

        @SubscribeEvent
        public static void onLivingHurt3(LivingIncomingDamageEvent event) {
            onInteract(event.getEntity());
        }

        @SubscribeEvent
        public static void onLivingIncomingDamageDealing(LivingIncomingDamageEvent event) {
            if (!(event.getSource().getEntity() instanceof LivingEntity entity) || entity.level().isClientSide() || event.getEntity() == entity)
                return;

            for (var stack : EntityUtils.findEquippedCurios(entity, ModItems.SCARF_OF_INVISIBILITY.value())) {
                if (!(stack.getItem() instanceof ScarfOfInvisibilityItem relic))
                    continue;

                var ability = relic.getRelicData(entity, stack).getAbilitiesData().getAbilityData("invisibility");

                if (!ability.canPlayerUse(entity) || relic.getStrikeTicks(stack) <= 0)
                    continue;

                if (ability.isRankModifierUnlocked("strike")) {
                    var bonus = Math.max(0D, ability.getStatData("damage").getValue());

                    if (bonus > 0D)
                        event.setAmount((float) (event.getAmount() * (1D + bonus)));
                }

                if (ability.isRankModifierUnlocked("stun") && event.getEntity() instanceof LivingEntity target) {
                    var stunSeconds = Math.max(0D, ability.getStatData("stun").getValue());
                    var stunTicks = Math.max(0, (int) Math.round(stunSeconds * 20D));

                    if (stunTicks > 0)
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stunTicks, 6, false, true));
                }

                relic.setStrikeTicks(stack, 0);
            }
        }

        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            onInteract(event.getEntity());
        }

        @SubscribeEvent
        public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
            onInteract(event.getEntity());
        }

        @SubscribeEvent
        public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
            onInteract(event.getEntity());
        }

        @SubscribeEvent
        public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
            onInteract(event.getEntity());
        }

        @SubscribeEvent
        public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
            onInteract(event.getEntity());
        }

        @SubscribeEvent
        public static void onBlockBreakAttempt(PlayerEvent.BreakSpeed event) {
            onInteract(event.getEntity());
        }

        @SubscribeEvent
        public static void onAttackEntity(AttackEntityEvent event) {
            onInteract(event.getEntity(), true);
        }

        @SubscribeEvent
        public static void onItemToss(ItemTossEvent event) {
            onInteract(event.getPlayer());
        }

        @SubscribeEvent
        public static void onItemPickup(ItemEntityPickupEvent.Post event) {
            onInteract(event.getPlayer());
        }
    }
}
