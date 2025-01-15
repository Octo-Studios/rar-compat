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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

import java.awt.*;

public class WitheredBraceletItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("withered")
                                .stat(StatData.builder("chance")
                                        .initialValue(0.2D, 0.4D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())
                                .stat(StatData.builder("time")
                                        .initialValue(2D, 4D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.2D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .research(ResearchData.builder()
                                        .star(0, 11, 27).star(1, 11, 18).star(2, 6, 22).star(3, 16, 21)
                                        .star(4, 11, 14).star(5, 11, 9).star(6, 6, 12).star(7, 7, 7)
                                        .star(8, 16, 9).star(9, 13, 6)
                                        .link(0, 1).link(1, 2).link(1, 3).link(1, 4).link(4, 5).link(5, 6).link(5, 7).link(5, 8).link(5, 9)
                                        .link(7, 9).link(9, 8).link(4, 8).link(4, 6).link(6, 7)
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff52453d)
                                .borderBottom(0xff2e261f)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFF52453d)
                                .endColor(0x002e261f)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData.abilityBuilder("withered")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.YELLOW)
                                        .build())
                                .build())
                        .build())
                .loot(LootData.builder()
                        .entry(LootEntries.WILDCARD, LootEntries.NETHER_LIKE, LootEntries.THE_NETHER)
                        .build())
                .build();
    }

    @EventBusSubscriber
    public static class WitheredBraceletEvent {
        @SubscribeEvent
        public static void onReceivingDamage(AttackEntityEvent event) {
            var player = event.getEntity();
            var level = player.getCommandSenderWorld();

            if (!(event.getTarget() instanceof LivingEntity attacker) || level.isClientSide() || attacker == player)
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.WITHERED_BRACELET.value());
            var random = player.getRandom();

            if (!(stack.getItem() instanceof WitheredBraceletItem relic) || random.nextDouble() > relic.getStatValue(stack, "withered", "chance")
                    || !relic.isAbilityUnlocked(stack, "withered"))
                return;

            attacker.addEffect(new MobEffectInstance(MobEffects.WITHER, (int) relic.getStatValue(stack, "withered", "time") * 20, 1));

            relic.spreadRelicExperience(player, stack, 1);

            ((ServerLevel) level).sendParticles(ParticleUtils.constructSimpleSpark(new Color(75, 0, 130), 0.9F, 60, 0.95F),
                    attacker.getX(), attacker.getY() + attacker.getBbHeight() / 2F, attacker.getZ(), 10, attacker.getBbWidth() / 2F, attacker.getBbHeight() / 2F, attacker.getBbWidth() / 2F, 0.025F);
        }
    }
}
