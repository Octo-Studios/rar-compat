package it.hurts.octostudios.rarcompat.items.hands;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.octostudios.rarcompat.utils.MathBaseUtils;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootData;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootCollections;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.TooltipData;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;
import java.util.Comparator;
import java.util.List;

public class FireGauntletItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("caster")
                                .stat(StatData.builder("chance")
                                        .initialValue(0.2D, 0.3D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.15D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())
                                .stat(StatData.builder("damage")
                                        .initialValue(0.05D, 0.15D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.235D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())
                                .stat(StatData.builder("duration")
                                        .initialValue(30D, 50D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.3D)
                                        .formatValue(value -> MathUtils.round(value / 20, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xfffcbc11)
                                .borderBottom(0xffd12e00)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.NETHER)
                        .build())
                .build();
    }

    @Mod.EventBusSubscriber
    public static class FireGauntletEvent {
        @SubscribeEvent
        public static void onAttack(AttackEntityEvent event) {
            var player = event.getEntity();
            var level = player.getCommandSenderWorld();

            if (level.isClientSide() || !(event.getTarget() instanceof LivingEntity))
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.FIRE_GAUNTLET.get());

            if (!(stack.getItem() instanceof FireGauntletItem relic) || !relic.canPlayerUseActiveAbility(player, stack, "caster"))
                return;

            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(player.getAttribute(ForgeMod.BLOCK_REACH.get()).getValue()),
                    entity -> player.hasLineOfSight(entity) && !entity.getUUID().equals(player.getUUID()) && !entity.isInvisible());
            targets.sort(Comparator.comparingDouble(targetEntity -> targetEntity.distanceTo(player)));

            var targetIndex = 0;
            var random = player.getRandom();
            var sparksCount = Math.min(MathBaseUtils.multicast(random, relic.getAbilityValue(stack, "caster", "chance")), targets.size());

            var sparkDamage = (float) (player.getAttributes().getValue(Attributes.ATTACK_DAMAGE) * (1.0 + relic.getAbilityValue(stack, "caster", "damage")));

            var experienceAwarded = false;

            for (int i = 0; i < sparksCount; i++) {
                LivingEntity target = targets.get(targetIndex);

                createFireSparkEffect((ServerLevel) level, player, target);

                target.invulnerableTime = 0;

                if (target.hurt(level.damageSources().playerAttack(player), sparkDamage)) {
                    var fireTicks = (int) relic.getAbilityValue(stack, "caster", "duration") + 20;
                    target.setRemainingFireTicks(fireTicks);

                    if (!experienceAwarded) {
                        relic.spreadExperience(player, stack, 1);
                        experienceAwarded = true;
                    }
                }
                targetIndex = (targetIndex + 1) % targets.size();
            }
        }

        private static void createFireSparkEffect(ServerLevel level, Player player, LivingEntity target) {
            var random = level.getRandom();

            double angle = random.nextDouble() * 2 * Math.PI;
            Vec3 startPos = player.position().add(Math.cos(angle), 1, Math.sin(angle));
            Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);

            var distance = startPos.distanceTo(targetPos);
            var particleCount = Math.max(8, (int) Math.ceil(distance * 2));

            for (int i = 0; i <= particleCount; i++) {
                var progress = (double) i / particleCount;
                var particlePos = startPos.lerp(targetPos, progress);

                particlePos = particlePos.add((random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.2);

                level.sendParticles(ParticleUtils.constructSimpleSpark(new Color(200 + random.nextInt(55), 100 + random.nextInt(100), random.nextInt(50)), (float) (0.6F + (progress * 0.4F)), (int)(progress * 20) + random.nextInt(3), 0.85F),
                        particlePos.x, particlePos.y, particlePos.z, 1, 0.05, 0.05, 0.05, 0.02);
            }

            for (int i = 0; i < 12; i++) {
                var offset = new Vec3((random.nextDouble() - 0.5) * 0.5, (random.nextDouble() - 0.5) * 0.5, (random.nextDouble() - 0.5) * 0.5);

                level.sendParticles(ParticleUtils.constructSimpleSpark(new Color(255, 150 + random.nextInt(100), random.nextInt(100)), 1.0F, 5 + random.nextInt(3), 0.9F),
                        targetPos.x + offset.x, targetPos.y + offset.y, targetPos.z + offset.z,
                        1, 0.02, 0.02, 0.02, 0.08);
            }
        }
    }
}