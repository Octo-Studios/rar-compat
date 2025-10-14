package it.hurts.octostudios.rarcompat.items.charm;

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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class AntidoteVesselItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("antidote")
                                .stat(StatData.builder("amount")
                                        .initialValue(0.2D, 0.4D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())
                                .build())
                        .ability(AbilityData.builder("devourer")
                                .requiredLevel(5)
                                .stat(StatData.builder("duration")
                                        .initialValue(1D, 1.5D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.4D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xffe19d25)
                                .borderBottom(0xff7c4023)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(15)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.JUNGLE)
                        .build())
                .build();
    }

    @Mod.EventBusSubscriber
    public static class AntidoteVeselEvents {
        @SubscribeEvent
        public static void onIncomingDamage(AttackEntityEvent event) {
            if (!(event.getTarget() instanceof LivingEntity target) || target.getCommandSenderWorld().isClientSide()
                    || target.getActiveEffects().isEmpty())
                return;

            var player = event.getEntity();
            var stack = EntityUtils.findEquippedCurio(player, ModItems.ANTIDOTE_VESSEL.get());

            if (!(stack.getItem() instanceof AntidoteVesselItem relic) || !relic.canPlayerUseActiveAbility(player, stack, "devourer")
                    || player.getAttackStrengthScale(0) <= 0.9F)
                return;

            for (var activeEffect : target.getActiveEffects().stream().filter(effect -> effect.getEffect().isBeneficial()).toList()) {
                var transferDuration = (int) (activeEffect.getDuration() - (relic.getAbilityValue(stack, "devourer", "duration") * 20));

                target.removeEffect(activeEffect.getEffect());
                target.addEffect(new MobEffectInstance(activeEffect.getEffect(), transferDuration, activeEffect.getAmplifier()));

                MobEffectInstance existingEffect = player.getEffect(activeEffect.getEffect());

                var amplifier = existingEffect == null ? 1 : activeEffect.getAmplifier();
                var transferDuration1 = (int) relic.getAbilityValue(stack, "devourer", "duration") * 20;

                if (existingEffect != null) {
                    transferDuration1 += (existingEffect.getDuration());
                    player.removeEffect(activeEffect.getEffect());
                }

                player.addEffect(new MobEffectInstance(activeEffect.getEffect(), transferDuration1, amplifier));

                relic.spreadExperience(player, stack, 1);
            }
        }

        @SubscribeEvent
        public static void onAddedEffect(MobEffectEvent.Added event) {
            var effectDuration = event.getEffectInstance();

            if (!(event.getEntity() instanceof Player player) || player.getCommandSenderWorld().isClientSide()
                    || effectDuration.getEffect().isBeneficial())
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.ANTIDOTE_VESSEL.get());

            if (!(stack.getItem() instanceof AntidoteVesselItem relic) || !relic.canPlayerUseActiveAbility(player, stack, "antidote"))
                return;

            if (effectDuration.getEffect() == MobEffects.BAD_OMEN)
                return;

            effectDuration.duration = (int) (effectDuration.getDuration() * (1 - relic.getAbilityValue(stack, "antidote", "amount")));

            relic.spreadExperience(player, stack, 1);
        }
    }
}
