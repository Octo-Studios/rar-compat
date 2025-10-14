package it.hurts.octostudios.rarcompat.items.bracelet;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class OnionRingItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("onion")
                                .stat(StatData.builder("amount")
                                        .initialValue(0.01D, 0.015D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.2D)
                                        .formatValue(value -> MathUtils.round(value * 100, 1))
                                        .build())
                                .build())
                        .ability(AbilityData.builder("saturation")
                                .requiredLevel(5)
                                .stat(StatData.builder("chance")
                                        .initialValue(0.05D, 0.15D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> MathUtils.round(value * 100, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xfff0852a)
                                .borderBottom(0xff934311)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(15)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.ANTHROPOGENIC)
                        .build())
                .build();
    }

    @Mod.EventBusSubscriber
    public static class OnionRingEvent {
        @SubscribeEvent
        public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
            Player player = event.getEntity();
            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.ONION_RING.get());

            if (!(stack.getItem() instanceof OnionRingItem relic) || !relic.canUseAbility(stack, "onion"))
                return;

            int currentHunger = player.getFoodData().getFoodLevel();
            double modifier = relic.getAbilityValue(stack, "onion", "amount");

            event.setNewSpeed((float) (event.getNewSpeed() + (event.getNewSpeed() * (currentHunger * modifier))));
        }

        @SubscribeEvent
        public static void onBlockDestroy(BlockEvent.BreakEvent event) {
            Player player = event.getPlayer();
            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.ONION_RING.get());

            if (!(stack.getItem() instanceof OnionRingItem relic))
                return;

            var random = player.getRandom();

            if (relic.canUseAbility(stack, "onion") && event.getState().getDestroySpeed(player.level(), player.blockPosition()) >= 0.5
                    && player.getFoodData().getFoodLevel() / 20D >= random.nextDouble())
                relic.spreadExperience(player, stack, 1);

            var footData = player.getFoodData();

            if (relic.canUseAbility(stack, "saturation") && random.nextDouble() <= relic.getAbilityValue(stack, "saturation", "chance")
                    && footData.needsFood()) {

                footData.eat(1, 0.5F);

                relic.spreadExperience(player, stack, 1);
            }
        }
    }
}
