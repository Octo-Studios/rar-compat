package it.hurts.octostudios.rarcompat.items.necklace;

import artifacts.registry.ModItems;
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
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import top.theillusivec4.curios.api.SlotContext;

public class CharmOfSinkingItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("dive")
                                .maxLevel(0)
                                .build())
                        .ability(AbilityData.builder("dipping")
                                .stat(StatData.builder("air")
                                        .initialValue(0.2D, 0.4D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.2)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .research(ResearchData.builder()
                                        .star(0, 12, 9).star(1, 9, 14).star(2, 16, 14)
                                        .star(3, 9, 19).star(4, 4, 24).star(5, 12, 26)
                                        .link(0, 1).link(1, 2).link(1, 3).link(1, 3).link(3, 4).link(3, 5)
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff3c7090)
                                .borderBottom(0xff1c212d)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFF3c7090)
                                .endColor(0x001c212d)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData.abilityBuilder("dipping")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.CYAN)
                                        .build())
                                .build())
                        .build())
                .loot(LootData.builder()
                        .entry(LootEntries.WILDCARD, LootEntries.AQUATIC)
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || !isAbilityUnlocked(stack, "dive"))
            return;

        if (player.isUnderWater()) {
            EntityUtils.applyAttribute(player, stack, Attributes.GRAVITY, 2F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

            if (!isAbilityUnlocked(stack, "dipping"))
                return;

            if (!player.onGround())
                setToggled(stack, true);

            if (player.tickCount % 20 == 0 && player.onGround() && player.getMaxAirSupply() > player.getAirSupply()) {
                player.setAirSupply((int) (player.getAirSupply() + getStatValue(stack, "dipping", "air") * 20));

                if (getToggled(stack)) {
                    setToggled(stack, false);

                    spreadRelicExperience(player, stack, 1);
                }
            }
        } else {
            EntityUtils.removeAttribute(player, stack, Attributes.GRAVITY, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (newStack.getItem() == stack.getItem() || !(slotContext.entity() instanceof Player player))
            return;

        EntityUtils.removeAttribute(player, stack, Attributes.GRAVITY, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    public void setToggled(ItemStack stack, boolean val) {
        stack.set(DataComponentRegistry.TOGGLED, val);
    }

    public boolean getToggled(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.TOGGLED, true);
    }

    @EventBusSubscriber
    public static class CharmOfSinkingEvent {
        @SubscribeEvent
        public static void onPlayerBreathe(LivingBreatheEvent event) {
            if (!(event.getEntity() instanceof Player player))
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.CHARM_OF_SINKING.value());

            if (!(stack.getItem() instanceof CharmOfSinkingItem relic) || !player.isUnderWater()
                    || !relic.isAbilityUnlocked(stack, "dipping") || !player.onGround() || player.getAirSupply() == player.getMaxAirSupply())
                return;

            event.setConsumeAirAmount(0);
        }
    }
}
