package it.hurts.octostudios.rarcompat.items.hat;

import artifacts.registry.ModItems;
import artifacts.registry.ModSoundEvents;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
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
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import it.hurts.sskirillss.relics.utils.NBTUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;

public class WhoopeeCushionItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("push")
                                .stat(StatData.builder("chance")
                                        .initialValue(0.2D, 0.4D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.075)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())
                                .stat(StatData.builder("radius")
                                        .initialValue(3D, 5D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.05)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xffdd5959)
                                .borderBottom(0xff7c2a2a)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.VILLAGE)
                        .entry(LootCollections.ANTHROPOGENIC)
                        .entry(LootCollections.SCULK)
                        .entry(LootCollections.JUNGLE)
                        .entry(LootCollections.DESERT)
                        .entry(LootCollections.AQUATIC)
                        .entry(LootCollections.COLD)
                        .build())
                .build();
    }

    @Override
    public void wornTick(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player) || player.getCommandSenderWorld().isClientSide() || !canUseAbility(stack, "push")
                || !player.onGround())
            return;

        var isSneaking = player.isShiftKeyDown();
        var random = player.getRandom();

        if (isSneaking && !getToggled(stack)
                && random.nextDouble() < getAbilityValue(stack, "push", "chance"))
            createWhoopee((ServerLevel) player.level(), player, stack, random);

        setToggled(stack, isSneaking);
    }

    public void setToggled(ItemStack stack, boolean val) {
        NBTUtils.setBoolean(stack, "toggled", val);
    }

    public boolean getToggled(ItemStack stack) {
        return NBTUtils.getBoolean(stack, "toggled", false);
    }

    public void createWhoopee(ServerLevel level, Player player, ItemStack stack, RandomSource random) {
        level.playSound(null, player.blockPosition(), ModSoundEvents.FART.get(), player.getSoundSource(), 1F, 0.75F + player.getRandom().nextFloat());

        spreadExperience(player, stack, 1);

        for (LivingEntity livingEntity : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(getAbilityValue(stack, "push", "radius"))).stream().filter(livingEntity -> !livingEntity.getUUID().equals(player.getUUID())).toList()) {
            var vec3 = livingEntity.position().subtract(player.position()).normalize();

            if (livingEntity instanceof ServerPlayer serverPlayer)
                NetworkHandler.sendToClient(new PacketPlayerMotion(vec3.x, 0.2, vec3.z), serverPlayer);
            else
                livingEntity.setDeltaMovement(vec3.x, 0.2, vec3.z);

            livingEntity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 1));
        }

        level.sendParticles(ParticleUtils.constructSimpleSpark(new Color(0, 100 + random.nextInt(50), 0), 0.5F, 50, 0.9F),
                player.getX(), player.getY() + 0.5, player.getZ(), 30, 0.25, 0.3, 0.25, 0.1);
    }

    @Mod.EventBusSubscriber
    public static class WhoopeeCushionEvent {
        @SubscribeEvent
        public static void onAttackPlayer(LivingHurtEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.getCommandSenderWorld().isClientSide()
                    || event.getSource().getEntity() == player)
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.WHOOPEE_CUSHION.get());

            if (!(stack.getItem() instanceof WhoopeeCushionItem relic) || !relic.canUseAbility(stack, "push")
                    || player.getRandom().nextDouble() > relic.getAbilityValue(stack, "push", "chance"))
                return;

            relic.createWhoopee((ServerLevel) player.getCommandSenderWorld(), player, stack, player.getRandom());
        }
    }
}
