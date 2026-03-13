package it.hurts.octostudios.rarcompat.items;

import artifacts.network.NetworkHandler;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.init.DataComponentRegistry;
import it.hurts.octostudios.rarcompat.network.packets.UmbrellaBouncePacket;
import it.hurts.sskirillss.relics.api.relics.AbilityMetricTemplate;
import it.hurts.sskirillss.relics.api.relics.AbilityStatisticTemplate;
import it.hurts.sskirillss.relics.api.relics.VisibilityState;
import it.hurts.sskirillss.relics.init.RelicsMobEffects;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.ExperienceSourceTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.ExperienceSourcesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.SlotContext;

public class UmbrellaItem extends WearableRelicItem {
    private static final int BOUNCE_ITEM_COOLDOWN_TICKS = 10;

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("glider")
                                .rankModifier(1, "bounce")
                                .rankModifier(5, "vanishing")
                                .stat(AbilityStatTemplate.builder("bounces")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("strength")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(0.8D, 1.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.1D)
                                        .formatValue(value -> MathUtils.round(value, 2))
                                        .build())
                                .stat(AbilityStatTemplate.builder("vanishing_duration")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.1D)
                                        .formatValue(value -> MathUtils.round(value, 2))
                                        .build())

                                .stat(AbilityStatTemplate.builder("fall_speed")
                                        .thresholdValue(0.01D, Double.MAX_VALUE)
                                        .initialValue(0.15D, 0.1D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), -0.04D)
                                        .formatValue(value -> MathUtils.round(value, 3))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("slow_fall_distance").build())
                                        .source(ExperienceSourceTemplate.builder("bounce")
                                                .rankModifierVisibilityState("bounce", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("flight_duration")
                                                .formatValue(value -> MathUtils.formatTime(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("bounces_done")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("bounce", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .ability(AbilityTemplate.builder("shield")
                                .rankModifier(3, "repel")
                                .stat(AbilityStatTemplate.builder("hits")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(2D, 4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("repel_distance")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(0.6D, 1D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.06D)
                                        .formatValue(value -> MathUtils.round(value, 2))
                                        .build())
                                .stat(AbilityStatTemplate.builder("cooldown")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(6D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), -0.06D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("blocked_hit").build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("blocked_hits")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(stack.getItem()))
            return InteractionResultHolder.fail(stack);

        if (!canUseShield(player, stack))
            return InteractionResultHolder.pass(stack);

        syncShieldData(player, stack);

        if (!hasShieldHits(player, stack))
            return InteractionResultHolder.fail(stack);

        player.startUsingItem(hand);

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        return ItemAbilities.DEFAULT_SHIELD_ACTIONS.contains(itemAbility) || super.canPerformAction(stack, itemAbility);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!(entity instanceof Player player)) {
            setShowShieldBar(stack, false);
            return;
        }

        setShowShieldBar(stack, isUsingShield(player, stack));

        syncBounceData(player, stack);
        syncShieldData(player, stack);

        var isHeld = isHeld(player, stack);

        if (player.onGround()) {
            setBounceCount(stack, 0);
            setBounceLandingRequired(stack, false);
        }

        if (isBounceLandingRequired(stack)) {
            if (isHeld && isFalling(player))
                applySlowFall(player, stack);

            return;
        }

        if (isHeld && isFalling(player))
            applySlowFall(player, stack);
    }

    private void applySlowFall(Player player, ItemStack stack) {
        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("glider");

        if (!ability.canPlayerUse(player) || player.isInLiquid() || player.getDeltaMovement().y > 0)
            return;

        if (!player.isShiftKeyDown() && !isUsingShield(player, stack)) {
            var fallSpeed = Math.max(0D, ability.getStatData("fall_speed").getValue());

            if (fallSpeed <= 0D)
                return;

            var motion = new Vec3(player.getDeltaMovement().x(), -fallSpeed, player.getDeltaMovement().z());

            player.setDeltaMovement(motion.x(), motion.y(), motion.z());

            if (!player.level().isClientSide()) {
                this.getRelicData(player, stack).getLevelingData().addExperience("glider", "slow_fall_distance", motion.length());

                if (player.tickCount % 20 == 0)
                    ability.getStatisticData().getMetricData("flight_duration").addValue(1D);
            }
        }

        player.fallDistance = 0;
    }

    public static boolean tryBounce(Player player) {
        return tryBounce(player, player.getLookAngle(), player.getYRot());
    }

    public static boolean tryBounce(Player player, Vec3 look, float yaw) {
        var stack = getHeldUmbrella(player);

        if (!(stack.getItem() instanceof UmbrellaItem relic))
            return false;

        return relic.tryBounce(player, stack, look, yaw);
    }

    private boolean tryBounce(Player player, ItemStack stack, Vec3 look, float yaw) {
        if (player.level().isClientSide() || !canUseBounce(player, stack) || !hasBounceCharges(player, stack) || player.getCooldowns().isOnCooldown(stack.getItem()))
            return false;

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("glider");
        var force = Math.max(0D, ability.getStatData("strength").getValue()) * 1.5D;

        if (force <= 0D)
            return false;

        var direction = calculateBounceDirection(look, yaw);

        if (direction.lengthSqr() <= 1.0E-6D)
            return false;

        var motion = player.getDeltaMovement().add(direction.scale(force));

        player.setDeltaMovement(motion);
        player.hasImpulse = true;
        player.fallDistance = 0F;

        setBounceCount(stack, getBounceCount(stack) + 1);
        setBounceLandingRequired(stack, true);
        player.getCooldowns().addCooldown(stack.getItem(), BOUNCE_ITEM_COOLDOWN_TICKS);
        applyVanishingOnBounce(player, stack);
        ability.getStatisticData().getMetricData("bounces_done").addValue(1D);
        this.getRelicData(player, stack).getLevelingData().addExperience("glider", "bounce", 1D);

        return true;
    }

    private static Vec3 calculateBounceDirection(Vec3 look, float yaw) {
        var push = new Vec3(-look.x, Math.max(-look.y, 0D), -look.z);

        if (push.lengthSqr() <= 1.0E-6D)
            return Vec3.ZERO;

        return push.normalize();
    }

    private void syncBounceData(Player player, ItemStack stack) {
        var maxBounces = getMaxBounces(player, stack);

        setMaxBounces(stack, maxBounces);

        if (getBounceCount(stack) > maxBounces)
            setBounceCount(stack, maxBounces);

        if (getBounceCount(stack) <= 0)
            setBounceLandingRequired(stack, false);
    }

    private void syncShieldData(Player player, ItemStack stack) {
        var maxHits = getMaxShieldHits(player, stack);

        setShieldMaxHits(stack, maxHits);

        if (!isUsingShield(player, stack)) {
            setShieldHitCount(stack, 0);
            return;
        }

        if (getShieldHitCount(stack) > maxHits)
            setShieldHitCount(stack, maxHits);
    }

    private boolean canUseBounce(Player player, ItemStack stack) {
        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("glider");

        return ability.canPlayerUse(player) && ability.isRankModifierUnlocked("bounce");
    }

    private void applyVanishingOnBounce(Player player, ItemStack stack) {
        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("glider");

        if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("vanishing"))
            return;

        var durationSeconds = Math.max(0D, ability.getStatData("vanishing_duration").getValue());
        var durationTicks = Math.max(0, (int) Math.round(durationSeconds * 20D));

        if (durationTicks <= 0)
            return;

        player.addEffect(new MobEffectInstance(RelicsMobEffects.VANISHING, durationTicks, 0, false, false));
    }

    private boolean canUseShield(Player player, ItemStack stack) {
        return this.getRelicData(player, stack).getAbilitiesData().getAbilityData("shield").canPlayerUse(player);
    }

    private boolean hasBounceCharges(Player player, ItemStack stack) {
        var max = getMaxBounces(player, stack);

        return max > 0 && getBounceCount(stack) < max;
    }

    private boolean hasShieldHits(Player player, ItemStack stack) {
        var max = getMaxShieldHits(player, stack);

        return max > 0 && getShieldHitCount(stack) < max;
    }

    private int getMaxBounces(Player player, ItemStack stack) {
        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("glider");

        if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("bounce"))
            return 0;

        return Math.max(0, (int) MathUtils.round(ability.getStatData("bounces").getValue(), 0));
    }

    private int getMaxShieldHits(Player player, ItemStack stack) {
        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("shield");

        if (!ability.canPlayerUse(player))
            return 0;

        return Math.max(0, (int) MathUtils.round(ability.getStatData("hits").getValue(), 0));
    }

    private int getShieldCooldownTicks(Player player, ItemStack stack) {
        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("shield");

        if (!ability.canPlayerUse(player))
            return 0;

        var seconds = Math.max(0D, ability.getStatData("cooldown").getValue());

        return Math.max(0, (int) Math.round(seconds * 20D));
    }

    private boolean isUsingShield(Player player, ItemStack stack) {
        return player.isUsingItem() && player.getUseItem() == stack;
    }

    private boolean isHeld(Player player, ItemStack stack) {
        return player.getMainHandItem() == stack || player.getOffhandItem() == stack;
    }

    private static ItemStack getHeldUmbrella(Player player) {
        var main = player.getMainHandItem();

        if (main.getItem() instanceof UmbrellaItem)
            return main;

        var offhand = player.getOffhandItem();

        if (offhand.getItem() instanceof UmbrellaItem)
            return offhand;

        return ItemStack.EMPTY;
    }

    private static boolean isFalling(Player player) {
        return !player.onGround()
                && !player.isInWaterOrBubble()
                && !player.onClimbable()
                && !player.getAbilities().flying
                && !player.isFallFlying()
                && (player.getDeltaMovement().y < -0.01D || player.fallDistance > 0F);
    }

    private int getBounceCount(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.UMBRELLA_BOUNCE_COUNT.get(), 0);
    }

    private void setBounceCount(ItemStack stack, int count) {
        stack.set(DataComponentRegistry.UMBRELLA_BOUNCE_COUNT.get(), Math.max(0, count));
    }

    private boolean isBounceLandingRequired(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.UMBRELLA_BOUNCE_NEEDS_LANDING.get(), false);
    }

    private void setBounceLandingRequired(ItemStack stack, boolean value) {
        stack.set(DataComponentRegistry.UMBRELLA_BOUNCE_NEEDS_LANDING.get(), value);
    }

    private int getMaxBounces(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.UMBRELLA_MAX_BOUNCES.get(), 0);
    }

    private void setMaxBounces(ItemStack stack, int maxBounces) {
        stack.set(DataComponentRegistry.UMBRELLA_MAX_BOUNCES.get(), Math.max(0, maxBounces));
    }

    private int getShieldHitCount(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.UMBRELLA_SHIELD_HITS.get(), 0);
    }

    private void setShieldHitCount(ItemStack stack, int count) {
        stack.set(DataComponentRegistry.UMBRELLA_SHIELD_HITS.get(), Math.max(0, count));
    }

    private int getShieldMaxHits(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.UMBRELLA_SHIELD_MAX_HITS.get(), 0);
    }

    private void setShieldMaxHits(ItemStack stack, int maxHits) {
        stack.set(DataComponentRegistry.UMBRELLA_SHIELD_MAX_HITS.get(), Math.max(0, maxHits));
    }

    private boolean isShowingShieldBar(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.UMBRELLA_SHOW_SHIELD_BAR.get(), false);
    }

    private void setShowShieldBar(ItemStack stack, boolean value) {
        stack.set(DataComponentRegistry.UMBRELLA_SHOW_SHIELD_BAR.get(), value);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        if (isShowingShieldBar(stack))
            return getShieldMaxHits(stack) > 0;

        return getMaxBounces(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        if (isShowingShieldBar(stack)) {
            var maxHits = getShieldMaxHits(stack);

            if (maxHits <= 0)
                return 0;

            var remaining = Math.max(0, maxHits - getShieldHitCount(stack));

            return Math.max(0, Math.min(13, Math.round(13F * remaining / (float) maxHits)));
        }

        var maxBounces = getMaxBounces(stack);

        if (maxBounces <= 0)
            return 0;

        var remaining = Math.max(0, maxBounces - getBounceCount(stack));

        return Math.max(0, Math.min(13, Math.round(13F * remaining / (float) maxBounces)));
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return false;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x51D64E;
    }

    public static boolean isHoldingUmbrella(LivingEntity entity, InteractionHand hand) {
        return entity.getItemInHand(hand).getItem() instanceof UmbrellaItem;
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onShieldBlock(LivingShieldBlockEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || !event.getBlocked())
                return;

            var stack = player.getUseItem();

            if (!(stack.getItem() instanceof UmbrellaItem relic))
                return;

            if (!relic.canUseShield(player, stack)) {
                event.setBlocked(false);
                player.stopUsingItem();
                return;
            }

            relic.syncShieldData(player, stack);

            var maxHits = relic.getShieldMaxHits(stack);

            if (maxHits <= 0) {
                event.setBlocked(false);
                player.stopUsingItem();
                return;
            }

            var hits = relic.getShieldHitCount(stack);

            if (hits >= maxHits) {
                event.setBlocked(false);
                player.stopUsingItem();
                return;
            }

            event.setShieldDamage(0F);
            relic.setShieldHitCount(stack, hits + 1);

            var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("shield");
            ability.getStatisticData().getMetricData("blocked_hits").addValue(1D);
            relic.getRelicData(player, stack).getLevelingData().addExperience("shield", "blocked_hit", 1D);

            if (ability.isRankModifierUnlocked("repel") && event.getDamageSource().getDirectEntity() instanceof LivingEntity target && target != player) {
                var distance = Math.max(0D, ability.getStatData("repel_distance").getValue());

                if (distance > 0D) {
                    var direction = target.position().subtract(player.position());
                    var horizontal = new Vec3(direction.x, 0D, direction.z);

                    if (horizontal.lengthSqr() <= 1.0E-6D) {
                        var look = player.getLookAngle();

                        horizontal = new Vec3(look.x, 0D, look.z);
                    }

                    if (horizontal.lengthSqr() > 1.0E-6D) {
                        var normalized = horizontal.normalize();

                        target.knockback(distance, -normalized.x, -normalized.z);
                    }
                }
            }

            if (hits + 1 >= maxHits) {
                var cooldownTicks = relic.getShieldCooldownTicks(player, stack);

                if (cooldownTicks > 0)
                    player.getCooldowns().addCooldown(stack.getItem(), cooldownTicks);

                player.stopUsingItem();
            }
        }
    }

    @EventBusSubscriber(modid = RARCompat.MODID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
            handleLeftClick(event);
        }

        @SubscribeEvent
        public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
            if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START && event.getEntity().level().isClientSide())
                handleLeftClick(event);
        }

        private static void handleLeftClick(PlayerInteractEvent event) {
            var player = event.getEntity();
            var stack = getHeldUmbrella(player);

            if (!(stack.getItem() instanceof UmbrellaItem relic) || !relic.canUseBounce(player, stack)
                    || !relic.hasBounceCharges(player, stack) || player.getCooldowns().isOnCooldown(stack.getItem()))
                return;

            var look = player.getLookAngle();
            var yaw = player.getYRot();

            NetworkHandler.sendToServer(new UmbrellaBouncePacket(look, yaw));

            var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("glider");
            var force = Math.max(0D, ability.getStatData("strength").getValue()) * 1.5D;

            if (force <= 0D)
                return;

            var direction = calculateBounceDirection(look, yaw);

            if (direction.lengthSqr() <= 1.0E-6D)
                return;

            var motion = player.getDeltaMovement().add(direction.scale(force));

            player.setDeltaMovement(motion);
            player.hasImpulse = true;
            player.fallDistance = 0F;
            player.getCooldowns().addCooldown(stack.getItem(), BOUNCE_ITEM_COOLDOWN_TICKS);
        }

        @SubscribeEvent
        public static void onLivingRender(RenderLivingEvent.Pre<?, ?> event) {
            if (!(event.getEntity() instanceof Player player) || !(event.getRenderer().getModel() instanceof HumanoidModel<?> humanoidModel))
                return;

            var isHoldingOffHand = isHoldingUmbrella(player, InteractionHand.OFF_HAND);
            var isHoldingMainHand = isHoldingUmbrella(player, InteractionHand.MAIN_HAND);

            var isRightHanded = player.getMainArm() == HumanoidArm.RIGHT;

            if (player.isShiftKeyDown() && !player.onGround() && (isHoldingMainHand || isHoldingOffHand))
                return;

            if (player.isUsingItem() && player.getUseItem().getItem() instanceof UmbrellaItem)
                return;

            if ((isHoldingMainHand && isRightHanded) || (isHoldingOffHand && !isRightHanded))
                humanoidModel.rightArmPose = HumanoidModel.ArmPose.THROW_SPEAR;

            if ((isHoldingMainHand && !isRightHanded) || (isHoldingOffHand && isRightHanded))
                humanoidModel.leftArmPose = HumanoidModel.ArmPose.THROW_SPEAR;
        }
    }
}

