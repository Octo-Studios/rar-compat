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
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import javax.annotation.Nonnull;
import java.util.List;

public class PowerJumpPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PowerJumpPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReliquifiedArtifacts.MODID, "power_jump"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PowerJumpPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buf, PowerJumpPacket packet) {
        }

        @Nonnull
        @Override
        public PowerJumpPacket decode(@Nonnull RegistryFriendlyByteBuf buf) {
            return new PowerJumpPacket();
        }
    };

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            var stack = EntityUtils.findEquippedCurio(player, ModItems.BUNNY_HOPPERS.value());

            if (!(stack.getItem() instanceof BunnyHoppersItem))
                return;

            var equipped = CuriosApi.getCuriosInventory(player)
                    .map(inventory -> inventory.findCurios(ModItems.BUNNY_HOPPERS.value()).stream().map(SlotResult::stack).toList())
                    .orElse(List.of());

            if (equipped.isEmpty() || player.tickCount % equipped.size() != 0)
                return;

            for (var entry : equipped) {
                if (entry.getItem() instanceof BunnyHoppersItem relic)
                    relic.registerPowerJumpTick(player, entry);
            }
        });
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
