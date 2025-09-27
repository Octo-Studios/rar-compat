package it.hurts.octostudios.rarcompat.items.hat;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.init.SoundRegistry;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.CastData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastStage;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastType;
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
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

public class NightVisionGogglesItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("vision")
                                .active(CastData.builder()
                                        .type(CastType.TOGGLEABLE)
                                        .build())
                                .stat(StatData.builder("amount")
                                        .initialValue(0.1D, 0.15)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.5D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff84fc40)
                                .borderBottom(0xff00e03e)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.ANTHROPOGENIC)
                        .entry(LootCollections.SCULK)
                        .build())
                .build();
    }

    @Override
    public void wornTick(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player))
            return;

        if (isAbilityTicking(stack, "vision")) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 10, 0, false, false));

            var percent = (Math.abs(1 - (player.getCommandSenderWorld().getMaxLocalRawBrightness(player.blockPosition()) / 15.0F)));

            if (player.getRandom().nextFloat() <= percent && player.tickCount % 60 == 0 && !(Math.abs(player.getDeltaMovement().x) <= 0.01D
                    || Math.abs(player.getDeltaMovement().z) <= 0.01D))
                spreadExperience(player, stack, 1);
        } else {
            if (isNightVision(player.getActiveEffects()))
                player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    @Override
    public void castActiveAbility(ItemStack stack, Player player, String ability, CastType type, CastStage stage) {
        if (ability.equals("vision") && player.getCommandSenderWorld().isClientSide && stage == CastStage.START)
            player.playSound(SoundRegistry.NIGHT_VISION_TOGGLE.get(), 1F, 0.75F + player.getRandom().nextFloat() * 0.5F);
    }


    public boolean isNightVision(Collection<MobEffectInstance> activeEffects) {
        return !activeEffects.isEmpty() && activeEffects.stream().anyMatch(entity -> entity.getEffect() == MobEffects.NIGHT_VISION
                && entity.getDuration() <= 10);
    }

    @Mod.EventBusSubscriber(Dist.CLIENT)
    public static class NightVisionGogglesClientEvent {
        @SubscribeEvent
        public static void onFogRender(ViewportEvent.RenderFog event) {
            Player player = Minecraft.getInstance().player;

            if (player == null)
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.NIGHT_VISION_GOGGLES.get());

            if (!(stack.getItem() instanceof NightVisionGogglesItem relic) || !relic.isAbilityTicking(stack, "vision")
                    || !player.hasEffect(MobEffects.BLINDNESS) && !player.hasEffect(MobEffects.DARKNESS))
                return;

            var statValue = relic.getAbilityValue(stack, "vision", "amount") * (player.hasEffect(MobEffects.BLINDNESS) ? 9 : 1);

            event.scaleFarPlaneDistance((float) (event.getFarPlaneDistance() * statValue));

            event.setCanceled(true);
        }
    }
}