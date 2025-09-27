package it.hurts.octostudios.rarcompat.items.hat;

import artifacts.registry.ModItems;
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
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import it.hurts.sskirillss.relics.utils.NBTUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class SnorkelItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("passive")
                                .maxLevel(0)
                                .build())
                        .ability(AbilityData.builder("diving")
                                .stat(StatData.builder("duration")
                                        .initialValue(5D, 10D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.2D)
                                        .formatValue(value -> (int) MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff22b818)
                                .borderBottom(0xff00869c)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.AQUATIC)
                        .build())
                .build();
    }

    @Override
    public void wornTick(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player) || player.tickCount % 10 != 0 || !canPlayerUseActiveAbility(player, stack, "diving"))
            return;

        var toggled = getToggled(stack);

        if (player.isUnderWater()) {
            if (!toggled) {
                setToggled(stack, true);

                var effect = player.getEffect(MobEffects.WATER_BREATHING);

                var currentDuration = effect != null ? effect.getDuration() : 0;
                var resultDuration = (int) getAbilityValue(stack, "diving", "duration");

                if (resultDuration * 20 > currentDuration) {
                    spreadExperience(player, stack, (int) Math.abs(Math.ceil((resultDuration - currentDuration) / 20F)));

                    player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, resultDuration * 20, 0, true, true));
                }
            }
        } else if (toggled)
            setToggled(stack, false);
    }

    public void setToggled(ItemStack stack, boolean val) {
        NBTUtils.setBoolean(stack, "toggled", val);
    }

    public boolean getToggled(ItemStack stack) {
        return NBTUtils.getBoolean(stack, "toggled", false);
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT)
    public static class SnorkelClientEvent {
        @SubscribeEvent
        public static void onFogRender(ViewportEvent.RenderFog event) {
            Player player = Minecraft.getInstance().player;

            if (player == null)
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.SNORKEL.get());

            player.getCommandSenderWorld().getFluidState(BlockPos.containing(player.getEyePosition()));

            if (!(stack.getItem() instanceof SnorkelItem) || !player.isInFluidType())
                return;

            event.scaleFarPlaneDistance(150);
            event.setCanceled(true);
        }
    }
}