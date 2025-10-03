package it.hurts.octostudios.rarcompat.items.hands;

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
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff0c71e0)
                                .borderBottom(0xff151989)
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

    @Mod.EventBusSubscriber
    public static class DiggingClawsEvent {
        @SubscribeEvent
        public static void onDiggingClawsHarvestCheck(PlayerEvent.HarvestCheck event) {
            BlockState blockState = event.getTargetBlock();
            Player player = event.getEntity();

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.DIGGING_CLAWS.get());

            if (player.getCommandSenderWorld().isClientSide() || !(stack.getItem() instanceof DiggingClawsItem) || event.canHarvest())
                return;

            if (player.getMainHandItem().getItem() instanceof TieredItem tieredItem) {
                int tier = getTierFromString(tieredItem.getTier());

                if (tier + 1 >= getRequiredToolTier(blockState))
                    event.setCanHarvest(true);
            } else if (!blockState.requiresCorrectToolForDrops() || getRequiredToolTier(blockState) <= 1)
                event.setCanHarvest(true);
        }

        @SubscribeEvent
        public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
            Player player = event.getEntity();

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.DIGGING_CLAWS.get());

            if (!(stack.getItem() instanceof DiggingClawsItem relic) || !relic.canPlayerUseActiveAbility(player, stack, "fast_mining"))
                return;

            var original = event.getOriginalSpeed();

            event.setNewSpeed((float) (original + (original * relic.getAbilityValue(stack, "fast_mining", "modifier"))));
        }

        @SubscribeEvent
        public static void onBlockDestroy(BlockEvent.BreakEvent event) {
            Player player = event.getPlayer();
            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.DIGGING_CLAWS.get());

            if (!(stack.getItem() instanceof DiggingClawsItem relic))
                return;

            float hardness = (event.getState().getDestroySpeed(player.level(), player.blockPosition()) / 20);

            if (player.getRandom().nextDouble() <= hardness)
                relic.spreadExperience(player, stack, 1);
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