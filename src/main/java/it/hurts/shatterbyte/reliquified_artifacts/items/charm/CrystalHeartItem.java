package it.hurts.shatterbyte.reliquified_artifacts.items.charm;

import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import artifacts.registry.ModItems;
import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
import it.hurts.shatterbyte.reliquified_artifacts.init.RADataComponent;
import it.hurts.shatterbyte.reliquified_artifacts.items.base.RAWearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.AbilityMetricTemplate;
import it.hurts.sskirillss.relics.api.relics.AbilityStatisticTemplate;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.VisibilityState;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.ExperienceSourceTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.ExperienceSourcesTemplate;
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

public class CrystalHeartItem extends RAWearableRelicItem {

    private static final ResourceLocation HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(ReliquifiedArtifacts.MODID, "crystal_heart_bonus_health");

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("heart")
                                .rankModifier(1, "bastion")
                                .rankModifier(3, "recuperation")
                                .rankModifier(5, "survival")
                                .stat(AbilityStatTemplate.builder("bonus_health")
                                        .initialValue(2D, 6D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0667D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("cooldown")
                                        .initialValue(50D, 60D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), -0.02379D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("full_health_resistance")
                                        .initialValue(0.1D, 0.25D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0457D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("low_health_heal_bonus")
                                        .initialValue(0.15D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0531D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("heavy_hit_threshold")
                                        .initialValue(0.75D, 0.5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), -0.0143D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("immortality_duration")
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0667D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("bonus_health_healed").build())
                                        .source(ExperienceSourceTemplate.builder("immortality_trigger")
                                                .rankModifierVisibilityState("survival", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("immortality_triggers")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("survival", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.CAVE, LootEntries.MINESHAFT)
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

        var baseHealthThreshold = (float) Math.max(0D, player.getMaxHealth() - bonusHealth);

        if (currentHealth < lastHealth && currentHealth <= baseHealthThreshold + 1.0E-3F) {
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
        return Math.max(0, stack.getOrDefault(RADataComponent.CRYSTAL_HEART_COOLDOWN_TICKS.get(), 0));
    }

    private void setCooldownTicks(ItemStack stack, int ticks) {
        stack.set(RADataComponent.CRYSTAL_HEART_COOLDOWN_TICKS.get(), Math.max(0, ticks));
    }

    private float getLastHealth(ItemStack stack) {
        return stack.getOrDefault(RADataComponent.CRYSTAL_HEART_LAST_HEALTH.get(), -1D).floatValue();
    }

    private void setLastHealth(ItemStack stack, float health) {
        stack.set(RADataComponent.CRYSTAL_HEART_LAST_HEALTH.get(), (double) health);
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

    @EventBusSubscriber(modid = ReliquifiedArtifacts.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getAmount() <= 0F)
                return;

            var incomingAmount = event.getAmount();
            var reduction = 0D;
            var immortalityTicks = 0;
            CrystalHeartItem immortalityRelic = null;
            ItemStack immortalityStack = ItemStack.EMPTY;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.CRYSTAL_HEART.value())) {
                if (!(stack.getItem() instanceof CrystalHeartItem relic))
                    continue;

                var relicData = relic.getRelicData(player, stack);
                var ability = relicData.getAbilitiesData().getAbilityData("heart");

                if (!ability.canPlayerUse(player) || relic.getCooldownTicks(stack) > 0)
                    continue;

                if (ability.getRankModifierData("bastion").isUnlocked() && player.getHealth() >= player.getMaxHealth() - 1.0E-3F) {
                    var resist = Math.max(0D, Math.min(1D, ability.getStatData("full_health_resistance").getValue()));
                    reduction = Math.max(reduction, resist);
                }

                if (ability.getRankModifierData("survival").isUnlocked()) {
                    var threshold = Math.max(0D, Math.min(1D, ability.getStatData("heavy_hit_threshold").getValue()));

                    if (incomingAmount >= player.getMaxHealth() * threshold) {
                        var durationTicks = secondsToTicks(ability.getStatData("immortality_duration").getValue());

                        if (durationTicks > immortalityTicks) {
                            immortalityTicks = (int) durationTicks;
                            immortalityRelic = relic;
                            immortalityStack = stack;
                        }
                    }
                }
            }

            if (reduction > 0D)
                event.setAmount((float) Math.max(0D, incomingAmount * (1D - reduction)));

            if (immortalityTicks > 0) {
                player.addEffect(new MobEffectInstance(RelicsMobEffects.IMMORTALITY, immortalityTicks, 0, false, false));

                if (immortalityRelic != null) {
                    var relicData = immortalityRelic.getRelicData(player, immortalityStack);
                    var ability = relicData.getAbilitiesData().getAbilityData("heart");

                    relicData.getLevelingData().addExperience("heart", "immortality_trigger", 1D);
                    ability.getStatisticData().getMetricData("immortality_triggers").addValue(1D);
                }
            }
        }

        @SubscribeEvent
        public static void onLivingHeal(LivingHealEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getAmount() <= 0F)
                return;

            var currentHealth = player.getHealth();
            var maxHealth = player.getMaxHealth();
            var incomingHeal = Math.min(event.getAmount(), Math.max(0F, maxHealth - currentHealth));
            var bonusExtraHeal = 0D;
            CrystalHeartItem experienceRelic = null;
            ItemStack experienceStack = ItemStack.EMPTY;
            var experienceBonusHealth = 0D;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.CRYSTAL_HEART.value())) {
                if (!(stack.getItem() instanceof CrystalHeartItem relic))
                    continue;

                var relicData = relic.getRelicData(player, stack);
                var ability = relicData.getAbilitiesData().getAbilityData("heart");

                if (!ability.canPlayerUse(player) || relic.getCooldownTicks(stack) > 0)
                    continue;

                var bonusHealth = Math.max(0D, ability.getStatData("bonus_health").getValue());

                if (bonusHealth > experienceBonusHealth) {
                    experienceBonusHealth = bonusHealth;
                    experienceRelic = relic;
                    experienceStack = stack;
                }

                if (!ability.getRankModifierData("recuperation").isUnlocked())
                    continue;

                var healBonus = Math.max(0D, ability.getStatData("low_health_heal_bonus").getValue());

                if (healBonus <= 0D || incomingHeal <= 0F)
                    continue;

                var baseHealthThreshold = Math.max(0D, maxHealth - bonusHealth);
                var healEnd = Math.min(maxHealth, currentHealth + incomingHeal);
                var healedInBonusRange = Math.max(0D, healEnd - Math.max(currentHealth, baseHealthThreshold));

                if (healedInBonusRange <= 0D)
                    continue;

                bonusExtraHeal = Math.max(bonusExtraHeal, healedInBonusRange * healBonus);
            }

            if (bonusExtraHeal > 0D)
                event.setAmount((float) (event.getAmount() + bonusExtraHeal));

            if (experienceRelic == null || experienceBonusHealth <= 0D || event.getAmount() <= 0F)
                return;

            var healApplied = Math.min(event.getAmount(), Math.max(0F, maxHealth - currentHealth));

            if (healApplied <= 0F)
                return;

            var baseHealthThreshold = Math.max(0D, maxHealth - experienceBonusHealth);
            var start = currentHealth;
            var end = Math.min(maxHealth, currentHealth + healApplied);
            var healedInBonusHealth = Math.max(0D, end - Math.max(start, baseHealthThreshold));

            if (healedInBonusHealth <= 0D)
                return;

            experienceRelic.getRelicData(player, experienceStack).getLevelingData().addExperience("heart", "bonus_health_healed", healedInBonusHealth);
        }
    }
}

