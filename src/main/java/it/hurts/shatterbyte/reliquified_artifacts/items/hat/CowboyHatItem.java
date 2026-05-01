package it.hurts.shatterbyte.reliquified_artifacts.items.hat;

import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import artifacts.registry.ModItems;
import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
import it.hurts.shatterbyte.reliquified_artifacts.init.RADataComponent;
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
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import top.theillusivec4.curios.api.SlotContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CowboyHatItem extends RAWearableRelicItem {
    private static final int NO_MOUNT = -1;
    private static final Map<UUID, Vec3> LAST_MOUNT_POSITIONS = new HashMap<>();

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("riding")
                                .rankModifier(1, "taming")
                                .rankModifier(3, "reach")
                                .rankModifier(5, "mounted_absorption")
                                .stat(AbilityStatTemplate.builder("amount")
                                        .initialValue(0.1D, 0.25D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.2D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("reach")
                                        .initialValue(0.15D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.3796D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("absorption")
                                        .initialValue(2D, 6D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0667D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("mounted_distance").build())
                                        .source(ExperienceSourceTemplate.builder("tamed_mount")
                                                .rankModifierVisibilityState("taming", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("mounted_distance")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("tamed_mounts")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("taming", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("absorbed_damage")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("mounted_absorption", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.SAVANNA, LootEntries.VILLAGE)
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide())
            return;

        var relicData = this.getRelicData(player, stack);
        var ability = relicData.getAbilitiesData().getAbilityData("riding");

        if (!ability.canPlayerUse(player)) {
            clearMountBuff(player, stack);
            removeRiderReachBuff(player, stack);
            removeRiderAbsorptionBonus(player, stack);
            clearDistanceTracking(player);
            return;
        }

        if (!(player.getVehicle() instanceof LivingEntity mounted)) {
            clearMountBuff(player, stack);
            removeRiderReachBuff(player, stack);
            removeRiderAbsorptionBonus(player, stack);
            clearDistanceTracking(player);
            return;
        }

        var mountedChanged = syncMountedEntity(player, stack, mounted);

        trackMountedDistance(player, stack, mounted, mountedChanged);

        var amount = Math.max(0F, (float) ability.getStatData("amount").getValue());

        if (amount > 0F) {
            applyMountedBuff(stack, mounted, amount);
        } else {
            removeMountedBuff(stack, mounted);
        }

        if (!ability.getRankModifierData("reach").isUnlocked()) {
            removeRiderReachBuff(player, stack);
        } else {
            var reach = Math.max(0F, (float) ability.getStatData("reach").getValue());

            if (reach > 0F)
                applyRiderReachBuff(player, stack, reach);
            else
                removeRiderReachBuff(player, stack);
        }

        if (!ability.getRankModifierData("mounted_absorption").isUnlocked()) {
            removeRiderAbsorptionBonus(player, stack);
            return;
        }

        var absorption = (int) ability.getStatData("absorption").getValue();

        if (absorption > 0D) {
            var previousRemaining = getRiderAbsorptionRemaining(stack);

            applyRiderAbsorptionBonus(player, stack, absorption, mountedChanged);

            var consumed = Math.max(0D, previousRemaining - getRiderAbsorptionRemaining(stack));

            if (consumed > 0D)
                ability.getStatisticData().getMetricData("absorbed_damage").addValue(consumed);
        } else {
            removeRiderAbsorptionBonus(player, stack);
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide() || stack.getItem() == newStack.getItem())
            return;

        clearMountBuff(player, stack);
        removeRiderReachBuff(player, stack);
        removeRiderAbsorptionBonus(player, stack);
        clearDistanceTracking(player);
    }

    private void trackMountedDistance(Player player, ItemStack stack, LivingEntity mounted, boolean mountedChanged) {
        var playerId = player.getUUID();
        var currentPos = mounted.position();

        if (mountedChanged || !LAST_MOUNT_POSITIONS.containsKey(playerId)) {
            LAST_MOUNT_POSITIONS.put(playerId, currentPos);
            return;
        }

        var previousPos = LAST_MOUNT_POSITIONS.get(playerId);

        LAST_MOUNT_POSITIONS.put(playerId, currentPos);

        if (previousPos == null)
            return;

        var distance = previousPos.distanceTo(currentPos);

        if (distance <= 1.0E-4D)
            return;

        var relicData = this.getRelicData(player, stack);
        var ability = relicData.getAbilitiesData().getAbilityData("riding");

        ability.getStatisticData().getMetricData("mounted_distance").addValue(distance);

        var remainder = getRidingDistanceRemainder(stack) + distance;
        var experience = (int) Math.floor(remainder / 10D);

        if (experience > 0) {
            relicData.getLevelingData().addExperience("riding", "mounted_distance", experience);
            remainder -= experience * 10D;
        }

        setRidingDistanceRemainder(stack, remainder);
    }

    private void clearDistanceTracking(Player player) {
        LAST_MOUNT_POSITIONS.remove(player.getUUID());
    }

    private boolean syncMountedEntity(Player player, ItemStack stack, LivingEntity mounted) {
        var lastMountId = getLastMountId(stack);

        if (lastMountId == mounted.getId())
            return false;

        removeMountBuffById(player, stack, lastMountId);
        setLastMountId(stack, mounted.getId());
        return true;
    }

    private void clearMountBuff(Player player, ItemStack stack) {
        removeMountBuffById(player, stack, getLastMountId(stack));
        setLastMountId(stack, NO_MOUNT);
    }

    private void removeMountBuffById(Player player, ItemStack stack, int entityId) {
        if (entityId == NO_MOUNT)
            return;

        if (player.level().getEntity(entityId) instanceof LivingEntity mounted)
            removeMountedBuff(stack, mounted);
    }

    private void applyMountedBuff(ItemStack stack, LivingEntity mounted, float amount) {
        resetMountedMaxHealthKeepingRatio(mounted, stack, amount);
        resetLivingAttribute(mounted, stack, Attributes.MOVEMENT_SPEED, amount);
        resetLivingAttribute(mounted, stack, Attributes.JUMP_STRENGTH, amount);
        resetLivingAttribute(mounted, stack, Attributes.ATTACK_DAMAGE, amount);
        resetLivingAttribute(mounted, stack, Attributes.ATTACK_KNOCKBACK, amount);
        resetLivingAttribute(mounted, stack, Attributes.FLYING_SPEED, amount);
        resetLivingAttribute(mounted, stack, Attributes.ARMOR, amount);
        resetLivingAttribute(mounted, stack, Attributes.ARMOR_TOUGHNESS, amount);
        resetLivingAttribute(mounted, stack, Attributes.KNOCKBACK_RESISTANCE, amount);
    }

    private void removeMountedBuff(ItemStack stack, LivingEntity mounted) {
        removeMountedMaxHealthKeepingRatio(mounted, stack);
        removeLivingAttribute(mounted, stack, Attributes.MOVEMENT_SPEED);
        removeLivingAttribute(mounted, stack, Attributes.JUMP_STRENGTH);
        removeLivingAttribute(mounted, stack, Attributes.ATTACK_DAMAGE);
        removeLivingAttribute(mounted, stack, Attributes.ATTACK_KNOCKBACK);
        removeLivingAttribute(mounted, stack, Attributes.FLYING_SPEED);
        removeLivingAttribute(mounted, stack, Attributes.ARMOR);
        removeLivingAttribute(mounted, stack, Attributes.ARMOR_TOUGHNESS);
        removeLivingAttribute(mounted, stack, Attributes.KNOCKBACK_RESISTANCE);
    }

    private void resetMountedMaxHealthKeepingRatio(LivingEntity mounted, ItemStack stack, float amount) {
        var oldMaxHealth = Math.max(1.0E-4F, mounted.getMaxHealth());
        var healthRatio = Math.max(0D, Math.min(1D, mounted.getHealth() / oldMaxHealth));

        resetLivingAttribute(mounted, stack, Attributes.MAX_HEALTH, amount);

        var newMaxHealth = mounted.getMaxHealth();

        if (newMaxHealth <= 0F)
            return;

        mounted.setHealth((float) Math.max(0F, Math.min(newMaxHealth, newMaxHealth * healthRatio)));
    }

    private void removeMountedMaxHealthKeepingRatio(LivingEntity mounted, ItemStack stack) {
        var oldMaxHealth = Math.max(1.0E-4F, mounted.getMaxHealth());
        var healthRatio = Math.max(0D, Math.min(1D, mounted.getHealth() / oldMaxHealth));

        removeLivingAttribute(mounted, stack, Attributes.MAX_HEALTH);

        var newMaxHealth = mounted.getMaxHealth();

        if (newMaxHealth <= 0F)
            return;

        mounted.setHealth((float) Math.max(0F, Math.min(newMaxHealth, newMaxHealth * healthRatio)));
    }

    private void applyRiderReachBuff(Player player, ItemStack stack, float amount) {
        resetLivingAttribute(player, stack, Attributes.ENTITY_INTERACTION_RANGE, amount);
        resetLivingAttribute(player, stack, Attributes.BLOCK_INTERACTION_RANGE, amount);
    }

    private void removeRiderReachBuff(Player player, ItemStack stack) {
        removeLivingAttribute(player, stack, Attributes.ENTITY_INTERACTION_RANGE);
        removeLivingAttribute(player, stack, Attributes.BLOCK_INTERACTION_RANGE);
    }

    private void applyRiderAbsorptionBonus(Player player, ItemStack stack, double bonus, boolean grantOnMount) {
        setRiderMaxAbsorption(player, stack, bonus);

        if (grantOnMount) {
            player.setAbsorptionAmount((float) (Math.max(0D, player.getAbsorptionAmount()) + bonus));
            setRiderAbsorptionRemaining(stack, getRiderAbsorptionRemaining(stack) + bonus);
        }

        var remaining = getRiderAbsorptionRemaining(stack);
        var current = Math.max(0D, player.getAbsorptionAmount());

        setRiderAbsorptionRemaining(stack, Math.min(remaining, current));
    }

    private void removeRiderAbsorptionBonus(Player player, ItemStack stack) {
        var remaining = getRiderAbsorptionRemaining(stack);
        var current = Math.max(0D, player.getAbsorptionAmount());

        removeRiderMaxAbsorption(player, stack);

        if (remaining > 0D)
            player.setAbsorptionAmount((float) Math.max(0D, current - remaining));

        setRiderAbsorptionRemaining(stack, 0D);
    }

    private void setRiderMaxAbsorption(Player player, ItemStack stack, double value) {
        if (player.getAttribute(Attributes.MAX_ABSORPTION) != null)
            EntityUtils.resetAttribute(player, stack, Attributes.MAX_ABSORPTION, (float) Math.max(0D, value), AttributeModifier.Operation.ADD_VALUE);
    }

    private void removeRiderMaxAbsorption(Player player, ItemStack stack) {
        if (player.getAttribute(Attributes.MAX_ABSORPTION) != null)
            EntityUtils.removeAttribute(player, stack, Attributes.MAX_ABSORPTION, AttributeModifier.Operation.ADD_VALUE);
    }

    private void resetLivingAttribute(LivingEntity entity, ItemStack stack, Holder<Attribute> attribute, float amount) {
        if (entity.getAttribute(attribute) != null)
            EntityUtils.resetAttribute(entity, stack, attribute, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private void removeLivingAttribute(LivingEntity entity, ItemStack stack, Holder<Attribute> attribute) {
        if (entity.getAttribute(attribute) != null)
            EntityUtils.removeAttribute(entity, stack, attribute, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private int getLastMountId(ItemStack stack) {
        return stack.getOrDefault(RADataComponent.COWBOY_HAT_LAST_MOUNT_ID.get(), NO_MOUNT);
    }

    private void setLastMountId(ItemStack stack, int id) {
        stack.set(RADataComponent.COWBOY_HAT_LAST_MOUNT_ID.get(), id);
    }

    private double getRiderAbsorptionRemaining(ItemStack stack) {
        return stack.getOrDefault(RADataComponent.COWBOY_HAT_ABSORPTION_REMAINING.get(), 0D);
    }

    private void setRiderAbsorptionRemaining(ItemStack stack, double value) {
        stack.set(RADataComponent.COWBOY_HAT_ABSORPTION_REMAINING.get(), Math.max(0D, value));
    }

    private double getRidingDistanceRemainder(ItemStack stack) {
        return stack.getOrDefault(RADataComponent.COWBOY_HAT_RIDING_DISTANCE_REMAINDER.get(), 0D);
    }

    private void setRidingDistanceRemainder(ItemStack stack, double value) {
        stack.set(RADataComponent.COWBOY_HAT_RIDING_DISTANCE_REMAINDER.get(), Math.max(0D, value));
    }

    @EventBusSubscriber(modid = ReliquifiedArtifacts.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
            tryInstantTame(event.getEntity(), event.getTarget());
        }

        @SubscribeEvent
        public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
            tryInstantTame(event.getEntity(), event.getTarget());
        }

        private static void tryInstantTame(Player player, Entity target) {
            if (player.level().isClientSide() || !(target instanceof AbstractHorse horse) || horse.isTamed())
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.COWBOY_HAT.value());

            if (!(stack.getItem() instanceof CowboyHatItem relic))
                return;

            var relicData = relic.getRelicData(player, stack);
            var ability = relicData.getAbilitiesData().getAbilityData("riding");

            if (!ability.canPlayerUse(player) || !ability.getRankModifierData("taming").isUnlocked())
                return;

            horse.tameWithName(player);

            if (horse.isTamed()) {
                ability.getStatisticData().getMetricData("tamed_mounts").addValue(1D);
                relicData.getLevelingData().addExperience("riding", "tamed_mount", 1D);
            }
        }
    }
}

