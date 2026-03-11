package it.hurts.octostudios.rarcompat.items.feet;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.init.DataComponentRegistry;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.octostudios.rarcompat.network.packets.BunnyJumpReleasePacket;
import it.hurts.octostudios.rarcompat.network.packets.PowerJumpPacket;
import it.hurts.sskirillss.relics.api.relics.IRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.api.relics.synergies.SynergyTemplate;
import it.hurts.sskirillss.relics.api.relics.synergies.conditions.AbilityConditionTemplate;
import it.hurts.sskirillss.relics.api.relics.synergies.conditions.RelicConditionTemplate;
import it.hurts.sskirillss.relics.init.RelicsRelicContainers;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BunnyHoppersItem extends WearableRelicItem {
    private static final Set<UUID> CLIENT_JUMP_LOCK = new HashSet<>();

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
                        .synergy(SynergyTemplate.builder("cloud_jump")
                                .condition(RelicConditionTemplate.builder(() -> (IRelicItem) ModItems.BUNNY_HOPPERS.value())
                                        .container(RelicsRelicContainers.CURIOS.get())
                                        .condition(AbilityConditionTemplate.builder("jump").build())
                                        .build())
                                .condition(RelicConditionTemplate.builder(() -> (IRelicItem) ModItems.CLOUD_IN_A_BOTTLE.value())
                                        .container(RelicsRelicContainers.CURIOS.get())
                                        .condition(AbilityConditionTemplate.builder("jump").build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player))
            return;

        if (player.level().isClientSide() && player.onGround())
            setClientJumpLocked(player, false);

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("jump");

        if (!ability.canPlayerUse(player)) {
            if (!player.level().isClientSide())
                clearRuntimeState(stack);

            if (player.level().isClientSide())
                setClientJumpLocked(player, false);

            return;
        }

        if (!player.level().isClientSide()) {
            if (getTime(stack) > 0)
                setJumpPeakY(stack, Math.max(getJumpPeakY(stack), player.getY()));

            if (player.onGround()) {
                setToggled(stack, false);
                setTime(stack, 0);
                setUsedMaxDuration(stack, false);
                setJumpStartY(stack, 0D);
                setJumpPeakY(stack, 0D);
                setJumpLocked(stack, false);
            }

            return;
        }

        if (!(player instanceof LocalPlayer) || player.onGround() || player.isFallFlying() || player.getAbilities().flying || !getToggled(stack))
            return;

        if (!Minecraft.getInstance().options.keyJump.isDown()) {
            if (!isClientJumpLocked(player) && !getJumpLocked(stack)) {
                setClientJumpLocked(player, true);
                NetworkHandler.sendToServer(new BunnyJumpReleasePacket());
            }

            return;
        }

        if (isClientJumpLocked(player) || getJumpLocked(stack))
            return;

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

        if (!slotContext.entity().level().isClientSide())
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

        if (getTime(stack) <= 0) {
            setJumpStartY(stack, player.getY());
            setJumpPeakY(stack, player.getY());
        }

        var next = Math.min(maxDurationTicks, getTime(stack) + 1);

        setTime(stack, next);
        setJumpPeakY(stack, Math.max(getJumpPeakY(stack), player.getY()));

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
        setJumpStartY(stack, 0D);
        setJumpPeakY(stack, 0D);
        setJumpLocked(stack, false);
    }

    public void armHighJump(Player player, ItemStack stack) {
        if (player.level().isClientSide()) {
            setClientJumpLocked(player, false);
            return;
        }

        setToggled(stack, true);
        setTime(stack, 0);
        setUsedMaxDuration(stack, false);
        setJumpStartY(stack, player.getY());
        setJumpPeakY(stack, player.getY());
        setJumpLocked(stack, false);
    }

    public void armHighJumpFromCloudSynergy(Player player, ItemStack stack) {
        var abilities = this.getRelicData(player, stack).getAbilitiesData();
        var ability = abilities.getAbilityData("jump");

        if (!ability.canPlayerUse(player))
            return;

        var synergy = abilities.getSynergyData("cloud_jump");

        if (!synergy.isUnlocked() || !synergy.isEnabled())
            return;

        armHighJump(player, stack);
    }

    public static void setClientJumpLocked(Player player, boolean locked) {
        if (player == null)
            return;

        if (locked)
            CLIENT_JUMP_LOCK.add(player.getUUID());
        else
            CLIENT_JUMP_LOCK.remove(player.getUUID());
    }

    public static boolean isClientJumpLocked(Player player) {
        return player != null && CLIENT_JUMP_LOCK.contains(player.getUUID());
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

    public void setJumpLocked(ItemStack stack, boolean value) {
        stack.set(DataComponentRegistry.BUNNY_HOPPERS_JUMP_LOCKED.get(), value);
    }

    public boolean getJumpLocked(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.BUNNY_HOPPERS_JUMP_LOCKED.get(), false);
    }

    private void setUsedMaxDuration(ItemStack stack, boolean value) {
        stack.set(DataComponentRegistry.BUNNY_HOPPERS_USED_MAX_DURATION.get(), value);
    }

    private boolean getUsedMaxDuration(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.BUNNY_HOPPERS_USED_MAX_DURATION.get(), false);
    }
    private void setJumpStartY(ItemStack stack, double value) {
        stack.set(DataComponentRegistry.BUNNY_HOPPERS_JUMP_START_Y.get(), value);
    }

    private double getJumpStartY(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.BUNNY_HOPPERS_JUMP_START_Y.get(), 0D);
    }

    private void setJumpPeakY(ItemStack stack, double value) {
        stack.set(DataComponentRegistry.BUNNY_HOPPERS_JUMP_PEAK_Y.get(), value);
    }

    private double getJumpPeakY(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.BUNNY_HOPPERS_JUMP_PEAK_Y.get(), 0D);
    }

    @EventBusSubscriber(modid = RARCompat.MODID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onJumpKeyRelease(InputEvent.Key event) {
            var minecraft = Minecraft.getInstance();
            var player = minecraft.player;

            if (player == null || player.onGround() || player.isFallFlying() || player.getAbilities().flying || event.getAction() != 0
                    || event.getKey() != minecraft.options.keyJump.getKey().getValue())
                return;

            if (!(EntityUtils.findEquippedCurio(player, ModItems.BUNNY_HOPPERS.value()).getItem() instanceof BunnyHoppersItem))
                return;

            setClientJumpLocked(player, true);
            NetworkHandler.sendToServer(new BunnyJumpReleasePacket());
        }
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
                    if (!player.level().isClientSide())
                        relic.clearRuntimeState(stack);
                    continue;
                }

                if (player.level().isClientSide()) {
                    if (player.onGround())
                        setClientJumpLocked(player, false);
                } else if (player.onGround()) {
                    relic.armHighJump(player, stack);
                }

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

                nearby.knockback(0.325D, player.getX() - nearby.getX(), player.getZ() - nearby.getZ());

                if (nearby instanceof Mob mob) {
                    mob.setTarget(null);
                    mob.getNavigation().stop();
                }
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

            var spentTicks = relic.getTime(stack);

            if (spentTicks <= 0)
                return;

            var jumpStartY = relic.getJumpStartY(stack);
            var jumpPeakY = Math.max(jumpStartY, relic.getJumpPeakY(stack));
            var jumpHeight = (float) Math.max(0D, jumpPeakY - jumpStartY);
            var safeHeight = jumpHeight * 1.1F;

            event.setDistance(Math.max(0F, event.getDistance() - safeHeight));

            if (ability.isRankModifierUnlocked("safe_landing"))
                event.setCanceled(true);
            
            relic.setTime(stack, 0);
            relic.setUsedMaxDuration(stack, false);
            relic.setToggled(stack, false);
            relic.setJumpStartY(stack, 0D);
            relic.setJumpPeakY(stack, 0D);
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

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("impact") || player.onGround() || player.isFallFlying() || player.getAbilities().flying)
                    continue;

                var jumpStartY = relic.getJumpStartY(stack);
                var jumpPeakY = Math.max(jumpStartY, relic.getJumpPeakY(stack));

                if (relic.getTime(stack) <= 0 || jumpPeakY <= jumpStartY)
                    continue;

                var perBlockBonus = Math.max(0D, ability.getStatData("impact_damage_bonus").getValue());
                var climbedBlocks = jumpPeakY - jumpStartY;

                bonus = Math.max(bonus, perBlockBonus * climbedBlocks);
            }

            if (bonus > 0D)
                event.setAmount((float) (event.getAmount() * (1D + bonus)));
        }
    }
}
