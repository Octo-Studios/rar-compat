package it.hurts.octostudios.rarcompat.items;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.network.packets.RepulsionUmbrellaPacket;
import it.hurts.sskirillss.relics.init.CreativeTabRegistry;
import it.hurts.sskirillss.relics.init.DataComponentRegistry;
import it.hurts.sskirillss.relics.items.misc.CreativeContentConstructor;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.*;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.GemColor;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.GemShape;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootData;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import it.hurts.sskirillss.relics.items.relics.base.data.research.ResearchData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.BeamsData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.TooltipData;
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;

public class UmbrellaItem extends WearableRelicItem {
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
                                .research(ResearchData.builder()
                                        .star(0, 3, 10).star(1, 11, 5).star(2, 19, 10)
                                        .star(3, 11, 22).star(4, 15, 22).star(5, 13, 25)
                                        .link(0, 1).link(1, 2).link(1, 3).link(3, 5).link(5, 4)
                                        .build())
                                .build())
                        .ability(AbilityData.builder("shield")
                                .requiredLevel(5)
                                .stat(StatData.builder("knockback")
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .research(ResearchData.builder()
                                        .star(0, 2, 5).star(1, 11, 3).star(2, 20, 6)
                                        .star(3, 11, 13).star(4, 3, 19).star(5, 19, 19).star(6, 11, 25)
                                        .link(0, 3).link(1, 3).link(2, 3).link(3, 4).link(4, 6).link(6, 5).link(3, 5)
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xffb63a2b)
                                .borderBottom(0xff600f15)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFFb63a2b)
                                .endColor(0x00600f15)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(15)
                        .step(100)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData.abilityBuilder("glider_1", "glider")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.CYAN)
                                        .build())
                                .source(LevelingSourceData.abilityBuilder("glider_2", "glider")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.BLUE)
                                        .build())
                                .source(LevelingSourceData.abilityBuilder("shield")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.YELLOW)
                                        .build())
                                .build())
                        .build())
                .loot(LootData.builder()
                        .entry(LootEntries.WILDCARD, LootEntries.VILLAGE, LootEntries.MOUNTAIN)
                        .build())
                .build();
    }

    @Override
    public void gatherCreativeTabContent(CreativeContentConstructor constructor) {
        ItemStack stack = this.getDefaultInstance();

        setCharges(stack, getMaxCharges(stack));

        constructor.entry(CreativeTabRegistry.RELICS_TAB.get(), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY, stack);
        constructor.entry(ModItems.CREATIVE_TAB.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS, stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean isSelected) {
        if (!(entity instanceof Player player) || !canPlayerUseAbility(player, stack, "glider"))
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

        if (!hasUmbrella || player.isInLiquid() || player.getDeltaMovement().y > 0)
            return;

        var motion = player.getDeltaMovement();

        player.setDeltaMovement(motion.x(), -(player.isShiftKeyDown() ? 0.65F : 0.15F), motion.z());
        player.fallDistance = 0;

        if (player.tickCount % 20 == 0 && !isOnGround)
            spreadRelicExperience(player, stack, 1);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (canPlayerUseAbility(player, player.getItemInHand(hand), "shield")) {
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
    public int getUseDuration(ItemStack stack, LivingEntity livingEntity) {
        return 72000;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return false;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round((13F * getCharges(stack)) / getMaxCharges(stack));
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return this.getCharges(stack) != getMaxCharges(stack) && hasUnlockedAbility(stack);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return Mth.hsvToRgb(Math.max(0F, (float) getCharges(stack) / getMaxCharges(stack)) / 3F, 1F, 1F);
    }

    public int getMaxCharges(ItemStack stack) {
        return (int) MathUtils.round(getStatValue(stack, "glider", "count"), 0);
    }

    public int getCharges(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.CHARGE, 0);
    }

    public void setCharges(ItemStack stack, int amount) {
        stack.set(DataComponentRegistry.CHARGE, Math.max(amount, 0));
    }

    public void addCharges(ItemStack stack, int amount) {
        setCharges(stack, getCharges(stack) + amount);
    }

    public static boolean isHoldingUmbrella(LivingEntity entity, InteractionHand hand) {
        return entity.getItemInHand(hand).getItem() instanceof UmbrellaItem && (!entity.isUsingItem() || entity.getUsedItemHand() != hand);
    }

    @EventBusSubscriber(value = Dist.CLIENT)
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

            if (!(stack.getItem() instanceof UmbrellaItem relic) || !relic.canPlayerUseAbility(player, stack, "glider")
                    || player.getCooldowns().isOnCooldown(relic) || relic.getCharges(stack) <= 0)
                return;

            var angle = player.getLookAngle().scale(-1.15F);
            var motion = player.getDeltaMovement().add(angle);

            player.setDeltaMovement(motion.x(), angle.y(), motion.z());

            NetworkHandler.sendToServer(new RepulsionUmbrellaPacket());
        }

        @SubscribeEvent
        public static void onLivingRender(RenderLivingEvent.Pre<?, ?> event) {
            if (!(event.getEntity() instanceof Player player) || !(event.getRenderer().getModel() instanceof HumanoidModel<?> humanoidModel))
                return;

            var isHoldingOffHand = isHoldingUmbrella(player, InteractionHand.OFF_HAND);
            var isHoldingMainHand = isHoldingUmbrella(player, InteractionHand.MAIN_HAND);

            var isRightHanded = player.getMainArm() == HumanoidArm.RIGHT;

            if ((isHoldingMainHand && isRightHanded) || (isHoldingOffHand && !isRightHanded))
                humanoidModel.rightArmPose = HumanoidModel.ArmPose.THROW_SPEAR;

            if ((isHoldingMainHand && !isRightHanded) || (isHoldingOffHand && isRightHanded))
                humanoidModel.leftArmPose = HumanoidModel.ArmPose.THROW_SPEAR;
        }
    }

    @EventBusSubscriber
    public static class UmbrellaCommonEvents {
        @SubscribeEvent
        public static void onPlayerHurt(LivingIncomingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player))
                return;

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

            if (!player.isUsingItem() || !(event.getSource().getEntity() instanceof LivingEntity source)
                    || source.position().subtract(player.position()).normalize().dot(player.getLookAngle().normalize()) < 0.65F)
                return;

            var level = player.getCommandSenderWorld();

            event.setCanceled(true);

            relic.spreadRelicExperience(player, stack, 1);

            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(2), entity -> entity != player && entity.isAlive())) {
                var motion = entity.position().subtract(player.position()).normalize().scale(0.4F + (relic.getStatValue(stack, "glider", "count") * 0.2F));

                entity.setDeltaMovement(motion);

                var pos = source.position().add(new Vec3(0F, source.getBbHeight() / 2F, 0F));
                var velocity = motion.normalize().scale(0.5F);

                if (!level.isClientSide())
                    ((ServerLevel) level).sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 10, velocity.x, velocity.y, velocity.z, 0.1F);
            }

            level.playSound(null, player.blockPosition(), SoundEvents.ALLAY_HURT, SoundSource.MASTER, 0.3F, 1 + (player.getRandom().nextFloat() * 0.25F));
        }
    }
}