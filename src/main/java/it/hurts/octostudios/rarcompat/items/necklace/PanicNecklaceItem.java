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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

public class PanicNecklaceItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("panic")
                                .stat(StatData.builder("movement")
                                        .initialValue(0.1D, 0.2D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> MathUtils.round(value * 10, 1))
                                        .build())
                                .stat(StatData.builder("radius")
                                        .initialValue(6D, 8D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .research(ResearchData.builder()
                                        .star(0, 7, 13).star(1, 15, 13).star(2, 11, 18)
                                        .link(0, 1).link(1, 2).link(2, 0)
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xffdc291b)
                                .borderBottom(0xff57000d)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFFdc291b)
                                .endColor(0x0057000d)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData.abilityBuilder("panic")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.YELLOW)
                                        .build())
                                .build())
                        .build())
                .loot(LootData.builder()
                        .entry(LootEntries.CAVE, LootEntries.MINESHAFT)
                        .build())
                .build();
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (stack.getItem() == newStack.getItem() || !(slotContext.entity() instanceof Player player))
            return;

        EntityUtils.removeAttribute(player, stack, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.getCommandSenderWorld().isClientSide() || !canPlayerUseAbility(player, stack, "panic"))
            return;

        var radius = getStatValue(stack, "panic", "radius");

        double target = player.getCommandSenderWorld().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(radius)).stream().filter(mob -> mob.getTarget() == player).count() * this.getStatValue(stack, "panic", "movement")
                + player.getCommandSenderWorld().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(radius)).size();

        double speed = getSpeed(stack);
        double step = 0.01D;
        double modifier = speed < target ? step : speed > target ? -step : 0D;

        if (modifier != 0D)
            addSpeed(stack, modifier);

        EntityUtils.resetAttribute(player, stack, Attributes.MOVEMENT_SPEED, (float) getSpeed(stack), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    public void addSpeed(ItemStack stack, double val) {
        setSpeed(stack, getSpeed(stack) + val);
    }

    public double getSpeed(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.SPEED, 0D);
    }

    public void setSpeed(ItemStack stack, double val) {
        stack.set(DataComponentRegistry.SPEED, Math.max(val, 0D));
    }

    @EventBusSubscriber
    public static class PanicNecklaceEvent {
        @SubscribeEvent
        public static void onPlayerDamage(LivingIncomingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || !(event.getSource().getEntity() instanceof Mob))
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.PANIC_NECKLACE.value());

            if (!(stack.getItem() instanceof PanicNecklaceItem relic) || !relic.isAbilityUnlocked(stack, "panic"))
                return;

            relic.spreadRelicExperience(player, stack, 1);
        }
    }
}