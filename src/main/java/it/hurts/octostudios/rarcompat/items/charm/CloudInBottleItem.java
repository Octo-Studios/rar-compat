package it.hurts.octostudios.rarcompat.items.charm;

import it.hurts.sskirillss.relics.api.relics.synergies.stats.SynergyStatTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.init.DataComponentRegistry;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.octostudios.rarcompat.items.feet.BunnyHoppersItem;
import it.hurts.octostudios.rarcompat.items.hat.WhoopeeCushionItem;
import it.hurts.octostudios.rarcompat.network.packets.DoubleJumpPacket;
import it.hurts.sskirillss.relics.api.relics.AbilityMetricTemplate;
import it.hurts.sskirillss.relics.api.relics.AbilityStatisticTemplate;
import it.hurts.sskirillss.relics.api.relics.IRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.VisibilityState;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.ExperienceSourceTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.ExperienceSourcesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.api.relics.synergies.SynergyTemplate;
import it.hurts.sskirillss.relics.api.relics.synergies.conditions.AbilityConditionTemplate;
import it.hurts.sskirillss.relics.api.relics.synergies.conditions.RelicConditionTemplate;
import it.hurts.sskirillss.relics.init.RelicsRelicContainers;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

public class CloudInBottleItem extends WearableRelicItem {

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("jump")
                                .rankModifier(1, "slow_fall")
                                .rankModifier(3, "updraft")
                                .rankModifier(5, "combat_recovery")
                                .stat(AbilityStatTemplate.builder("count")
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0667D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("slow_fall_duration")
                                        .initialValue(3D, 5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0286D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("updraft_bonus")
                                        .initialValue(0.25D, 0.5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.1143D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("air_jump").build())
                                        .source(ExperienceSourceTemplate.builder("combat_recovery_hit")
                                                .rankModifierVisibilityState("combat_recovery", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("air_jumps_done")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("slow_fall_duration")
                                                .formatValue(value -> MathUtils.formatTime(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("slow_fall", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("updraft_jumps_done")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("updraft", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("combat_recovery_bonus_damage")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("combat_recovery", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .synergy(SynergyTemplate.builder("cloud_burst")
                                .stat(SynergyStatTemplate.builder("chance")
                                        .thresholdValue(0.25D, 1D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 0))
                                        .build())
                                .condition(RelicConditionTemplate.builder(() -> (IRelicItem) ModItems.CLOUD_IN_A_BOTTLE.value())
                                        .container(RelicsRelicContainers.CURIOS.get())
                                        .condition(AbilityConditionTemplate.builder("jump").build())
                                        .build())
                                .condition(RelicConditionTemplate.builder(() -> (IRelicItem) ModItems.WHOOPEE_CUSHION.value())
                                        .container(RelicsRelicContainers.CURIOS.get())
                                        .condition(AbilityConditionTemplate.builder("push").build())
                                        .build())
                                .build())
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.MOUNTAIN)
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player))
            return;

        if (player.onGround()) {
            setCount(stack, 0);
            setSlowFallTicks(stack, 0);
            return;
        }

        var relicData = this.getRelicData(player, stack);
        var ability = relicData.getAbilitiesData().getAbilityData("jump");

        if (!ability.canPlayerUse(player)) {
            setCount(stack, 0);
            setSlowFallTicks(stack, 0);
            return;
        }

        if (isSlowFallSuppressed(player)) {
            if (getSlowFallTicks(stack) > 0)
                setSlowFallTicks(stack, 0);
        }

        if (ability.isRankModifierUnlocked("slow_fall")) {
            if (getSlowFallTicks(stack) > 0) {
                if (!player.level().isClientSide() && isSlowFallActive(player) && player.tickCount % 20 == 0)
                    ability.getStatisticData().getMetricData("slow_fall_duration").addValue(1D);

                applySlowFall(player);
                addSlowFallTicks(stack, -1);
            }
        } else if (getSlowFallTicks(stack) > 0) {
            setSlowFallTicks(stack, 0);
        }

        var maxJumps = getMaxJumps(player, stack);

        if (getCount(stack) > maxJumps)
            setCount(stack, maxJumps);
    }

    public boolean canAirJump(Player player, ItemStack stack) {
        if (player == null || player.onGround() || player.isFallFlying() || player.getAbilities().flying)
            return false;

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("jump");

        if (!ability.canPlayerUse(player))
            return false;

        return getCount(stack) < getMaxJumps(player, stack);
    }

    public boolean performAirJump(Player player, ItemStack stack) {
        if (!canAirJump(player, stack))
            return false;

        var relicData = this.getRelicData(player, stack);
        var ability = relicData.getAbilitiesData().getAbilityData("jump");
        var previousY = player.getDeltaMovement().y;

        addCount(stack, 1);

        if (!player.level().isClientSide()) {
            relicData.getLevelingData().addExperience("jump", "air_jump", 1D);
            ability.getStatisticData().getMetricData("air_jumps_done").addValue(1D);
        }

        player.jumpFromGround();
        player.hasImpulse = true;
        player.fallDistance = 0F;

        if (ability.isRankModifierUnlocked("updraft") && player.getLookAngle().y > 0.70D) {
            var bonus = Math.max(0D, ability.getStatData("updraft_bonus").getValue());
            var currentMotion = player.getDeltaMovement();
            var jumpImpulse = currentMotion.y - previousY;

            if (bonus > 0D && jumpImpulse > 0D) {
                player.setDeltaMovement(currentMotion.x, previousY + jumpImpulse * (1D + bonus), currentMotion.z);

                if (!player.level().isClientSide())
                    ability.getStatisticData().getMetricData("updraft_jumps_done").addValue(1D);
            }
        }

        if (ability.isRankModifierUnlocked("slow_fall"))
            setSlowFallTicks(stack, secondsToTicks(ability.getStatData("slow_fall_duration").getValue()));

        var bunnyStack = EntityUtils.findEquippedCurio(player, ModItems.BUNNY_HOPPERS.value());

        if (bunnyStack.getItem() instanceof BunnyHoppersItem bunny)
            bunny.armHighJumpFromCloudSynergy(player, bunnyStack);

        if (!player.level().isClientSide()) {
            var abilities = this.getRelicData(player, stack).getAbilitiesData();
            var synergy = abilities.getSynergyData("cloud_burst");

            if (synergy.isUnlocked() && synergy.isEnabled()) {
                var chance = Math.max(0D, Math.min(1D, synergy.getStatData("chance").getValue()));

                if (chance <= 0D || player.getRandom().nextDouble() > chance)
                    return true;

                var whoopeeStack = EntityUtils.findEquippedCurio(player, ModItems.WHOOPEE_CUSHION.value());

                if (whoopeeStack.getItem() instanceof WhoopeeCushionItem whoopee)
                    whoopee.activateFromCloudSynergy(player, whoopeeStack);
            }
        }

        return true;
    }

    public int getMaxJumps(Player player, ItemStack stack) {
        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("jump");

        return Math.max(0, (int) MathUtils.round(ability.getStatData("count").getValue(), 0));
    }

    public void restoreOneJump(ItemStack stack) {
        setCount(stack, Math.max(0, getCount(stack) - 1));
    }

    public void addCount(ItemStack stack, int amount) {
        setCount(stack, getCount(stack) + amount);
    }

    public int getCount(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(DataComponentRegistry.COUNT, 0));
    }

    public void setCount(ItemStack stack, int val) {
        stack.set(DataComponentRegistry.COUNT, Math.max(val, 0));
    }

    public int getSlowFallTicks(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(DataComponentRegistry.CLOUD_IN_BOTTLE_SLOW_FALL_TICKS.get(), 0));
    }

    public void setSlowFallTicks(ItemStack stack, int ticks) {
        stack.set(DataComponentRegistry.CLOUD_IN_BOTTLE_SLOW_FALL_TICKS.get(), Math.max(0, ticks));
    }

    public void addSlowFallTicks(ItemStack stack, int ticks) {
        setSlowFallTicks(stack, getSlowFallTicks(stack) + ticks);
    }

    private boolean isSlowFallActive(Player player) {
        return !isSlowFallSuppressed(player) && !player.isInFluidType() && player.getDeltaMovement().y <= 0D && !player.isShiftKeyDown();
    }

    private void applySlowFall(Player player) {
        if (isSlowFallSuppressed(player) || player.isInFluidType() || player.getDeltaMovement().y > 0D)
            return;

        if (!player.isShiftKeyDown()) {
            var motion = player.getDeltaMovement();

            player.setDeltaMovement(motion.x, -0.15D, motion.z);
        }

        player.fallDistance = 0F;
    }

    private boolean isSlowFallSuppressed(Player player) {
        return player.isSpectator() || player.getAbilities().flying || player.isFallFlying();
    }

    private static int secondsToTicks(double seconds) {
        return Math.max(0, (int) Math.round(Math.max(0D, seconds) * 20D));
    }

    @EventBusSubscriber(value = Dist.CLIENT)
    public static class ClientEvent {
        @SubscribeEvent
        public static void onMouseInput(InputEvent.Key event) {
            var minecraft = Minecraft.getInstance();
            var player = minecraft.player;

            if (player == null)
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.CLOUD_IN_A_BOTTLE.value());

            if (minecraft.screen != null || event.getAction() != 1 || !(stack.getItem() instanceof CloudInBottleItem relic)
                    || event.getKey() != minecraft.options.keyJump.getKey().getValue())
                return;

            if (!relic.performAirJump(player, stack))
                return;

            NetworkHandler.sendToServer(new DoubleJumpPacket());
        }
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamageDealing(LivingIncomingDamageEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player) || player.level().isClientSide() || event.getEntity() == player || event.getAmount() <= 0F)
                return;

            if (player.onGround() || player.isFallFlying())
                return;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.CLOUD_IN_A_BOTTLE.value())) {
                if (!(stack.getItem() instanceof CloudInBottleItem relic))
                    continue;

                var relicData = relic.getRelicData(player, stack);
                var ability = relicData.getAbilitiesData().getAbilityData("jump");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("combat_recovery"))
                    continue;

                if (relic.getCount(stack) <= 0)
                    continue;

                relic.restoreOneJump(stack);
                relicData.getLevelingData().addExperience("jump", "combat_recovery_hit", 1D);
                ability.getStatisticData().getMetricData("combat_recovery_bonus_damage").addValue(event.getAmount());
                break;
            }
        }
    }
}
