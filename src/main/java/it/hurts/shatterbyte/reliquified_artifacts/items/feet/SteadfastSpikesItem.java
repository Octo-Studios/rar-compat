package it.hurts.shatterbyte.reliquified_artifacts.items.feet;

import it.hurts.sskirillss.relics.api.events.utility.LivingSlippingEvent;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import artifacts.registry.ModItems;
import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
import it.hurts.shatterbyte.reliquified_artifacts.items.base.RAWearableRelicItem;
import it.hurts.shatterbyte.reliquified_artifacts.network.packets.SteadfastSpikesPacket;
import it.hurts.sskirillss.relics.api.relics.AbilityMetricTemplate;
import it.hurts.sskirillss.relics.api.relics.AbilityStatisticTemplate;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.ExperienceSourceTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.ExperienceSourcesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import top.theillusivec4.curios.api.SlotContext;

public class SteadfastSpikesItem extends RAWearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("resistance")
                                .rankModifier(3, "crouch")
                                .rankModifier(5, "anchor")
                                .stat(AbilityStatTemplate.builder("modifier")
                                        .initialValue(0.15D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0531D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("damage_taken").build())
                                        .build())
                                .build())
                        .ability(AbilityTemplate.builder("wall_slide")
                                .requiredLevel(5)
                                .modes("enabled", "disabled")
                                .rankModifier(1, "damage_resistance")
                                .stat(AbilityStatTemplate.builder("damage_resistance")
                                        .initialValue(0.15D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0531D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("wall_slide_time").build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("wall_slide_time")
                                                .formatValue(value -> MathUtils.formatTime(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .build())
                                .build())
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.MOUNTAIN, LootEntries.CAVE)
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player))
            return;

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("wall_slide");

        if (!ability.canPlayerUse(player) || !ability.getMode().equals("enabled") || !player.level().isClientSide() || !isWallSliding(player))
            return;

        NetworkHandler.sendToServer(new SteadfastSpikesPacket());

        player.setDeltaMovement(player.getDeltaMovement().x, -0.05D, player.getDeltaMovement().z);
    }

    public static boolean isStandingStill(Player player) {
        if (!player.onGround() && !player.isInFluidType())
            return false;

        if (player.getKnownMovement().horizontalDistanceSqr() > 1.0E-6D)
            return false;

        return player.getDeltaMovement().horizontalDistanceSqr() <= 1.0E-4D;
    }

    public static boolean isAnchorActive(Player player) {
        var stack = EntityUtils.findEquippedCurio(player, ModItems.STEADFAST_SPIKES.value());

        if (!(stack.getItem() instanceof SteadfastSpikesItem relic))
            return false;

        var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("resistance");

        return ability.canPlayerUse(player) && ability.getRankModifierData("anchor").isEnabled() && isStandingStill(player);
    }

    private static double getEffectiveResistance(Player player, double baseResistance, boolean crouchUnlocked, boolean anchorUnlocked) {
        var resistance = Math.max(0D, Math.min(1D, baseResistance));

        if (crouchUnlocked && player.isCrouching())
            resistance = Math.min(1D, resistance * 2D);

        if (anchorUnlocked && isStandingStill(player))
            resistance = 1D;

        return resistance;
    }

    private static boolean isWallSliding(Player player) {
        return !player.onGround() && player.horizontalCollision && player.getDeltaMovement().y < -1.0E-3D;
    }

    @EventBusSubscriber(modid = ReliquifiedArtifacts.MODID)
    public static class SteadfastSpikesEvent {
        @SubscribeEvent
        public static void onLivingDamagePost(LivingDamageEvent.Post event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getNewDamage() <= 0F)
                return;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.STEADFAST_SPIKES.value())) {
                if (!(stack.getItem() instanceof SteadfastSpikesItem relic))
                    continue;

                var relicData = relic.getRelicData(player, stack);
                var ability = relicData.getAbilitiesData().getAbilityData("resistance");

                if (!ability.canPlayerUse(player))
                    continue;

                relicData.getLevelingData().addExperience("resistance", "damage_taken", 1D);
            }
        }
        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getAmount() <= 0F || !isWallSliding(player))
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.STEADFAST_SPIKES.value());

            if (!(stack.getItem() instanceof SteadfastSpikesItem relic))
                return;

            var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("wall_slide");

            if (!ability.canPlayerUse(player) || !ability.getMode().equals("enabled") || !ability.getRankModifierData("damage_resistance").isEnabled())
                return;

            var resistance = Math.max(0D, Math.min(1D, ability.getStatData("damage_resistance").getValue()));

            if (resistance <= 0D)
                return;

            event.setAmount((float) (event.getAmount() * (1D - resistance)));
        }

        @SubscribeEvent
        public static void onLivingKnockBack(LivingKnockBackEvent event) {
            if (!(event.getEntity() instanceof Player player))
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.STEADFAST_SPIKES.value());

            if (!(stack.getItem() instanceof SteadfastSpikesItem relic))
                return;

            var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("resistance");

            if (!ability.canPlayerUse(player))
                return;

            var resistance = getEffectiveResistance(player,
                    ability.getStatData("modifier").getValue(),
                    ability.getRankModifierData("crouch").isEnabled(),
                    ability.getRankModifierData("anchor").isEnabled());

            if (resistance <= 0D)
                return;

            event.setStrength((float) Math.max(0D, event.getStrength() * (1D - resistance)));

            if (resistance >= 0.999D)
                event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onLivingSlipping(LivingSlippingEvent event) {
            if (event.getFriction() <= 0.6F || !(event.getEntity() instanceof Player player)
                    || player.isInWater() || player.isInLava() || player.isSwimming())
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.STEADFAST_SPIKES.value());

            if (!(stack.getItem() instanceof SteadfastSpikesItem relic))
                return;

            var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("resistance");

            if (!ability.canPlayerUse(player))
                return;

            var resistance = getEffectiveResistance(player,
                    ability.getStatData("modifier").getValue(),
                    ability.getRankModifierData("crouch").isEnabled(),
                    ability.getRankModifierData("anchor").isEnabled());

            if (resistance <= 0D)
                return;

            var currentFriction = event.getFriction();
            var targetFriction = currentFriction - (currentFriction - 0.6F) * resistance;

            event.setFriction((float) Math.max(0.6D, targetFriction));
        }
    }
}

