package it.hurts.octostudios.rarcompat.network.packets;

import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.items.UmbrellaItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

@Data
@AllArgsConstructor
public class RepulsionUmbrellaPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RepulsionUmbrellaPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RARCompat.MODID, "repulsion"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RepulsionUmbrellaPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buf, RepulsionUmbrellaPacket packet) {

        }

        @Nonnull
        @Override
        public RepulsionUmbrellaPacket decode(@Nonnull RegistryFriendlyByteBuf buf) {
            return new RepulsionUmbrellaPacket();
        }
    };

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            var stack = player.getMainHandItem();

            if (!(stack.getItem() instanceof UmbrellaItem relic))
                return;

            var level = player.getCommandSenderWorld();

            level.playSound(null, player.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.MASTER, 1F, 1 + (player.getRandom().nextFloat() * 0.25F));

            player.getCooldowns().addCooldown(relic, (int) (relic.getStatValue(stack, "glider", "cooldown") * 20));

            relic.spreadRelicExperience(player, stack, 1);
            relic.addCharges(stack, -1);

            var pos = player.position().add(player.getLookAngle());

            if (!level.isClientSide())
                ((ServerLevel) level).sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 40, 0.5F, 0.5F, 0.5F, 0.05F);
        });
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}