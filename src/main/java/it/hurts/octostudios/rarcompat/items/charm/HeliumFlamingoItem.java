package it.hurts.octostudios.rarcompat.items.charm;

import artifacts.registry.ModItems;
import be.florens.expandability.api.EventResult;
import be.florens.expandability.api.forge.PlayerSwimEvent;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.init.DataComponentRegistry;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.octostudios.rarcompat.network.packets.FlamingoSwimPacket;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

public class HeliumFlamingoItem extends WearableRelicItem {

    private static final double STATIONARY_HORIZONTAL_SPEED_SQR = 0.0016D;
    private static final double STATIONARY_VERTICAL_SPEED = 0.04D;

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("flying")
                                .rankModifier(1, "aerial_archery")
                                .rankModifier(3, "aerial_guard")
                                .rankModifier(5, "efficient_hover")
                                .stat(AbilityStatTemplate.builder("time")
                                        .thresholdValue(1D, Double.MAX_VALUE)
                                        .initialValue(4D, 10D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("speed_bonus")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.12D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("ranged_damage_bonus")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.1D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("resistance")
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
        if (!(slotContext.entity() instanceof Player player))
            return;

        if (player.level().isClientSide())
            return;

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("flying");

        if (!ability.canPlayerUse(player)) {
            disableHover(player, stack, true);
            return;
        }

        if (isHoverResetState(player)) {
            disableHover(player, stack, true);
            return;
        }

        if (!getToggled(stack))
            return;

        if (getTime(stack) >= getMaxHoverSeconds(player, stack)) {
            disableHover(player, stack, false);
            return;
        }

        player.fallDistance = 0F;

        if (!shouldConsumeHoverTime(player, ability.isRankModifierUnlocked("efficient_hover")) || player.tickCount % 20 != 0)
            return;

        addTime(stack, 1);

        if (getTime(stack) >= getMaxHoverSeconds(player, stack))
            disableHover(player, stack, false);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (!(slotContext.entity() instanceof Player player) || stack.getItem() == newStack.getItem())
            return;

        disableHover(player, stack, true);
    }

    public boolean trySetHovering(Player player, ItemStack stack, boolean toggled) {
        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("flying");

        if (!ability.canPlayerUse(player)) {
            disableHover(player, stack, true);
            return false;
        }

        if (!toggled) {
            setToggled(stack, false);

            return true;
        }

        if (isHoverResetState(player) || getTime(stack) >= getMaxHoverSeconds(player, stack))
            return false;

        setToggled(stack, true);

        player.fallDistance = 0F;

        return true;
    }

    public int getMaxHoverSeconds(Player player, ItemStack stack) {
        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("flying");

        return Math.max(1, (int) MathUtils.round(ability.getStatData("time").getValue(), 0));
    }

    public boolean isHovering(Player player, ItemStack stack) {
        if (!getToggled(stack))
            return false;

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("flying");

        return ability.canPlayerUse(player) && !isHoverResetState(player) && getTime(stack) < getMaxHoverSeconds(player, stack);
    }

    private boolean shouldConsumeHoverTime(Player player, boolean efficientHoverUnlocked) {
        if (!efficientHoverUnlocked)
            return true;

        var movement = player.getDeltaMovement();
        var almostStill = movement.horizontalDistanceSqr() <= STATIONARY_HORIZONTAL_SPEED_SQR && Math.abs(movement.y) <= STATIONARY_VERTICAL_SPEED;

        return !almostStill;
    }

    private static boolean isHoverResetState(Player player) {
        return player.onGround() || player.isInLiquid() || player.isFallFlying() || player.getAbilities().flying;
    }

    private void disableHover(Player player, ItemStack stack, boolean resetTime) {
        setToggled(stack, false);

        if (resetTime)
            setTime(stack, 0);

        EntityUtils.removeAttribute(player, stack, NeoForgeMod.SWIM_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    public boolean getToggled(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.TOGGLED, false);
    }

    public void setToggled(ItemStack stack, boolean val) {
        stack.set(DataComponentRegistry.TOGGLED, val);
    }

    public void addTime(ItemStack stack, int time) {
        setTime(stack, getTime(stack) + time);
    }

    public int getTime(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(DataComponentRegistry.TIME, 0));
    }

    public void setTime(ItemStack stack, int val) {
        stack.set(DataComponentRegistry.TIME, Math.max(val, 0));
    }

    @EventBusSubscriber(value = Dist.CLIENT)
    public static class HeliumFlamingoClientEvent {
        @SubscribeEvent
        public static void onClientTick(InputEvent.Key event) {
            var minecraft = Minecraft.getInstance();
            var player = minecraft.player;

            if (player == null)
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.HELIUM_FLAMINGO.value());

            if (minecraft.screen != null || event.getAction() != 1 || !(stack.getItem() instanceof HeliumFlamingoItem relic)
                    || event.getKey() != minecraft.options.keyJump.getKey().getValue())
                return;

            if (player.onGround() || player.isInLiquid() || player.getAbilities().flying)
                return;

            if (player.mayFly()) {
                if (relic.getToggled(stack))
                    NetworkHandler.sendToServer(new FlamingoSwimPacket(false));

                return;
            }

            var targetState = !relic.getToggled(stack);

            if (targetState && relic.getTime(stack) >= relic.getMaxHoverSeconds(player, stack))
                return;

            NetworkHandler.sendToServer(new FlamingoSwimPacket(targetState));
        }
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvent {
        @SubscribeEvent
        public static void onSwimAir(PlayerSwimEvent event) {
            var player = event.getEntity();
            var stack = EntityUtils.findEquippedCurio(player, ModItems.HELIUM_FLAMINGO.value());

            if (!(stack.getItem() instanceof HeliumFlamingoItem relic))
                return;

            var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("flying");

            if (!ability.canPlayerUse(player) || !relic.isHovering(player, stack)) {
                event.setResult(EventResult.PASS);
                EntityUtils.removeAttribute(player, stack, NeoForgeMod.SWIM_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
                return;
            }

            event.setResult(EventResult.SUCCESS);

            var speedBonus = Math.max(0D, ability.getStatData("speed_bonus").getValue());
            EntityUtils.applyAttribute(player, stack, NeoForgeMod.SWIM_SPEED, (float) speedBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }

        @SubscribeEvent
        public static void onLivingIncomingDamageDealing(LivingIncomingDamageEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player) || player.level().isClientSide() || event.getEntity() == player || event.getAmount() <= 0F)
                return;

            if (!event.getSource().is(DamageTypeTags.IS_PROJECTILE))
                return;

            var bonus = 0D;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.HELIUM_FLAMINGO.value())) {
                if (!(stack.getItem() instanceof HeliumFlamingoItem relic) || !relic.isHovering(player, stack))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("flying");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("aerial_archery"))
                    continue;

                bonus = Math.max(bonus, Math.max(0D, ability.getStatData("ranged_damage_bonus").getValue()));
            }

            if (bonus > 0D)
                event.setAmount((float) (event.getAmount() * (1D + bonus)));
        }

        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getAmount() <= 0F)
                return;

            var reduction = 0D;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.HELIUM_FLAMINGO.value())) {
                if (!(stack.getItem() instanceof HeliumFlamingoItem relic) || !relic.isHovering(player, stack))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("flying");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("aerial_guard"))
                    continue;

                reduction = Math.max(reduction, Math.max(0D, Math.min(1D, ability.getStatData("resistance").getValue())));
            }

            if (reduction > 0D)
                event.setAmount((float) Math.max(0D, event.getAmount() * (1D - reduction)));
        }
    }
}


