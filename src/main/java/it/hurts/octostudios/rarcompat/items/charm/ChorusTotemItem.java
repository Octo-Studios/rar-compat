package it.hurts.octostudios.rarcompat.items.charm;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
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
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public class ChorusTotemItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("past")
                                .stat(StatData.builder("chance")
                                        .initialValue(0.02D, 0.035D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> MathUtils.round(value * 100, 0))
                                        .build())
                                .stat(StatData.builder("radius")
                                        .initialValue(5D, 7D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> (int) MathUtils.round(value, 1))
                                        .build())
                                .research(ResearchData.builder()
                                        .star(0, 11, 19).star(1, 11, 27).star(2, 3, 12).star(3, 19, 12)
                                        .star(4, 13, 8).star(5, 9, 8)
                                        .link(0, 1).link(1, 2).link(0, 2).link(0, 3).link(1, 3).link(4, 0).link(5, 0)
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff9045a6)
                                .borderBottom(0xff258273)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFF7a09b0)
                                .endColor(0x003c0357)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData.abilityBuilder("past")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.PURPLE)
                                        .build())
                                .build())
                        .build())
                .loot(LootData.builder()
                        .entry(LootEntries.END_LIKE, LootEntries.THE_END)
                        .build())
                .build();
    }

    @EventBusSubscriber
    public static class ChorusTotemEvent {
        @SubscribeEvent
        public static void onDimensionChange(LivingDamageEvent.Post event) {
            var attacker = event.getSource().getEntity();

            if (!(event.getEntity() instanceof Player player) || player.getCommandSenderWorld().isClientSide() || attacker == null
                    || attacker.getStringUUID().equals(player.getStringUUID()))
                return;

            var level = player.getCommandSenderWorld();
            var stack = EntityUtils.findEquippedCurio(player, ModItems.CHORUS_TOTEM.value());
            var random = level.getRandom();

            if (!(stack.getItem() instanceof ChorusTotemItem relic) || !relic.canPlayerUseAbility(player, stack, "past") || player.getHealth() < 1
                    || (player.getMaxHealth() - player.getHealth()) * relic.getStatValue(stack, "past", "chance") < random.nextFloat())
                return;

            var radius = (int) relic.getStatValue(stack, "past", "radius");
            Vec3 pose = null;

            for (int i = 0; i < 50; i++) {
                int x = (int) (player.getX() + (random.nextInt(radius) * (random.nextBoolean() ? 1 : -1)));
                int y = (int) (player.getY() + (random.nextInt(radius) * (random.nextBoolean() ? 1 : -1)));
                int z = (int) (player.getZ() + (random.nextInt(radius) * (random.nextBoolean() ? 1 : -1)));

                var targetPos = new BlockPos(x, y, z);

                if (!level.getBlockState(targetPos.below()).blocksMotion() || level.isEmptyBlock(targetPos) || !level.getBlockState(targetPos).liquid())
                    continue;

                pose = new Vec3(x, y, z);
                break;
            }

            if (pose == null)
                return;

            if (attacker instanceof Player attacketPlayer)
                level.broadcastEntityEvent(attacketPlayer, (byte) 35);

            attacker.teleportTo(pose.x, pose.y, pose.z);
            attacker.setDeltaMovement(0, 0, 0);

            relic.spreadRelicExperience(player, stack, 1);
        }
    }
}
