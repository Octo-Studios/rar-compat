package it.hurts.octostudios.rarcompat.items.charm;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.init.DataComponentRegistry;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.CastData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastStage;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import top.theillusivec4.curios.api.SlotContext;

public class UniversalAttractorItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("attractor")
                                .active(CastData.builder()
                                        .type(CastType.INTERRUPTIBLE)
                                        .build())
                                .icon((player, stack, ability) -> ability + (stack.getOrDefault(DataComponentRegistry.TOGGLED, true) ? "_attract" : "_repel"))
                                .stat(StatData.builder("radius")
                                        .initialValue(3D, 5D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.18D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .research(ResearchData.builder()
                                        .star(0, 8, 12).star(1, 11, 5).star(2, 19, 8).star(3, 15, 16)
                                        .star(4, 3, 18).star(5, 11, 19).star(6, 14, 25)
                                        .star(7, 2, 24).star(8, 8, 26).star(9, 13, 30)
                                        .link(0, 1).link(1, 2).link(2, 3)
                                        .link(4, 5).link(5, 6)
                                        .link(7, 8).link(8, 9)
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xffd40000)
                                .borderBottom(0xff175dea)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFFe01010)
                                .endColor(0x006e230a)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData.abilityBuilder("attractor")
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
    public void castActiveAbility(ItemStack stack, Player player, String ability, CastType type, CastStage stage) {
        if (ability.equals("attractor"))
            stack.set(DataComponentRegistry.TOGGLED, !stack.getOrDefault(DataComponentRegistry.TOGGLED, true));
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.getCommandSenderWorld().isClientSide()
                || !canPlayerUseAbility(player, stack, "attractor"))
            return;

        double range = getStatValue(stack, "attractor", "radius");
        int amountItem = 0;

        Vec3 pos = player.position();

        for (ItemEntity item : player.getCommandSenderWorld().getEntitiesOfClass(ItemEntity.class, new AABB(pos.x - range, pos.y - range, pos.z - range,
                pos.x + range, pos.y + range, pos.z + range))) {
            if (item.position().y > pos.y)
                continue;

            if (stack.getOrDefault(DataComponentRegistry.TOGGLED, true) && item.isAlive() && !item.hasPickUpDelay()) {
                if (amountItem++ > 50)
                    break;

                Vec3 motion = pos.subtract(item.position().add(0, item.getBbHeight() / 2, 0));

                if (Math.sqrt(motion.x * motion.x + motion.y * motion.y + motion.z * motion.z) > 1)
                    motion = motion.normalize();

                item.setDeltaMovement(motion.scale(0.6));
            } else {
                if (item.isAlive() && !item.hasPickUpDelay()) {
                    if (amountItem++ > 50)
                        break;

                    Vec3 motion = item.position().add(0, item.getBbHeight() / 2, 0).subtract(pos);

                    if (Math.sqrt(motion.x * motion.x + motion.y * motion.y + motion.z * motion.z) > 1)
                        motion = motion.normalize();

                    item.setDeltaMovement(motion.scale(0.6));
                }
            }
        }
    }

    @EventBusSubscriber
    public static class UniversalAttractorEvent {
        @SubscribeEvent
        public static void onItemPickedUp(ItemEntityPickupEvent.Post event) {
            Player player = event.getPlayer();

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.UNIVERSAL_ATTRACTOR.value());

            if (!(stack.getItem() instanceof UniversalAttractorItem relic) || !stack.getOrDefault(DataComponentRegistry.TOGGLED, true)
                    || player.getCommandSenderWorld().isClientSide())
                return;

            ItemEntity item = event.getItemEntity();

            if (item.getRandom().nextFloat() >= player.getRandom().nextFloat() && item.getOwner() != player)
                relic.spreadRelicExperience(player, stack, 1);
        }
    }
}
