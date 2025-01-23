package it.hurts.octostudios.rarcompat.network.packets;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.items.charm.CloudInBottleItem;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

public class DoubleJumpPacket implements CustomPacketPayload {
    public static final Type<DoubleJumpPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RARCompat.MODID, "check_double_jump"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DoubleJumpPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buf, DoubleJumpPacket packet) {
        }

        @Nonnull
        @Override
        public DoubleJumpPacket decode(@Nonnull RegistryFriendlyByteBuf buf) {
            return new DoubleJumpPacket();
        }
    };

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            var stack = EntityUtils.findEquippedCurio(player, ModItems.CLOUD_IN_A_BOTTLE.value());

            if (!(stack.getItem() instanceof CloudInBottleItem relic))
                return;

            relic.addCount(stack, 1);
            relic.spreadRelicExperience(player, stack, 1);

            player.hasImpulse = true;
            player.fallDistance = 0;
            player.awardStat(Stats.JUMP);

            NeoForge.EVENT_BUS.post(new LivingEvent.LivingJumpEvent(player));

            Level level = player.getCommandSenderWorld();

            for (int i = 0; i < 50; i++) {
                double angle = 2 * Math.PI * i / 50;

                ((ServerLevel) level).sendParticles(ParticleTypes.CLOUD, player.getX() + Math.cos(angle), player.getY(), player.getZ() + Math.sin(angle),
                        0, 0, 0.0, 0, 0);
            }

            level.playSound(null, player.blockPosition(), SoundEvents.WOOL_PLACE, player.getSoundSource(), 1F, 0.75F + player.getRandom().nextFloat());
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
