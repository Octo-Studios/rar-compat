package it.hurts.shatterbyte.reliquified_artifacts.network.packets;

import artifacts.registry.ModItems;
import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
import it.hurts.shatterbyte.reliquified_artifacts.items.feet.SteadfastSpikesItem;
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

import javax.annotation.Nonnull;

@Data
@AllArgsConstructor
public class SteadfastSpikesPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SteadfastSpikesPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReliquifiedArtifacts.MODID, "spikes"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SteadfastSpikesPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buf, SteadfastSpikesPacket packet) {
        }

        @Nonnull
        @Override
        public SteadfastSpikesPacket decode(@Nonnull RegistryFriendlyByteBuf buf) {
            return new SteadfastSpikesPacket();
        }
    };

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.STEADFAST_SPIKES.value());

            if (!(stack.getItem() instanceof SteadfastSpikesItem relic))
                return;

            var relicData = relic.getRelicData(player, stack);
            var ability = relicData.getAbilitiesData().getAbilityData("wall_slide");

            if (!ability.canPlayerUse(player) || !ability.getMode().equals("enabled"))
                return;

            player.fallDistance = 0;

            var data = player.getPersistentData();
            var gameTime = player.level().getGameTime();
            var lastTick = data.getLong("reliquified_artifacts_steadfast_spikes_wall_slide_tick");

            if (lastTick != gameTime) {
                var ticks = data.getInt("reliquified_artifacts_steadfast_spikes_wall_slide_ticks") + 1;
                var seconds = ticks / 20;

                if (seconds > 0) {
                    relicData.getLevelingData().addExperience("wall_slide", "wall_slide_time", seconds);
                    ability.getStatisticData().getMetricData("wall_slide_time").addValue(seconds);
                    ticks %= 20;
                }

                data.putInt("reliquified_artifacts_steadfast_spikes_wall_slide_ticks", ticks);
            }

            data.putLong("reliquified_artifacts_steadfast_spikes_wall_slide_tick", gameTime);
        });
    }
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
