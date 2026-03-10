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
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

public class ObsidianSkullItem extends WearableRelicItem {
    private static final ResourceLocation LAVA_SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(RARCompat.MODID, "obsidian_skull_lava_speed");

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("lava")
                                .rankModifier(1, "heat_surge")
                                .rankModifier(3, "lava_launch")
                                .rankModifier(5, "obsidian_skin")
                                .stat(AbilityStatTemplate.builder("duration")
                                        .thresholdValue(1D, Double.MAX_VALUE)
                                        .initialValue(8D, 16D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("low_reserve_speed_bonus")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.12D, 0.3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("jump_boost")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(0.25D, 0.55D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 2))
                                        .build())
                                .stat(AbilityStatTemplate.builder("damage_reduction")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.08D, 0.25D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide())
            return;

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("lava");

        if (!ability.canPlayerUse(player)) {
            removeLavaSpeedModifier(player);
            setFallProtectionActive(stack, false);

            return;
        }

        var maxLavaTicks = getMaxLavaTicks(ability.getStatData("duration").getValue());

        if (!stack.has(DataComponentRegistry.OBSIDIAN_SKULL_LAVA_TICKS.get()))
            setLavaTicks(stack, maxLavaTicks);

        var lavaTicks = Math.max(0, Math.min(maxLavaTicks, getLavaTicks(stack)));
        var inLava = player.isInLava();

        if (inLava) {
            if (lavaTicks > 0) {
                lavaTicks--;
                player.clearFire();
            }
        } else if (lavaTicks < maxLavaTicks) {
            lavaTicks++;
        }

        setLavaTicks(stack, lavaTicks);

        var isLowReserve = inLava && lavaTicks > 0 && lavaTicks <= Math.max(1, maxLavaTicks / 4);

        if (ability.isRankModifierUnlocked("heat_surge") && isLowReserve) {
            var speedBonus = Math.max(0D, ability.getStatData("low_reserve_speed_bonus").getValue());
            applyLavaSpeedModifier(player, speedBonus);
        } else {
            removeLavaSpeedModifier(player);
        }

        if (isFallProtectionActive(stack) && player.onGround())
            setFallProtectionActive(stack, false);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (stack.getItem() == newStack.getItem())
            return;

        if (slotContext.entity() instanceof Player player && !player.level().isClientSide())
            removeLavaSpeedModifier(player);

        setFallProtectionActive(stack, false);
    }

    private int getMaxLavaTicks(double seconds) {
        return Math.max(1, (int) Math.round(Math.max(1D, seconds) * 20D));
    }

    private int getLavaTicks(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(DataComponentRegistry.OBSIDIAN_SKULL_LAVA_TICKS.get(), 0));
    }

    private void setLavaTicks(ItemStack stack, int ticks) {
        stack.set(DataComponentRegistry.OBSIDIAN_SKULL_LAVA_TICKS.get(), Math.max(0, ticks));
    }

    private boolean isFallProtectionActive(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.OBSIDIAN_SKULL_FALL_PROTECTION.get(), false);
    }

    private void setFallProtectionActive(ItemStack stack, boolean active) {
        stack.set(DataComponentRegistry.OBSIDIAN_SKULL_FALL_PROTECTION.get(), active);
    }

    private void applyLavaSpeedModifier(Player player, double bonus) {
        var attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);

        if (attribute == null)
            return;

        attribute.addOrUpdateTransientModifier(new AttributeModifier(LAVA_SPEED_MODIFIER_ID, Math.max(0D, bonus), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private void removeLavaSpeedModifier(Player player) {
        var attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);

        if (attribute == null)
            return;

        attribute.removeModifier(new AttributeModifier(LAVA_SPEED_MODIFIER_ID, 0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || !player.isInLava())
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.OBSIDIAN_SKULL.value());

            if (!(stack.getItem() instanceof ObsidianSkullItem relic))
                return;

            var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("lava");

            if (!ability.canPlayerUse(player))
                return;

            if (relic.getLavaTicks(stack) <= 0)
                return;

            if (event.getSource().is(DamageTypes.LAVA) || event.getSource().is(DamageTypes.IN_FIRE) || event.getSource().is(DamageTypes.ON_FIRE)) {
                event.setCanceled(true);

                return;
            }

            if (ability.isRankModifierUnlocked("obsidian_skin")) {
                var reduction = Math.max(0D, Math.min(1D, ability.getStatData("damage_reduction").getValue()));

                if (reduction > 0D)
                    event.setAmount((float) Math.max(0D, event.getAmount() * (1D - reduction)));
            }
        }

        @SubscribeEvent
        public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || !player.isInLava())
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.OBSIDIAN_SKULL.value());

            if (!(stack.getItem() instanceof ObsidianSkullItem relic))
                return;

            var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("lava");

            if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("lava_launch") || relic.getLavaTicks(stack) <= 0)
                return;

            var jumpBoost = Math.max(0D, ability.getStatData("jump_boost").getValue());

            if (jumpBoost > 0D)
                player.setDeltaMovement(player.getDeltaMovement().x, player.getDeltaMovement().y + jumpBoost, player.getDeltaMovement().z);

            relic.setFallProtectionActive(stack, true);
        }

        @SubscribeEvent
        public static void onLivingFall(LivingFallEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide())
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.OBSIDIAN_SKULL.value());

            if (!(stack.getItem() instanceof ObsidianSkullItem relic))
                return;

            if (!relic.isFallProtectionActive(stack))
                return;

            event.setCanceled(true);
            relic.setFallProtectionActive(stack, false);
        }
    }
}

