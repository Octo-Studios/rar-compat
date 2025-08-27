package it.hurts.octostudios.rarcompat.items.hat;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
                                .build())
                        .ability(AbilityData.builder("nutrition")
                                .requiredLevel(5)
                                .stat(StatData.builder("hunger")
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.15D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(15)
                        .step(100)
                        .build())
                .style(StyleData.builder()
                        .tooltip((player, stack) ->
                                stack.getItem() == ModItems.PLASTIC_DRINKING_HAT.get()
                                        ? TooltipData.builder()
                                        .borderTop(0xfffc933c)
                                        .borderBottom(0xffad3923)
                                        .build()
                                        : TooltipData.builder()
                                        .borderTop(0xff415db0)
                                        .borderBottom(0xff1d205d)
                                        .build())
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.VILLAGE)
                        .build())
                .build();
    }

    @Mod.EventBusSubscriber
    public static class DrinkingHatEvents {
        @SubscribeEvent
        public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
            if (!(event.getEntity() instanceof Player player) || player.getCommandSenderWorld().isClientSide())
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.PLASTIC_DRINKING_HAT.get());

            if (stack.isEmpty())
                stack = EntityUtils.findEquippedCurio(player, ModItems.NOVELTY_DRINKING_HAT.get());

            if (!(stack.getItem() instanceof DrinkingHatItem relic) || event.getItem().getUseAnimation() != UseAnim.DRINK)
                return;

            event.setDuration((int) (event.getDuration() * (1 - relic.getAbilityValue(stack, "drinking", "speed"))));
        }

        @SubscribeEvent
        public static void onUseItem(LivingEntityUseItemEvent.Finish event) {
            if (!(event.getEntity() instanceof Player player) || player.getCommandSenderWorld().isClientSide())
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.PLASTIC_DRINKING_HAT.get());

            if (stack.isEmpty())
                stack = EntityUtils.findEquippedCurio(player, ModItems.NOVELTY_DRINKING_HAT.get());

            if (!(stack.getItem() instanceof DrinkingHatItem relic) || event.getItem().getUseAnimation() != UseAnim.DRINK
                    || !relic.canUseAbility(stack, "drinking"))
                return;

            relic.spreadExperience(player, stack, (int) Math.ceil(event.getDuration() / 20F));

            if (!relic.canPlayerUseActiveAbility(player, stack, "nutrition"))
                return;

            int hunger = (int) relic.getAbilityValue(stack, "nutrition", "hunger");

            player.getFoodData().eat(hunger, hunger / 2F);
        }
    }
}