package it.hurts.octostudios.rarcompat.items.hands;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.entities.SparkEntity;
import it.hurts.octostudios.rarcompat.init.EntityRegistry;
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
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.network.packets.sync.S2CEntityTargetPacket;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

import java.util.Comparator;
import java.util.List;

public class FireGauntletItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("caster")
                                .stat(StatData.builder("chance")
                                        .initialValue(0.2D, 0.3D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.15D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())
                                .stat(StatData.builder("damage")
                                        .initialValue(0.05D, 0.15D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.235D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())
                                .stat(StatData.builder("duration")
                                        .initialValue(30D, 50D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.3D)
                                        .formatValue(value -> MathUtils.round(value / 20, 1))
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
                                .source(LevelingSourceData.abilityBuilder("caster")
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
            var player = event.getEntity();
            var level = player.getCommandSenderWorld();

            if (level.isClientSide() || !(event.getTarget() instanceof LivingEntity))
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.FIRE_GAUNTLET.value());

            if (!(stack.getItem() instanceof FireGauntletItem relic) || !relic.canPlayerUseAbility(player, stack, "caster"))
                return;

            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).getValue()),
                    entity -> player.hasLineOfSight(entity) && !entity.getUUID().equals(player.getUUID()) && !entity.isInvisible());
            targets.sort(Comparator.comparingDouble(targetEntity -> targetEntity.distanceTo(player)));

            var targetIndex = 0;
            var random = player.getRandom();
            var sparksCount = Math.min(MathUtils.multicast(random, relic.getStatValue(stack, "caster", "chance")), targets.size());

            for (int i = 0; i < sparksCount; i++) {
                var spark = new SparkEntity(EntityRegistry.SPARK.value(), level);

                spark.setOwner(player);
                spark.setRelicStack(stack);
                spark.setTarget(targets.get(targetIndex));
                spark.setDamage((float) (player.getAttributes().getValue(Attributes.ATTACK_DAMAGE) * relic.getStatValue(stack, "caster", "damage")));

                double angle = random.nextDouble() * 2 * Math.PI;

                spark.setPos(player.position().add(Math.cos(angle), 1, Math.sin(angle)));

                level.addFreshEntity(spark);

                NetworkHandler.sendToClientsTrackingEntityAndSelf(new S2CEntityTargetPacket(player.getId(), spark.getId()), spark);
                targetIndex = (targetIndex + 1) % targets.size();
            }
        }
    }
}
