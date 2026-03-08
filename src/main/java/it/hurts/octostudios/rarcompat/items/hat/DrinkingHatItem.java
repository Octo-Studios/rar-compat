package it.hurts.octostudios.rarcompat.items.hat;

import artifacts.registry.ModAttributes;
import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicAttributeModifier;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;

public class DrinkingHatItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("drinking")
                                .rankModifier(1, "breathing")
                                .rankModifier(3, "nutrition")
                                .rankModifier(5, "healing")
                                .stat(AbilityStatTemplate.builder("speed")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(0.2D, 0.5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("hunger")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(2D, 4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("health")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("air")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(40D, 100D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Override
    public @Nullable RelicAttributeModifier getRelicAttributeModifiers(LivingEntity entity, ItemStack stack) {
        var ability = this.getRelicData(entity, stack).getAbilitiesData().getAbilityData("drinking");

        if (!ability.canPlayerUse(entity))
            return null;

        var speed = Math.max(0D, ability.getStatData("speed").getValue());

        if (speed <= 0D)
            return null;

        return RelicAttributeModifier.builder()
                .attribute(new RelicAttributeModifier.Modifier(ModAttributes.DRINKING_SPEED, (float) speed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL))
                .build();
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || newStack.getItem() == stack.getItem())
            return;

        EntityUtils.removeAttribute(player, stack, ModAttributes.DRINKING_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @EventBusSubscriber
    public static class HatEvents {
        @SubscribeEvent
        public static void onUseItem(LivingEntityUseItemEvent.Finish event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getItem().getUseAnimation() != UseAnim.DRINK)
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.PLASTIC_DRINKING_HAT.value());

            if (stack.isEmpty())
                stack = EntityUtils.findEquippedCurio(player, ModItems.NOVELTY_DRINKING_HAT.value());

            if (!(stack.getItem() instanceof DrinkingHatItem relic))
                return;

            var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("drinking");

            if (!ability.canPlayerUse(player))
                return;

            if (ability.isRankModifierUnlocked("breathing")) {
                var air = Math.max(0, (int) MathUtils.round(ability.getStatData("air").getValue(), 0));

                if (air > 0)
                    player.setAirSupply(Math.min(player.getMaxAirSupply(), player.getAirSupply() + air));
            }

            if (ability.isRankModifierUnlocked("nutrition")) {
                var hunger = Math.max(0, (int) MathUtils.round(ability.getStatData("hunger").getValue(), 0));

                if (hunger > 0)
                    player.getFoodData().eat(hunger, 0F);
            }

            if (ability.isRankModifierUnlocked("healing")) {
                var heal = (float) Math.max(0D, ability.getStatData("health").getValue());

                if (heal > 0F)
                    player.heal(heal);
            }
        }
    }
}