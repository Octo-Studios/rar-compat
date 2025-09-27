package it.hurts.octostudios.rarcompat.network.packets;

import it.hurts.octostudios.rarcompat.items.UmbrellaItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@Data
@AllArgsConstructor
public class RepulsionUmbrellaPacket {

    public static void encode(RepulsionUmbrellaPacket packet, FriendlyByteBuf buf) {}

    public static RepulsionUmbrellaPacket decode(FriendlyByteBuf buf) {
        return new RepulsionUmbrellaPacket();
    }

    public static void handle(RepulsionUmbrellaPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Player player = ctx.get().getSender();
            if (player == null) return;

            var stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof UmbrellaItem relic))
                return;

            var level = player.getCommandSenderWorld();

            level.playSound(null, player.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.MASTER, 1F, 1 + (player.getRandom().nextFloat() * 0.25F));
            player.getCooldowns().addCooldown(relic, (int) (relic.getAbilityValue(stack, "glider", "cooldown") * 20));
            relic.spreadExperience(player, stack, 1);
            relic.addCharges(stack, -1);

            var pos = player.position().add(player.getLookAngle());
            if (!level.isClientSide())
                ((ServerLevel) level).sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 40, 0.5F, 0.5F, 0.5F, 0.05F);
        });
        ctx.get().setPacketHandled(true);
    }
}
