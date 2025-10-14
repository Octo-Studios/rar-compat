package it.hurts.octostudios.rarcompat.items.charm;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.CastData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastStage;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastType;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootData;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootCollections;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.TooltipData;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import it.hurts.sskirillss.relics.utils.NBTUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;
import java.util.Locale;

public class UniversalAttractorItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("attractor")
                                .active(CastData.builder()
                                        .type(CastType.INTERRUPTIBLE)
                                        .build())
                                .icon((player, stack, ability) -> ability + "_" + getMode(stack).name().toLowerCase(Locale.ROOT)).stat(StatData.builder("radius")
                                        .initialValue(3D, 5D)
                                        .upgradeModifier(UpgradeOperation.ADD, 1D)
                                        .formatValue(value -> (int) MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xffd40000)
                                .borderBottom(0xff175dea)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.ANTHROPOGENIC)
                        .build())
                .build();
    }

    public Mode getMode(ItemStack stack) {
        return Mode.byIndex(NBTUtils.getInt(stack, "mode", Mode.ATTRACT.getIndex()));
    }

    public void setMode(ItemStack stack, Mode mode) {
        NBTUtils.setInt(stack, "mode", mode.getIndex());
    }

    public void cycleMode(ItemStack stack, int steps) {
        setMode(stack, getMode(stack).cycle(steps));
    }

    @Override
    public void castActiveAbility(ItemStack stack, Player player, String ability, CastType type, CastStage stage) {
        cycleMode(stack, 1);
    }

    @Override
    public void wornTick(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player) || player.getCommandSenderWorld().isClientSide()
                || !canPlayerUseActiveAbility(player, stack, "attractor") || getMode(stack) == Mode.NEUTRAL)
            return;

        var pos = player.position();
        var random = player.getRandom();
        var level = player.getCommandSenderWorld();

        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(getAbilityValue(stack, "attractor", "radius")))) {
            if (!item.isAlive() || item.hasPickUpDelay())
                continue;

            var oldPos = new Vec3(item.getX(), item.getY() - item.getBbHeight() * 3, item.getZ());

            if (getMode(stack) == Mode.ATTRACT) {
                item.moveTo(pos);

                createLine(ParticleUtils.constructSimpleSpark(new Color(200 + random.nextInt(55), random.nextInt(50), random.nextInt(50)), 0.2F, 20, 0.8F), level, pos, oldPos);
            } else {
                item.setDeltaMovement(item.position().subtract(pos).normalize().scale(0.5));

                if (!item.horizontalCollision)
                    createLine(ParticleUtils.constructSimpleSpark(new Color(random.nextInt(50), random.nextInt(50), 200 + random.nextInt(55)), 0.2F, 3, 0.5F), level, pos, oldPos);
            }
        }
    }

    public static void createLine(ParticleOptions particle, Level level, Vec3 start, Vec3 end) {
        var delta = end.subtract(start);
        var dir = delta.normalize();
        var amount = delta.length() * 3;

        for (double i = 0; i < amount; ++i) {
            var progress = i * delta.length() / amount;

            ((ServerLevel) level).sendParticles(particle, start.x + dir.x * progress, start.y + dir.y * progress + 1, start.z + dir.z * progress, 0, 0, 0, 0, 0);
        }
    }

    @Getter
    @AllArgsConstructor
    public enum Mode {
        ATTRACT(1),
        REPEL(2),
        NEUTRAL(3);

        private final int index;

        public static Mode byIndex(int index) {
            for (var mode : Mode.values())
                if (mode.getIndex() == index)
                    return mode;

            throw new IllegalArgumentException();
        }

        public Mode cycle(int steps) {
            var modes = Mode.values();
            int index = (this.ordinal() + steps) % modes.length;

            if (index < 0)
                index += modes.length;

            return modes[index];
        }
    }

    @Mod.EventBusSubscriber
    public static class UniversalAttractorEvent {
        @SubscribeEvent
        public static void onItemPickedUp(EntityItemPickupEvent event) {
            var player = event.getEntity();

            var stack = EntityUtils.findEquippedCurio(player, ModItems.UNIVERSAL_ATTRACTOR.get());

            if (player.getCommandSenderWorld().isClientSide() || !(stack.getItem() instanceof UniversalAttractorItem relic)
                    || !NBTUtils.getBoolean(stack, "toggled", false))
                return;

            var item = event.getItem().getItem();
            var itemEntity = event.getItem();

            if (itemEntity.getCommandSenderWorld().random.nextDouble() <= (double) item.getCount() / item.getMaxStackSize()) {
                if (itemEntity.getOwner() != null && itemEntity.getOwner().getUUID().equals(player.getUUID()))
                    return;

                relic.spreadExperience(player, stack, 1);
            }
        }
    }
}
