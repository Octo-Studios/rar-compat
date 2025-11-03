package it.hurts.octostudios.rarcompat.items;

import it.hurts.octostudios.rarcompat.network.packets.EntityMotionPacket;
import it.hurts.octostudios.rarcompat.network.packets.RepulsionUmbrellaPacket;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootData;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootCollections;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.TooltipData;
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.network.packets.PacketPlayerMotion;
import it.hurts.sskirillss.relics.utils.MathUtils;
import it.hurts.sskirillss.relics.utils.NBTUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;

public class UmbrellaItem extends RelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("glider")
                                .requiredPoints(2)
                                .stat(StatData.builder("count")
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(UpgradeOperation.ADD, 1D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(StatData.builder("cooldown")
                                        .initialValue(2D, 1.5D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, -0.07D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .ability(AbilityData.builder("shield")
                                .requiredLevel(5)
                                .stat(StatData.builder("knockback")
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xffb63a2b)
                                .borderBottom(0xff600f15)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(15)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.VILLAGE)
                        .build())
                .build();
    }


    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean isSelected) {
        if (!(entity instanceof Player player) || !canPlayerUseActiveAbility(player, stack, "glider"))
            return;

        var isOnGround = player.onGround();

        if (isOnGround && getCharges(stack) != getMaxCharges(stack))
            setCharges(stack, getMaxCharges(stack));

        var hasUmbrella = false;

        for (var hand : InteractionHand.values())
            if (player.getItemInHand(hand) == stack) {
                hasUmbrella = true;

                break;
            }

        if (!hasUmbrella || player.isInFluidType() || player.getDeltaMovement().y > 0 || player.isShiftKeyDown())
            return;

        var motion = player.getDeltaMovement();

        player.setDeltaMovement(motion.x(), -0.15D, motion.z());
        player.fallDistance = 0;

        if (player.tickCount % 20 == 0 && !isOnGround)
            spreadExperience(player, stack, 1);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (canPlayerUseActiveAbility(player, player.getItemInHand(hand), "shield")) {
            player.startUsingItem(hand);

            return InteractionResultHolder.consume(player.getItemInHand(hand));
        }

        return InteractionResultHolder.fail(player.getItemInHand(hand));
    }

    @Override
    public @NotNull UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();

        setCharges(stack, getMaxCharges(stack));

        return stack;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round((13F * getCharges(stack)) / getMaxCharges(stack));
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return this.getCharges(stack) != getMaxCharges(stack);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return Mth.hsvToRgb(Math.max(0F, (float) getCharges(stack) / getMaxCharges(stack)) / 3F, 1F, 1F);
    }

    public int getMaxCharges(ItemStack stack) {
        return (int) MathUtils.round(getAbilityValue(stack, "glider", "count"), 0);
    }

    public int getCharges(ItemStack stack) {
        return NBTUtils.getInt(stack, "charge", 0);
    }

    public void setCharges(ItemStack stack, int amount) {
        NBTUtils.setInt(stack, "charge", Math.max(amount, 0));
    }

    public void addCharges(ItemStack stack, int amount) {
        setCharges(stack, getCharges(stack) + amount);
    }

    public static boolean isHoldingUmbrella(LivingEntity entity, InteractionHand hand) {
        return entity.getItemInHand(hand).getItem() instanceof UmbrellaItem && (!entity.isUsingItem() || entity.getUsedItemHand() != hand);
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT)
    public static class UmbrellaClientEvents {
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
            var stack = player.getMainHandItem();

            if (!(stack.getItem() instanceof UmbrellaItem relic) || !relic.canPlayerUseActiveAbility(player, stack, "glider")
                    || player.getCooldowns().isOnCooldown(relic) || relic.getCharges(stack) <= 0)
                return;

            it.hurts.octostudios.rarcompat.network.NetworkHandler.sendToServer(new RepulsionUmbrellaPacket());

            var angle = player.getLookAngle().scale(-1.15F);
            var motion = player.getDeltaMovement().add(angle);

            if (angle.y < 0)
                player.setDeltaMovement(new Vec3(motion.x(), 0, motion.z()));
            else
                player.setDeltaMovement(motion.x(), angle.y(), motion.z());
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

            if ((isHoldingMainHand && isRightHanded) || (isHoldingOffHand && !isRightHanded))
                humanoidModel.rightArmPose = HumanoidModel.ArmPose.THROW_SPEAR;

            if ((isHoldingMainHand && !isRightHanded) || (isHoldingOffHand && isRightHanded))
                humanoidModel.leftArmPose = HumanoidModel.ArmPose.THROW_SPEAR;
        }
    }

    @Mod.EventBusSubscriber
    public static class UmbrellaCommonEvents {
        @SubscribeEvent
        public static void onPlayerHurt(LivingAttackEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.getCommandSenderWorld().isClientSide())
                return;

            var level = player.getCommandSenderWorld();

            var stack = ItemStack.EMPTY;

            for (var hand : InteractionHand.values()) {
                var entry = player.getItemInHand(hand);

                if (entry.getItem() instanceof UmbrellaItem) {
                    stack = entry;

                    break;
                }
            }

            if (stack.isEmpty())
                return;

            var relic = (UmbrellaItem) stack.getItem();

            if (level.isClientSide() || !player.isUsingItem() || !(event.getSource().getEntity() instanceof LivingEntity source)
                    || source.position().subtract(player.position()).normalize().dot(player.getLookAngle().normalize()) < 0.65F
                    || player.getCooldowns().isOnCooldown(stack.getItem()))
                return;

            if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow) {
                it.hurts.octostudios.rarcompat.network.NetworkHandler.sendToClientsTrackingEntityAndSelf(
                        new EntityMotionPacket(arrow.getId(), arrow.getDeltaMovement()),
                        arrow
                );
            }

            if (source.getMainHandItem().getItem() instanceof AxeItem) {
                player.getCooldowns().addCooldown(relic, 110);
                player.stopUsingItem();

                level.playSound(null, player.blockPosition(), SoundEvents.ALLAY_DEATH, SoundSource.MASTER, 0.3F, 1 + (player.getRandom().nextFloat() * 0.25F));
            } else
                level.playSound(null, player.blockPosition(), SoundEvents.ALLAY_HURT, SoundSource.MASTER, 0.3F, 1 + (player.getRandom().nextFloat() * 0.25F));

            event.setCanceled(true);
            relic.spreadExperience(player, stack, 1);

            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(3), entity -> !entity.getUUID().equals(player.getUUID()) && entity.isAlive())) {
                var motion = entity.position().subtract(player.position()).normalize().scale(0.4F + (relic.getAbilityValue(stack, "glider", "count") * 0.2F));

                if (entity instanceof Player serverPlayer)
                    NetworkHandler.sendToClient(new PacketPlayerMotion(motion.x(), motion.y() / 5, motion.z()), (ServerPlayer) serverPlayer);
                else
                    entity.setDeltaMovement(motion.x(), motion.y() / 5, motion.z());

                var pos = source.position().add(new Vec3(0F, source.getBbHeight() / 2F, 0F));
                var velocity = motion.normalize().scale(0.5F);

                ((ServerLevel) level).sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 10, velocity.x, velocity.y, velocity.z, 0.1F);
            }

        }
    }
}
