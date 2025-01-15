package it.hurts.octostudios.rarcompat.items.hands;

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
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

public class FireGauntletItem extends WearableRelicItem {

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("arson")
                                .stat(StatData.builder("sector")
                                        .initialValue(0.7D, 0.9D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.35D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(StatData.builder("time")
                                        .initialValue(2D, 3D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.15D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .research(ResearchData.builder()
                                        .star(0, 13, 29).star(1, 11, 22).star(2, 5, 22).star(3, 6, 17)
                                        .star(4, 9, 16).star(5, 13, 17).star(6, 16, 19)
                                        .link(0, 1).link(1, 2).link(1, 3).link(1, 4).link(1, 5).link(1, 6)
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xfffcbc11)
                                .borderBottom(0xffd12e00)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFFfcbc11)
                                .endColor(0x00d12e00)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData.abilityBuilder("arson")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.ORANGE)
                                        .build())
                                .build())
                        .build())
                .loot(LootData.builder()
                        .entry(LootEntries.WILDCARD, LootEntries.NETHER_LIKE, LootEntries.THE_NETHER)
                        .build())
                .build();
    }

    @EventBusSubscriber
    public static class FireGauntletEvent {

        @SubscribeEvent
        public static void onAttack(AttackEntityEvent event) {
            Player player = event.getEntity();

            if (!(event.getTarget() instanceof LivingEntity) || player.getCommandSenderWorld().isClientSide())
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.FIRE_GAUNTLET.value());

            if (!(stack.getItem() instanceof FireGauntletItem relic) || !relic.canPlayerUseAbility(player, stack, "arson"))
                return;

            double attackRange = Objects.requireNonNull(player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE)).getValue();
            double sector = relic.getStatValue(stack, "arson", "sector") * 10;
            int flameTime = (int) relic.getStatValue(stack, "arson", "time");

            for (LivingEntity entity : findMobsInCone(player, attackRange, sector * 10)) {
                entity.setRemainingFireTicks(flameTime * 20);

                relic.spreadRelicExperience(player, stack, 1);
            }

            spawnDirectionalArc(player, sector, attackRange);
        }

        private static void spawnDirectionalArc(LivingEntity player, double arcAngle, double rangeAttack) {
            double centerAngle = Math.toRadians(player.getYRot()) + 20.4;

            double startAngle = centerAngle - Math.toRadians(arcAngle);
            double endAngle = centerAngle + Math.toRadians(arcAngle);

            Random random = new Random();

            for (double d = 0; d <= rangeAttack; d += 0.4) {
                int particleCount = (int) (arcAngle / 4 + d);

                for (int i = 0; i <= particleCount; i++) {
                    double angleStep = (endAngle - startAngle) / particleCount;

                    double angle = startAngle + angleStep * i;
                    double x = player.getX() + d * Math.cos(angle);
                    double z = player.getZ() + d * Math.sin(angle);


                    ((ServerLevel) player.level()).sendParticles(ParticleUtils.constructSimpleSpark(
                                    new Color(200 + random.nextInt(56), random.nextInt(100), random.nextInt(20)),
                                    0.8F, 20, 0.9F),
                            x, player.getY() + 1, z,
                            0, 0, 0, 0, 0);
                }
            }
        }

        public static List<LivingEntity> findMobsInCone(Player player, double attackRange, double angle) {
            double playerY = player.getY();

            return player.level().getEntities(player, new AABB(player.blockPosition()).inflate(attackRange), entity -> entity instanceof LivingEntity).stream()
                    .map(entity -> (LivingEntity) entity)
                    .filter(livingEntity -> {
                        double entityY = livingEntity.getY();
                        if (entityY < playerY - 2 || entityY > playerY + 2) return false;

                        return player.getLookAngle().normalize()
                                .dot(livingEntity.position().subtract(player.position()).normalize()) > Math.cos(Math.toRadians(angle) * 0.2);
                    })
                    .collect(Collectors.toList());
        }
    }
}
