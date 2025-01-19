package it.hurts.octostudios.rarcompat.items.charm;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.init.DataComponentRegistry;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.*;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.GemColor;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.GemShape;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootData;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import it.hurts.sskirillss.relics.items.relics.base.data.research.ResearchData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.BeamsData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.TooltipData;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

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
                                .research(ResearchData.builder()
                                        .star(0, 8, 25).star(1, 3, 16).star(2, 6, 8).star(3, 12, 4)
                                        .star(4, 17, 7).star(5, 19, 13).star(6, 14, 22).star(7, 11, 15)
                                        .link(0, 1).link(0, 7).link(2, 3).link(3, 4).link(4, 5).link(5, 7).link(5, 6).link(7, 6).link(1, 2).link(1, 7)
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff3e265a)
                                .borderBottom(0xff150b2c)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFF45015c)
                                .endColor(0x00700c91)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData.abilityBuilder("hell")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.ORANGE)
                                        .build())
                                .build())
                        .build())
                .loot(LootData.builder()
                        .entry(LootEntries.NETHER_LIKE, LootEntries.THE_NETHER)
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.getCommandSenderWorld().isClientSide())
            return;

        addCooldown(stack, 1);

        if (getCooldown(stack) >= 60)
            addTime(stack, -1);

        if (getTime(stack) == 0)
            setCooldown(stack, 0);
    }

    public void addTime(ItemStack stack, int time) {
        setTime(stack, getTime(stack) + time);
    }

    public int getTime(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.TIME, 0);
    }

    public void setTime(ItemStack stack, int val) {
        stack.set(DataComponentRegistry.TIME, Math.max(val, 0));
    }

    public void addCooldown(ItemStack stack, int time) {
        setCooldown(stack, getCooldown(stack) + time);
    }

    public int getCooldown(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.COOLDOWN, 0);
    }

    public void setCooldown(ItemStack stack, int val) {
        stack.set(DataComponentRegistry.COOLDOWN, Math.max(val, 0));
    }

    @EventBusSubscriber
    public static class ObsidianSkull {
        @SubscribeEvent
        public static void onIncomingDamage(LivingIncomingDamageEvent event) {
            Level level = event.getEntity().getCommandSenderWorld();

            if (!(event.getEntity() instanceof Player player) || !event.getSource().is(DamageTypeTags.IS_FIRE) || level.isClientSide())
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.OBSIDIAN_SKULL.value());

            if (!(stack.getItem() instanceof ObsidianSkullItem relic) || !relic.isAbilityUnlocked(stack, "hell"))
                return;

            var statValue = (int) relic.getStatValue(stack, "hell", "duration");

            relic.addTime(stack, 1);
            relic.setCooldown(stack, 0);

            if (relic.getTime(stack) > statValue) {
                relic.setTime(stack, statValue);
            } else {
                event.setCanceled(true);

                if (player.tickCount % 20 == 0)
                    relic.spreadRelicExperience(player, stack, 1);

                RandomSource random = level.getRandom();

                ((ServerLevel) level).sendParticles(ParticleUtils.constructSimpleSpark(
                                new Color(64 + random.nextInt(64), random.nextInt(50), 200 + random.nextInt(55)),
                                0.5F, 10, 0.9F),
                        player.getX(), player.getY() + player.getBbHeight() / 2F, player.getZ(),
                        10, player.getBbWidth() / 2F, player.getBbHeight() / 2F, player.getBbWidth() / 2F,
                        0.025F);
            }
        }
    }
}
