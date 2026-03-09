package it.hurts.octostudios.rarcompat.items.hands;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import top.theillusivec4.curios.api.SlotContext;

public class PowerGloveItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("power")
                                .rankModifier(1, "shield_break")
                                .rankModifier(3, "armor_pierce")
                                .rankModifier(5, "echo_strike")
                                .stat(AbilityStatTemplate.builder("hits_required")
                                        .thresholdValue(1D, Double.MAX_VALUE)
                                        .initialValue(5D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), -0.06D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("power_damage_bonus")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(0.2D, 0.6D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("armor_ignore")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.15D, 0.45D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (stack.getItem() == newStack.getItem())
            return;

        clearState(stack);
    }

    private int getHitCounter(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(DataComponentRegistry.POWER_GLOVE_HIT_COUNTER.get(), 0));
    }

    private void setHitCounter(ItemStack stack, int value) {
        stack.set(DataComponentRegistry.POWER_GLOVE_HIT_COUNTER.get(), Math.max(0, value));
    }

    private boolean getForceNext(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.POWER_GLOVE_FORCE_NEXT.get(), false);
    }

    private void setForceNext(ItemStack stack, boolean value) {
        stack.set(DataComponentRegistry.POWER_GLOVE_FORCE_NEXT.get(), value);
    }

    private boolean getPowerStrikeActive(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.POWER_GLOVE_POWER_STRIKE_ACTIVE.get(), false);
    }

    private void setPowerStrikeActive(ItemStack stack, boolean value) {
        stack.set(DataComponentRegistry.POWER_GLOVE_POWER_STRIKE_ACTIVE.get(), value);
    }

    private void clearState(ItemStack stack) {
        setHitCounter(stack, 0);
        setForceNext(stack, false);
        setPowerStrikeActive(stack, false);
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamageDealing(LivingIncomingDamageEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player) || event.getSource().getDirectEntity() != player || player.level().isClientSide() || event.getEntity() == player || event.getAmount() <= 0F)
                return;

            var damageBonus = 0D;
            var armorIgnore = 0D;
            var hasPowerStrike = false;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.POWER_GLOVE.value())) {
                if (!(stack.getItem() instanceof PowerGloveItem relic))
                    continue;

                relic.setPowerStrikeActive(stack, false);

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("power");

                if (!ability.canPlayerUse(player)) {
                    relic.clearState(stack);
                    continue;
                }

                var hitsRequired = Math.max(1, (int) MathUtils.round(ability.getStatData("hits_required").getValue(), 0));
                var powerStrike = relic.getForceNext(stack);

                if (powerStrike) {
                    relic.setForceNext(stack, false);
                    relic.setHitCounter(stack, 0);
                } else {
                    var counter = relic.getHitCounter(stack) + 1;

                    if (counter >= hitsRequired) {
                        powerStrike = true;
                        counter = 0;
                    }

                    relic.setHitCounter(stack, counter);
                }

                if (!powerStrike)
                    continue;

                hasPowerStrike = true;
                relic.setPowerStrikeActive(stack, true);

                damageBonus = Math.max(damageBonus, Math.max(0D, ability.getStatData("power_damage_bonus").getValue()));

                if (ability.isRankModifierUnlocked("armor_pierce")) {
                    var value = Math.max(0D, Math.min(1D, ability.getStatData("armor_ignore").getValue()));

                    armorIgnore = Math.max(armorIgnore, value);
                }
            }

            if (!hasPowerStrike)
                return;

            if (damageBonus > 0D)
                event.setAmount((float) (event.getAmount() * (1D + damageBonus)));

            if (armorIgnore > 0D) {
                var armorIgnoreFinal = armorIgnore;

                event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) ->
                        Math.max(0F, (float) (reduction * (1D - armorIgnoreFinal))));
            }
        }

        @SubscribeEvent
        public static void onLivingShieldBlock(LivingShieldBlockEvent event) {
            if (!event.getBlocked())
                return;

            if (!(event.getDamageSource().getEntity() instanceof Player player) || event.getDamageSource().getDirectEntity() != player || player.level().isClientSide())
                return;

            if (!(event.getEntity() instanceof Player target))
                return;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.POWER_GLOVE.value())) {
                if (!(stack.getItem() instanceof PowerGloveItem relic) || !relic.getPowerStrikeActive(stack))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("power");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("shield_break"))
                    continue;

                target.disableShield();
                break;
            }
        }

        @SubscribeEvent
        public static void onLivingDamagePost(LivingDamageEvent.Post event) {
            if (!(event.getSource().getEntity() instanceof Player player) || event.getSource().getDirectEntity() != player || player.level().isClientSide() || event.getEntity() == player || event.getNewDamage() <= 0F)
                return;

            var killed = !event.getEntity().isAlive() || event.getEntity().getHealth() <= 0F;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.POWER_GLOVE.value())) {
                if (!(stack.getItem() instanceof PowerGloveItem relic) || !relic.getPowerStrikeActive(stack))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("power");

                if (killed && ability.canPlayerUse(player) && ability.isRankModifierUnlocked("echo_strike"))
                    relic.setForceNext(stack, true);

                relic.setPowerStrikeActive(stack, false);
            }
        }
    }
}