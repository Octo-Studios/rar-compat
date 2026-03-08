package it.hurts.octostudios.rarcompat.network.packets;

import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.items.UmbrellaItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

public class UmbrellaBouncePacket implements CustomPacketPayload {
    public static final Type<UmbrellaBouncePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RARCompat.MODID, "umbrella_bounce"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UmbrellaBouncePacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buf, UmbrellaBouncePacket packet) {
            buf.writeDouble(packet.lookX);
            buf.writeDouble(packet.lookY);
            buf.writeDouble(packet.lookZ);
            buf.writeFloat(packet.yaw);
        }

        @Nonnull
        @Override
        public UmbrellaBouncePacket decode(@Nonnull RegistryFriendlyByteBuf buf) {
            return new UmbrellaBouncePacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat());
        }
    };

    private final double lookX;
    private final double lookY;
    private final double lookZ;
    private final float yaw;

    public UmbrellaBouncePacket(Vec3 look, float yaw) {
        this(look.x, look.y, look.z, yaw);
    }

    private UmbrellaBouncePacket(double lookX, double lookY, double lookZ, float yaw) {
        this.lookX = lookX;
        this.lookY = lookY;
        this.lookZ = lookZ;
        this.yaw = yaw;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> UmbrellaItem.tryBounce(ctx.player(), new Vec3(lookX, lookY, lookZ), yaw));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}