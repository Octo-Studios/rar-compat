package it.hurts.octostudios.rarcompat.items.feet;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicAttributeModifier;
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
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.network.packets.PacketItemActivation;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import it.hurts.sskirillss.relics.utils.NBTUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
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
                                .build())
                        .ability(AbilityData.builder("resurrected")
                                .requiredLevel(5)
                                .stat(StatData.builder("chance")
                                        .initialValue(0.05D, 0.1D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.25)
                                        .formatValue(value -> MathUtils.round(value * 100, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xffededed)
                                .borderBottom(0xff696969)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(15)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.JUNGLE)
                        .entry(LootCollections.VILLAGE)
                        .build())
                .build();
    }

    @Override
    public void wornTick(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player) || player.getCommandSenderWorld().isClientSide())
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

    @Mod.EventBusSubscriber
    public static class KittySlippersEvent {
        @SubscribeEvent
        public static void onLivingChangeTargetEvent(LivingChangeTargetEvent event) {
            if ((event.getEntity() instanceof Creeper || event.getEntity() instanceof Phantom) && event.getNewTarget() instanceof Player player) {
                var itemStack = EntityUtils.findEquippedCurio(player, ModItems.KITTY_SLIPPERS.get());

                if (!(itemStack.getItem() instanceof KittySlippersItem))
                    return;

                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onLivingHurt(LivingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player))
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.KITTY_SLIPPERS.get());

            if (!(stack.getItem() instanceof KittySlippersItem))
                return;

            NBTUtils.setInt(stack, "count", (int) event.getAmount());
        }


        @SubscribeEvent
        public static void onPlayerFall(LivingFallEvent event) {
            if (!(event.getEntity() instanceof Player player))
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.KITTY_SLIPPERS.get());

            if (!(stack.getItem() instanceof KittySlippersItem relic))
                return;

            var distance = event.getDistance();
            var modifier = (float) relic.getAbilityValue(stack, "fall", "modifier");

            if (distance <= modifier)
                event.setDistance(0);
            else
                event.setDistance(distance - modifier);

            if (distance > 4.0F)
                relic.spreadExperience(player, stack, 1);
        }


        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            if (!(event.getEntity() instanceof Player player))
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.KITTY_SLIPPERS.get());

            var level = player.getCommandSenderWorld();
            var random = player.getRandom();

            if (!(stack.getItem() instanceof KittySlippersItem relic) || level.isClientSide() || !relic.canPlayerUseActiveAbility(player, stack, "resurrected")
                    || random.nextFloat() > relic.getAbilityValue(stack, "resurrected", "chance"))
                return;

            NetworkHandler.sendToClient(new PacketItemActivation(stack), (ServerPlayer) player);

            level.playSound(null, player, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 0.9F + random.nextFloat() * 0.2F);

            relic.spreadExperience(player, stack, NBTUtils.getInt(stack,"count", 1));

            player.setHealth(1.0F);

            event.setCanceled(true);

            NBTUtils.setBoolean(stack,"toggled", false);

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
