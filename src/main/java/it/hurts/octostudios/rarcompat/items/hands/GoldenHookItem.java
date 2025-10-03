package it.hurts.octostudios.rarcompat.items.hands;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
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
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.List;

public class GoldenHookItem extends WearableRelicItem {

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("hook")
                                .stat(StatData.builder("amount")
                                        .initialValue(0.2D, 0.4D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.15D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xfffced59)
                                .borderBottom(0xffe6af15)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.BASTION)
                        .build())
                .build();
    }

    // From EntityUtils.findEquippedCurios() (Relics 1.21.1 by SSKirilSS)
    // This method doesn't exist in 1.20.1 relics version
    private static List<ItemStack> findEquippedCurios(Entity entity, Item item) {
        if (!(entity instanceof Player player))
            return List.of();

        return CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.findCurios(item).stream()
                        .map(SlotResult::stack)
                        .toList())
                .orElse(List.of());
    }

    @Mod.EventBusSubscriber
    public static class GoldenHookEvent {

        @SubscribeEvent
        public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
            var player = event.getAttackingPlayer();
            var stacks = findEquippedCurios(player, ModItems.GOLDEN_HOOK.get());

            if (stacks.isEmpty())
                return;

            var percentage = stacks.stream().mapToDouble(stack -> {
                if (!(stack.getItem() instanceof GoldenHookItem relic) || !relic.canUseAbility(stack, "hook"))
                    return 0d;

                relic.spreadExperience(player, stack, 1);

                return relic.getAbilityValue(stack, "hook", "amount");
            }).sum();

            var droppedExp = event.getDroppedExperience();

            event.setDroppedExperience((int) (droppedExp + (droppedExp * percentage)));
        }
    }
}
