package it.hurts.octostudios.rarcompat.items.hat;

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
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import top.theillusivec4.curios.api.SlotContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WhoopeeCushionItem extends WearableRelicItem {
    private static final Map<ResourceKey<Level>, Map<UUID, CloudContext>> ACTIVE_CLOUDS = new HashMap<>();

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("push")
                                .rankModifier(1, "retaliation")
                                .rankModifier(3, "panic")
                                .rankModifier(5, "toxic_cloud")
                                .stat(AbilityStatTemplate.builder("chance")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.1D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("distance")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(2D, 6D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("retaliation_chance")
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

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("push");

        if (!ability.canPlayerUse(player)) {
            stack.set(DataComponentRegistry.TOGGLED, false);
            return;
        }

        var crouching = player.isCrouching();
        var wasCrouching = stack.getOrDefault(DataComponentRegistry.TOGGLED, false);

        if (crouching && !wasCrouching) {
            var chance = Math.max(0D, Math.min(1D, ability.getStatData("chance").getValue()));

            if (chance > 0D && player.getRandom().nextDouble() <= chance)
                activateAbility(player, stack);
        }

        stack.set(DataComponentRegistry.TOGGLED, crouching);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (stack.getItem() == newStack.getItem())
            return;

        stack.set(DataComponentRegistry.TOGGLED, false);
    }

    private void activateAbility(Player player, ItemStack stack) {
        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("push");

        if (!ability.canPlayerUse(player))
            return;

        var distance = Math.max(0D, ability.getStatData("distance").getValue());

        if (distance <= 0D)
            return;

        var level = player.level();

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1F,
                0.9F + player.getRandom().nextFloat() * 0.2F);

        var maxDistanceSq = distance * distance;
        var strength = Math.max(0.2D, distance * 0.35D);

        for (var target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(distance), entity -> entity.isAlive() && entity != player)) {
            if (target.distanceToSqr(player) > maxDistanceSq)
                continue;

            target.knockback(strength, player.getX() - target.getX(), player.getZ() - target.getZ());

            if (ability.isRankModifierUnlocked("panic") && target instanceof PathfinderMob mob)
                forceFleeFromPlayer(mob, player);
        }

        if (ability.isRankModifierUnlocked("toxic_cloud"))
            spawnToxicCloud(player, Math.max(1.5F, (float) Math.min(6D, distance * 0.75D)));
    }

    private static void forceFleeFromPlayer(PathfinderMob mob, Player player) {
        if (mob.getTarget() == player)
            mob.setTarget(null);

        var awayPos = DefaultRandomPos.getPosAway(mob, 12, 6, player.position());

        if (awayPos != null)
            mob.getNavigation().moveTo(awayPos.x, awayPos.y, awayPos.z, 1.2D);
    }

    private static void spawnToxicCloud(Player player, float radius) {
        var level = player.level();
        var cloud = new AreaEffectCloud(level, player.getX(), player.getY(), player.getZ());

        cloud.setOwner(player);
        cloud.setRadius(Math.max(0.5F, radius));
        cloud.setWaitTime(0);
        cloud.setDuration(120);
        cloud.setRadiusOnUse(0F);
        cloud.setDurationOnUse(0);

        level.addFreshEntity(cloud);

        ACTIVE_CLOUDS.computeIfAbsent(level.dimension(), key -> new HashMap<>())
                .put(cloud.getUUID(), new CloudContext(player.getUUID(), level.getGameTime()));
    }

    private static class CloudContext {
        private final UUID owner;
        private long nextApplyTick;

        private CloudContext(UUID owner, long nextApplyTick) {
            this.owner = owner;
            this.nextApplyTick = nextApplyTick;
        }
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingDamagePost(LivingDamageEvent.Post event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getNewDamage() <= 0F)
                return;

            var chance = 0D;
            WhoopeeCushionItem selectedRelic = null;
            ItemStack selectedStack = ItemStack.EMPTY;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.WHOOPEE_CUSHION.value())) {
                if (!(stack.getItem() instanceof WhoopeeCushionItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("push");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("retaliation"))
                    continue;

                var value = Math.max(0D, Math.min(1D, ability.getStatData("retaliation_chance").getValue()));

                if (value <= chance)
                    continue;

                chance = value;
                selectedRelic = relic;
                selectedStack = stack;
            }

            if (selectedRelic == null || chance <= 0D || player.getRandom().nextDouble() > chance)
                return;

            selectedRelic.activateAbility(player, selectedStack);
        }

        @SubscribeEvent
        public static void onLevelTickPost(LevelTickEvent.Post event) {
            if (!(event.getLevel() instanceof ServerLevel level))
                return;

            var clouds = ACTIVE_CLOUDS.get(level.dimension());

            if (clouds == null || clouds.isEmpty())
                return;

            var iterator = clouds.entrySet().iterator();
            var now = level.getGameTime();

            while (iterator.hasNext()) {
                var entry = iterator.next();
                var entity = level.getEntity(entry.getKey());

                if (!(entity instanceof AreaEffectCloud cloud) || !cloud.isAlive()) {
                    iterator.remove();
                    continue;
                }

                var context = entry.getValue();
                var owner = level.getServer() == null ? null : level.getServer().getPlayerList().getPlayer(context.owner);

                if (owner == null || !owner.isAlive()) {
                    iterator.remove();
                    continue;
                }

                var radius = Math.max(0.5D, cloud.getRadius());
                var maxDistanceSq = radius * radius;
                var applyEffects = now >= context.nextApplyTick;

                if (applyEffects)
                    context.nextApplyTick = now + 5L;

                for (var target : level.getEntitiesOfClass(LivingEntity.class, cloud.getBoundingBox().inflate(0.1D), living -> living.isAlive() && living != owner)) {
                    var dx = target.getX() - cloud.getX();
                    var dz = target.getZ() - cloud.getZ();

                    if (dx * dx + dz * dz > maxDistanceSq)
                        continue;

                    if (target instanceof Mob mob && mob.getTarget() == owner)
                        mob.setTarget(null);

                    if (!applyEffects)
                        continue;

                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, true), cloud);
                    target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, true), cloud);
                }
            }

            if (clouds.isEmpty())
                ACTIVE_CLOUDS.remove(level.dimension());
        }
    }
}
