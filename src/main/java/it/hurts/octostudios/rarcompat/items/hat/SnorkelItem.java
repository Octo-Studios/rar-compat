package it.hurts.octostudios.rarcompat.items.hat;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
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
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import top.theillusivec4.curios.api.SlotContext;

import static it.hurts.sskirillss.relics.init.DataComponentRegistry.TOGGLED;

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
                                .research(ResearchData.builder()
                                        .star(0, 20, 9).star(1, 16, 14).star(2, 6, 10)
                                        .star(3, 12, 20).star(4, 4, 16).star(5, 16, 24)
                                        .star(6, 9, 24)
                                        .link(0, 1).link(1, 2).link(1, 3).link(4, 2).link(4, 3).link(1, 5).link(5, 6).link(6, 4)
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff22b818)
                                .borderBottom(0xff00869c)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFF22b818)
                                .endColor(0x0000869c)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData.abilityBuilder("diving")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.CYAN)
                                        .build())
                                .build())
                        .build())
                .loot(LootData.builder()
                        .entry(LootEntries.AQUATIC)
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.tickCount % 10 != 0 || !canPlayerUseAbility(player, stack, "diving"))
            return;

        var toggled = stack.getOrDefault(TOGGLED, false);

        if (player.isUnderWater()) {
            if (!toggled) {
                stack.set(TOGGLED, true);

                var effect = player.getEffect(MobEffects.WATER_BREATHING);

                var currentDuration = effect != null ? effect.getDuration() : 0;
                var resultDuration = (int) getStatValue(stack, "diving", "duration");

                if (resultDuration * 20 > currentDuration) {
                    spreadRelicExperience(player, stack, (int) Math.abs(Math.ceil((resultDuration - currentDuration) / 20F)));

                    player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, resultDuration * 20, 0, true, true));
                }
            }
        } else if (toggled)
            stack.set(TOGGLED, false);
    }

    @EventBusSubscriber(value = Dist.CLIENT)
    public static class SnorkelEvent {
        @SubscribeEvent
        public static void onFogRender(ViewportEvent.RenderFog event) {
            Player player = Minecraft.getInstance().player;

            if (player == null)
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.SNORKEL.value());

            player.getCommandSenderWorld().getFluidState(BlockPos.containing(player.getEyePosition()));

            if (!(stack.getItem() instanceof SnorkelItem) || !player.isInLiquid())
                return;

            event.scaleFarPlaneDistance(150);
            event.setCanceled(true);
        }
    }
}