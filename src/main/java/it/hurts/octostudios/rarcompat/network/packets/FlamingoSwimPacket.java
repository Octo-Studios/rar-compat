package it.hurts.octostudios.rarcompat.network.packets;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.items.charm.HeliumFlamingoItem;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@Data
@AllArgsConstructor
public class FlamingoSwimPacket implements CustomPacketPayload {
    private final boolean toggled;

    public static final CustomPacketPayload.Type<FlamingoSwimPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RARCompat.MODID, "swim"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FlamingoSwimPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, FlamingoSwimPacket::isToggled,
            FlamingoSwimPacket::new);

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.HELIUM_FLAMINGO.value());

            if (!(stack.getItem() instanceof HeliumFlamingoItem relic) || player.isInWater())
                return;

            if (toggled) {
                player.setSprinting(true);

                relic.setToggled(stack, true);
            } else {
                player.setSprinting(false);

                relic.setTime(stack, (int) relic.getStatValue(stack, "flying", "time"));
                relic.setToggled(stack, false);
            }
        });
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
