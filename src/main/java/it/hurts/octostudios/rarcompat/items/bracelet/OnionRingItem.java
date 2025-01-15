package it.hurts.octostudios.rarcompat.items.bracelet;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public class OnionRingItem extends WearableRelicItem {

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("onion")
                                .stat(StatData.builder("amount")
                                        .initialValue(0.01D, 0.025D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.25D)
                                        .formatValue(value -> MathUtils.round(value * 100, 1))
                                        .build())
                                .research(ResearchData.builder()
                                        .star(0, 11, 25).star(1, 5, 21).star(2, 3, 14).star(3, 5, 7)
                                        .star(4, 11, 4).star(5, 17, 7).star(6, 19, 14).star(7, 17, 21)
                                        .star(8, 6, 11).star(9, 11, 10).star(10, 16, 11)
                                        .link(0, 1).link(1, 2).link(2, 3).link(3, 4).link(4, 5).link(5, 6).link(6, 7).link(7, 0)
                                        .link(0, 9).link(8, 9).link(9, 10).link(2, 8).link(10, 6)
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xfff0852a)
                                .borderBottom(0xff934311)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFFf0852a)
                                .endColor(0x00934311)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData.abilityBuilder("onion")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.YELLOW)
                                        .build())
                                .build())
                        .build())
                .loot(LootData.builder()
                        .entry(LootEntries.WILDCARD, LootEntries.CAVE, LootEntries.MINESHAFT, LootEntries.VILLAGE)
                        .build())
                .build();
    }

    @EventBusSubscriber
    public static class Event {

        @SubscribeEvent
        public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
            Player player = event.getEntity();
            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.ONION_RING.value());

            if (!(stack.getItem() instanceof OnionRingItem relic) || !relic.isAbilityUnlocked(stack, "onion"))
                return;

            int currentHunger = player.getFoodData().getFoodLevel();
            double modifier = relic.getStatValue(stack, "onion", "amount");

            event.setNewSpeed((float) (event.getNewSpeed() + (event.getNewSpeed() * (currentHunger * modifier))));
        }

        @SubscribeEvent
        public static void onBlockDestroy(BlockEvent.BreakEvent event) {
            Player player = event.getPlayer();
            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.ONION_RING.value());

            if (!(stack.getItem() instanceof OnionRingItem relic) || !relic.isAbilityUnlocked(stack, "onion"))
                return;

            float hardness = event.getState().getDestroySpeed(player.level(), player.blockPosition());
            float currentHunger = player.getFoodData().getFoodLevel();

            if (hardness >= 0.5 && currentHunger / 20 >= player.getRandom().nextFloat())
                relic.spreadRelicExperience(player, stack, 1);
        }
    }
}
