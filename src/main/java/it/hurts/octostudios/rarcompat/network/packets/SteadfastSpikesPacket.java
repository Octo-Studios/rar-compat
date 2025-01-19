package it.hurts.octostudios.rarcompat.network.packets;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.items.feet.SteadfastSpikesItem;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

@Data
@AllArgsConstructor
public class SteadfastSpikesPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SteadfastSpikesPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RARCompat.MODID, "spikes"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SteadfastSpikesPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buf, SteadfastSpikesPacket packet) {
        }

        @Nonnull
        @Override
        public SteadfastSpikesPacket decode(@Nonnull RegistryFriendlyByteBuf buf) {
            return new SteadfastSpikesPacket();
        }
    };

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.STEADFAST_SPIKES.value());

            if (!(stack.getItem() instanceof SteadfastSpikesItem))
                return;

            player.fallDistance = 0;
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
