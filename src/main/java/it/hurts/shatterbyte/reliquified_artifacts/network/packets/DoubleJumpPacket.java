package it.hurts.shatterbyte.reliquified_artifacts.network.packets;

import artifacts.registry.ModItems;
import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
import it.hurts.shatterbyte.reliquified_artifacts.items.charm.CloudInBottleItem;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import java.awt.*;

public class DoubleJumpPacket implements CustomPacketPayload {
    public static final Type<DoubleJumpPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ReliquifiedArtifacts.MODID, "check_double_jump"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DoubleJumpPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buf, DoubleJumpPacket packet) {
        }

        @Nonnull
        @Override
        public DoubleJumpPacket decode(@Nonnull RegistryFriendlyByteBuf buf) {
            return new DoubleJumpPacket();
        }
    };

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            var stack = EntityUtils.findEquippedCurio(player, ModItems.CLOUD_IN_A_BOTTLE.value());

            if (!(stack.getItem() instanceof CloudInBottleItem relic))
                return;

            if (!relic.performAirJump(player, stack))
                return;

            player.awardStat(Stats.JUMP);

            Level level = player.getCommandSenderWorld();

            spawnCloudJumpParticles(player);

            level.playSound(null, player.blockPosition(), SoundEvents.WOOL_PLACE, player.getSoundSource(), 1F, 0.75F + player.getRandom().nextFloat());
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void spawnCloudJumpParticles(Player player) {
        if (!(player.level() instanceof ServerLevel level))
            return;

        var random = player.getRandom();
        var origin = player.position().add(0D, 0.18D, 0D);

        for (var i = 0; i < 90; i++) {
            var angle = random.nextDouble() * Math.PI * 2D;
            var radius = Math.pow(random.nextDouble(), 0.65D) * 1.05D;
            var x = Math.cos(angle);
            var z = Math.sin(angle);

            var radial = new Vec3(x, 0D, z);
            var pos = origin.add(radial.scale(radius)).add(0D, (random.nextDouble() - 0.5D) * 0.18D, 0D);

            var velocity = radial.scale(0.035D + random.nextDouble() * 0.04D).add(0D, 0.045D + random.nextDouble() * 0.04D, 0D);

            level.sendParticles(ParticleUtils.constructSimpleSpark(new Color(255, 255, 255), 1F + random.nextFloat() * 0.5F, 20 + random.nextInt(10), 0.975F), pos.x, pos.y, pos.z, 1, velocity.x, velocity.y, velocity.z, 0D);
        }
    }
}
