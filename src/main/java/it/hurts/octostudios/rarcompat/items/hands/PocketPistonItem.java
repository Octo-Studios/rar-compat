package it.hurts.octostudios.rarcompat.items.hands;

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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import top.theillusivec4.curios.api.SlotContext;

public class PocketPistonItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("discarding")
                                .stat(StatData.builder("interaction")
                                        .initialValue(0.2D, 0.4D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.15D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())
                                .research(ResearchData.builder()
                                        .star(0, 11, 27).star(1, 11, 23).star(2, 4, 19).star(3, 4, 23)
                                        .star(4, 18, 19).star(5, 18, 23).star(6, 11, 15).star(7, 11, 6)
                                        .star(8, 4, 10).star(9, 18, 10)
                                        .link(0, 1).link(1, 2).link(1, 4).link(3, 0).link(0, 5).link(5, 4).link(2, 3).link(1, 6).link(2, 6)
                                        .link(4, 6).link(3, 0).link(0, 5).link(5, 4).link(2, 3).link(6, 8).link(6, 9).link(7, 8).link(7, 9)
                                        .build())
                                .build())
                        .ability(AbilityData.builder("interaction")
                                .requiredLevel(5)
                                .stat(StatData.builder("range")
                                        .initialValue(0.2D, 0.4D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.15D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())
                                .research(ResearchData.builder()
                                        .star(0, 11, 27).star(1, 11, 23).star(2, 4, 19).star(3, 4, 23)
                                        .star(4, 18, 19).star(5, 18, 23).star(6, 11, 15).star(7, 11, 6)
                                        .star(8, 4, 10).star(9, 18, 10)
                                        .link(0, 1).link(1, 2).link(1, 4).link(3, 0).link(0, 5).link(5, 4).link(2, 3).link(1, 6).link(2, 6)
                                        .link(4, 6).link(3, 0).link(0, 5).link(5, 4).link(2, 3).link(6, 8).link(6, 9).link(7, 8).link(7, 9)
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xffcb9848)
                                .borderBottom(0xff6a6a6a)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFFcb9848)
                                .endColor(0x006a6a6a)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(15)
                        .step(100)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData.abilityBuilder("discarding")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.ORANGE)
                                        .build())
                                .source(LevelingSourceData.abilityBuilder("interaction")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.RED)
                                        .build())
                                .build())
                        .build())
                .loot(LootData.builder()
                        .entry(LootEntries.WILDCARD, LootEntries.VILLAGE)
                        .build())
                .build();
    }

    @Override
    public RelicAttributeModifier getRelicAttributeModifiers(ItemStack stack) {
        float modifier = (float) getStatValue(stack, "interaction", "range");

        if (!isAbilityUnlocked(stack, "interaction"))
            return super.getRelicAttributeModifiers(stack);

        return RelicAttributeModifier.builder()
                .attribute(new RelicAttributeModifier.Modifier(Attributes.ENTITY_INTERACTION_RANGE, modifier, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL))
                .attribute(new RelicAttributeModifier.Modifier(Attributes.BLOCK_INTERACTION_RANGE, modifier, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL))
                .build();
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || newStack.getItem() == stack.getItem())
            return;

        EntityUtils.removeAttribute(player, stack, Attributes.ENTITY_INTERACTION_RANGE, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        EntityUtils.removeAttribute(player, stack, Attributes.BLOCK_INTERACTION_RANGE, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @EventBusSubscriber
    public static class PocketPistonEvent {

        @SubscribeEvent
        public static void onContacted(BlockEvent.EntityPlaceEvent event) {
            if (!(event.getEntity() instanceof Player player))
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.POCKET_PISTON.value());

            if (!(stack.getItem() instanceof PocketPistonItem relic) || !relic.canPlayerUseAbility(player, stack, "discarding"))
                return;

            int distance = (int) Math.sqrt(event.getPos().distToCenterSqr(player.getX(), player.getY() + player.getEyeHeight(), player.getZ()));

            if (distance >= 5)
                relic.spreadRelicExperience(player, stack, 1);
        }

        @SubscribeEvent
        public static void onContacted(LivingDestroyBlockEvent event) {
            if (!(event.getEntity() instanceof Player player))
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.POCKET_PISTON.value());

            if (!(stack.getItem() instanceof PocketPistonItem relic) || !relic.canPlayerUseAbility(player, stack, "discarding"))
                return;

            int distance = (int) Math.sqrt(event.getPos().distToCenterSqr(player.getX(), player.getY() + player.getEyeHeight(), player.getZ()));

            if (distance >= 5)
                relic.spreadRelicExperience(player, stack, 1);
        }

        @SubscribeEvent
        public static void onAttacking(AttackEntityEvent event) {
            Player player = event.getEntity();
            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.POCKET_PISTON.value());

            if (!(event.getTarget() instanceof LivingEntity target) || !(stack.getItem() instanceof PocketPistonItem relic)
                    || !relic.canPlayerUseAbility(player, stack, "discarding"))
                return;

            float modifier = (float) relic.getStatValue(stack, "discarding", "interaction");

            relic.spreadRelicExperience(player, stack, 1);

            if (player.distanceTo(target) > 3)
                relic.spreadRelicExperience(player, stack, 1);

            Vec3 toEntity = target.position().subtract(player.position()).normalize().scale(modifier);

            target.setDeltaMovement(toEntity.x, toEntity.y / 2, toEntity.z);
        }
    }
}
