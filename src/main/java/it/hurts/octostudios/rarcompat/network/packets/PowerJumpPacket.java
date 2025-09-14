package it.hurts.octostudios.rarcompat.network.packets;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.feet.BunnyHoppersItem;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.List;
import java.util.function.Supplier;

@Data
@AllArgsConstructor
public class PowerJumpPacket {

    public static void encode(PowerJumpPacket packet, FriendlyByteBuf buf) {}

    public static PowerJumpPacket decode(FriendlyByteBuf buf) {
        return new PowerJumpPacket();
    }

    public static void handle(PowerJumpPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Player player = ctx.get().getSender();

            if (player == null) return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.BUNNY_HOPPERS.get());

            if (!(stack.getItem() instanceof BunnyHoppersItem relic))
                return;

            if (relic.getTime(stack) >= relic.getAbilityValue(stack, "hold", "duration") - 1)
                relic.spreadExperience(player, stack, 1);

            var countRelic = CuriosApi.getCuriosInventory(player).map(inventory -> inventory.findCurios(ModItems.BUNNY_HOPPERS.get()).stream()
                    .map(SlotResult::stack).toList()).orElse(List.of());

            for (var entry : countRelic)
                if (player.tickCount % countRelic.size() == 0)
                    relic.addTime(entry, 1);
        });
        ctx.get().setPacketHandled(true);
    }
}