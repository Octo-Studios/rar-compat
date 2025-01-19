package it.hurts.octostudios.rarcompat.items.necklace;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.init.DataComponentRegistry;
import it.hurts.sskirillss.relics.init.EffectRegistry;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import top.theillusivec4.curios.api.SlotContext;

public class ScarfOfInvisibilityItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("invisible")
                                .stat(StatData.builder("time")
                                        .initialValue(140D, 100D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, -0.05D)
                                        .formatValue(value -> MathUtils.round(value / 20, 1))
                                        .build())
                                .research(ResearchData.builder()
                                        .star(0, 11, 27).star(1, 11, 12).star(2, 11, 8)
                                        .star(3, 17, 12).star(4, 6, 12).star(5, 6, 17)
                                        .link(0, 1).link(1, 2).link(1, 3).link(1, 4).link(4, 5)
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff51a4df)
                                .borderBottom(0xff2c3b70)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFF51a4df)
                                .endColor(0x002c3b70)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData.abilityBuilder("invisible")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.BLUE)
                                        .build())
                                .build())
                        .build())
                .loot(LootData.builder()
                        .entry(LootEntries.END_LIKE, LootEntries.THE_END)
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.getCommandSenderWorld().isClientSide()
                || !isAbilityUnlocked(stack, "invisible"))
            return;

        var time = getTime(stack);

        if (time == 0 && player.getCommandSenderWorld().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(15)).stream().anyMatch(mob -> mob.getTarget() == player)) {
            if (!player.hasEffect(EffectRegistry.VANISHING))
                spreadRelicExperience(player, stack, 1);

            player.addEffect(new MobEffectInstance(EffectRegistry.VANISHING, 4, 0, true, false));
        } else if (time > 0)
            addTime(stack, -1);
    }

    public void addTime(ItemStack stack, int time) {
        setTime(stack, getTime(stack) + time);
    }

    public void setTime(ItemStack stack, int time) {
        stack.set(DataComponentRegistry.TIME, Math.max(time, 0));
    }

    public int getTime(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.TIME, 0);
    }

    @EventBusSubscriber
    public static class ScarfOfInvisibilityEvent {
        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            onInteract(event.getEntity());
        }

        @SubscribeEvent
        public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
            onInteract(event.getEntity());
        }

        @SubscribeEvent
        public static void onHarvestCheck(PlayerEvent.BreakSpeed event) {
            onInteract(event.getEntity());
        }

        @SubscribeEvent
        public static void onAttacking(AttackEntityEvent event) {
            onInteract(event.getEntity());
        }

        private static void onInteract(Player player) {
            if (player.getCommandSenderWorld().isClientSide())
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.SCARF_OF_INVISIBILITY.value());

            if (!(stack.getItem() instanceof ScarfOfInvisibilityItem relic))
                return;

            relic.setTime(stack, (int) relic.getStatValue(stack, "invisible", "time"));
        }
    }
}