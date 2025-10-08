package it.hurts.octostudios.rarcompat.items.charm;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class ObsidianSkullItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("hell")
                                .stat(StatData.builder("duration")
                                        .initialValue(40D, 60D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.3D)
                                        .formatValue(value -> MathUtils.round(value / 20, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff3e265a)
                                .borderBottom(0xff150b2c)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.NETHER)
                        .build())
                .build();
    }

    @Override
    public void wornTick(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player) || player.getCommandSenderWorld().isClientSide())
            return;

        if (getCooldown(stack) >= 60)
            addTime(stack, -1);
        else {
            if (getTime(stack) > 0)
                addCooldown(stack, 1);
        }

        if (!player.isOnFire() || getTime(stack) >= getMaxTime(stack))
            return;

        if (player.tickCount % 20 == 0)
            spreadExperience(player, stack, 1);

        addTime(stack, 1);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.max(0, Math.round((13F * (getMaxTime(stack) - getTime(stack))) / getMaxTime(stack)));
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return this.getTime(stack) != 0 && canUseAbility(stack, "hell");
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return Mth.hsvToRgb(Math.max(0F, Math.min(1F, (1F - Math.max(0F, (float) getTime(stack) / getMaxTime(stack)))) / 3F), 1F, 1F);
    }

    public int getMaxTime(ItemStack stack) {
        return (int) MathUtils.round(getAbilityValue(stack, "hell", "duration"), 0);
    }

    public void addTime(ItemStack stack, int time) {
        setTime(stack, getTime(stack) + time);
    }

    public int getTime(ItemStack stack) {
        return NBTUtils.getInt(stack, "time", 0);
    }

    public void setTime(ItemStack stack, int val) {
        NBTUtils.setInt(stack, "time", Math.max(val, 0));
    }

    public void addCooldown(ItemStack stack, int time) {
        setCooldown(stack, getCooldown(stack) + time);
    }

    public int getCooldown(ItemStack stack) {
        return NBTUtils.getInt(stack, "cooldown", 0);
    }

    public void setCooldown(ItemStack stack, int val) {
        NBTUtils.setInt(stack, "cooldown", Math.max(val, 0));
    }

    @Mod.EventBusSubscriber
    public static class ObsidianSkull {
        @SubscribeEvent
        public static void onIncomingDamage(LivingAttackEvent event) {
            Level level = event.getEntity().getCommandSenderWorld();

            if (!(event.getEntity() instanceof Player player) || !event.getSource().is(DamageTypeTags.IS_FIRE) || level.isClientSide())
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.OBSIDIAN_SKULL.get());

            if (!(stack.getItem() instanceof ObsidianSkullItem relic) || !relic.canUseAbility(stack, "hell"))
                return;

            if (!player.isOnFire())
                relic.addTime(stack, 1);

            relic.setCooldown(stack, 0);

            if (relic.getTime(stack) >= relic.getMaxTime(stack)) {
                relic.setCooldown(stack, 0);
            } else {
                event.setCanceled(true);

                relic.setCooldown(stack, 0);

                RandomSource random = level.getRandom();

                ((ServerLevel) level).sendParticles(ParticleUtils.constructSimpleSpark(new Color(64 + random.nextInt(64), random.nextInt(50), 200 + random.nextInt(55)), 0.35F, 10, 0.9F),
                        player.getX(), player.getY() + player.getBbHeight() / 2F, player.getZ(), 1, player.getBbWidth() / 2F, player.getBbHeight() / 2F, player.getBbWidth() / 2F, 0.025F);
            }
        }
    }
}
