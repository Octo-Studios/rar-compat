package it.hurts.octostudios.rarcompat.items.feet;

import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.CastData;
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
import it.hurts.sskirillss.relics.utils.MathUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.awt.*;

public class RootedBootsItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("devouring")
                                .active(CastData.builder().type(CastType.TOGGLEABLE)
                                        .build())
                                .stat(StatData.builder("frequency")
                                        .initialValue(140D, 120D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, -0.071)
                                        .formatValue(value -> MathUtils.round(value / 20, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff533021)
                                .borderBottom(0xff8ac100)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.VILLAGE)
                        .entry(LootCollections.DESERT)
                        .entry(LootCollections.JUNGLE)
                        .entry(LootCollections.AQUATIC)
                        .entry(LootCollections.PILLAGE)
                        .entry(LootCollections.COLD)
                        .entry(LootCollections.SCULK)
                        .entry(LootCollections.ANTHROPOGENIC)
                        .build())
                .build();
    }

    @Override
    public void wornTick(LivingEntity entity, ItemStack stack) {
        var level = entity.getCommandSenderWorld();

        if (!(entity instanceof Player player) || level.isClientSide() || !isAbilityTicking(stack, "devouring"))
            return;

        var blockPos = player.blockPosition().below();
        var footData = player.getFoodData();

        if (player.tickCount % Math.round(this.getAbilityValue(stack, "devouring", "frequency")) != 0 || !footData.needsFood()
                || !level.getBlockState(blockPos).is(Blocks.GRASS_BLOCK))
            return;

        footData.eat(1, 1F);

        spreadExperience(player, stack, 1);

        level.setBlock(blockPos, Blocks.DIRT.defaultBlockState(), 3);

        var random = player.getRandom();

        level.playSound(null, player, SoundEvents.GRASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.9F + random.nextFloat() * 0.2F);
        ((ServerLevel) level).sendParticles(ParticleUtils.constructSimpleSpark(new Color(random.nextInt(50), 100 + random.nextInt(155), random.nextInt(50)),
                0.3F, 40, 0.9F), player.getX(), player.getY() + 0.2, player.getZ(), 20, 0.25, 0, 0.25, 0.05);
    }
}
