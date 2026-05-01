package it.hurts.shatterbyte.reliquified_artifacts.items.necklace;

import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import artifacts.registry.ModItems;
import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
import it.hurts.shatterbyte.reliquified_artifacts.init.RADataComponent;
import it.hurts.shatterbyte.reliquified_artifacts.items.base.RAWearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.AbilityMetricTemplate;
import it.hurts.sskirillss.relics.api.relics.AbilityStatisticTemplate;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.VisibilityState;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.ExperienceSourceTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.ExperienceSourcesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsMobEffects;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.effect.MobEffectInstance;
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

public class ScarfOfInvisibilityItem extends RAWearableRelicItem {
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
                                        .initialValue(10D, 15D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), -0.0143D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("cooldown")
                                        .initialValue(25D, 30D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), -0.0229D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("damage")
                                        .initialValue(0.25D, 0.5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.1143D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("stun")
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.019D)
                                        .formatValue(value -> MathUtils.round(value, 2))
                                        .build())
                                .stat(AbilityStatTemplate.builder("regeneration")
                                        .initialValue(0.1D, 0.25D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0857D)
                                        .formatValue(value -> MathUtils.round(value, 2))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("toggle").build())
                                        .source(ExperienceSourceTemplate.builder("attack")
                                                .rankModifierVisibilityState("strike", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("entries")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("duration")
                                                .formatValue(value -> MathUtils.formatTime(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("attacks")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("strike", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("bonus_damage")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("strike", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("stun_time")
                                                .formatValue(value -> MathUtils.formatTime(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("stun", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("healing")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("regeneration", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.SCULK, LootEntries.CAVE)
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        var entity = slotContext.entity();

        if (entity.level().isClientSide())
            return;

        var relicData = this.getRelicData(entity, stack);
        var ability = relicData.getAbilitiesData().getAbilityData("invisibility");

        if (!ability.canPlayerUse(entity)) {
            if (isInvisibilityActive(stack)) {
                entity.removeEffect(RelicsMobEffects.VANISHING);
                onInvisibilityExit(entity, stack);
            }

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

            if (entity.tickCount % 20 == 0)
                ability.getStatisticData().getMetricData("duration").addValue(1D);

            if (ability.getRankModifierData("regeneration").isUnlocked() && entity.tickCount % 20 == 0) {
                var heal = (float) Math.max(0D, ability.getStatData("regeneration").getValue());

                if (heal > 0F) {
                    var beforeHealth = entity.getHealth();

                    entity.heal(heal);

                    var restored = Math.max(0F, entity.getHealth() - beforeHealth);

                    if (restored > 0F)
                        ability.getStatisticData().getMetricData("healing").addValue(restored);
                }
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
        onInvisibilityEnter(entity, stack);
        entity.addEffect(new MobEffectInstance(RelicsMobEffects.VANISHING, 10, 0, false, false));
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (stack.getItem() == newStack.getItem())
            return;

        var entity = slotContext.entity();

        if (!entity.level().isClientSide() && isInvisibilityActive(stack)) {
            entity.removeEffect(RelicsMobEffects.VANISHING);
            onInvisibilityExit(entity, stack);
        }

        setInvisibilityActive(stack, false);
        setStationaryTicks(stack, 0);
        setInvisibilityCooldown(stack, 0);
        setNeedsOutOfSight(stack, false);
        setStrikeTicks(stack, 0);
    }

    private void onInvisibilityEnter(LivingEntity entity, ItemStack stack) {
        var relicData = this.getRelicData(entity, stack);
        var ability = relicData.getAbilitiesData().getAbilityData("invisibility");

        ability.getStatisticData().getMetricData("entries").addValue(1D);
        relicData.getLevelingData().addExperience("invisibility", "toggle", 1D);
    }

    private void onInvisibilityExit(LivingEntity entity, ItemStack stack) {
        this.getRelicData(entity, stack).getLevelingData().addExperience("invisibility", "toggle", 1D);
    }

    private void onInvisibilityAttack(LivingEntity entity, ItemStack stack) {
        var relicData = this.getRelicData(entity, stack);
        var ability = relicData.getAbilitiesData().getAbilityData("invisibility");

        if (!ability.getRankModifierData("strike").isUnlocked())
            return;

        ability.getStatisticData().getMetricData("attacks").addValue(1D);
        relicData.getLevelingData().addExperience("invisibility", "attack", 1D);
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

        if (entity.getKnownMovement().multiply(1D, 0D, 1D).length() > STILL_HORIZONTAL_THRESHOLD)
            return false;

        var deltaX = entity.getX() - entity.xOld;
        var deltaZ = entity.getZ() - entity.zOld;

        return deltaX * deltaX + deltaZ * deltaZ <= STILL_HORIZONTAL_THRESHOLD;
    }

    private static boolean isTrackedByVisibleTargets(LivingEntity entity) {
        var level = entity.getCommandSenderWorld();

        return level.getEntitiesOfClass(Mob.class, entity.getBoundingBox().inflate(16D)).stream()
                .anyMatch(mob -> mob.getTarget() == entity && mob.hasLineOfSight(entity));
    }

    private int getStationaryTicks(ItemStack stack) {
        return stack.getOrDefault(RADataComponent.SCARF_OF_INVISIBILITY_STATIONARY_TICKS.get(), 0);
    }

    private void setStationaryTicks(ItemStack stack, int ticks) {
        stack.set(RADataComponent.SCARF_OF_INVISIBILITY_STATIONARY_TICKS.get(), Math.max(0, ticks));
    }

    private void addStationaryTicks(ItemStack stack, int ticks) {
        setStationaryTicks(stack, getStationaryTicks(stack) + ticks);
    }

    private int getInvisibilityCooldown(ItemStack stack) {
        return stack.getOrDefault(RADataComponent.SCARF_OF_INVISIBILITY_COOLDOWN.get(), 0);
    }

    private void setInvisibilityCooldown(ItemStack stack, int cooldown) {
        stack.set(RADataComponent.SCARF_OF_INVISIBILITY_COOLDOWN.get(), Math.max(0, cooldown));
    }

    private void addInvisibilityCooldown(ItemStack stack, int ticks) {
        setInvisibilityCooldown(stack, getInvisibilityCooldown(stack) + ticks);
    }

    private boolean isInvisibilityActive(ItemStack stack) {
        return stack.getOrDefault(RADataComponent.SCARF_OF_INVISIBILITY_ACTIVE.get(), false);
    }

    private void setInvisibilityActive(ItemStack stack, boolean value) {
        stack.set(RADataComponent.SCARF_OF_INVISIBILITY_ACTIVE.get(), value);
    }

    private boolean needsOutOfSight(ItemStack stack) {
        return stack.getOrDefault(RADataComponent.SCARF_OF_INVISIBILITY_NEEDS_OUT_OF_SIGHT.get(), false);
    }

    private void setNeedsOutOfSight(ItemStack stack, boolean value) {
        stack.set(RADataComponent.SCARF_OF_INVISIBILITY_NEEDS_OUT_OF_SIGHT.get(), value);
    }

    private int getStrikeTicks(ItemStack stack) {
        return stack.getOrDefault(RADataComponent.SCARF_OF_INVISIBILITY_STRIKE_TICKS.get(), 0);
    }

    private void setStrikeTicks(ItemStack stack, int ticks) {
        stack.set(RADataComponent.SCARF_OF_INVISIBILITY_STRIKE_TICKS.get(), Math.max(0, ticks));
    }

    private void addStrikeTicks(ItemStack stack, int ticks) {
        setStrikeTicks(stack, getStrikeTicks(stack) + ticks);
    }

    @EventBusSubscriber(modid = ReliquifiedArtifacts.MODID)
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

                if (!ability.canPlayerUse(entity))
                    continue;

                if (!relic.isInvisibilityActive(stack)) {
                    relic.setStationaryTicks(stack, 0);
                    continue;
                }

                if (prepareStrike)
                    relic.onInvisibilityAttack(entity, stack);

                if (prepareStrike && (ability.getRankModifierData("strike").isUnlocked() || ability.getRankModifierData("stun").isUnlocked()))
                    relic.setStrikeTicks(stack, 2);

                relic.onInvisibilityExit(entity, stack);

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

                if (ability.getRankModifierData("strike").isUnlocked()) {
                    var bonus = Math.max(0D, ability.getStatData("damage").getValue());

                    if (bonus > 0D) {
                        var baseDamage = event.getAmount();
                        var boostedDamage = (float) (baseDamage * (1D + bonus));
                        var additionalDamage = Math.max(0F, boostedDamage - baseDamage);

                        event.setAmount(boostedDamage);

                        if (additionalDamage > 0F)
                            ability.getStatisticData().getMetricData("bonus_damage").addValue(additionalDamage);
                    }
                }

                if (ability.getRankModifierData("stun").isUnlocked() && event.getEntity() instanceof LivingEntity target) {
                    var stunSeconds = Math.max(0D, ability.getStatData("stun").getValue());
                    var stunTicks = Math.max(0, (int) Math.round(stunSeconds * 20D));

                    if (stunTicks > 0) {
                        target.addEffect(new MobEffectInstance(RelicsMobEffects.STUN, stunTicks, 0, false, true));
                        ability.getStatisticData().getMetricData("stun_time").addValue(stunTicks / 20D);
                    }
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
