package it.hurts.shatterbyte.reliquified_artifacts.items.charm;

import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import artifacts.registry.ModItems;
import be.florens.expandability.api.EventResult;
import be.florens.expandability.api.forge.PlayerSwimEvent;
import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
import it.hurts.shatterbyte.reliquified_artifacts.init.RADataComponent;
import it.hurts.shatterbyte.reliquified_artifacts.items.base.RAWearableRelicItem;
import it.hurts.shatterbyte.reliquified_artifacts.network.packets.FlamingoSwimPacket;
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
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

public class HeliumFlamingoItem extends RAWearableRelicItem {

    private static final double STATIONARY_HORIZONTAL_SPEED_SQR = 0.0016D;
    private static final double STATIONARY_VERTICAL_SPEED = 0.04D;
    private static final double STATIONARY_POSITION_DELTA_SQR = 1.0E-6D;

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("flying")
                                .rankModifier(1, "aerial_archery")
                                .rankModifier(3, "aerial_guard")
                                .rankModifier(5, "efficient_hover")
                                .stat(AbilityStatTemplate.builder("time")
                                        .initialValue(5D, 10D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.1429D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("speed_bonus")
                                        .initialValue(0.15D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0531D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("ranged_damage_bonus")
                                        .initialValue(0.15D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0531D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("resistance")
                                        .initialValue(0.05D, 0.15D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0667D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("flight_second").build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("hover_duration")
                                                .formatValue(value -> MathUtils.formatTime(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("aerial_archery_bonus_damage")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("aerial_archery", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("aerial_guard_damage_reduced")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("aerial_guard", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.AQUATIC)
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player))
            return;

        var relicData = this.getRelicData(player, stack);
        var ability = relicData.getAbilitiesData().getAbilityData("flying");

        if (!ability.canPlayerUse(player)) {
            if (!player.level().isClientSide())
                setWaterTakeoffReady(stack, false);

            disableHover(player, stack, true);
            return;
        }

        if (!player.level().isClientSide()) {
            if (player.isSwimming() && player.isInWater())
                setWaterTakeoffReady(stack, true);

            if (getWaterTakeoffReady(stack) && !getToggled(stack) && player.isInWater() && !player.isUnderWater()
                    && getTime(stack) < getMaxHoverSeconds(player, stack) && trySetHovering(player, stack, true)) {
                setWaterTakeoffReady(stack, false);
            }

            if (player.onGround() || player.getAbilities().flying || player.isFallFlying() || player.isInLava())
                setWaterTakeoffReady(stack, false);
        }

        if (isHoverResetState(player)) {
            disableHover(player, stack, true);
            return;
        }

        if (!getToggled(stack)) {
            EntityUtils.removeAttribute(player, stack, Attributes.GRAVITY, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            return;
        }

        if (getTime(stack) >= getMaxHoverSeconds(player, stack)) {
            disableHover(player, stack, false);
            return;
        }

        EntityUtils.resetAttribute(player, stack, Attributes.GRAVITY, -1F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        player.fallDistance = 0F;

        if (player.tickCount % 20 == 0) {
            relicData.getLevelingData().addExperience("flying", "flight_second", 1D);
            ability.getStatisticData().getMetricData("hover_duration").addValue(1D);
        }

        if (!shouldConsumeHoverTime(player, ability.getRankModifierData("efficient_hover").isEnabled()) || player.tickCount % 20 != 0)
            return;

        addTime(stack, 1);

        if (getTime(stack) >= getMaxHoverSeconds(player, stack))
            disableHover(player, stack, false);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (!(slotContext.entity() instanceof Player player) || stack.getItem() == newStack.getItem())
            return;

        if (!player.level().isClientSide())
            setWaterTakeoffReady(stack, false);

        disableHover(player, stack, true);
    }

    public boolean trySetHovering(Player player, ItemStack stack, boolean toggled) {
        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("flying");

        if (!ability.canPlayerUse(player)) {
            disableHover(player, stack, true);
            return false;
        }

        if (!toggled) {
            setToggled(stack, false);
            setWaterTakeoffReady(stack, false);
            EntityUtils.removeAttribute(player, stack, Attributes.GRAVITY, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

            return true;
        }

        if (isHoverResetState(player) || getTime(stack) >= getMaxHoverSeconds(player, stack))
            return false;

        setToggled(stack, true);
        setWaterTakeoffReady(stack, false);
        EntityUtils.resetAttribute(player, stack, Attributes.GRAVITY, -1F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        player.fallDistance = 0F;

        return true;
    }

    public int getMaxHoverSeconds(Player player, ItemStack stack) {
        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("flying");

        return Math.max(1, (int) MathUtils.round(ability.getStatData("time").getValue(), 0));
    }

    public boolean isHovering(Player player, ItemStack stack) {
        if (!getToggled(stack))
            return false;

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("flying");

        return ability.canPlayerUse(player) && !isHoverResetState(player) && getTime(stack) < getMaxHoverSeconds(player, stack);
    }

    private boolean shouldConsumeHoverTime(Player player, boolean efficientHoverUnlocked) {
        if (!efficientHoverUnlocked)
            return true;

        var knownMovement = player.getKnownMovement();

        if (knownMovement.lengthSqr() > STATIONARY_HORIZONTAL_SPEED_SQR)
            return true;

        var velocity = player.getDeltaMovement();

        if (velocity.horizontalDistanceSqr() > STATIONARY_HORIZONTAL_SPEED_SQR || Math.abs(velocity.y) > STATIONARY_VERTICAL_SPEED)
            return true;

        var deltaX = player.getX() - player.xOld;
        var deltaY = player.getY() - player.yOld;
        var deltaZ = player.getZ() - player.zOld;

        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ > STATIONARY_POSITION_DELTA_SQR;
    }

    private static boolean canHoverFromLiquidSurface(Player player) {
        return player.isInLiquid()
                && !player.isEyeInFluidType(NeoForgeMod.WATER_TYPE.value())
                && !player.isEyeInFluidType(NeoForgeMod.LAVA_TYPE.value());
    }

    private static boolean isHoverResetState(Player player) {
        return player.onGround() || (player.isInLiquid() && !canHoverFromLiquidSurface(player)) || player.isFallFlying() || player.getAbilities().flying;
    }

    private void disableHover(Player player, ItemStack stack, boolean resetTime) {
        setToggled(stack, false);

        if (resetTime)
            setTime(stack, 0);

        EntityUtils.removeAttribute(player, stack, Attributes.GRAVITY, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        EntityUtils.removeAttribute(player, stack, NeoForgeMod.SWIM_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    public boolean getToggled(ItemStack stack) {
        return stack.getOrDefault(RADataComponent.TOGGLED, false);
    }

    public void setToggled(ItemStack stack, boolean val) {
        stack.set(RADataComponent.TOGGLED, val);
    }

    public boolean getWaterTakeoffReady(ItemStack stack) {
        return stack.getOrDefault(RADataComponent.HELIUM_FLAMINGO_WATER_TAKEOFF_READY.get(), false);
    }

    public void setWaterTakeoffReady(ItemStack stack, boolean val) {
        stack.set(RADataComponent.HELIUM_FLAMINGO_WATER_TAKEOFF_READY.get(), val);
    }

    public void addTime(ItemStack stack, int time) {
        setTime(stack, getTime(stack) + time);
    }

    public int getTime(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(RADataComponent.TIME, 0));
    }

    public void setTime(ItemStack stack, int val) {
        stack.set(RADataComponent.TIME, Math.max(val, 0));
    }

    @EventBusSubscriber(value = Dist.CLIENT)
    public static class HeliumFlamingoClientEvent {
        private static final int STOP_DOUBLE_PRESS_WINDOW_TICKS = 10;
        private static boolean waitingStopPress = false;
        private static int lastStopPressTick = -1000;

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            var player = Minecraft.getInstance().player;

            if (player == null || player.getForcedPose() != Pose.SWIMMING)
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.HELIUM_FLAMINGO.value());

            if (!(stack.getItem() instanceof HeliumFlamingoItem relic) || !relic.isHovering(player, stack))
                player.setForcedPose(null);
        }

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            var minecraft = Minecraft.getInstance();
            var player = minecraft.player;

            if (player == null)
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.HELIUM_FLAMINGO.value());

            if (minecraft.screen != null || event.getAction() != 1 || !(stack.getItem() instanceof HeliumFlamingoItem relic)
                    || event.getKey() != minecraft.options.keyJump.getKey().getValue())
                return;

            if (player.onGround() || player.getAbilities().flying) {
                waitingStopPress = false;
                return;
            }

            if (player.isInLiquid() && !canHoverFromLiquidSurface(player)) {
                waitingStopPress = false;
                return;
            }

            if (player.mayFly()) {
                if (relic.getToggled(stack))
                    NetworkHandler.sendToServer(new FlamingoSwimPacket(false));

                waitingStopPress = false;
                return;
            }

            if (relic.getToggled(stack)) {
                var ticksSinceLastPress = player.tickCount - lastStopPressTick;

                if (waitingStopPress && ticksSinceLastPress <= STOP_DOUBLE_PRESS_WINDOW_TICKS) {
                    waitingStopPress = false;
                    NetworkHandler.sendToServer(new FlamingoSwimPacket(false));
                } else {
                    waitingStopPress = true;
                    lastStopPressTick = player.tickCount;
                }

                return;
            }

            waitingStopPress = false;

            if (relic.getTime(stack) >= relic.getMaxHoverSeconds(player, stack))
                return;


            NetworkHandler.sendToServer(new FlamingoSwimPacket(true));
        }
    }

    @EventBusSubscriber(modid = ReliquifiedArtifacts.MODID)
    public static class CommonEvent {
        @SubscribeEvent
        public static void onSwimAir(PlayerSwimEvent event) {
            var player = event.getEntity();
            var stack = EntityUtils.findEquippedCurio(player, ModItems.HELIUM_FLAMINGO.value());

            if (!(stack.getItem() instanceof HeliumFlamingoItem relic)) {
                if (player.getForcedPose() == Pose.SWIMMING)
                    player.setForcedPose(null);

                return;
            }

            var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("flying");

            if (!ability.canPlayerUse(player) || !relic.isHovering(player, stack)) {
                event.setResult(EventResult.PASS);

                EntityUtils.removeAttribute(player, stack, NeoForgeMod.SWIM_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
                EntityUtils.removeAttribute(player, stack, Attributes.GRAVITY, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

                player.setForcedPose(null);

                return;
            }

            event.setResult(EventResult.SUCCESS);

            var speedBonus = Math.max(0D, ability.getStatData("speed_bonus").getValue());

            EntityUtils.applyAttribute(player, stack, NeoForgeMod.SWIM_SPEED, (float) speedBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

            player.setForcedPose(Pose.SWIMMING);
        }

        @SubscribeEvent
        public static void onLivingIncomingDamageDealing(LivingIncomingDamageEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player) || player.level().isClientSide() || event.getEntity() == player || event.getAmount() <= 0F)
                return;

            if (!event.getSource().is(DamageTypeTags.IS_PROJECTILE))
                return;

            var bonus = 0D;
            HeliumFlamingoItem bestRelic = null;
            ItemStack bestStack = ItemStack.EMPTY;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.HELIUM_FLAMINGO.value())) {
                if (!(stack.getItem() instanceof HeliumFlamingoItem relic) || !relic.isHovering(player, stack))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("flying");

                if (!ability.canPlayerUse(player) || !ability.getRankModifierData("aerial_archery").isEnabled())
                    continue;

                var localBonus = Math.max(0D, ability.getStatData("ranged_damage_bonus").getValue());

                if (localBonus > bonus) {
                    bonus = localBonus;
                    bestRelic = relic;
                    bestStack = stack;
                }
            }

            if (bonus <= 0D)
                return;

            var baseDamage = event.getAmount();
            var boostedDamage = (float) (baseDamage * (1D + bonus));
            var extraDamage = Math.max(0F, boostedDamage - baseDamage);

            event.setAmount(boostedDamage);

            if (bestRelic != null && extraDamage > 0F) {
                var ability = bestRelic.getRelicData(player, bestStack).getAbilitiesData().getAbilityData("flying");
                ability.getStatisticData().getMetricData("aerial_archery_bonus_damage").addValue(extraDamage);
            }
        }

        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getAmount() <= 0F)
                return;

            var reduction = 0D;
            HeliumFlamingoItem bestRelic = null;
            ItemStack bestStack = ItemStack.EMPTY;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.HELIUM_FLAMINGO.value())) {
                if (!(stack.getItem() instanceof HeliumFlamingoItem relic) || !relic.isHovering(player, stack))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("flying");

                if (!ability.canPlayerUse(player) || !ability.getRankModifierData("aerial_guard").isEnabled())
                    continue;

                var localReduction = Math.max(0D, Math.min(1D, ability.getStatData("resistance").getValue()));

                if (localReduction > reduction) {
                    reduction = localReduction;
                    bestRelic = relic;
                    bestStack = stack;
                }
            }

            if (reduction <= 0D)
                return;

            var baseDamage = event.getAmount();
            var reducedDamage = (float) Math.max(0D, baseDamage * (1D - reduction));
            var blockedDamage = Math.max(0F, baseDamage - reducedDamage);

            event.setAmount(reducedDamage);

            if (bestRelic != null && blockedDamage > 0F) {
                var ability = bestRelic.getRelicData(player, bestStack).getAbilitiesData().getAbilityData("flying");
                ability.getStatisticData().getMetricData("aerial_guard_damage_reduced").addValue(blockedDamage);
            }
        }
    }
}

