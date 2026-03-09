package it.hurts.octostudios.rarcompat.items.charm;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.init.DataComponentRegistry;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsMobEffects;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

public class CrystalHeartItem extends WearableRelicItem {

    private static final ResourceLocation HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(RARCompat.MODID, "crystal_heart_bonus_health");

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("heart")
                                .rankModifier(1, "bastion")
                                .rankModifier(3, "recuperation")
                                .rankModifier(5, "survival")
                                .stat(AbilityStatTemplate.builder("bonus_health")
                                        .thresholdValue(1D, Double.MAX_VALUE)
                                        .initialValue(4D, 12D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("cooldown")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(6D, 16D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("full_health_resistance")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.06D, 0.2D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("low_health_heal_bonus")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.1D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("heavy_hit_threshold")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.2D, 0.45D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("immortality_duration")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(1D, 4D)
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

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("heart");

        if (!ability.canPlayerUse(player)) {
            removeBonusHealth(player);
            setLastHealth(stack, player.getHealth());
            return;
        }

        var cooldown = getCooldownTicks(stack);

        if (cooldown > 0) {
            setCooldownTicks(stack, cooldown - 1);
            removeBonusHealth(player);
            setLastHealth(stack, player.getHealth());
            return;
        }

        var bonusHealth = Math.max(0D, ability.getStatData("bonus_health").getValue());
        applyBonusHealth(player, bonusHealth);

        var currentHealth = player.getHealth();
        var lastHealth = getLastHealth(stack);

        if (lastHealth < 0F)
            lastHealth = currentHealth;

        if (currentHealth < lastHealth && currentHealth < bonusHealth) {
            var cooldownTicks = secondsToTicks(ability.getStatData("cooldown").getValue());

            if (cooldownTicks > 0)
                setCooldownTicks(stack, (int) cooldownTicks);

            removeBonusHealth(player);
        }

        setLastHealth(stack, currentHealth);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (stack.getItem() == newStack.getItem())
            return;

        if (slotContext.entity() instanceof Player player && !player.level().isClientSide())
            removeBonusHealth(player);

        setLastHealth(stack, -1F);
    }

    private int getCooldownTicks(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(DataComponentRegistry.CRYSTAL_HEART_COOLDOWN_TICKS.get(), 0));
    }

    private void setCooldownTicks(ItemStack stack, int ticks) {
        stack.set(DataComponentRegistry.CRYSTAL_HEART_COOLDOWN_TICKS.get(), Math.max(0, ticks));
    }

    private float getLastHealth(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.CRYSTAL_HEART_LAST_HEALTH.get(), -1D).floatValue();
    }

    private void setLastHealth(ItemStack stack, float health) {
        stack.set(DataComponentRegistry.CRYSTAL_HEART_LAST_HEALTH.get(), (double) health);
    }

    private static void applyBonusHealth(Player player, double bonusHealth) {
        var attribute = player.getAttribute(Attributes.MAX_HEALTH);

        if (attribute == null)
            return;

        attribute.removeModifier(new AttributeModifier(HEALTH_MODIFIER_ID, 0D, AttributeModifier.Operation.ADD_VALUE));

        if (bonusHealth > 0D)
            attribute.addOrUpdateTransientModifier(new AttributeModifier(HEALTH_MODIFIER_ID, bonusHealth, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void removeBonusHealth(Player player) {
        var attribute = player.getAttribute(Attributes.MAX_HEALTH);

        if (attribute == null)
            return;

        attribute.removeModifier(new AttributeModifier(HEALTH_MODIFIER_ID, 0D, AttributeModifier.Operation.ADD_VALUE));

        var maxHealth = (float) player.getMaxHealth();

        if (player.getHealth() > maxHealth)
            player.setHealth(maxHealth);
    }

    private static long secondsToTicks(double seconds) {
        return Math.max(0L, Math.round(Math.max(0D, seconds) * 20D));
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getAmount() <= 0F)
                return;

            var incomingAmount = event.getAmount();
            var reduction = 0D;
            var immortalityTicks = 0;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.CRYSTAL_HEART.value())) {
                if (!(stack.getItem() instanceof CrystalHeartItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("heart");

                if (!ability.canPlayerUse(player) || relic.getCooldownTicks(stack) > 0)
                    continue;

                if (ability.isRankModifierUnlocked("bastion") && player.getHealth() >= player.getMaxHealth() - 1.0E-3F) {
                    var resist = Math.max(0D, Math.min(1D, ability.getStatData("full_health_resistance").getValue()));
                    reduction = Math.max(reduction, resist);
                }

                if (ability.isRankModifierUnlocked("survival")) {
                    var threshold = Math.max(0D, Math.min(1D, ability.getStatData("heavy_hit_threshold").getValue()));

                    if (incomingAmount >= player.getMaxHealth() * threshold) {
                        var durationTicks = secondsToTicks(ability.getStatData("immortality_duration").getValue());

                        immortalityTicks = Math.max(immortalityTicks, (int) durationTicks);
                    }
                }
            }

            if (reduction > 0D)
                event.setAmount((float) Math.max(0D, incomingAmount * (1D - reduction)));

            if (immortalityTicks > 0)
                player.addEffect(new MobEffectInstance(RelicsMobEffects.IMMORTALITY, immortalityTicks, 0, false, false));
        }

        @SubscribeEvent
        public static void onLivingHeal(LivingHealEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getAmount() <= 0F)
                return;

            var bonus = 0D;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.CRYSTAL_HEART.value())) {
                if (!(stack.getItem() instanceof CrystalHeartItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("heart");

                if (!ability.canPlayerUse(player) || relic.getCooldownTicks(stack) > 0 || !ability.isRankModifierUnlocked("recuperation"))
                    continue;

                var bonusHealth = Math.max(0D, ability.getStatData("bonus_health").getValue());

                if (player.getHealth() > bonusHealth)
                    continue;

                var healBonus = Math.max(0D, ability.getStatData("low_health_heal_bonus").getValue());
                bonus = Math.max(bonus, healBonus);
            }

            if (bonus > 0D)
                event.setAmount((float) (event.getAmount() * (1D + bonus)));
        }
    }
}

