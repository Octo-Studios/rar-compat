package it.hurts.octostudios.rarcompat.network.packets;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@Data
@AllArgsConstructor
public class EntityMotionPacket {
    private final int id;
    private final double x;
    private final double y;
    private final double z;

    public EntityMotionPacket(int id, Vec3 motion) {
        this(id, motion.x(), motion.y(), motion.z());
    }

    public static EntityMotionPacket decode(FriendlyByteBuf buf) {
        return new EntityMotionPacket(
                buf.readInt(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble()
        );
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                handleClientSide();
            } else {
                var player = context.getSender();
                if (player != null) {
                    var level = player.getCommandSenderWorld();
                    var entity = level.getEntity(id);
                    if (entity != null) {
                        entity.setDeltaMovement(x, y, z);
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }

    private void handleClientSide() {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        var entity = level.getEntity(id);
        if (entity == null) return;

        entity.setDeltaMovement(x, y, z);
    }
}