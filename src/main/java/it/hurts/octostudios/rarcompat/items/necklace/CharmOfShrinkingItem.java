package it.hurts.octostudios.rarcompat.items.necklace;

import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicAttributeModifier;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootData;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import it.hurts.sskirillss.relics.items.relics.base.data.research.ResearchData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.BeamsData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.TooltipData;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;

public class CharmOfShrinkingItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("shrinking")
                                .stat(StatData.builder("multiplier")
                                        .initialValue(0.2D, 0.3D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.1)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())
                                .research(ResearchData.builder()
                                        .star(0, 11, 28).star(1, 11, 17).star(2, 11, 11).star(3, 11, 4)
                                        .star(4, 6, 21).star(5, 16, 21).star(6, 4, 17).star(7, 18, 17)
                                        .star(8, 2, 14).star(9, 20, 14)
                                        .link(0, 1).link(1, 2).link(2, 3).link(1, 4).link(1, 5).link(2, 6).link(2, 7).link(3, 8).link(3, 9)
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff7b59b2)
                                .borderBottom(0xff390f35)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFF7b59b2)
                                .endColor(0x00390f35)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootEntries.CAVE, LootEntries.MINESHAFT)
                        .build())
                .build();
    }

    @Override
    public RelicAttributeModifier getRelicAttributeModifiers(ItemStack stack) {
        if (!isAbilityUnlocked(stack, "shrinking"))
            return super.getRelicAttributeModifiers(stack);

        return RelicAttributeModifier.builder()
                .attribute(new RelicAttributeModifier.Modifier(Attributes.SCALE, (float) -getStatValue(stack, "shrinking", "multiplier")))
                .build();
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || newStack.getItem() == stack.getItem()
                || !isAbilityUnlocked(stack, "shrinking"))
            return;

        playSound(player, SoundEvents.PUFFER_FISH_BLOW_UP);
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || prevStack.getItem() == stack.getItem()
                || !isAbilityUnlocked(stack, "shrinking"))
            return;

        playSound(player, SoundEvents.PUFFER_FISH_BLOW_OUT);
    }

    public void playSound(Player player, SoundEvent events) {
        player.getCommandSenderWorld().playSound(null, player, events, SoundSource.PLAYERS,
                1.0F, 0.9F + player.getRandom().nextFloat() * 0.2F);
    }
}
