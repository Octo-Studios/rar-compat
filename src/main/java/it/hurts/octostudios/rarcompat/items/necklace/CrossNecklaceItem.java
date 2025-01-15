package it.hurts.octostudios.rarcompat.items.necklace;

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
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

public class CrossNecklaceItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("invulnerability")
                                .stat(StatData.builder("modifier")
                                        .initialValue(0.3D, 0.5D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.2D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .research(ResearchData.builder()
                                        .star(0, 4, 6).star(1, 11, 6).star(2, 4, 20).star(3, 10, 19)
                                        .star(4, 20, 15).star(5, 20, 21).star(6, 11, 25).star(7, 20, 6)
                                        .link(0, 2).link(2, 6).link(6, 5).link(5, 4).link(7, 4)
                                        .link(4, 1).link(1, 3)
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xfffce94e)
                                .borderBottom(0xffb06311)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFFfce94e)
                                .endColor(0x00b06311)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData.abilityBuilder("invulnerability")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.YELLOW)
                                        .build())
                                .build())
                        .build())
                .loot(LootData.builder()
                        .entry(LootEntries.WILDCARD, LootEntries.VILLAGE, LootEntries.DESERT)
                        .build())
                .build();
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (newStack.getItem() == stack.getItem() || !(slotContext.entity() instanceof Player player))
            return;

        player.invulnerableTime -= (int) getStatValue(stack, "invulnerability", "modifier");
    }

    @EventBusSubscriber
    public static class CrossNecklaceEvent {
        @SubscribeEvent
        public static void onLivingDamaged(LivingDamageEvent.Post event) {
            if (!(event.getEntity() instanceof Player player))
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.CROSS_NECKLACE.value());

            if (!(stack.getItem() instanceof CrossNecklaceItem relic) || !relic.canPlayerUseAbility(player, stack, "invulnerability"))
                return;

            relic.spreadRelicExperience(player, stack, 1);

            player.invulnerableTime += (int) (relic.getStatValue(stack, "invulnerability", "modifier") * 20);
        }
    }
}