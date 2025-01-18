package it.hurts.octostudios.rarcompat.items.hat;

import artifacts.registry.ModAttributes;
import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicAttributeModifier;
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
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import top.theillusivec4.curios.api.SlotContext;

public class DrinkingHatItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("drinking")
                                .stat(StatData.builder("speed")
                                        .initialValue(0.3D, 0.4D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.15D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())
                                .research(ResearchData.builder()
                                        .star(0, 14, 4).star(1, 19, 9).star(2, 5, 11)
                                        .star(3, 13, 17).star(4, 8, 16).star(5, 4, 25)
                                        .star(6, 12, 26)
                                        .link(0, 1).link(0, 2).link(1, 3).link(4, 2).link(4, 3).link(4, 5).link(4, 6)
                                        .build())
                                .build())
                        .ability(AbilityData.builder("nutrition")
                                .requiredLevel(5)
                                .stat(StatData.builder("hunger")
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.15D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .research(ResearchData.builder()
                                        .star(0, 14, 2).star(1, 13, 9).star(2, 19, 13)
                                        .star(3, 10, 16).star(4, 19, 20).star(5, 13, 25)
                                        .star(6, 4, 23).star(7, 4, 29)
                                        .link(0, 1).link(1, 2).link(1, 3).link(2, 4).link(4, 5).link(6, 3).link(6, 5).link(6, 7)
                                        .build())
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(15)
                        .step(100)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData.abilityBuilder("nutrition")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.CYAN)
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip((player, stack) ->
                                stack.getItem() == ModItems.PLASTIC_DRINKING_HAT.value()
                                        ? TooltipData.builder()
                                        .borderTop(0xfffc933c)
                                        .borderBottom(0xffad3923)
                                        .build()
                                        : TooltipData.builder()
                                        .borderTop(0xff415db0)
                                        .borderBottom(0xff1d205d)
                                        .build())
                        .beams((player, stack) ->
                                stack.getItem() == ModItems.PLASTIC_DRINKING_HAT.value()
                                        ? BeamsData.builder()
                                        .startColor(0xFFfc933c)
                                        .endColor(0x00ad3923)
                                        .build()
                                        : BeamsData.builder()
                                        .startColor(0xFF415db0)
                                        .endColor(0x001d205d)
                                        .build())
                        .build())
                .loot(LootData.builder()
                        .entry(LootEntries.VILLAGE)
                        .build())
                .build();
    }

    @Override
    public RelicAttributeModifier getRelicAttributeModifiers(ItemStack stack) {
        if (!isAbilityUnlocked(stack, "drinking"))
            return super.getRelicAttributeModifiers(stack);

        return RelicAttributeModifier.builder()
                .attribute(new RelicAttributeModifier.Modifier(ModAttributes.DRINKING_SPEED, (float) getStatValue(stack, "drinking", "speed"), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL))
                .build();
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || newStack.getItem() == stack.getItem())
            return;

        EntityUtils.removeAttribute(player, stack, ModAttributes.DRINKING_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @EventBusSubscriber
    public static class DrinkingHatEvents {
        @SubscribeEvent
        public static void onUseItem(LivingEntityUseItemEvent.Finish event) {
            if (!(event.getEntity() instanceof Player player) || player.getCommandSenderWorld().isClientSide())
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.PLASTIC_DRINKING_HAT.value());

            if (stack.isEmpty())
                stack = EntityUtils.findEquippedCurio(player, ModItems.NOVELTY_DRINKING_HAT.value());

            if (!(stack.getItem() instanceof DrinkingHatItem relic) || event.getItem().getUseAnimation() != UseAnim.DRINK
                    || !relic.isAbilityUnlocked(stack, "drinking"))
                return;

            relic.spreadRelicExperience(player, stack, (int) Math.ceil(event.getDuration() / 20F));

            if (!relic.canPlayerUseAbility(player, stack, "nutrition"))
                return;

            int hunger = (int) relic.getStatValue(stack, "nutrition", "hunger");

            player.getFoodData().eat(hunger, hunger / 2F);
        }
    }
}