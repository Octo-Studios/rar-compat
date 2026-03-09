package it.hurts.octostudios.rarcompat.items.necklace;

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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

public class CharmOfShrinkingItem extends WearableRelicItem {

    private static final ResourceLocation SCALE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(RARCompat.MODID, "charm_of_shrinking_scale");
    private static final ResourceLocation HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(RARCompat.MODID, "charm_of_shrinking_health");

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("size")
                                .modes("stabilize", "shrink", "grow")
                                .rankModifier(1, "evasion")
                                .rankModifier(3, "growth")
                                .rankModifier(5, "vitality")
                                .stat(AbilityStatTemplate.builder("min_scale")
                                        .thresholdValue(0.125D, 1D)
                                        .initialValue(0.75D, 0.45D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), -0.06D)
                                        .formatValue(value -> MathUtils.round(value, 2))
                                        .build())
                                .stat(AbilityStatTemplate.builder("max_scale")
                                        .thresholdValue(1D, 10D)
                                        .initialValue(1.25D, 2.5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 2))
                                        .build())
                                .stat(AbilityStatTemplate.builder("change_per_second")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(0.04D, 0.12D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 2))
                                        .build())
                                .stat(AbilityStatTemplate.builder("evasion_per_size")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.08D, 0.2D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("health_per_size")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(2D, 8D)
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

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("size");
        var currentScale = getCurrentScale(stack);

        if (!ability.canPlayerUse(player)) {
            applyScaleModifier(player, currentScale);
            applyHealthModifier(player, 0D);

            return;
        }

        var changePerSecond = Math.max(0D, ability.getStatData("change_per_second").getValue());
        var stepPerTick = changePerSecond / 20D;
        var targetScale = currentScale;

        switch (ability.getMode()) {
            case "shrink" -> {
                var minScale = clampScale(Math.min(1D, ability.getStatData("min_scale").getValue()));
                targetScale = minScale;
            }
            case "grow" -> {
                if (ability.isRankModifierUnlocked("growth")) {
                    var maxScale = clampScale(Math.max(1D, ability.getStatData("max_scale").getValue()));
                    targetScale = maxScale;
                }
            }
            default -> {
            }
        }

        var newScale = stepPerTick > 0D ? moveTowards(currentScale, targetScale, stepPerTick) : currentScale;

        newScale = clampScale(newScale);
        setCurrentScale(stack, newScale);
        applyScaleModifier(player, newScale);

        if (ability.isRankModifierUnlocked("vitality") && newScale > 1D) {
            var perSize = Math.max(0D, ability.getStatData("health_per_size").getValue());
            var bonusHealth = (newScale - 1D) * perSize;

            applyHealthModifier(player, bonusHealth);
        } else {
            applyHealthModifier(player, 0D);
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (stack.getItem() == newStack.getItem())
            return;

        if (slotContext.entity() instanceof Player player && !player.level().isClientSide()) {
            removeScaleModifier(player);
            applyHealthModifier(player, 0D);
        }

        setCurrentScale(stack, 1D);
    }

    private void applyScaleModifier(Player player, double scale) {
        var attribute = player.getAttribute(Attributes.SCALE);

        if (attribute == null)
            return;

        var amount = clampScale(scale) - 1D;

        attribute.addOrUpdateTransientModifier(new AttributeModifier(SCALE_MODIFIER_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private void removeScaleModifier(Player player) {
        var attribute = player.getAttribute(Attributes.SCALE);

        if (attribute == null)
            return;

        attribute.removeModifier(new AttributeModifier(SCALE_MODIFIER_ID, 0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private void applyHealthModifier(Player player, double bonusHealth) {
        var attribute = player.getAttribute(Attributes.MAX_HEALTH);

        if (attribute == null)
            return;

        attribute.removeModifier(new AttributeModifier(HEALTH_MODIFIER_ID, 0D, AttributeModifier.Operation.ADD_VALUE));

        if (bonusHealth > 0D)
            attribute.addOrUpdateTransientModifier(new AttributeModifier(HEALTH_MODIFIER_ID, bonusHealth, AttributeModifier.Operation.ADD_VALUE));

        var maxHealth = (float) player.getMaxHealth();

        if (player.getHealth() > maxHealth)
            player.setHealth(maxHealth);
    }

    private double getCurrentScale(ItemStack stack) {
        return clampScale(stack.getOrDefault(DataComponentRegistry.CHARM_OF_SHRINKING_CURRENT_SCALE.get(), 1D));
    }

    private void setCurrentScale(ItemStack stack, double scale) {
        stack.set(DataComponentRegistry.CHARM_OF_SHRINKING_CURRENT_SCALE.get(), clampScale(scale));
    }

    private static double moveTowards(double current, double target, double maxDelta) {
        if (maxDelta <= 0D)
            return current;

        if (current < target)
            return Math.min(target, current + maxDelta);

        if (current > target)
            return Math.max(target, current - maxDelta);

        return current;
    }

    private static double clampScale(double scale) {
        return Math.max(0.125D, Math.min(10D, scale));
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide())
                return;

            if (!(event.getSource().getEntity() instanceof LivingEntity))
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.CHARM_OF_SHRINKING.value());

            if (!(stack.getItem() instanceof CharmOfShrinkingItem relic))
                return;

            var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("size");

            if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("evasion"))
                return;

            var scale = relic.getCurrentScale(stack);

            if (scale >= 1D)
                return;

            var evasionPerSize = Math.max(0D, ability.getStatData("evasion_per_size").getValue());
            var chance = Math.max(0D, Math.min(1D, (1D - scale) * evasionPerSize));

            if (chance <= 0D)
                return;

            if (player.getRandom().nextDouble() < chance)
                event.setAmount(0F);
        }
    }
}


