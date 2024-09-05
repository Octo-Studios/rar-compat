package it.hurts.octostudios.rarcompat.items;

import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import it.hurts.sskirillss.relics.items.relics.base.data.misc.StatIcons;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public class UmbrellaItem extends WearableRelicItem {
    public static double fallDistanceTick = 0;
    public static double shiftHoldTime = 0;

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("glider")
                                .stat(StatData.builder("speed")
                                        .icon(StatIcons.JUMP_HEIGHT)
                                        .initialValue(20D, 40D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.0625D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .ability(AbilityData.builder("shield")
                                .stat(StatData.builder("knockback")
                                        .icon(StatIcons.DISTANCE)
                                        .initialValue(0.25D, 1D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.4D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .build())
                .leveling(new LevelingData(100, 10, 100))
                .build();
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean isSelected) {
        if (!(entity instanceof Player player)) return;

        // Обработка падения с зонтом
        if (player.onGround() || !isHoldingUmbrellaUpright(player))
            fallDistanceTick = 0;

        if (!player.isShiftKeyDown())
            shiftHoldTime = 0.0;

        if (!player.onGround() && !player.isInWater()
                && player.getDeltaMovement().y < 0
                && !player.hasEffect(MobEffects.SLOW_FALLING)
                && isHoldingUmbrellaUpright(player)) {

            createParticle(level, player);

            fallDistanceTick++;
            Vec3 motion = player.getDeltaMovement();

            double modifyVal = ((UmbrellaItem) stack.getItem()).getStatValue(stack, "glider", "speed") / 100.0;
            double fallDistanceRatio = fallDistanceTick / 130.0;
            double logFactor = Math.min(Math.log1p(modifyVal * fallDistanceRatio), 0.3);

            double newFallSpeed = motion.y * (1.0 - logFactor);

            if (player.isShiftKeyDown()) {
                shiftHoldTime += 0.03;

                if (logFactor >= 0.2)
                    newFallSpeed -= Math.min(shiftHoldTime * 0.02, 0.3);
            }

            if (logFactor == 0.3)
                player.fallDistance = 0;

            player.setDeltaMovement(motion.x, newFallSpeed, motion.z);
        }
    }

    public void createParticle(Level level, Player player) {
        if (level.isClientSide) return;

        for (int i = 0; i < 2; i++) {

            Vec3 armPosition;
            if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof UmbrellaItem)
                armPosition = player.getEyePosition(1.0F)
                        .add(player.getLookAngle().scale(0.5))
                        .add(player.getUpVector(1.0F).scale(-0.25))
                        .add(player.getLookAngle().cross(new Vec3(0, 1, 0)).scale(0.3));
            else
                armPosition = player.getEyePosition(1.0F)
                        .add(player.getLookAngle().scale(0.5))
                        .add(player.getUpVector(1.0F).scale(-0.25))
                        .add(player.getLookAngle().cross(new Vec3(0, 1, 0)).scale(-0.3));


            ((ServerLevel) level).sendParticles(
                    ParticleTypes.CLOUD,
                    armPosition.x,
                    player.getY() + 2.8,
                    armPosition.z,
                    1,
                    0, 0, 0,
                    player.getDeltaMovement().y
            );
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        Vec3 center = player.position().add(player.getLookAngle().scale(1));

        level.getEntitiesOfClass(LivingEntity.class, new AABB(center.x - 0.5, center.y - 0.5, center.z - 0.5, center.x + 0.5, center.y + 0.5, center.z + 0.5),
                entity -> entity != null && entity != player).forEach(entity -> pushAwayEntity(player, entity));

        player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.MASTER, 0.5F, 1 + (player.getRandom().nextFloat() * 0.25F));
        player.startUsingItem(hand);

        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        if (entity instanceof Player player)
            player.getCooldowns().addCooldown(this, 60);

        super.onStopUsing(stack, entity, count);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack itemStack, LivingEntity livingEntity) {
        return 72000;
    }

    public boolean pushAwayEntity(Player player, LivingEntity attacker) {
        if (player.level().isClientSide) return false;

        Vec3 toTarget = new Vec3(attacker.getX() - player.getX(), 0, attacker.getZ() - player.getZ()).normalize();

        if (player.getLookAngle().normalize().dot(toTarget) > 0) {
            Vec3 knockbackDirection = toTarget.scale(getStatValue(player.getUseItem(), "shield", "knockback"));
            attacker.setDeltaMovement(attacker.getDeltaMovement().add(knockbackDirection));

            Vec3 startPosition = attacker.position().add(new Vec3(0, attacker.getBbHeight() / 2.0, 0));

            Vec3 particleVelocity = knockbackDirection.normalize().scale(0.5);

            ((ServerLevel) player.level()).sendParticles(
                    ParticleTypes.CLOUD,
                    startPosition.x,
                    startPosition.y,
                    startPosition.z,
                    10,
                    particleVelocity.x,
                    particleVelocity.y,
                    particleVelocity.z,
                    0.1
            );
            return true;
        }

        return false;
    }

    public static boolean isHoldingUmbrellaUpright(LivingEntity entity, InteractionHand hand) {
        return entity.getItemInHand(hand).getItem() instanceof UmbrellaItem && (!entity.isUsingItem() || entity.getUsedItemHand() != hand);
    }

    public static boolean isHoldingUmbrellaUpright(LivingEntity entity) {
        return isHoldingUmbrellaUpright(entity, InteractionHand.MAIN_HAND) || isHoldingUmbrellaUpright(entity, InteractionHand.OFF_HAND);
    }

    @EventBusSubscriber
    public static class Events {

        @SubscribeEvent
        public static void onPlayerJump(LivingEvent.LivingJumpEvent event) {
            if (!(event.getEntity() instanceof Player player) || isHoldingUmbrellaUpright(player)
                    || !(player.getUseItem().getItem() instanceof UmbrellaItem relic)
                    || !player.isUsingItem()) return;

            Vec3 lookDirection = player.getLookAngle();
            int modifierVal = (int) relic.getStatValue(new ItemStack(relic), "shield", "knockback");

            Vec3 knockbackDirection = new Vec3((-lookDirection.x + modifierVal) * 1.2, (-lookDirection.y + modifierVal) * 0.7, (-lookDirection.z + modifierVal) * 1.2);

            player.setDeltaMovement(knockbackDirection);
        }

        @SubscribeEvent
        public static void onLivingRender(RenderLivingEvent.Pre<?, ?> event) {
            if (!(event.getRenderer().getModel() instanceof HumanoidModel<?> humanoidModel) || !(event.getEntity() instanceof Player player)
                    || !isHoldingUmbrellaUpright(player))
                return;

            boolean isHoldingOffHand = isHoldingUmbrellaUpright(player, InteractionHand.OFF_HAND);
            boolean isHoldingMainHand = isHoldingUmbrellaUpright(player, InteractionHand.MAIN_HAND);
            boolean isRightHanded = player.getMainArm() == HumanoidArm.RIGHT;

            if ((isHoldingMainHand && isRightHanded) || (isHoldingOffHand && !isRightHanded))
                humanoidModel.rightArmPose = HumanoidModel.ArmPose.THROW_SPEAR;

            if ((isHoldingMainHand && !isRightHanded) || (isHoldingOffHand && isRightHanded))
                humanoidModel.leftArmPose = HumanoidModel.ArmPose.THROW_SPEAR;
        }

        @SubscribeEvent
        public static void onEntityHurt(LivingIncomingDamageEvent event) {
            if (event.getEntity() instanceof Player player
                    && player.getUseItem().getItem() instanceof UmbrellaItem relic
                    && player.isUsingItem()
                    && event.getSource().getEntity() instanceof LivingEntity attacker) {

                if (relic.pushAwayEntity(player, attacker)) {
                    event.setCanceled(true);
                    player.level().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.MASTER, 0.3f, 1 + (player.getRandom().nextFloat() * 0.25F));
                }
            }
        }

    }
}
