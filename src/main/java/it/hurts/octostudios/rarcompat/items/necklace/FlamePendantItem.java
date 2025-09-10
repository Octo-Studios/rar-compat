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
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.TooltipData;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xffe15200)
                                .borderBottom(0xff740000)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .build()).loot(LootData.builder()
                        .entry(LootCollections.NETHER)
                        .build())
                .build();
    }

    @Mod.EventBusSubscriber
    public static class FlamePendantEvent {
        @SubscribeEvent
        public static void onReceivingDamage(LivingDamageEvent event) {
            var attacker = event.getSource().getEntity();

            if (!(event.getEntity() instanceof Player player) || attacker == null || attacker.getStringUUID().equals(player.getStringUUID()))
                return;

            var level = attacker.getCommandSenderWorld();
            var stack = EntityUtils.findEquippedCurio(player, ModItems.FLAME_PENDANT.get());
            var random = player.getRandom();

            if (level.isClientSide() || !(stack.getItem() instanceof FlamePendantItem relic) || !relic.canPlayerUseActiveAbility(player, stack, "fire")
                    || random.nextDouble() > relic.getAbilityValue(stack, "fire", "chance"))
                return;

            relic.spreadExperience(player, stack, 1);

            attacker.setRemainingFireTicks((int) relic.getAbilityValue(stack, "fire", "time") * 20);

            ((ServerLevel) level).sendParticles(ParticleUtils.constructSimpleSpark(new Color(200, 150 + random.nextInt(50), random.nextInt(50)), 0.4F, 30, 0.95F),
                    attacker.getX(), attacker.getY() + attacker.getBbHeight() / 2F, attacker.getZ(), 10, attacker.getBbWidth() / 2F, attacker.getBbHeight() / 2F, attacker.getBbWidth() / 2F, 0.025F);
        }
    }
}
