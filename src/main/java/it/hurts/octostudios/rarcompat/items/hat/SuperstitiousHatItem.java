package it.hurts.octostudios.rarcompat.items.hat;

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
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;

public class SuperstitiousHatItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("looting")
                                .stat(StatData.builder("chance")
                                        .initialValue(0.1D, 0.2D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.25)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())
                                .research(ResearchData.builder()
                                        .star(0, 3, 9).star(1, 9, 3).star(2, 11, 12)
                                        .star(3, 17, 4).star(4, 19, 11).star(5, 19, 16)
                                        .star(6, 16, 20).star(7, 14, 24).star(8, 11, 18)
                                        .star(9, 4, 24).star(10, 4, 19).star(11, 2, 14)
                                        .link(0, 1).link(1, 2).link(2, 3).link(5, 6).link(7, 8).link(2, 0).link(2, 10)
                                        .link(2, 6).link(2, 5).link(2, 3).link(2, 4).link(7, 8).link(7, 8).link(3, 4)
                                        .link(9, 7).link(9, 8).link(2, 11).link(2, 8).link(11, 10)
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff55b014)
                                .borderBottom(0xff206a2a)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFFe39211)
                                .endColor(0x0013521f)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData.abilityBuilder("looting")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.GREEN)
                                        .build())
                                .build())
                        .build())
                .loot(LootData.builder()
                        .entry(LootEntries.CAVE, LootEntries.MINESHAFT)
                        .build())
                .build();
    }

    @Override
    public int getLootingLevel(SlotContext slotContext, @Nullable LootContext lootContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || !canPlayerUseAbility(player, stack, "looting"))
            return super.getLootingLevel(slotContext, lootContext, stack);

        var random = player.getRandom();
        var amount = MathUtils.multicast(random, getStatValue(stack, "looting", "chance"));

        if (amount > 0)
            spreadRelicExperience(player, stack, random.nextInt(amount) + 1);

        return amount;
    }
}