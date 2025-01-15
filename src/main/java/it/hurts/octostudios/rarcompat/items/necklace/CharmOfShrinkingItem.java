package it.hurts.octostudios.rarcompat.items.necklace;

import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.init.DataComponentRegistry;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.CastData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastStage;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastType;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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
                                .active(CastData.builder().type(CastType.CYCLICAL)
                                        .build())
                                .stat(StatData.builder("time")
                                        .initialValue(8D, 10D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.2)
                                        .formatValue(value -> (int) MathUtils.round(value, 1))
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
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData.abilityBuilder("shrinking")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.CYAN)
                                        .build())
                                .build())
                        .build())
                .loot(LootData.builder()
                        .entry(LootEntries.WILDCARD, LootEntries.CAVE, LootEntries.MINESHAFT)
                        .build())
                .build();
    }

    @Override
    public void castActiveAbility(ItemStack stack, Player player, String ability, CastType type, CastStage stage) {
        if ((player.getCommandSenderWorld().isClientSide()))
            return;

        if (getProgress(stack) < 70 && stage == CastStage.TICK) {
            setProgress(stack, 0.5);

            EntityUtils.resetAttribute(player, stack, Attributes.SCALE, (float) -getProgress(stack) / 100, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

            setCurrentTick(stack, -getCurrentTick(stack));
        } else if (stage == CastStage.START && getProgress(stack) > 0) {
            setCurrentTick(stack, -getCurrentTick(stack));

            removeAttribute(player, stack);

            setProgress(stack, -getProgress(stack));

            addAbilityCooldown(stack, "shrinking", 200);

            playSound(player, SoundEvents.PUFFER_FISH_BLOW_UP);
        }

        if (stage == CastStage.START)
            playSound(player, SoundEvents.PUFFER_FISH_BLOW_OUT);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.getCommandSenderWorld().isClientSide())
            return;

        if (getCurrentTick(stack) >= getStatValue(stack, "shrinking", "time") && getProgress(stack) > 0) {
            setCurrentTick(stack, -getCurrentTick(stack));

            setProgress(stack, -getProgress(stack));

            removeAttribute(player, stack);

            playSound(player, SoundEvents.PUFFER_FISH_BLOW_UP);

            addAbilityCooldown(stack, "shrinking", 200);
        }

        if (player.tickCount % 20 == 0 && getProgress(stack) >= 5) {
            spreadRelicExperience(player, stack, 1);

            setCurrentTick(stack, 1);
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (newStack.getItem() == stack.getItem() || !(slotContext.entity() instanceof Player player))
            return;

        removeAttribute(player, stack);
    }

    public void playSound(Player player, SoundEvent events) {
        player.getCommandSenderWorld().playSound(null, player, events, SoundSource.PLAYERS,
                1.0F, 0.9F + player.getRandom().nextFloat() * 0.2F);
    }

    public void setCurrentTick(ItemStack stack, int val) {
        stack.set(DataComponentRegistry.TIME, getCurrentTick(stack) + val);
    }

    public int getCurrentTick(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.TIME, 0);
    }

    public void setProgress(ItemStack stack, double val) {
        stack.set(DataComponentRegistry.HEIGHT, getProgress(stack) + val);
    }

    public double getProgress(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.HEIGHT, 0D);
    }

    private void removeAttribute(LivingEntity entity, ItemStack stack) {
        EntityUtils.removeAttribute(entity, stack, Attributes.SCALE, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
