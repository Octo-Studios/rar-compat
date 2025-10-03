package it.hurts.octostudios.rarcompat.network;

import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.network.packets.EntityMotionPacket;
import it.hurts.octostudios.rarcompat.network.packets.PowerJumpPacket;
import it.hurts.octostudios.rarcompat.network.packets.RepulsionUmbrellaPacket;
import it.hurts.octostudios.rarcompat.network.packets.SteadfastSpikesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class NetworkHandler {
    private static SimpleChannel INSTANCE;
    private static int ID = 0;

    private static int nextID() {
        return ID++;
    }

    public static void register() {
        INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation(RARCompat.MODID, "network"),
                () -> "1.0",
                s -> true,
                s -> true);

        INSTANCE.registerMessage(nextID(), PowerJumpPacket.class,
                PowerJumpPacket::encode,
                PowerJumpPacket::decode,
                PowerJumpPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        INSTANCE.registerMessage(nextID(), SteadfastSpikesPacket.class,
                SteadfastSpikesPacket::encode,
                SteadfastSpikesPacket::decode,
                SteadfastSpikesPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        INSTANCE.registerMessage(nextID(), RepulsionUmbrellaPacket.class,
                RepulsionUmbrellaPacket::encode,
                RepulsionUmbrellaPacket::decode,
                RepulsionUmbrellaPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        INSTANCE.registerMessage(nextID(), EntityMotionPacket.class,
                EntityMotionPacket::encode,
                EntityMotionPacket::decode,
                EntityMotionPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

    }

    public static void sendToClient(Object packet, ServerPlayer player) {
        INSTANCE.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }

    public static void sendToClients(PacketDistributor.PacketTarget target, Object packet) {
        INSTANCE.send(target, packet);
    }

    public static void sendToClientsTrackingEntityAndSelf(Object packet, Entity entity) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }
}