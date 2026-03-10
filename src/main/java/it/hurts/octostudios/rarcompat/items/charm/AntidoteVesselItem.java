package it.hurts.octostudios.rarcompat.items.charm;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.init.DataComponentRegistry;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AntidoteVesselItem extends WearableRelicItem {

    private static final ThreadLocal<Boolean> INTERNAL_EFFECT_APPLICATION = ThreadLocal.withInitial(() -> false);

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("antidote")
                                .rankModifier(1, "resilience")
                                .rankModifier(3, "recovery")
                                .rankModifier(5, "limiter")
                                .stat(AbilityStatTemplate.builder("duration_reduction")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.15D, 0.4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("resistance_per_effect")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.03D, 0.08D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("immunity_duration")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(2D, 6D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide())
            return;

        var gameTime = player.level().getGameTime();

        if (player.tickCount % 20 == 0)
            pruneImmunityMap(stack, gameTime);

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("antidote");

        if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("limiter"))
            return;

        enforceNegativeAmplifierCap(player);
    }

    private Map<ResourceLocation, Long> getImmunityMap(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.ANTIDOTE_VESSEL_IMMUNITY_UNTIL.get(), Map.of());
    }

    private void setImmunityMap(ItemStack stack, Map<ResourceLocation, Long> value) {
        stack.set(DataComponentRegistry.ANTIDOTE_VESSEL_IMMUNITY_UNTIL.get(), Map.copyOf(value));
    }

    private long getImmunityUntil(ItemStack stack, ResourceLocation effectId) {
        return getImmunityMap(stack).getOrDefault(effectId, 0L);
    }

    private void setImmunityUntil(ItemStack stack, ResourceLocation effectId, long gameTime) {
        var map = new HashMap<>(getImmunityMap(stack));
        map.put(effectId, gameTime);

        setImmunityMap(stack, map);
    }

    private void pruneImmunityMap(ItemStack stack, long gameTime) {
        var map = new HashMap<>(getImmunityMap(stack));

        if (!map.entrySet().removeIf(entry -> entry.getValue() <= gameTime))
            return;

        setImmunityMap(stack, map);
    }

    private void enforceNegativeAmplifierCap(Player player) {
        var replacements = new ArrayList<MobEffectInstance>();

        for (var effect : player.getActiveEffects()) {
            if (!isNegative(effect) || effect.getAmplifier() <= 0)
                continue;

            replacements.add(copyEffectWithAdjustedValues(effect, effect.getDuration(), 0));
        }

        if (replacements.isEmpty())
            return;

        INTERNAL_EFFECT_APPLICATION.set(true);

        try {
            for (var effect : replacements) {
                player.removeEffect(effect.getEffect());
                player.addEffect(effect, null);
            }
        } finally {
            INTERNAL_EFFECT_APPLICATION.set(false);
        }
    }

    private static void applyEffectInternally(Player player, MobEffectInstance effect, @Nullable Entity source) {
        INTERNAL_EFFECT_APPLICATION.set(true);

        try {
            player.addEffect(effect, source);
        } finally {
            INTERNAL_EFFECT_APPLICATION.set(false);
        }
    }

    private static MobEffectInstance copyEffectWithAdjustedValues(MobEffectInstance source, int duration, int amplifier) {
        var copy = new MobEffectInstance(source.getEffect(), duration, amplifier, source.isAmbient(), source.isVisible(), source.showIcon());

        copy.getCures().clear();
        copy.getCures().addAll(source.getCures());

        return copy;
    }

    @Nullable
    private static ResourceLocation getEffectId(MobEffectInstance effect) {
        return effect.getEffect().unwrapKey().map(key -> key.location()).orElse(null);
    }

    private static int countNegativeEffects(Player player) {
        var count = 0;

        for (var effect : player.getActiveEffects()) {
            if (isNegative(effect))
                count++;
        }

        return count;
    }

    private static boolean isNegative(MobEffectInstance effect) {
        return effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL;
    }

    private static long secondsToTicks(double seconds) {
        return Math.max(0L, Math.round(Math.max(0D, seconds) * 20D));
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
            if (INTERNAL_EFFECT_APPLICATION.get())
                return;

            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide())
                return;

            var effect = event.getEffectInstance();

            if (effect == null || !isNegative(effect))
                return;

            var effectId = getEffectId(effect);

            if (effectId == null)
                return;

            var gameTime = player.level().getGameTime();
            var durationReduction = 0D;
            var hasAmplifierCap = false;
            var blockedByImmunity = false;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.ANTIDOTE_VESSEL.value())) {
                if (!(stack.getItem() instanceof AntidoteVesselItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("antidote");

                if (!ability.canPlayerUse(player))
                    continue;

                durationReduction = Math.max(durationReduction, Math.max(0D, Math.min(1D, ability.getStatData("duration_reduction").getValue())));

                if (ability.isRankModifierUnlocked("recovery") && relic.getImmunityUntil(stack, effectId) > gameTime)
                    blockedByImmunity = true;

                if (ability.isRankModifierUnlocked("limiter"))
                    hasAmplifierCap = true;
            }

            if (!blockedByImmunity && durationReduction <= 0D && !hasAmplifierCap)
                return;

            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);

            if (blockedByImmunity)
                return;

            var duration = effect.getDuration();

            if (durationReduction > 0D && duration > 0)
                duration = Math.max(1, (int) Math.round(duration * (1D - durationReduction)));

            var amplifier = hasAmplifierCap ? Math.min(effect.getAmplifier(), 1) : effect.getAmplifier();
            var adjusted = copyEffectWithAdjustedValues(effect, duration, amplifier);

            applyEffectInternally(player, adjusted, event.getEffectSource());
        }

        @SubscribeEvent
        public static void onMobEffectExpired(MobEffectEvent.Expired event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide())
                return;

            var effect = event.getEffectInstance();

            if (effect == null || !isNegative(effect))
                return;

            var effectId = getEffectId(effect);

            if (effectId == null)
                return;

            var gameTime = player.level().getGameTime();

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.ANTIDOTE_VESSEL.value())) {
                if (!(stack.getItem() instanceof AntidoteVesselItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("antidote");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("recovery"))
                    continue;

                var protectionTicks = secondsToTicks(ability.getStatData("immunity_duration").getValue());

                if (protectionTicks <= 0L)
                    continue;

                relic.setImmunityUntil(stack, effectId, gameTime + protectionTicks);
            }
        }

        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getAmount() <= 0F)
                return;

            var negativeEffects = countNegativeEffects(player);

            if (negativeEffects <= 0)
                return;

            var reduction = 0D;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.ANTIDOTE_VESSEL.value())) {
                if (!(stack.getItem() instanceof AntidoteVesselItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("antidote");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("resilience"))
                    continue;

                var perEffect = Math.max(0D, Math.min(1D, ability.getStatData("resistance_per_effect").getValue()));
                reduction = Math.max(reduction, Math.max(0D, Math.min(1D, perEffect * negativeEffects)));
            }

            if (reduction > 0D)
                event.setAmount((float) Math.max(0D, event.getAmount() * (1D - reduction)));
        }
    }
}

