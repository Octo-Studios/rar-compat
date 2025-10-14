package it.hurts.octostudios.rarcompat.network.packets;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.charm.CloudInBottleItem;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@Data
@AllArgsConstructor
public class DoubleJumpPacket {

    public static void encode(DoubleJumpPacket packet, FriendlyByteBuf buf) {}

    public static DoubleJumpPacket decode(FriendlyByteBuf buf) {
        return new DoubleJumpPacket();
    }

    public static void handle(DoubleJumpPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Player player = ctx.get().getSender();

            if (player == null) return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.CLOUD_IN_A_BOTTLE.get());

            if (!(stack.getItem() instanceof CloudInBottleItem relic))
                return;

            relic.addCount(stack, 1);
            relic.spreadExperience(player, stack, 1);

            player.hasImpulse = true;
            player.fallDistance = 0;
            player.awardStat(Stats.JUMP);

            player.jumpFromGround();

            Level level = player.getCommandSenderWorld();

            for (int i = 0; i < 50; i++) {
                double angle = 2 * Math.PI * i / 50;

                ((ServerLevel) level).sendParticles(ParticleTypes.CLOUD, player.getX() + Math.cos(angle), player.getY(), player.getZ() + Math.sin(angle),
                        0, 0, 0.0, 0, 0);
            }

            level.playSound(null, player.blockPosition(), SoundEvents.WOOL_PLACE, player.getSoundSource(), 1F, 0.75F + player.getRandom().nextFloat());
        });
        ctx.get().setPacketHandled(true);
    }
}
