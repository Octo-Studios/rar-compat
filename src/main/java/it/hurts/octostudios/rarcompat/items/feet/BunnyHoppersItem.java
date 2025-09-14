package it.hurts.octostudios.rarcompat.items.feet;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.octostudios.rarcompat.network.NetworkHandler;
import it.hurts.octostudios.rarcompat.network.packets.PowerJumpPacket;
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
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import it.hurts.sskirillss.relics.utils.NBTUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;

public class BunnyHoppersItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("hold")
                                .stat(StatData.builder("duration")
                                        .initialValue(3D, 5D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.2)
                                        .formatValue(value -> MathUtils.round(value / 20, 2))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xffa89075)
                                .borderBottom(0xff473a2f)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.VILLAGE)
                        .entry(LootCollections.JUNGLE)
                        .build())
                .build();
    }

    @Override
    public void wornTick(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player) || !canPlayerUseActiveAbility(player, stack, "hold"))
            return;

        if (player.onGround())
            addTime(stack, -getTime(stack));

        var statValue = getAbilityValue(stack, "hold", "duration");

        if (player.hasEffect(MobEffects.JUMP)) {
            MobEffectInstance jumpBoost = player.getEffect(MobEffects.JUMP);

            if (jumpBoost != null)
                statValue += (jumpBoost.getAmplifier() + 1) * 4;
        }

        var level = player.getCommandSenderWorld();

        if (!level.isClientSide() || !(player instanceof LocalPlayer localPlayer) || getTime(stack) >= statValue
                || player.isFallFlying() || !getToggled(stack))
            return;

        if (!localPlayer.input.jumping) {
            setToggled(stack, false);
        } else {
            NetworkHandler.sendToServer(new PowerJumpPacket());

            player.setDeltaMovement(new Vec3(player.getDeltaMovement().x, 0.6, player.getDeltaMovement().z));

            var random = player.getRandom();

            for (int i = 0; i < 10; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 0.5;
                double offsetY = (random.nextDouble() - 0.5) * 0.5;
                double offsetZ = (random.nextDouble() - 0.5) * 0.5;

                level.addParticle(ParticleUtils.constructSimpleSpark(new Color(200 + random.nextInt(56), 200 + random.nextInt(56), 200 + random.nextInt(56)),
                                0.5F, 40, 0.9F),
                        player.getX() + offsetX,
                        player.getY() + 0.1 + offsetY,
                        player.getZ() + offsetZ,
                        0, 0, 0);
            }
        }
    }


    public void addTime(ItemStack stack, int val) {
        NBTUtils.setInt(stack, "time", getTime(stack) + val);
    }

    public int getTime(ItemStack stack) {
        return NBTUtils.getInt(stack, "time", 0);
    }

    public void setToggled(ItemStack stack, boolean val) {
        NBTUtils.setBoolean(stack,"toggled", val);
    }

    public boolean getToggled(ItemStack stack) {
        return NBTUtils.getBoolean(stack,"toggled", false);
    }

    @Mod.EventBusSubscriber
    public static class BunnyHoppersEvent {
        @SubscribeEvent
        public static void onPlayerJumping(LivingEvent.LivingJumpEvent event) {
            if (!(event.getEntity() instanceof Player player))
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.BUNNY_HOPPERS.get());

            if (!(stack.getItem() instanceof BunnyHoppersItem relic) || !relic.canPlayerUseActiveAbility(player, stack, "hold"))
                return;

            relic.setToggled(stack, true);
        }

        @SubscribeEvent
        public static void onPlayerFall(LivingFallEvent event) {
            if (!(event.getEntity() instanceof Player player))
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.BUNNY_HOPPERS.get());

            if (!(stack.getItem() instanceof BunnyHoppersItem relic) || player.getCommandSenderWorld().isClientSide())
                return;

            event.setDistance(Math.max(event.getDistance() - relic.getTime(stack) * 1.20F, 0));
        }
    }
}
