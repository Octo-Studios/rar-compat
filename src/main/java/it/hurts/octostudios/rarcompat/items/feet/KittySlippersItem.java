package it.hurts.octostudios.rarcompat.items.feet;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.init.DataComponentRegistry;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.AbilityMetricTemplate;
import it.hurts.sskirillss.relics.api.relics.AbilityStatisticTemplate;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.VisibilityState;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.ExperienceSourceTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.ExperienceSourcesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

public class KittySlippersItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("feline_aura")
                                .rankModifier(1, "crouch_speed")
                                .stat(AbilityStatTemplate.builder("radius")
                                        .thresholdValue(1D, Double.MAX_VALUE)
                                        .initialValue(8D, 16D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("crouch_speed_bonus")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.12D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("crouch_movement_time")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("crouch_speed", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .ability(AbilityTemplate.builder("nine_lives")
                                .rankModifier(3, "soft_landing")
                                .rankModifier(5, "evasion")
                                .stat(AbilityStatTemplate.builder("survival_chance")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.08D, 0.2D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("fall_damage_reduction")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.12D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("evasion_chance")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.08D, 0.25D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("survival_trigger").build())
                                        .source(ExperienceSourceTemplate.builder("evasion_miss")
                                                .rankModifierVisibilityState("evasion", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("survival_triggers")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("fall_damage_reduced")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("soft_landing", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("evasion_misses")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("evasion", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide())
            return;

        var aura = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("feline_aura");

        if (aura.canPlayerUse(player) && player.tickCount % 10 == 0) {
            var radius = Math.max(0D, aura.getStatData("radius").getValue());

            if (radius > 0D)
                applyFelineAura(player, radius);
        }

        if (aura.canPlayerUse(player) && aura.isRankModifierUnlocked("crouch_speed")) {
            var bonus = Math.max(0D, aura.getStatData("crouch_speed_bonus").getValue());

            if (bonus > 0D)
                EntityUtils.resetAttribute(player, stack, Attributes.SNEAKING_SPEED, (float) (bonus * 0.3D), AttributeModifier.Operation.ADD_VALUE);
            else
                EntityUtils.removeAttribute(player, stack, Attributes.SNEAKING_SPEED, AttributeModifier.Operation.ADD_VALUE);

            if (player.tickCount % 20 == 0 && player.isShiftKeyDown() && player.getKnownMovement().multiply(1D, 0D, 1D).length() > 1.0E-4D)
                aura.getStatisticData().getMetricData("crouch_movement_time").addValue(1D);
        } else {
            EntityUtils.removeAttribute(player, stack, Attributes.SNEAKING_SPEED, AttributeModifier.Operation.ADD_VALUE);
        }

        var nineLives = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("nine_lives");

        if (!nineLives.canPlayerUse(player)) {
            setDodgeReady(stack, false);
            return;
        }

        if (!nineLives.isRankModifierUnlocked("evasion"))
            setDodgeReady(stack, false);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (stack.getItem() == newStack.getItem() || !(slotContext.entity() instanceof Player player) || player.level().isClientSide())
            return;

        setDodgeReady(stack, false);
        EntityUtils.removeAttribute(player, stack, Attributes.SNEAKING_SPEED, AttributeModifier.Operation.ADD_VALUE);
    }

    private boolean isDodgeReady(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.KITTY_SLIPPERS_DODGE_READY.get(), false);
    }

    private void setDodgeReady(ItemStack stack, boolean value) {
        stack.set(DataComponentRegistry.KITTY_SLIPPERS_DODGE_READY.get(), value);
    }

    private static void applyFelineAura(Player player, double radius) {
        var level = player.level();
        var maxDistanceSq = radius * radius;

        for (var creeper : level.getEntitiesOfClass(Creeper.class, player.getBoundingBox().inflate(radius), entity -> entity.isAlive())) {
            if (creeper.distanceToSqr(player) > maxDistanceSq)
                continue;

            makeMobAvoidPlayer(creeper, player);
            creeper.setSwellDir(-1);
        }

        for (var phantom : level.getEntitiesOfClass(Phantom.class, player.getBoundingBox().inflate(radius), entity -> entity.isAlive())) {
            if (phantom.distanceToSqr(player) > maxDistanceSq)
                continue;

            makeMobAvoidPlayer(phantom, player);
        }
    }

    public static boolean shouldSuppressCreeperExplosion(Creeper creeper) {
        if (creeper == null || !creeper.isAlive() || creeper.level().isClientSide())
            return false;

        var level = creeper.level();

        for (var player : level.players()) {
            if (!player.isAlive() || player.isSpectator())
                continue;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.KITTY_SLIPPERS.value())) {
                if (!(stack.getItem() instanceof KittySlippersItem relic))
                    continue;

                var aura = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("feline_aura");

                if (!aura.canPlayerUse(player))
                    continue;

                var radius = Math.max(0D, aura.getStatData("radius").getValue());

                if (radius > 0D && creeper.distanceToSqr(player) <= radius * radius)
                    return true;
            }
        }

        return false;
    }

    private static void makeMobAvoidPlayer(Mob mob, Player player) {
        if (mob.getTarget() == player)
            mob.setTarget(null);

        var awayPos = mob instanceof PathfinderMob pathfinderMob
                ? DefaultRandomPos.getPosAway(pathfinderMob, 16, 7, player.position())
                : null;

        if (awayPos == null) {
            var awayDirection = mob.position().subtract(player.position());

            if (awayDirection.lengthSqr() > 1.0E-6D)
                awayPos = mob.position().add(awayDirection.normalize().scale(8D));
        }

        if (awayPos != null)
            mob.getNavigation().moveTo(awayPos.x, awayPos.y, awayPos.z, 1.25D);

        var direction = mob.position().subtract(player.position());

        if (direction.lengthSqr() > 1.0E-6D) {
            var push = direction.normalize().scale(0.18D);

            mob.setDeltaMovement(mob.getDeltaMovement().add(push.x, mob.onGround() ? 0.1D : 0D, push.z));
        }
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getAmount() <= 0F)
                return;

            var originalAmount = event.getAmount();
            var dodgeChance = 0D;
            KittySlippersItem dodgeRelic = null;
            ItemStack dodgeStack = ItemStack.EMPTY;
            var hasDodgeRoll = false;
            var fallReduction = 0D;
            KittySlippersItem fallRelic = null;
            ItemStack fallStack = ItemStack.EMPTY;
            var isFallDamage = event.getSource().is(DamageTypes.FALL);

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.KITTY_SLIPPERS.value())) {
                if (!(stack.getItem() instanceof KittySlippersItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("nine_lives");

                if (!ability.canPlayerUse(player)) {
                    relic.setDodgeReady(stack, false);
                    continue;
                }

                if (ability.isRankModifierUnlocked("evasion") && relic.isDodgeReady(stack)) {
                    hasDodgeRoll = true;
                    var value = Math.max(0D, Math.min(1D, ability.getStatData("evasion_chance").getValue()));

                    if (dodgeRelic == null || value > dodgeChance) {
                        dodgeChance = value;
                        dodgeRelic = relic;
                        dodgeStack = stack;
                    }

                    relic.setDodgeReady(stack, false);
                }

                if (isFallDamage && ability.isRankModifierUnlocked("soft_landing")) {
                    var value = Math.max(0D, Math.min(1D, ability.getStatData("fall_damage_reduction").getValue()));

                    if (fallRelic == null || value > fallReduction) {
                        fallReduction = value;
                        fallRelic = relic;
                        fallStack = stack;
                    }
                }
            }

            if (hasDodgeRoll && dodgeChance > 0D && player.getRandom().nextDouble() <= dodgeChance) {
                event.setAmount(0F);
                event.setCanceled(true);

                if (dodgeRelic != null && !dodgeStack.isEmpty()) {
                    var relicData = dodgeRelic.getRelicData(player, dodgeStack);
                    var ability = relicData.getAbilitiesData().getAbilityData("nine_lives");

                    relicData.getLevelingData().addExperience("nine_lives", "evasion_miss", 1D);
                    ability.getStatisticData().getMetricData("evasion_misses").addValue(1D);
                }

                return;
            }

            if (isFallDamage && fallReduction > 0D) {
                var newAmount = (float) (originalAmount * (1D - fallReduction));
                event.setAmount(newAmount);

                var reduced = Math.max(0F, originalAmount - newAmount);

                if (reduced > 0F && fallRelic != null && !fallStack.isEmpty())
                    fallRelic.getRelicData(player, fallStack).getAbilitiesData().getAbilityData("nine_lives")
                            .getStatisticData().getMetricData("fall_damage_reduced").addValue(reduced);
            }
        }

        @SubscribeEvent
        public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
            var entity = event.getEntity();

            if (!(entity instanceof Player player) || entity.level().isClientSide() || event.getNewDamage() <= 0F)
                return;

            var lethalThreshold = entity.getHealth() + entity.getAbsorptionAmount();

            if (event.getNewDamage() < lethalThreshold)
                return;

            var chance = 0D;
            KittySlippersItem survivalRelic = null;
            ItemStack survivalStack = ItemStack.EMPTY;

            for (var stack : EntityUtils.findEquippedCurios(entity, ModItems.KITTY_SLIPPERS.value())) {
                if (!(stack.getItem() instanceof KittySlippersItem relic))
                    continue;

                var ability = relic.getRelicData(entity, stack).getAbilitiesData().getAbilityData("nine_lives");

                if (!ability.canPlayerUse(entity)) {
                    relic.setDodgeReady(stack, false);
                    continue;
                }

                var value = Math.max(0D, Math.min(1D, ability.getStatData("survival_chance").getValue()));

                if (survivalRelic == null || value > chance) {
                    chance = value;
                    survivalRelic = relic;
                    survivalStack = stack;
                }
            }

            if (chance <= 0D || survivalRelic == null || survivalStack.isEmpty() || entity.getRandom().nextDouble() > chance)
                return;

            var safeDamage = Math.max(0F, lethalThreshold - 1F);

            event.setNewDamage(Math.min(event.getNewDamage(), safeDamage));

            var relicData = survivalRelic.getRelicData(player, survivalStack);
            var ability = relicData.getAbilitiesData().getAbilityData("nine_lives");

            relicData.getLevelingData().addExperience("nine_lives", "survival_trigger", 1D);
            ability.getStatisticData().getMetricData("survival_triggers").addValue(1D);
        }

        @SubscribeEvent
        public static void onLivingDamagePost(LivingDamageEvent.Post event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getNewDamage() <= 0F)
                return;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.KITTY_SLIPPERS.value())) {
                if (!(stack.getItem() instanceof KittySlippersItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("nine_lives");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("evasion")) {
                    relic.setDodgeReady(stack, false);
                    continue;
                }

                relic.setDodgeReady(stack, true);
            }
        }
    }
}
