package it.hurts.shatterbyte.reliquified_artifacts.items.hands;

import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import artifacts.registry.ModItems;
import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
import it.hurts.shatterbyte.reliquified_artifacts.init.RADataComponent;
import it.hurts.shatterbyte.reliquified_artifacts.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.AbilityMetricTemplate;
import it.hurts.sskirillss.relics.api.relics.AbilityStatisticTemplate;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.ExperienceSourceTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.ExperienceSourcesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

public class FeralClawsItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("feral")
                                .rankModifier(1, "retaliation")
                                .rankModifier(3, "linger")
                                .rankModifier(5, "rend")
                                .stat(AbilityStatTemplate.builder("window")
                                        .initialValue(1D, 2.5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0286D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("max_charges")
                                        .initialValue(3D, 5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.1143D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("attack_speed_per_charge")
                                        .initialValue(0.025D, 0.05D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0286D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("invulnerability_reduction")
                                        .initialValue(0.05D, 0.1D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0429D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("charge_gain").build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("charges_gained")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .build())
                                .build())
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.NETHER_LIKE, LootEntries.THE_NETHER)
                        .build())
                .build();
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (!(slotContext.entity() instanceof Player player) || stack.getItem() == newStack.getItem())
            return;

        clearState(stack);
        EntityUtils.removeAttribute(player, stack, Attributes.ATTACK_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide())
            return;

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("feral");

        if (!ability.canPlayerUse(player)) {
            clearState(stack);
            EntityUtils.removeAttribute(player, stack, Attributes.ATTACK_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

            return;
        }

        var charges = getCharges(stack);

        if (charges > 0) {
            var timeout = getTimeoutTicks(stack);

            if (timeout > 0) {
                setTimeoutTicks(stack, timeout - 1);
            } else if (ability.isRankModifierUnlocked("linger")) {
                var decay = getDecayTicks(stack) - 1;

                if (decay <= 0) {
                    charges--;
                    setCharges(stack, charges);
                    decay = 20;
                }

                setDecayTicks(stack, decay);
            } else {
                charges = 0;
                setCharges(stack, 0);
            }
        }

        if (charges <= 0) {
            clearState(stack);
            EntityUtils.removeAttribute(player, stack, Attributes.ATTACK_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

            return;
        }

        var bonus = Math.max(0D, ability.getStatData("attack_speed_per_charge").getValue()) * charges;

        if (bonus <= 0D)
            EntityUtils.removeAttribute(player, stack, Attributes.ATTACK_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        else
            EntityUtils.resetAttribute(player, stack, Attributes.ATTACK_SPEED, (float) bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static int secondsToTicks(double seconds) {
        return Math.max(0, (int) Math.round(Math.max(0D, seconds) * 20D));
    }

    private int getCharges(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(RADataComponent.FERAL_CLAWS_CHARGES.get(), 0));
    }

    private void setCharges(ItemStack stack, int value) {
        stack.set(RADataComponent.FERAL_CLAWS_CHARGES.get(), Math.max(0, value));
    }

    private int getTimeoutTicks(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(RADataComponent.FERAL_CLAWS_TIMEOUT_TICKS.get(), 0));
    }

    private void setTimeoutTicks(ItemStack stack, int value) {
        stack.set(RADataComponent.FERAL_CLAWS_TIMEOUT_TICKS.get(), Math.max(0, value));
    }

    private int getDecayTicks(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(RADataComponent.FERAL_CLAWS_DECAY_TICKS.get(), 0));
    }

    private void setDecayTicks(ItemStack stack, int value) {
        stack.set(RADataComponent.FERAL_CLAWS_DECAY_TICKS.get(), Math.max(0, value));
    }

    private void clearState(ItemStack stack) {
        setCharges(stack, 0);
        setTimeoutTicks(stack, 0);
        setDecayTicks(stack, 0);
    }

    private void registerAttack(Player player, ItemStack stack) {
        var relicData = this.getRelicData(player, stack);
        var ability = relicData.getAbilitiesData().getAbilityData("feral");

        if (!ability.canPlayerUse(player)) {
            clearState(stack);
            return;
        }

        var maxCharges = Math.max(1, (int) MathUtils.round(ability.getStatData("max_charges").getValue(), 0));
        var windowTicks = secondsToTicks(ability.getStatData("window").getValue());
        var previousCharges = getCharges(stack);
        var charges = previousCharges;

        if (charges > 0 && (getTimeoutTicks(stack) > 0 || ability.isRankModifierUnlocked("linger")))
            charges++;
        else
            charges = 1;

        charges = Math.min(charges, maxCharges);

        setCharges(stack, charges);
        setTimeoutTicks(stack, windowTicks);
        setDecayTicks(stack, 20);

        var gainedCharges = Math.max(0, charges - previousCharges);

        if (gainedCharges > 0) {
            relicData.getLevelingData().addExperience("feral", "charge_gain", gainedCharges);
            ability.getStatisticData().getMetricData("charges_gained").addValue(gainedCharges);
        }
    }

    private void refreshTimeout(Player player, ItemStack stack) {
        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("feral");

        if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("retaliation") || getCharges(stack) <= 0)
            return;

        setTimeoutTicks(stack, secondsToTicks(ability.getStatData("window").getValue()));
        setDecayTicks(stack, 20);
    }

    @EventBusSubscriber(modid = ReliquifiedArtifacts.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamageDealing(LivingIncomingDamageEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player) || event.getSource().getDirectEntity() != player || player.level().isClientSide() || event.getEntity() == player || event.getAmount() <= 0F)
                return;

            if (player.getAttackStrengthScale(0.5F) < 1F)
                return;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.FERAL_CLAWS.value())) {
                if (stack.getItem() instanceof FeralClawsItem relic)
                    relic.registerAttack(player, stack);
            }
        }

        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getAmount() <= 0F)
                return;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.FERAL_CLAWS.value())) {
                if (stack.getItem() instanceof FeralClawsItem relic)
                    relic.refreshTimeout(player, stack);
            }
        }

        @SubscribeEvent
        public static void onLivingDamagePost(LivingDamageEvent.Post event) {
            if (!(event.getSource().getEntity() instanceof Player player) || event.getSource().getDirectEntity() != player || player.level().isClientSide() || event.getEntity() == player || event.getNewDamage() <= 0F)
                return;

            var reduction = 0D;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.FERAL_CLAWS.value())) {
                if (!(stack.getItem() instanceof FeralClawsItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("feral");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("rend"))
                    continue;

                var value = Math.max(0D, Math.min(1D, ability.getStatData("invulnerability_reduction").getValue()));
                reduction = Math.max(reduction, value);
            }

            if (reduction <= 0D)
                return;

            var target = event.getEntity();
            var reducedTicks = Math.max(0, (int) Math.floor(target.invulnerableTime * (1D - reduction)));

            if (reducedTicks < target.invulnerableTime)
                target.invulnerableTime = reducedTicks;
        }
    }
}