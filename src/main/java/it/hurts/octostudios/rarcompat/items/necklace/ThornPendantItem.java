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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;

public class ThornPendantItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("poison")
                                .stat(StatData.builder("multiplier")
                                        .initialValue(0.05D, 0.15D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> MathUtils.round(value * 100, 1))
                                        .build())
                                .stat(StatData.builder("time")
                                        .initialValue(2D, 4D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.3D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(StatData.builder("chance")
                                        .initialValue(0.1D, 0.2D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.15D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 0))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff00ac2b)
                                .borderBottom(0xff004629)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.JUNGLE)
                        .build())
                .build();
    }

    @Mod.EventBusSubscriber
    public static class ThornPendantEvent {
        @SubscribeEvent
        public static void onReceivingDamage(LivingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || !(event.getSource().getEntity() instanceof LivingEntity attacker)
                    || attacker.getStringUUID().equals(player.getStringUUID()))
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.THORN_PENDANT.get());
            var random = player.getRandom();
            var level = player.getCommandSenderWorld();

            if (level.isClientSide() || !(stack.getItem() instanceof ThornPendantItem relic) || !relic.canPlayerUseActiveAbility(player, stack, "poison")
                    || random.nextDouble() > relic.getAbilityValue(stack, "poison", "chance"))
                return;

            relic.spreadExperience(player, stack, 1);

            attacker.invulnerableTime = 0;
            attacker.hurt(level.damageSources().thorns(player), (float) (event.getAmount() * relic.getAbilityValue(stack, "poison", "multiplier")));
            attacker.addEffect(new MobEffectInstance(MobEffects.POISON, (int) (relic.getAbilityValue(stack, "poison", "time") * 20), 1));

            ((ServerLevel) level).sendParticles(ParticleUtils.constructSimpleSpark(new Color(50 + random.nextInt(50), 200 + random.nextInt(55), 50 + random.nextInt(50)), 0.4F, 30, 0.95F),
                    attacker.getX(), attacker.getY() + attacker.getBbHeight() / 2F, attacker.getZ(), 10, attacker.getBbWidth() / 2F, attacker.getBbHeight() / 2F, attacker.getBbWidth() / 2F, 0.025F);
        }
    }
}
