package it.hurts.octostudios.rarcompat.items.necklace;

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
import it.hurts.sskirillss.relics.items.relics.base.data.misc.StatIcon;
import it.hurts.sskirillss.relics.items.relics.base.data.misc.StatIcons;
import it.hurts.sskirillss.relics.items.relics.base.data.research.ResearchData;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

public class CrossNecklaceItem extends WearableRelicItem {

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("invulnerability")
                                .stat(StatData.builder("modifier")
                                        .icon(StatIcons.MODIFIER)
                                        .initialValue(0.1D, 0.25D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.25D)
                                        .formatValue(value -> MathUtils.round(value, 2))
                                        .build())
                                .research(ResearchData.builder()
                                        .star(0, 4, 6).star(1, 11, 6).star(2, 4, 20).star(3, 10, 19)
                                        .star(4, 20, 15).star(5, 20, 21).star(6, 11, 25).star(7, 20, 6)
                                        .link(0, 2).link(2, 6).link(6, 5).link(5, 4).link(7, 4)
                                        .link(4, 1).link(1, 3)
                                        .build())
                                .build())
                        .build())
                .leveling(new LevelingData(100, 10, 100))
                .loot(LootData.builder()
                        .entry(LootCollections.DESERT)
                        .build())
                .build();
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (newStack == stack || !(slotContext.entity() instanceof Player player)) return;

        player.invulnerableTime -= (int) getStatValue(stack, "invulnerability", "modifier");
    }

    @EventBusSubscriber
    public static class Events {

        @SubscribeEvent
        public static void onLivingDamaged(LivingDamageEvent.Post event) {
            if (!(event.getEntity() instanceof Player player)) return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.CROSS_NECKLACE.value());

            if (!(stack.getItem() instanceof CrossNecklaceItem relic)) return;

            relic.spreadRelicExperience(player, stack, 1);

            player.invulnerableTime += (int) relic.getStatValue(stack, "invulnerability", "modifier");
        }
    }
}
