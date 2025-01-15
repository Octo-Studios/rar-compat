package it.hurts.octostudios.rarcompat.items.hands;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.init.DataComponentRegistry;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.CastData;
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
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import top.theillusivec4.curios.api.SlotContext;

import java.awt.*;

public class PickaxeHeaterItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("heater")
                                .active(CastData.builder()
                                        .type(CastType.TOGGLEABLE)
                                        .build())
                                .stat(StatData.builder("capacity")
                                        .initialValue(7D, 10D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.2D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .research(ResearchData.builder()
                                        .star(0, 11, 28).star(1, 11, 18).star(2, 19, 24)
                                        .star(3, 11, 6).star(4, 4, 9).star(5, 18, 9)
                                        .link(0, 1).link(1, 2).link(3, 1).link(3, 4).link(3, 5)
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff939ca2)
                                .borderBottom(0xff696b7c)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFF939ca2)
                                .endColor(0x00696b7c)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData.abilityBuilder("heater")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.ORANGE)
                                        .build())
                                .build())
                        .build())
                .loot(LootData.builder()
                        .entry(LootEntries.WILDCARD, LootEntries.CAVE, LootEntries.MINESHAFT, LootEntries.VILLAGE)
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.tickCount % 100 != 0
                || getCharges(stack) >= Math.round(getStatValue(stack, "heater", "capacity")))
            return;

        addCharges(stack, 1);
    }

    public static void addCharges(ItemStack stack, int amount) {
        stack.set(DataComponentRegistry.CHARGE, getCharges(stack) + amount);
    }

    public static int getCharges(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.CHARGE, 0);
    }

    @EventBusSubscriber
    public static class PickaxeHeaterEvent {
        @SubscribeEvent
        public static void onPlayerAttack(BlockDropsEvent event) {
            if (!(event.getBreaker() instanceof Player player))
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.PICKAXE_HEATER.value());
            var level = player.getCommandSenderWorld();

            if (level.isClientSide() || !(stack.getItem() instanceof PickaxeHeaterItem relic)
                    || !relic.isAbilityTicking(stack, "heater") || getCharges(stack) <= 1)
                return;

            var serverLevel = (ServerLevel) level;

            var smelted = false;

            for (ItemEntity drop : event.getDrops()) {
                var source = drop.getItem();
                var result = getSmeltingResult(source, serverLevel);

                if (!smelted && source != result)
                    smelted = true;

                drop.setItem(result);
            }

            if (smelted) {
                spawnBurstParticles(serverLevel, event.getPos());

                addCharges(stack, -1);

                relic.spreadRelicExperience(player, stack, 1);
            }
        }

        public static void spawnBurstParticles(ServerLevel level, BlockPos centerPos) {
            var center = centerPos.getCenter();
            var random = level.getRandom();

            level.sendParticles(ParticleUtils.constructSimpleSpark(new Color(150 + random.nextInt(106), random.nextInt(50), 50 + random.nextInt(51), 255),
                    0.6F, 20, 0.85F), center.x(), center.y(), center.z(), 25, 0.3, 0.3, 0.3, 0.01);
        }
    }

    public static ItemStack getSmeltingResult(ItemStack stack, ServerLevel level) {
        var entry = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level);

        if (entry.isPresent()) {
            var result = entry.get().value().getResultItem(level.registryAccess());

            if (!result.isEmpty())
                return result.copyWithCount(stack.getCount() * result.getCount());
        }

        return stack;
    }
}