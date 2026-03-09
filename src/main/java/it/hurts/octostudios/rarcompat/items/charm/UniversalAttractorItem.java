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

    private static final double ITEM_FORCE = 0.08D;
    private static final double XP_FORCE = 0.12D;
    private static final double PROJECTILE_FORCE = 0.2D;

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
            for (var item : items)
                teleportItem(player, item, radius, pull);
        } else {
            for (var item : items)
                applyDirectionalForce(item, player, radius, ITEM_FORCE, 1.0D, pull);
        }

        if (pull && ability.isRankModifierUnlocked("experience")) {
            var orbs = player.level().getEntitiesOfClass(ExperienceOrb.class, player.getBoundingBox().inflate(radius), Entity::isAlive);

            for (var orb : orbs)
                applyDirectionalForce(orb, player, radius, XP_FORCE, 1.0D, true);
        }

        if (!pull && ability.isRankModifierUnlocked("projectiles")) {
            var projectiles = player.level().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(radius), projectile -> projectile.isAlive() && isHostileProjectile(player, projectile));

            for (var projectile : projectiles)
                applyDirectionalForce(projectile, player, radius, PROJECTILE_FORCE, 2.5D, false);
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
        var fromPlayer = item.position().subtract(player.position());
        var direction = fromPlayer.lengthSqr() > 1.0E-6D ? fromPlayer.normalize() : randomHorizontalDirection(player);
        var target = pull
                ? player.position().add(0D, 0.3D, 0D)
                : player.position().add(direction.scale(Math.max(1D, radius)));

        item.teleportTo(target.x(), target.y(), target.z());
        item.setDeltaMovement(Vec3.ZERO);
        item.hasImpulse = true;
    }

    private static void applyDirectionalForce(Entity entity, Player player, double radius, double baseForce, double maxSpeed, boolean pull) {
        var delta = pull ? player.position().subtract(entity.position()) : entity.position().subtract(player.position());
        var distanceSqr = delta.lengthSqr();

        if (distanceSqr < 1.0E-6D)
            return;

        var distance = Math.sqrt(distanceSqr);
        var normalized = delta.scale(1D / distance);
        var distanceFactor = 1D - Mth.clamp(distance / Math.max(1D, radius), 0D, 1D);
        var force = baseForce * (0.35D + distanceFactor * 0.65D);

        var velocity = entity.getDeltaMovement().add(normalized.scale(force));
        var speed = velocity.length();

        if (speed > maxSpeed)
            velocity = velocity.scale(maxSpeed / speed);

        entity.setDeltaMovement(velocity);
        entity.hasImpulse = true;
    }

    private static Vec3 randomHorizontalDirection(Player player) {
        var angle = player.getRandom().nextDouble() * Math.PI * 2D;
        return new Vec3(Math.cos(angle), 0D, Math.sin(angle));
    }
}




