package it.hurts.octostudios.rarcompat.items.feet;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.init.DataComponentRegistry;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicAttributeModifier;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import top.theillusivec4.curios.api.SlotContext;

import java.awt.*;

public class KittySlippersItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("passive")
                                .maxLevel(0)
                                .build())
                        .ability(AbilityData.builder("fall")
                                .stat(StatData.builder("modifier")
                                        .initialValue(2D, 4D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.4)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .research(ResearchData.builder()
                                        .star(0, 6, 24).star(1, 2, 19).star(2, 2, 9).star(3, 6, 4)
                                        .star(4, 16, 4).star(5, 20, 9).star(6, 20, 19).star(7, 16, 24)
                                        .star(8, 11, 16).star(9, 8, 19).star(10, 5, 13).star(11, 8, 8)
                                        .star(12, 14, 19).star(13, 17, 13).star(14, 14, 8)
                                        .link(0, 1).link(1, 2).link(2, 3).link(3, 4).link(4, 5).link(5, 6).link(6, 7).link(7, 0)
                                        .link(8, 9).link(9, 10).link(8, 11).link(8, 12).link(12, 13).link(8, 14)
                                        .build())
                                .build())
                        .ability(AbilityData.builder("resurrected")
                                .requiredLevel(5)
                                .stat(StatData.builder("chance")
                                        .initialValue(0.05D, 0.1D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.25)
                                        .formatValue(value -> MathUtils.round(value * 100, 1))
                                        .build())
                                .research(ResearchData.builder()
                                        .star(0, 6, 25).star(1, 2, 20).star(2, 2, 9).star(3, 6, 4)
                                        .star(4, 16, 4).star(5, 20, 9).star(6, 20, 20).star(7, 16, 25)
                                        .star(8, 15, 21).star(9, 13, 17).star(10, 7, 13).star(11, 4, 17)
                                        .star(12, 17, 12).star(13, 16, 8)
                                        .link(0, 1).link(1, 2).link(2, 3).link(3, 4).link(4, 5).link(5, 6).link(6, 7).link(7, 0)
                                        .link(8, 9).link(9, 10).link(10, 11).link(9, 12).link(12, 13)
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xffededed)
                                .borderBottom(0xff696969)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFF59554f)
                                .endColor(0x00696258)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(15)
                        .step(100)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData.abilityBuilder("fall")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.PURPLE)
                                        .build())
                                .source(LevelingSourceData.abilityBuilder("resurrected")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.PURPLE)
                                        .build())
                                .build())
                        .build())
                .loot(LootData.builder()
                        .entry(LootEntries.TROPIC, LootEntries.VILLAGE)
                        .build())
                .build();
    }

    @Override
    public RelicAttributeModifier getRelicAttributeModifiers(ItemStack stack) {
        return RelicAttributeModifier.builder()
                .attribute(new RelicAttributeModifier.Modifier(Attributes.SAFE_FALL_DISTANCE, (float) getStatValue(stack, "fall", "modifier") / 3))
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.getCommandSenderWorld().isClientSide())
            return;

        var level = player.getCommandSenderWorld();

        for (Creeper creeper : level.getEntitiesOfClass(Creeper.class, player.getBoundingBox().inflate(5))) {
            var creeperPosition = creeper.position();
            var escapeDirection = player.position().subtract(creeperPosition).normalize().scale(-1);
            var escapePosition = creeperPosition.add(escapeDirection.scale(5));
            var navigation = creeper.getNavigation();
            var path = navigation.createPath(escapePosition.x, escapePosition.y, escapePosition.z, 0);

            if (path != null)
                navigation.moveTo(path, 1.5);

            creeper.setTarget(null);

            float yaw = (float) Math.toDegrees(Math.atan2(escapeDirection.z, escapeDirection.x));

            creeper.yBodyRot = yaw;
            creeper.yHeadRot = yaw;
        }

        for (Phantom phantom : level.getEntitiesOfClass(Phantom.class, player.getBoundingBox().inflate(5))) {
            var phantomPosition = phantom.position();
            var escapeDirection = player.position().subtract(phantomPosition).normalize().scale(-1);
            var escapePosition = phantomPosition.add(escapeDirection.scale(5));
            var navigation = phantom.getNavigation();
            var path = navigation.createPath(escapePosition.x, escapePosition.y, escapePosition.z, 0);

            if (path != null)
                navigation.moveTo(path, 1.5);

            phantom.setTarget(null);

            float yaw = (float) Math.toDegrees(Math.atan2(escapeDirection.z, escapeDirection.x));

            phantom.yBodyRot = yaw;
            phantom.yHeadRot = yaw;
        }
    }

    @EventBusSubscriber
    public static class KittySlippersEvent {
        @SubscribeEvent
        public static void onLivingChangeTargetEvent(LivingChangeTargetEvent event) {
            if ((event.getEntity() instanceof Creeper || event.getEntity() instanceof Phantom) && event.getNewAboutToBeSetTarget() instanceof Player player) {
                var itemStack = EntityUtils.findEquippedCurio(player, ModItems.KITTY_SLIPPERS.value());

                if (!(itemStack.getItem() instanceof KittySlippersItem))
                    return;

                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onLivingHurt(LivingDamageEvent.Pre event) {
            if (!(event.getEntity() instanceof Player player))
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.KITTY_SLIPPERS.value());

            if (!(stack.getItem() instanceof KittySlippersItem))
                return;

            stack.set(DataComponentRegistry.COUNT, (int) event.getOriginalDamage());
        }

        @SubscribeEvent
        public static void onPlayerFall(LivingFallEvent event) {
            if (!(event.getEntity() instanceof Player player))
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.KITTY_SLIPPERS.value());

            if (!(stack.getItem() instanceof KittySlippersItem relic))
                return;

            if (event.getDistance() > 4F)
                relic.spreadRelicExperience(player, stack, 1);
        }

        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            if (!(event.getEntity() instanceof Player player))
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.KITTY_SLIPPERS.value());

            var level = player.getCommandSenderWorld();
            var random = player.getRandom();

            if (!(stack.getItem() instanceof KittySlippersItem relic) || random.nextFloat() > relic.getStatValue(stack, "resurrected", "chance")
                    || !relic.canPlayerUseAbility(player, stack, "resurrected") || level.isClientSide())
                return;

            relic.spreadRelicExperience(player, stack, stack.getOrDefault(DataComponentRegistry.COUNT, 1));

            player.setHealth(1.0F);

            event.setCanceled(true);

            stack.set(DataComponentRegistry.TOGGLED, false);

            level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, player.getSoundSource(), 1F, 0.75F + random.nextFloat() * 0.5F);

            for (int i = 0; i < 50; i++)
                ((ServerLevel) level).sendParticles(
                        ParticleUtils.constructSimpleSpark(new Color(100 + random.nextInt(156), random.nextInt(100 + random.nextInt(156)), random.nextInt(100 + random.nextInt(156))), 0.5F, 60, 0.95F),
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        1,
                        (random.nextDouble() - 0.5) * 3.0,
                        random.nextDouble() * 1.5,
                        (random.nextDouble() - 0.5) * 3.0,
                        0.05
                );
        }
    }
}
