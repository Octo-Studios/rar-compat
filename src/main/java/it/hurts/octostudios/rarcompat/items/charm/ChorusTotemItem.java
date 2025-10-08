package it.hurts.octostudios.rarcompat.items.charm;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
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
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.network.packets.PacketItemActivation;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class ChorusTotemItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("past")
                                .stat(StatData.builder("chance")
                                        .initialValue(0.02D, 0.035D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> MathUtils.round(value * 100, 0))
                                        .build())
                                .stat(StatData.builder("radius")
                                        .initialValue(5D, 7D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> (int) MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff9045a6)
                                .borderBottom(0xff258273)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.END)
                        .build())
                .build();
    }

    @Mod.EventBusSubscriber
    public static class ChorusTotemEvent {
        @SubscribeEvent
        public static void onDimensionChange(LivingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.getCommandSenderWorld().isClientSide()
                    || !(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker.getStringUUID().equals(player.getStringUUID()))
                return;

            var level = player.getCommandSenderWorld();
            var stack = EntityUtils.findEquippedCurio(player, ModItems.CHORUS_TOTEM.get());
            var random = level.getRandom();

            if (!(stack.getItem() instanceof ChorusTotemItem relic) || !relic.canPlayerUseActiveAbility(player, stack, "past") || player.getHealth() - event.getAmount() < 1
                    || (player.getMaxHealth() - player.getHealth()) * relic.getAbilityValue(stack, "past", "chance") < random.nextFloat())
                return;

            attacker.randomTeleport(attacker.getX(), attacker.getY(), attacker.getZ(), false);
            attacker.setDeltaMovement(0, 0, 0);

            teleportPlayerToSafeSpot(attacker, level, (int) relic.getAbilityValue(stack, "past", "radius"));

            if (attacker instanceof ServerPlayer attackerPLayer) {
                NetworkHandler.sendToClient(new PacketItemActivation(stack), attackerPLayer);

                level.playSound(null, player, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 0.9F + random.nextFloat() * 0.2F);
            }

            relic.spreadExperience(player, stack, 1);
        }

        public static void teleportPlayerToSafeSpot(LivingEntity attacker, Level level, int maxRadius) {
            var random = attacker.getRandom();
            var oldPos = attacker.position();

            for (int radius = maxRadius; radius > 0; radius--) {
                double x = attacker.getX() + (random.nextInt(radius * 2 + 1) - radius);
                double y = attacker.getY() + (random.nextInt(radius * 2 + 1) - radius / 2.0);
                double z = attacker.getZ() + (random.nextInt(radius * 2 + 1) - radius);

                var targetPos = new BlockPos((int) x, (int) y, (int) z);

                while (targetPos.getY() > level.getMinBuildHeight() && !level.getBlockState(targetPos.below()).blocksMotion())
                    targetPos = targetPos.below();

                if (level.isEmptyBlock(targetPos.above())) {
                    ((ServerLevel) level).sendParticles(ParticleTypes.PORTAL, oldPos.x, oldPos.y + 1, oldPos.z, 40, -0.1F, 0, 0, 0.1);

                    attacker.teleportTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);

                    ((ServerLevel) level).sendParticles(ParticleTypes.PORTAL, targetPos.getX() + 0.5, targetPos.getY() + 1, targetPos.getZ() + 0.5, 40, 0.5, 0.5, 0.5, 0.1);

                    createLine(level, Vec3.atLowerCornerOf(targetPos), oldPos);

                    break;
                }
            }
        }

        public static void createLine(Level level, Vec3 start, Vec3 end) {
            var delta = end.subtract(start);
            var dir = delta.normalize();
            var amount = delta.length() * 5;

            for (double i = 0; i < amount; ++i) {
                var progress = i * delta.length() / amount;

                ((ServerLevel) level).sendParticles(ParticleTypes.PORTAL, start.x + dir.x * progress, start.y + dir.y * progress + 1, start.z + dir.z * progress, 5, 0.1, -0.01, 0.1, 0.1);
            }
        }
    }
}
