package it.hurts.shatterbyte.reliquified_artifacts.network;

import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
import it.hurts.shatterbyte.reliquified_artifacts.network.packets.BunnyJumpReleasePacket;
import it.hurts.shatterbyte.reliquified_artifacts.network.packets.DoubleJumpPacket;
import it.hurts.shatterbyte.reliquified_artifacts.network.packets.FlamingoSwimPacket;
import it.hurts.shatterbyte.reliquified_artifacts.network.packets.PowerJumpPacket;
import it.hurts.shatterbyte.reliquified_artifacts.network.packets.SteadfastSpikesPacket;
import it.hurts.shatterbyte.reliquified_artifacts.network.packets.UmbrellaBouncePacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ReliquifiedArtifacts.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {
    @SubscribeEvent
    public static void onRegisterPayloadHandler(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(ReliquifiedArtifacts.MODID)
                .versioned("1.0")
                .optional();

        registrar.playToServer(DoubleJumpPacket.TYPE, DoubleJumpPacket.STREAM_CODEC, DoubleJumpPacket::handle);
        registrar.playToServer(BunnyJumpReleasePacket.TYPE, BunnyJumpReleasePacket.STREAM_CODEC, BunnyJumpReleasePacket::handle);
        registrar.playToServer(PowerJumpPacket.TYPE, PowerJumpPacket.STREAM_CODEC, PowerJumpPacket::handle);
        registrar.playToServer(FlamingoSwimPacket.TYPE, FlamingoSwimPacket.STREAM_CODEC, FlamingoSwimPacket::handle);
        registrar.playToServer(SteadfastSpikesPacket.TYPE, SteadfastSpikesPacket.STREAM_CODEC, SteadfastSpikesPacket::handle);
        registrar.playToServer(UmbrellaBouncePacket.TYPE, UmbrellaBouncePacket.STREAM_CODEC, UmbrellaBouncePacket::handle);
    }
}
