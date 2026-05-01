package it.hurts.shatterbyte.reliquified_artifacts.items.charm;

import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import artifacts.registry.ModItems;
import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
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
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import top.theillusivec4.curios.api.SlotContext;

public class UniversalAttractorItem extends RAWearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("magnetism")
                                .modes("pull", "push", "disabled")
                                .rankModifier(1, "experience")
                                .rankModifier(3, "projectiles")
                                .rankModifier(5, "teleport")
                                .stat(AbilityStatTemplate.builder("radius")
                                        .initialValue(3D, 5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0286D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("pulled_item").build())
                                        .source(ExperienceSourceTemplate.builder("pulled_xp")
                                                .rankModifierVisibilityState("experience", VisibilityState.OBFUSCATED)
                                                .build())
                                        .source(ExperienceSourceTemplate.builder("repelled_projectile")
                                                .rankModifierVisibilityState("projectiles", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("pulled_items_picked")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("pulled_xp_picked")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("experience", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("repelled_projectiles")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("projectiles", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.MINESHAFT, LootEntries.CAVE)
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide())
            return;

        var relicData = this.getRelicData(player, stack);
        var ability = relicData.getAbilitiesData().getAbilityData("magnetism");

        if (!ability.canPlayerUse(player))
            return;

        var mode = ability.getMode();

        if ("disabled".equals(mode))
            return;

        var radius = Math.max(1D, ability.getStatData("radius").getValue());
        var pull = "pull".equals(mode);
        var teleportPullItems = pull && ability.getRankModifierData("teleport").isEnabled();

        var items = player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(radius), Entity::isAlive);

        if (teleportPullItems) {
            for (var item : items) {
                if (shouldDelayItemManipulation(player, item))
                    continue;

                markItemPulledByPlayer(player, item);
                teleportItem(player, item);
            }
        } else {
            for (var item : items) {
                if (shouldDelayItemManipulation(player, item))
                    continue;

                if (pull)
                    markItemPulledByPlayer(player, item);

                applyDirectionalForce(item, player, radius, 0.08D, 1.0D, pull, !pull);
            }
        }

        if (pull && ability.getRankModifierData("experience").isEnabled()) {
            var orbs = player.level().getEntitiesOfClass(ExperienceOrb.class, player.getBoundingBox().inflate(radius), Entity::isAlive);

            for (var orb : orbs) {
                markXpOrbPulledByPlayer(player, orb);
                applyDirectionalForce(orb, player, radius, 0.12D, 1.0D, true, false);
            }
        }

        if (!pull && ability.getRankModifierData("projectiles").isEnabled()) {
            var projectiles = player.level().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(radius), projectile -> projectile.isAlive() && isHostileProjectile(player, projectile));

            for (var projectile : projectiles) {
                if (!projectile.getPersistentData().getBoolean("reliquified_artifacts_universal_attractor_repelled_projectile_counted")) {
                    projectile.getPersistentData().putBoolean("reliquified_artifacts_universal_attractor_repelled_projectile_counted", true);
                    projectile.getPersistentData().putUUID("reliquified_artifacts_universal_attractor_repelled_projectile_owner", player.getUUID());
                    relicData.getLevelingData().addExperience("magnetism", "repelled_projectile", 1D);
                    ability.getStatisticData().getMetricData("repelled_projectiles").addValue(1D);
                }

                applyDirectionalForce(projectile, player, radius, 0.2D, 2.5D, false, false);
            }
        }
    }

    private static boolean isHostileProjectile(Player player, Projectile projectile) {
        var owner = projectile.getOwner();

        if (owner == null)
            return true;

        if (owner == player)
            return false;

        return !owner.isAlliedTo(player);
    }

    private static void teleportItem(Player player, ItemEntity item) {
        item.teleportTo(player.getX(), player.getY() + 0.3D, player.getZ());
        item.setDeltaMovement(Vec3.ZERO);
        item.hasImpulse = true;
    }

    private static boolean shouldDelayItemManipulation(Player player, ItemEntity item) {
        if (!item.hasPickUpDelay())
            return false;

        return item.getOwner() == player;
    }

    private static void markItemPulledByPlayer(Player player, ItemEntity item) {
        var data = item.getPersistentData();
        data.putBoolean("reliquified_artifacts_universal_attractor_pulled_item", true);
        data.putUUID("reliquified_artifacts_universal_attractor_pulled_item_owner", player.getUUID());
    }

    private static void markXpOrbPulledByPlayer(Player player, ExperienceOrb orb) {
        var data = orb.getPersistentData();
        data.putBoolean("reliquified_artifacts_universal_attractor_pulled_xp", true);
        data.putUUID("reliquified_artifacts_universal_attractor_pulled_xp_owner", player.getUUID());
    }

    public static void awardPulledItemPickup(Player player, ItemEntity itemEntity) {
        if (player.level().isClientSide())
            return;

        var data = itemEntity.getPersistentData();

        if (!data.getBoolean("reliquified_artifacts_universal_attractor_pulled_item"))
            return;

        if (!data.hasUUID("reliquified_artifacts_universal_attractor_pulled_item_owner") || !player.getUUID().equals(data.getUUID("reliquified_artifacts_universal_attractor_pulled_item_owner")))
            return;

        for (var stack : EntityUtils.findEquippedCurios(player, ModItems.UNIVERSAL_ATTRACTOR.value())) {
            if (!(stack.getItem() instanceof UniversalAttractorItem relic))
                continue;

            var relicData = relic.getRelicData(player, stack);
            var ability = relicData.getAbilitiesData().getAbilityData("magnetism");

            if (!ability.canPlayerUse(player))
                continue;

            relicData.getLevelingData().addExperience("magnetism", "pulled_item", 1D);
            ability.getStatisticData().getMetricData("pulled_items_picked").addValue(1D);

            break;
        }
    }

    private static void applyDirectionalForce(Entity entity, Player player, double radius, double baseForce, double maxSpeed, boolean pull, boolean horizontalOnly) {
        var delta = pull ? player.position().subtract(entity.position()) : entity.position().subtract(player.position());

        if (horizontalOnly)
            delta = new Vec3(delta.x, 0D, delta.z);

        var distanceSqr = delta.lengthSqr();

        if (distanceSqr < 1.0E-6D) {
            if (!horizontalOnly)
                return;

            delta = horizontalMotionDirection(entity);
            distanceSqr = delta.lengthSqr();

            if (distanceSqr < 1.0E-6D)
                return;
        }

        var distance = Math.sqrt(distanceSqr);
        var normalized = delta.scale(1D / distance);

        if (pull && entity instanceof ItemEntity) {
            normalized = new Vec3(normalized.x, normalized.y * 1.35D, normalized.z);

            var normalizedLengthSqr = normalized.lengthSqr();

            if (normalizedLengthSqr > 1.0E-6D)
                normalized = normalized.scale(1D / Math.sqrt(normalizedLengthSqr));
        }

        var proximity = 1D - Mth.clamp(distance / Math.max(1D, radius), 0D, 1D);
        var zoneFactor = 1D;

        if (pull && entity instanceof ItemEntity) {
            if (distance <= 0.75D) {
                var damped = entity.getDeltaMovement().scale(0.5D);

                if (damped.lengthSqr() < 1.0E-4D)
                    damped = Vec3.ZERO;

                entity.setDeltaMovement(damped);
                entity.hasImpulse = true;

                return;
            }

            if (distance < 1.5D) {
                var t = Mth.clamp((distance - 0.75D) / (1.5D - 0.75D), 0D, 1D);
                zoneFactor = 0.4D + t * 0.6D;
            }
        }

        var force = baseForce * (0.1D + proximity * proximity * 1.9D) * zoneFactor;
        var effectiveMaxSpeed = maxSpeed * (0.2D + proximity * 0.8D) * zoneFactor;

        var velocity = entity.getDeltaMovement().add(normalized.scale(force));
        var speed = velocity.length();

        if (speed > effectiveMaxSpeed)
            velocity = velocity.scale(effectiveMaxSpeed / speed);

        entity.setDeltaMovement(velocity);
        entity.hasImpulse = true;
    }

    private static Vec3 horizontalMotionDirection(Entity entity) {
        var motion = entity.getDeltaMovement();
        var horizontalMotion = new Vec3(motion.x, 0D, motion.z);

        if (horizontalMotion.lengthSqr() <= 1.0E-6D)
            return Vec3.ZERO;

        return horizontalMotion.normalize();
    }

    @EventBusSubscriber(modid = ReliquifiedArtifacts.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onItemPickup(ItemEntityPickupEvent.Post event) {
            awardPulledItemPickup(event.getPlayer(), event.getItemEntity());
        }

        @SubscribeEvent
        public static void onXpPickup(PlayerXpEvent.PickupXp event) {
            var player = event.getEntity();

            if (player.level().isClientSide())
                return;

            var data = event.getOrb().getPersistentData();

            if (!data.getBoolean("reliquified_artifacts_universal_attractor_pulled_xp"))
                return;

            if (!data.hasUUID("reliquified_artifacts_universal_attractor_pulled_xp_owner") || !player.getUUID().equals(data.getUUID("reliquified_artifacts_universal_attractor_pulled_xp_owner")))
                return;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.UNIVERSAL_ATTRACTOR.value())) {
                if (!(stack.getItem() instanceof UniversalAttractorItem relic))
                    continue;

                var relicData = relic.getRelicData(player, stack);
                var ability = relicData.getAbilitiesData().getAbilityData("magnetism");

                if (!ability.canPlayerUse(player) || !ability.getRankModifierData("experience").isEnabled())
                    continue;

                relicData.getLevelingData().addExperience("magnetism", "pulled_xp", 1D);
                ability.getStatisticData().getMetricData("pulled_xp_picked").addValue(1D);

                break;
            }
        }
    }
}

