package it.hurts.octostudios.rarcompat.network.packets;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.feet.SteadfastSpikesItem;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@Data
@AllArgsConstructor
public class SteadfastSpikesPacket {

    public static void encode(SteadfastSpikesPacket packet, FriendlyByteBuf buf) {}

    public static SteadfastSpikesPacket decode(FriendlyByteBuf buf) {
        return new SteadfastSpikesPacket();
    }

    public static void handle(SteadfastSpikesPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Player player = ctx.get().getSender();

            if (player == null) return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.STEADFAST_SPIKES.get());

            if (!(stack.getItem() instanceof SteadfastSpikesItem))
                return;

            player.fallDistance = 0;
        });
        ctx.get().setPacketHandled(true);
    }
}