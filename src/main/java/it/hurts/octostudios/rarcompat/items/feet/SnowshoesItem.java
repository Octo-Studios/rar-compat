package it.hurts.octostudios.rarcompat.items.feet;

import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.init.DataComponentRegistry;
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
import it.hurts.sskirillss.relics.utils.WorldUtils;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import top.theillusivec4.curios.api.SlotContext;

public class SnowshoesItem extends WearableRelicItem {

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("passive")
                                .maxLevel(0)
                                .build())
                        .ability(AbilityData.builder("speed")
                                .stat(StatData.builder("amount")
                                        .initialValue(0.2D, 0.4D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())
                                .research(ResearchData.builder()
                                        .star(0, 6, 7).star(1, 11, 3).star(2, 16, 8).star(3, 15, 16)
                                        .star(4, 14, 25).star(5, 8, 25).star(6, 9, 17)
                                        .link(0, 1).link(1, 2).link(2, 3).link(3, 4).link(4, 5).link(5, 6).link(6, 0)
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xffae894e)
                                .borderBottom(0xff614126)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFFae894e)
                                .endColor(0x00614126)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData.abilityBuilder("speed")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.CYAN)
                                        .build())
                                .build())
                        .build())
                .loot(LootData.builder()
                        .entry(LootEntries.WILDCARD, LootEntries.FROST, LootEntries.TAIGA, LootEntries.MOUNTAIN)
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.getCommandSenderWorld().isClientSide() || !canPlayerUseAbility(player, stack, "speed"))
            return;

        double step = 0.01D;
        double modifier = isStandingOnSnow(player) ? step : -step;

        if (modifier != 0D)
            addSpeed(stack, modifier);

        EntityUtils.resetAttribute(player, stack, Attributes.MOVEMENT_SPEED, (float) getSpeed(stack), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        if (isStandingOnSnow(player) && player.tickCount % 60 == 0 && (player.getKnownMovement().x != 0 || player.getKnownMovement().z != 0))
            spreadRelicExperience(player, stack, 1);
    }

    public void addSpeed(ItemStack stack, double val) {
        if (getSpeed(stack) <= getStatValue(stack, "speed", "amount") || val < 0)
            setSpeed(stack, getSpeed(stack) + val);
    }

    public double getSpeed(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.SPEED, 0D);
    }

    public void setSpeed(ItemStack stack, double val) {
        stack.set(DataComponentRegistry.SPEED, Math.max(val, 0D));
    }

    private boolean isStandingOnSnow(Player player) {
        for (int i = 0; i < 8; i++)
            if (player.getCommandSenderWorld().getBlockState(player.blockPosition().atY((int) Math.floor(WorldUtils.getGroundHeight(player, player.position().add(0, 0.1, 0), 8))).below(i)).is(BlockTags.SNOW))
                return true;

        return false;
    }

    @Override
    public boolean canWalkOnPowderedSnow(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player))
            return super.canWalkOnPowderedSnow(slotContext, stack);

        return canPlayerUseAbility(player, stack, "passive");
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (newStack.getItem() == stack.getItem())
            return;

        EntityUtils.removeAttribute(slotContext.entity(), stack, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }
}
