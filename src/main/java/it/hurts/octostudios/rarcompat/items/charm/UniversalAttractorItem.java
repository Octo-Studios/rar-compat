package it.hurts.octostudios.rarcompat.items.charm;

import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import top.theillusivec4.curios.api.SlotContext;

public class UniversalAttractorItem extends WearableRelicItem {
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
                                        .thresholdValue(1D, Double.MAX_VALUE)
                                        .initialValue(4D, 9D)
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

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("magnetism");

        if (!ability.canPlayerUse(player))
            return;

        var mode = ability.getMode();

        if ("disabled".equals(mode))
            return;

        var radius = Math.max(1D, ability.getStatData("radius").getValue());
        var teleportItems = ability.isRankModifierUnlocked("teleport");
        var pull = "pull".equals(mode);

        var items = player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(radius), Entity::isAlive);

        if (teleportItems) {
            for (var item : items) {
                if (shouldDelayItemManipulation(player, item))
                    continue;

                teleportItem(player, item, radius, pull);
            }
        } else {
            for (var item : items) {
                if (shouldDelayItemManipulation(player, item))
                    continue;

                applyDirectionalForce(item, player, radius, 0.08D, 1.0D, pull, !pull);
            }
        }

        if (pull && ability.isRankModifierUnlocked("experience")) {
            var orbs = player.level().getEntitiesOfClass(ExperienceOrb.class, player.getBoundingBox().inflate(radius), Entity::isAlive);

            for (var orb : orbs)
                applyDirectionalForce(orb, player, radius, 0.12D, 1.0D, true, false);
        }

        if (!pull && ability.isRankModifierUnlocked("projectiles")) {
            var projectiles = player.level().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(radius), projectile -> projectile.isAlive() && isHostileProjectile(player, projectile));

            for (var projectile : projectiles)
                applyDirectionalForce(projectile, player, radius, 0.2D, 2.5D, false, false);
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

    private static void teleportItem(Player player, ItemEntity item, double radius, boolean pull) {
        Vec3 target;

        if (pull) {
            target = player.position().add(0D, 0.3D, 0D);
        } else {
            var fromPlayer = item.position().subtract(player.position());
            var horizontal = new Vec3(fromPlayer.x, 0D, fromPlayer.z);
            var direction = horizontal.lengthSqr() > 1.0E-6D ? horizontal.normalize() : horizontalMotionDirection(item);

            if (direction.lengthSqr() <= 1.0E-6D)
                return;

            target = new Vec3(player.getX() + direction.x * Math.max(1D, radius), item.getY(), player.getZ() + direction.z * Math.max(1D, radius));
        }

        item.teleportTo(target.x(), target.y(), target.z());
        item.setDeltaMovement(Vec3.ZERO);
        item.hasImpulse = true;
    }

    private static boolean shouldDelayItemManipulation(Player player, ItemEntity item) {
        if (!item.hasPickUpDelay())
            return false;

        return item.getOwner() == player;
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
}
