package it.hurts.octostudios.rarcompat.items.necklace;

import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.init.DataComponentRegistry;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

public class PanicNecklaceItem extends WearableRelicItem {
    private static final double SMOOTH_FACTOR = 0.12D;
    private static final double EPSILON = 0.0005D;

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("panic")
                                .rankModifier(1, "resistance")
                                .rankModifier(3, "healing")
                                .rankModifier(5, "frenzy")
                                .stat(AbilityStatTemplate.builder("movement")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(0.01D, 0.03D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("radius")
                                        .thresholdValue(1D, Double.MAX_VALUE)
                                        .initialValue(8D, 16D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("max_movement")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(0.2D, 0.5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.06D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("heal")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(1D, 4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("resistance")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.1D, 0.25D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("attack_speed")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(0.02D, 0.05D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 1))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("targeted_hit").build())
                                        .source(ExperienceSourceTemplate.builder("targeted_kill")
                                                .rankModifierVisibilityState("healing", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("targeted_duration")
                                                .formatValue(value -> MathUtils.formatTime(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("absorbed_damage")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("resistance", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("healing_restored")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("healing", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.CAVE, LootEntries.MINESHAFT)
                        .build())
                .build();
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (stack.getItem() == newStack.getItem() || !(slotContext.entity() instanceof Player player))
            return;

        setSpeedBonus(stack, 0D);
        EntityUtils.removeAttribute(player, stack, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        EntityUtils.removeAttribute(player, stack, Attributes.ATTACK_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide())
            return;

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("panic");
        var targetBonus = 0D;
        var totalThreats = 0;

        if (ability.canPlayerUse(player)) {
            var radius = Math.max(1D, ability.getStatData("radius").getValue());
            var perMobBonus = Math.max(0D, ability.getStatData("movement").getValue());
            var maxBonus = Math.max(0D, ability.getStatData("max_movement").getValue());
            var targetingMobs = countTargetingMobs(player, radius);

            totalThreats = targetingMobs + countOtherPlayers(player, radius);
            targetBonus = Math.min(targetingMobs * perMobBonus, maxBonus);

            if (targetingMobs > 0 && player.tickCount % 20 == 0)
                ability.getStatisticData().getMetricData("targeted_duration").addValue(1D);
        }

        var currentBonus = getSpeedBonus(stack);
        var nextBonus = Mth.lerp(SMOOTH_FACTOR, currentBonus, targetBonus);

        if (Math.abs(nextBonus - targetBonus) <= EPSILON)
            nextBonus = targetBonus;

        setSpeedBonus(stack, nextBonus);

        if (nextBonus <= EPSILON)
            EntityUtils.removeAttribute(player, stack, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        else
            EntityUtils.resetAttribute(player, stack, Attributes.MOVEMENT_SPEED, (float) nextBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("frenzy")) {
            EntityUtils.removeAttribute(player, stack, Attributes.ATTACK_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            return;
        }

        var attackSpeed = Math.max(0D, ability.getStatData("attack_speed").getValue()) * totalThreats;

        if (attackSpeed <= EPSILON)
            EntityUtils.removeAttribute(player, stack, Attributes.ATTACK_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        else
            EntityUtils.resetAttribute(player, stack, Attributes.ATTACK_SPEED, (float) attackSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private double getSpeedBonus(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.PANIC_NECKLACE_SPEED_BONUS.get(), 0D);
    }

    private void setSpeedBonus(ItemStack stack, double value) {
        stack.set(DataComponentRegistry.PANIC_NECKLACE_SPEED_BONUS.get(), Math.max(0D, value));
    }

    private static int countTargetingMobs(Player player, double radius) {
        return player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius), mob -> mob.isAlive() && mob.getTarget() == player).size();
    }

    private static int countOtherPlayers(Player player, double radius) {
        return player.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(radius), other -> other != player && other.isAlive() && !other.isSpectator()).size();
    }

    private static boolean hasThreatInView(Player player, double radius) {
        var look = player.getLookAngle().normalize();
        var eyePos = player.getEyePosition();

        for (var mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius), mob -> mob.isAlive() && mob.getTarget() == player)) {
            if (isInView(player, look.x, look.y, look.z, eyePos.x, eyePos.y, eyePos.z, mob))
                return true;
        }

        for (var other : player.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(radius), other -> other != player && other.isAlive() && !other.isSpectator())) {
            if (isInView(player, look.x, look.y, look.z, eyePos.x, eyePos.y, eyePos.z, other))
                return true;
        }

        return false;
    }

    private static boolean isInView(Player viewer, double lookX, double lookY, double lookZ, double eyeX, double eyeY, double eyeZ, Entity target) {
        var toTarget = target.getEyePosition().subtract(eyeX, eyeY, eyeZ);

        if (toTarget.lengthSqr() <= 1.0E-6D)
            return true;

        var normalized = toTarget.normalize();

        return lookX * normalized.x + lookY * normalized.y + lookZ * normalized.z > 0D && viewer.hasLineOfSight(target);
    }

    private static boolean isThreatForPlayer(Entity threat, Player player, double radius) {
        if (threat == player || threat.distanceToSqr(player) > radius * radius)
            return false;

        if (threat instanceof Mob mob)
            return mob.getTarget() == player;

        return threat instanceof Player other && !other.isSpectator();
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getAmount() <= 0F)
                return;

            var source = event.getSource().getEntity();
            var fromTargetingMob = source instanceof Mob mob && mob.getTarget() == player;
            var resistance = 0D;
            PanicNecklaceItem resistanceRelic = null;
            ItemStack resistanceStack = ItemStack.EMPTY;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.PANIC_NECKLACE.value())) {
                if (!(stack.getItem() instanceof PanicNecklaceItem relic))
                    continue;

                var relicData = relic.getRelicData(player, stack);
                var ability = relicData.getAbilitiesData().getAbilityData("panic");

                if (!ability.canPlayerUse(player))
                    continue;

                if (fromTargetingMob)
                    relicData.getLevelingData().addExperience("panic", "targeted_hit", 1D);

                if (!ability.isRankModifierUnlocked("resistance"))
                    continue;

                if (hasThreatInView(player, Math.max(1D, ability.getStatData("radius").getValue())))
                    continue;

                var value = Math.max(0D, Math.min(1D, ability.getStatData("resistance").getValue()));

                if (value > resistance) {
                    resistance = value;
                    resistanceRelic = relic;
                    resistanceStack = stack;
                }
            }

            if (resistance <= 0D)
                return;

            var baseDamage = event.getAmount();
            var reducedDamage = (float) (baseDamage * (1D - resistance));
            var absorbed = Math.max(0F, baseDamage - reducedDamage);

            event.setAmount(reducedDamage);

            if (resistanceRelic != null && absorbed > 0F) {
                var ability = resistanceRelic.getRelicData(player, resistanceStack).getAbilitiesData().getAbilityData("panic");
                ability.getStatisticData().getMetricData("absorbed_damage").addValue(absorbed);
            }
        }

        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            if (event.getEntity().level().isClientSide())
                return;

            var threat = event.getEntity();

            if (!(threat instanceof Mob) && !(threat instanceof Player))
                return;

            for (var player : threat.level().players()) {
                if (!player.isAlive() || player == threat)
                    continue;

                var heal = 0D;
                PanicNecklaceItem healingRelic = null;
                ItemStack healingStack = ItemStack.EMPTY;

                for (var stack : EntityUtils.findEquippedCurios(player, ModItems.PANIC_NECKLACE.value())) {
                    if (!(stack.getItem() instanceof PanicNecklaceItem relic))
                        continue;

                    var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("panic");

                    if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("healing"))
                        continue;

                    var radius = Math.max(1D, ability.getStatData("radius").getValue());

                    if (!isThreatForPlayer(threat, player, radius))
                        continue;

                    var value = Math.max(0D, ability.getStatData("heal").getValue());

                    if (value > heal) {
                        heal = value;
                        healingRelic = relic;
                        healingStack = stack;
                    }
                }

                if (healingRelic == null)
                    continue;

                if (threat instanceof Mob)
                    healingRelic.getRelicData(player, healingStack).getLevelingData().addExperience("panic", "targeted_kill", 1D);

                if (heal <= 0D)
                    continue;

                var beforeHealth = player.getHealth();

                player.heal((float) heal);

                var restored = Math.max(0F, player.getHealth() - beforeHealth);

                if (restored > 0F) {
                    var ability = healingRelic.getRelicData(player, healingStack).getAbilitiesData().getAbilityData("panic");
                    ability.getStatisticData().getMetricData("healing_restored").addValue(restored);
                }
            }
        }
    }
}