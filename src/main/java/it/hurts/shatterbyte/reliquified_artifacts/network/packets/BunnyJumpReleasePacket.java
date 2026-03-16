package it.hurts.shatterbyte.reliquified_artifacts.network.packets;

import artifacts.registry.ModItems;
import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
import it.hurts.shatterbyte.reliquified_artifacts.items.feet.BunnyHoppersItem;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

public class BunnyJumpReleasePacket implements CustomPacketPayload {
    public static final Type<BunnyJumpReleasePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ReliquifiedArtifacts.MODID, "bunny_jump_release"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BunnyJumpReleasePacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buf, BunnyJumpReleasePacket packet) {
        }

        @Nonnull
        @Override
        public BunnyJumpReleasePacket decode(@Nonnull RegistryFriendlyByteBuf buf) {
            return new BunnyJumpReleasePacket();
        }
    };

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();

            if (player == null)
                return;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.BUNNY_HOPPERS.value())) {
                if (!(stack.getItem() instanceof BunnyHoppersItem relic))
                    continue;

                relic.setJumpLocked(stack, true);
                relic.setToggled(stack, false);
                relic.setFallingStarted(stack, false);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
