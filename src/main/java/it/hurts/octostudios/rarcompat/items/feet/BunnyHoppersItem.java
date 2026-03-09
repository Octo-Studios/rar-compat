package it.hurts.octostudios.rarcompat.items.feet;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.init.DataComponentRegistry;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.octostudios.rarcompat.network.packets.PowerJumpPacket;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

import java.awt.*;

public class BunnyHoppersItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("jump")
                                .rankModifier(1, "repel")
                                .rankModifier(3, "safe_landing")
                                .rankModifier(5, "impact")
                                .stat(AbilityStatTemplate.builder("duration")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("repel_radius")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(2D, 5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("impact_damage_bonus")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.12D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player))
            return;

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("jump");

        if (!ability.canPlayerUse(player)) {
            clearRuntimeState(stack);
            return;
        }

        if (!player.level().isClientSide()) {
            if (getLandingStrikeTicks(stack) > 0)
                setLandingStrikeTicks(stack, getLandingStrikeTicks(stack) - 1);

            if (player.onGround()) {
                setToggled(stack, false);
                setTime(stack, 0);
                setUsedMaxDuration(stack, false);
            }

            return;
        }

        if (!(player instanceof LocalPlayer localPlayer) || player.onGround() || player.isFallFlying() || player.getAbilities().flying || !getToggled(stack))
            return;

        if (!localPlayer.input.jumping) {
            setToggled(stack, false);
            return;
        }

        var maxDurationTicks = getMaxDurationTicks(ability.getStatData("duration").getValue());

        if (maxDurationTicks <= 0 || getTime(stack) >= maxDurationTicks)
            return;

        NetworkHandler.sendToServer(new PowerJumpPacket());

        player.setDeltaMovement(new Vec3(player.getDeltaMovement().x, 0.6D, player.getDeltaMovement().z));

        var random = player.getRandom();
        var level = player.getCommandSenderWorld();

        for (int i = 0; i < 10; i++) {
            double offsetX = (random.nextDouble() - 0.5D) * 0.5D;
            double offsetY = (random.nextDouble() - 0.5D) * 0.5D;
            double offsetZ = (random.nextDouble() - 0.5D) * 0.5D;

            level.addParticle(ParticleUtils.constructSimpleSpark(new Color(200 + random.nextInt(56), 200 + random.nextInt(56), 200 + random.nextInt(56)),
                            0.5F, 40, 0.9F),
                    player.getX() + offsetX,
                    player.getY() + 0.1D + offsetY,
                    player.getZ() + offsetZ,
                    0D, 0D, 0D);
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (stack.getItem() == newStack.getItem())
            return;

        clearRuntimeState(stack);
    }

    public void registerPowerJumpTick(Player player, ItemStack stack) {
        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("jump");

        if (!ability.canPlayerUse(player)) {
            clearRuntimeState(stack);
            return;
        }

        var maxDurationTicks = getMaxDurationTicks(ability.getStatData("duration").getValue());

        if (maxDurationTicks <= 0)
            return;

        var next = Math.min(maxDurationTicks, getTime(stack) + 1);

        setTime(stack, next);

        if (next >= maxDurationTicks)
            setUsedMaxDuration(stack, true);
    }

    private int getMaxDurationTicks(double seconds) {
        return Math.max(0, (int) Math.round(Math.max(0D, seconds) * 20D));
    }

    private void clearRuntimeState(ItemStack stack) {
        setTime(stack, 0);
        setToggled(stack, false);
        setUsedMaxDuration(stack, false);
        setLandingStrikeTicks(stack, 0);
    }

    public void setTime(ItemStack stack, int value) {
        stack.set(DataComponentRegistry.TIME, Math.max(0, value));
    }

    public int getTime(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(DataComponentRegistry.TIME, 0));
    }

    public void setToggled(ItemStack stack, boolean value) {
        stack.set(DataComponentRegistry.TOGGLED, value);
    }

    public boolean getToggled(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.TOGGLED, false);
    }

    private void setUsedMaxDuration(ItemStack stack, boolean value) {
        stack.set(DataComponentRegistry.BUNNY_HOPPERS_USED_MAX_DURATION.get(), value);
    }

    private boolean getUsedMaxDuration(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.BUNNY_HOPPERS_USED_MAX_DURATION.get(), false);
    }

    private void setLandingStrikeTicks(ItemStack stack, int ticks) {
        stack.set(DataComponentRegistry.BUNNY_HOPPERS_LANDING_STRIKE_TICKS.get(), Math.max(0, ticks));
    }

    private int getLandingStrikeTicks(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(DataComponentRegistry.BUNNY_HOPPERS_LANDING_STRIKE_TICKS.get(), 0));
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class BunnyHoppersEvent {
        @SubscribeEvent
        public static void onPlayerJumping(LivingEvent.LivingJumpEvent event) {
            if (!(event.getEntity() instanceof Player player))
                return;

            var repelRadius = 0D;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.BUNNY_HOPPERS.value())) {
                if (!(stack.getItem() instanceof BunnyHoppersItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("jump");

                if (!ability.canPlayerUse(player)) {
                    relic.setToggled(stack, false);
                    continue;
                }

                relic.setToggled(stack, true);

                if (player.level().isClientSide() || !ability.isRankModifierUnlocked("repel"))
                    continue;

                repelRadius = Math.max(repelRadius, Math.max(0D, ability.getStatData("repel_radius").getValue()));
            }

            if (player.level().isClientSide() || repelRadius <= 0D)
                return;

            var maxDistanceSq = repelRadius * repelRadius;

            for (var nearby : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(repelRadius), entity -> entity.isAlive() && entity != player)) {
                if (nearby.distanceToSqr(player) > maxDistanceSq)
                    continue;

                nearby.knockback(0.25D, player.getX() - nearby.getX(), player.getZ() - nearby.getZ());
            }
        }

        @SubscribeEvent
        public static void onPlayerFall(LivingFallEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide())
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.BUNNY_HOPPERS.value());

            if (!(stack.getItem() instanceof BunnyHoppersItem relic))
                return;

            var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("jump");

            if (!ability.canPlayerUse(player)) {
                relic.clearRuntimeState(stack);
                return;
            }

            var usedHighJump = relic.getTime(stack) > 0;

            if (!usedHighJump)
                return;

            if (ability.isRankModifierUnlocked("safe_landing"))
                event.setCanceled(true);

            if (ability.isRankModifierUnlocked("impact") && relic.getUsedMaxDuration(stack))
                relic.setLandingStrikeTicks(stack, Math.max(relic.getLandingStrikeTicks(stack), 10));

            relic.setTime(stack, 0);
            relic.setUsedMaxDuration(stack, false);
            relic.setToggled(stack, false);
        }

        @SubscribeEvent
        public static void onLivingIncomingDamageDealing(LivingIncomingDamageEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player) || event.getSource().getDirectEntity() != player || player.level().isClientSide() || event.getEntity() == player || event.getAmount() <= 0F)
                return;

            var bonus = 0D;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.BUNNY_HOPPERS.value())) {
                if (!(stack.getItem() instanceof BunnyHoppersItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("jump");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("impact") || relic.getLandingStrikeTicks(stack) <= 0)
                    continue;

                bonus = Math.max(bonus, Math.max(0D, Math.min(1D, ability.getStatData("impact_damage_bonus").getValue())));
                relic.setLandingStrikeTicks(stack, 0);
            }

            if (bonus > 0D)
                event.setAmount((float) (event.getAmount() * (1D + bonus)));
        }
    }
}
