package it.hurts.octostudios.rarcompat.items.feet;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

public class FlippersItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("swim")
                                .rankModifier(1, "buoyancy")
                                .rankModifier(3, "breathing")
                                .rankModifier(5, "evasion")
                                .stat(AbilityStatTemplate.builder("speed")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.1D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("air_loss_reduction")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.1D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("evasion_per_speed")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.03D, 0.12D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 1))
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide())
            return;

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("swim");

        if (!ability.canPlayerUse(player)) {
            EntityUtils.removeAttribute(player, stack, NeoForgeMod.SWIM_SPEED, AttributeModifier.Operation.ADD_VALUE);
            EntityUtils.removeAttribute(player, stack, Attributes.GRAVITY, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            return;
        }

        var inWater = player.isInWaterOrBubble();

        if (inWater) {
            var speedBonus = Math.max(0D, ability.getStatData("speed").getValue());

            if (speedBonus <= 0D)
                EntityUtils.removeAttribute(player, stack, NeoForgeMod.SWIM_SPEED, AttributeModifier.Operation.ADD_VALUE);
            else
                EntityUtils.applyAttribute(player, stack, NeoForgeMod.SWIM_SPEED, (float) speedBonus, AttributeModifier.Operation.ADD_VALUE);
        } else {
            EntityUtils.removeAttribute(player, stack, NeoForgeMod.SWIM_SPEED, AttributeModifier.Operation.ADD_VALUE);
        }

        if (inWater && ability.isRankModifierUnlocked("buoyancy"))
            EntityUtils.resetAttribute(player, stack, Attributes.GRAVITY, -1F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        else
            EntityUtils.removeAttribute(player, stack, Attributes.GRAVITY, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        if (!inWater || !ability.isRankModifierUnlocked("breathing") || !player.isEyeInFluid(FluidTags.WATER) || player.getAirSupply() >= player.getMaxAirSupply())
            return;

        var reduction = Math.max(0D, Math.min(1D, ability.getStatData("air_loss_reduction").getValue()));

        if (reduction > 0D && player.getRandom().nextDouble() <= reduction)
            player.setAirSupply(Math.min(player.getMaxAirSupply(), player.getAirSupply() + 1));
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (stack.getItem() == newStack.getItem() || !(slotContext.entity() instanceof Player player))
            return;

        EntityUtils.removeAttribute(player, stack, NeoForgeMod.SWIM_SPEED, AttributeModifier.Operation.ADD_VALUE);
        EntityUtils.removeAttribute(player, stack, Attributes.GRAVITY, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getAmount() <= 0F || !player.isInWaterOrBubble())
                return;

            var missChance = 0D;
            var underwaterSpeed = player.getKnownMovement().multiply(1,0,1).length();

            if (underwaterSpeed <= 0D)
                return;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.FLIPPERS.value())) {
                if (!(stack.getItem() instanceof FlippersItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("swim");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("evasion"))
                    continue;

                var perSpeed = Math.max(0D, ability.getStatData("evasion_per_speed").getValue());
                var chance = Math.max(0D, Math.min(1D, perSpeed * underwaterSpeed));

                missChance = Math.max(missChance, chance);
            }

            if (missChance <= 0D || player.getRandom().nextDouble() > missChance)
                return;

            event.setCanceled(true);
        }
    }
}
