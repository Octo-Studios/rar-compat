package it.hurts.octostudios.rarcompat.items.feet;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.octostudios.rarcompat.network.NetworkHandler;
import it.hurts.octostudios.rarcompat.network.packets.SteadfastSpikesPacket;
import it.hurts.sskirillss.relics.api.events.common.LivingSlippingEvent;
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
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;

public class SteadfastSpikesItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("passive")
                                .maxLevel(0)
                                .build())
                        .ability(AbilityData.builder("resistance")
                                .stat(StatData.builder("modifier")
                                        .initialValue(0.2, 0.3D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.15D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff542d20)
                                .borderBottom(0xff555b64)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.ANTHROPOGENIC)
                        .build())
                .build();
    }

    @Override
    public void wornTick(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player) || !player.getCommandSenderWorld().isClientSide() || !canUseAbility(stack, "passive")
                || player.onGround() || !player.horizontalCollision || player.getDeltaMovement().y >= -0.1)
            return;

        NetworkHandler.sendToServer(new SteadfastSpikesPacket());

        player.setDeltaMovement(player.getDeltaMovement().x, -0.05, player.getDeltaMovement().z);
        player.getCommandSenderWorld().addParticle(ParticleUtils.constructSimpleSpark(new Color(50, 20 + player.getRandom().nextInt(50), 0), 0.5F, 50, 0.9F),
                player.getX(), player.getY(), player.getZ(), 0, 0, 0);
    }

    @Mod.EventBusSubscriber
    public static class SteadfastSpikesEvent {
        @SubscribeEvent
        public static void onLivingKnockBack(LivingKnockBackEvent event) {
            if (!(event.getEntity() instanceof Player player))
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.STEADFAST_SPIKES.get());

            if (!(stack.getItem() instanceof SteadfastSpikesItem relic))
                return;

            event.setStrength((float) (event.getStrength() * (1 - relic.getAbilityValue(stack, "resistance", "modifier"))));

            relic.spreadExperience(player, stack, 1);
        }

        @SubscribeEvent
        public static void onLivingSlipping(LivingSlippingEvent event) {
            if (event.getFriction() <= 0.6F || !(event.getEntity() instanceof Player player)
                    || player.isInWater() || player.isInLava())
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.STEADFAST_SPIKES.get());

            if (!(stack.getItem() instanceof SteadfastSpikesItem relic))
                return;

            if (player.tickCount % 60 == 0 && player.onGround() && (player.getDeltaMovement().x != 0 || player.getDeltaMovement().z != 0))
                relic.spreadExperience(player, stack, 1);

            event.setFriction((float) (event.getFriction() * (1 - (relic.getAbilityValue(stack, "resistance", "modifier") / 3))));
        }
    }
}
