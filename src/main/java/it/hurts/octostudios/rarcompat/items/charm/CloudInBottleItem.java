package it.hurts.octostudios.rarcompat.items.charm;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.init.DataComponentRegistry;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.octostudios.rarcompat.network.packets.DoubleJumpPacket;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

public class CloudInBottleItem extends WearableRelicItem {

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("jump")
                                .rankModifier(1, "slow_fall")
                                .rankModifier(3, "updraft")
                                .rankModifier(5, "combat_recovery")
                                .stat(AbilityStatTemplate.builder("count")
                                        .thresholdValue(1D, Double.MAX_VALUE)
                                        .initialValue(1D, 4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("slow_fall_duration")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(1D, 4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("updraft_bonus")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.1D, 0.35D)
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

        if (player.onGround()) {
            setCount(stack, 0);
            return;
        }

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("jump");

        if (!ability.canPlayerUse(player)) {
            setCount(stack, 0);
            return;
        }

        var maxJumps = getMaxJumps(player, stack);

        if (getCount(stack) > maxJumps)
            setCount(stack, maxJumps);
    }

    public boolean canAirJump(Player player, ItemStack stack) {
        if (player == null || player.onGround() || player.isFallFlying() || player.getAbilities().flying)
            return false;

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("jump");

        if (!ability.canPlayerUse(player))
            return false;

        return getCount(stack) < getMaxJumps(player, stack);
    }

    public boolean performAirJump(Player player, ItemStack stack) {
        if (!canAirJump(player, stack))
            return false;

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("jump");
        var previousY = player.getDeltaMovement().y;

        addCount(stack, 1);

        player.jumpFromGround();
        player.hasImpulse = true;
        player.fallDistance = 0F;

        if (ability.isRankModifierUnlocked("updraft") && player.getLookAngle().y > 0D) {
            var bonus = Math.max(0D, ability.getStatData("updraft_bonus").getValue());
            var currentMotion = player.getDeltaMovement();
            var jumpImpulse = currentMotion.y - previousY;

            if (bonus > 0D && jumpImpulse > 0D)
                player.setDeltaMovement(currentMotion.x, previousY + jumpImpulse * (1D + bonus), currentMotion.z);
        }

        if (!player.level().isClientSide() && ability.isRankModifierUnlocked("slow_fall")) {
            var slowFallTicks = secondsToTicks(ability.getStatData("slow_fall_duration").getValue());

            if (slowFallTicks > 0)
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, slowFallTicks, 0, false, false));
        }

        return true;
    }

    public int getMaxJumps(Player player, ItemStack stack) {
        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("jump");

        return Math.max(0, (int) MathUtils.round(ability.getStatData("count").getValue(), 0));
    }

    public void restoreOneJump(ItemStack stack) {
        setCount(stack, Math.max(0, getCount(stack) - 1));
    }

    public void addCount(ItemStack stack, int amount) {
        setCount(stack, getCount(stack) + amount);
    }

    public int getCount(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(DataComponentRegistry.COUNT, 0));
    }

    public void setCount(ItemStack stack, int val) {
        stack.set(DataComponentRegistry.COUNT, Math.max(val, 0));
    }

    private static int secondsToTicks(double seconds) {
        return Math.max(0, (int) Math.round(Math.max(0D, seconds) * 20D));
    }

    @EventBusSubscriber(value = Dist.CLIENT)
    public static class ClientEvent {
        @SubscribeEvent
        public static void onMouseInput(InputEvent.Key event) {
            var minecraft = Minecraft.getInstance();
            var player = minecraft.player;

            if (player == null)
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.CLOUD_IN_A_BOTTLE.value());

            if (minecraft.screen != null || event.getAction() != 1 || !(stack.getItem() instanceof CloudInBottleItem relic)
                    || event.getKey() != minecraft.options.keyJump.getKey().getValue())
                return;

            if (!relic.performAirJump(player, stack))
                return;

            NetworkHandler.sendToServer(new DoubleJumpPacket());
        }
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamageDealing(LivingIncomingDamageEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player) || player.level().isClientSide() || event.getEntity() == player || event.getAmount() <= 0F)
                return;

            if (player.onGround() || player.isFallFlying())
                return;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.CLOUD_IN_A_BOTTLE.value())) {
                if (!(stack.getItem() instanceof CloudInBottleItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("jump");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("combat_recovery"))
                    continue;

                if (relic.getCount(stack) <= 0)
                    continue;

                relic.restoreOneJump(stack);
                break;
            }
        }
    }
}


