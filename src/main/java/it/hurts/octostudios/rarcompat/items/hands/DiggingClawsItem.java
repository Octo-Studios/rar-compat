package it.hurts.octostudios.rarcompat.items.hands;

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
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public class DiggingClawsItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("passive")
                                .maxLevel(0)
                                .build())
                        .ability(AbilityData.builder("fast_mining")
                                .stat(StatData.builder("modifier")
                                        .initialValue(0.15D, 0.35D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.4D)
                                        .formatValue(value -> (int) (MathUtils.round(value * 100, 0)))
                                        .build())
                                .research(ResearchData.builder()
                                        .star(0, 4, 15).star(1, 12, 3).star(2, 6, 19).star(3, 16, 6)
                                        .star(4, 9, 23).star(5, 17, 12).star(6, 13, 24).star(7, 17, 18)
                                        .link(0, 1).link(2, 3).link(4, 5).link(6, 7)
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff0c71e0)
                                .borderBottom(0xff151989)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFF151989)
                                .endColor(0x00c71e0)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData.abilityBuilder("fast_mining")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.CYAN)
                                        .build())
                                .build())
                        .build())
                .loot(LootData.builder()
                        .entry(LootEntries.CAVE, LootEntries.MINESHAFT)
                        .build())
                .build();
    }

    @EventBusSubscriber
    public static class DiggingClawsEvent {
        @SubscribeEvent
        private static void onDiggingClawsHarvestCheck(PlayerEvent.HarvestCheck event) {
            BlockState blockState = event.getTargetBlock();
            Player player = event.getEntity();

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.DIGGING_CLAWS.value());

            if (player.getCommandSenderWorld().isClientSide() || !(stack.getItem() instanceof DiggingClawsItem) || event.canHarvest())
                return;

            if (player.getMainHandItem().getItem() instanceof TieredItem tieredItem) {
                int tier = getTierFromString(tieredItem.getTier());

                if (tier + 1 >= getRequiredToolTier(blockState))
                    event.setCanHarvest(true);
            } else if (!event.getTargetBlock().is(BlockTags.INCORRECT_FOR_WOODEN_TOOL))
                event.setCanHarvest(true);
        }

        @SubscribeEvent
        public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
            Player player = event.getEntity();

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.DIGGING_CLAWS.value());

            if (!(stack.getItem() instanceof DiggingClawsItem relic) || !relic.canPlayerUseAbility(player, stack, "fast_mining"))
                return;

            var original = event.getOriginalSpeed();

            event.setNewSpeed((float) (original + (original * relic.getStatValue(stack, "fast_mining", "modifier"))));
        }

        @SubscribeEvent
        public static void onBlockDestroy(BlockEvent.BreakEvent event) {
            Player player = event.getPlayer();
            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.DIGGING_CLAWS.value());

            if (!(stack.getItem() instanceof DiggingClawsItem relic))
                return;

            float hardness = (event.getState().getDestroySpeed(player.level(), player.blockPosition()) / 20);

            if (player.getRandom().nextDouble() <= hardness)
                relic.spreadRelicExperience(player, stack, 1);
        }

        public static int getTierFromString(Tier tier) {
            if (tier == Tiers.STONE) return 2;
            if (tier == Tiers.IRON) return 3;
            if (tier == Tiers.GOLD) return 4;
            if (tier == Tiers.DIAMOND) return 5;
            if (tier == Tiers.NETHERITE) return 6;

            return 1;
        }

        public static int getRequiredToolTier(BlockState state) {
            if (!state.requiresCorrectToolForDrops())
                return 0;

            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return 5;
            else if (state.is(BlockTags.NEEDS_IRON_TOOL)) return 3;
            else if (state.is(BlockTags.NEEDS_STONE_TOOL)) return 2;

            return 1;
        }
    }
}