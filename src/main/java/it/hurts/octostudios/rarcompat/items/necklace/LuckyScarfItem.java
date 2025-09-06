package it.hurts.octostudios.rarcompat.items.necklace;

import it.hurts.octostudios.rarcompat.items.IRelicArtifact;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.octostudios.rarcompat.utils.MathBaseUtils;
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
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;

public class LuckyScarfItem extends WearableRelicItem implements IRelicArtifact {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("luck")
                                .stat(StatData.builder("chance")
                                        .initialValue(0.1D, 0.2D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.25)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff84ca26)
                                .borderBottom(0xff205c0e)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .build()).loot(LootData.builder()
                        .entry(LootCollections.ANTHROPOGENIC)
                        .build())
                .build();
    }


    @Override
    public int getFortuneLevel(ItemStack stack, SlotContext slotContext, @Nullable LootContext lootContext) {
        if (!(slotContext.entity() instanceof Player player) || !canPlayerUseActiveAbility(player, stack, "luck"))
            return super.getFortuneLevel();

        var random = player.getRandom();
        var amount = MathBaseUtils.multicast(random, getAbilityValue(stack, "luck", "chance"), 1F);

        if (amount > 0)
            spreadExperience(player, stack, random.nextInt(amount) + 1);

        return amount;
    }
}