package it.hurts.octostudios.rarcompat.network.packets;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.items.feet.BunnyHoppersItem;
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
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import javax.annotation.Nonnull;
import java.util.List;

@Data
@AllArgsConstructor
public class PowerJumpPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PowerJumpPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RARCompat.MODID, "power_jump"));

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
            Player player = ctx.player();

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.BUNNY_HOPPERS.value());

            if (!(stack.getItem() instanceof BunnyHoppersItem relic))
                return;

            var countRelic = CuriosApi.getCuriosInventory(player).map(inventory -> inventory.findCurios(ModItems.BUNNY_HOPPERS.value()).stream()
                    .map(SlotResult::stack).toList()).orElse(List.of());

            for (var entry : countRelic)
                if (player.tickCount % countRelic.size() == 0)
                    relic.addTime(entry, 1);
        });
    }


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
