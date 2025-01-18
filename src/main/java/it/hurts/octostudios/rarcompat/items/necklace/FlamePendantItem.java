package it.hurts.octostudios.rarcompat.items.necklace;

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
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.awt.*;

public class FlamePendantItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("fire")
                                .stat(StatData.builder("time")
                                        .initialValue(2D, 3D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.3D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(StatData.builder("chance")
                                        .initialValue(0.2D, 0.3D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())
                                .research(ResearchData.builder()
                                        .star(0, 10, 18).star(1, 4, 14).star(2, 11, 13)
                                        .star(3, 16, 16).star(4, 12, 29)
                                        .link(1, 0).link(2, 0).link(3, 0).link(4, 0)
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xffe15200)
                                .borderBottom(0xff740000)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFFe15200)
                                .endColor(0x00740000)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData.abilityBuilder("fire")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.ORANGE)
                                        .build())
                                .build())
                        .build()).loot(LootData.builder()
                        .entry(LootEntries.NETHER_LIKE, LootEntries.THE_NETHER)
                        .build())
                .build();
    }

    @EventBusSubscriber
    public static class FlamePendantEvent {
        @SubscribeEvent
        public static void onReceivingDamage(LivingDamageEvent.Post event) {
            var attacker = event.getSource().getEntity();

            if (!(event.getEntity() instanceof Player player) || attacker == null || attacker.getStringUUID().equals(player.getStringUUID()))
                return;

            var level = attacker.getCommandSenderWorld();
            var stack = EntityUtils.findEquippedCurio(player, ModItems.FLAME_PENDANT.value());
            var random = player.getRandom();

            if (level.isClientSide() || !(stack.getItem() instanceof FlamePendantItem relic) || !relic.canPlayerUseAbility(player, stack, "fire")
                    || random.nextDouble() > relic.getStatValue(stack, "fire", "chance"))
                return;

            relic.spreadRelicExperience(player, stack, 1);

            attacker.setRemainingFireTicks((int) relic.getStatValue(stack, "fire", "time") * 20);

            ((ServerLevel) level).sendParticles(ParticleUtils.constructSimpleSpark(new Color(200, 150 + random.nextInt(50), random.nextInt(50)), 0.4F, 30, 0.95F),
                    attacker.getX(), attacker.getY() + attacker.getBbHeight() / 2F, attacker.getZ(), 10, attacker.getBbWidth() / 2F, attacker.getBbHeight() / 2F, attacker.getBbWidth() / 2F, 0.025F);
        }
    }
}
