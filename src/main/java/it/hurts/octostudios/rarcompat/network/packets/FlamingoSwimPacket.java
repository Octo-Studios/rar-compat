package it.hurts.octostudios.rarcompat.network.packets;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.charm.HeliumFlamingoItem;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@Data
@AllArgsConstructor
public class FlamingoSwimPacket {
    private final boolean toggled;

    public static void encode(FlamingoSwimPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.toggled);
    }

    public static FlamingoSwimPacket decode(FriendlyByteBuf buf) {
        return new FlamingoSwimPacket(buf.readBoolean());
    }

    public static void handle(FlamingoSwimPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Player player = ctx.get().getSender();

            if (player == null) return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.HELIUM_FLAMINGO.get());

            if (!(stack.getItem() instanceof HeliumFlamingoItem relic))
                return;

            if (packet.toggled) {
                player.setSprinting(true);

                relic.setToggled(stack, true);
            } else {
                player.setSprinting(false);

                relic.setTime(stack, (int) relic.getAbilityValue(stack, "flying", "time") + 10);
                relic.setToggled(stack, false);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
