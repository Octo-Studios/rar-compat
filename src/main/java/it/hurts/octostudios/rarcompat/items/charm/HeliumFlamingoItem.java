package it.hurts.octostudios.rarcompat.items.charm;

import artifacts.registry.ModItems;
import be.florens.expandability.api.forge.PlayerSwimEvent;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.octostudios.rarcompat.network.NetworkHandler;
import it.hurts.octostudios.rarcompat.network.packets.FlamingoSwimPacket;
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
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class HeliumFlamingoItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("flying")
                                .stat(StatData.builder("time")
                                        .initialValue(3D, 5D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.2D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(StatData.builder("speed")
                                        .initialValue(0.2D, 0.3D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.2D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xfff47d92)
                                .borderBottom(0xffb43263)
                                .build())

                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.AQUATIC)
                        .entry(LootCollections.END)
                        .build())
                .build();
    }

    @Override
    public void wornTick(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player) || !canUseAbility(stack, "flying"))
            return;

        if (player.getCommandSenderWorld().isClientSide() && HeliumFlamingoClientEvent.onDoubleJump) {
            HeliumFlamingoClientEvent.ticKCount++;

            if (HeliumFlamingoClientEvent.ticKCount % 10 == 0) {
                HeliumFlamingoClientEvent.onDoubleJump = false;
                HeliumFlamingoClientEvent.ticKCount = 0;
            }
        }

        if (player.onGround() || player.isInWater())
            setTime(stack, 0);

        if (player.tickCount % 20 == 0 && getToggled(stack)) {
            addTime(stack, 1);
            spreadExperience(player, stack, 1);
        }

        if (getTime(stack) >= (int) MathUtils.round(getAbilityValue(stack, "flying", "time"), 0) && !player.isInWater()) {
            player.setDeltaMovement(player.getDeltaMovement().x, -0.25, player.getDeltaMovement().z);
            player.fallDistance = 0;

            setToggled(stack, false);
        }
    }

    @Override
    public void onUnequip(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player))
            return;

        EntityUtils.removeAttribute(player, stack, ForgeMod.SWIM_SPEED.get(), AttributeModifier.Operation.MULTIPLY_TOTAL);

        setToggled(stack, false);
        setTime(stack, 0);
    }

    public boolean getToggled(ItemStack stack) {
        return NBTUtils.getBoolean(stack, "toggled", false);
    }

    public void setToggled(ItemStack stack, boolean val) {
        NBTUtils.setBoolean(stack, "toggled", val);
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

    @Mod.EventBusSubscriber(Dist.CLIENT)
    public static class HeliumFlamingoClientEvent {
        private static boolean onDoubleJump = false;
        private static int ticKCount;

        @SubscribeEvent
        public static void onClientTick(InputEvent.Key event) {
            var minecraft = Minecraft.getInstance();
            var player = minecraft.player;

            if (player == null)
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.HELIUM_FLAMINGO.get());

            if (minecraft.screen != null || event.getAction() != 1 || !(stack.getItem() instanceof HeliumFlamingoItem relic)
                    || !relic.canPlayerUseActiveAbility(player, stack, "flying") || event.getKey() != minecraft.options.keyJump.getKey().getValue())
                return;

            if (!onDoubleJump)
                onDoubleJump = true;
            else {
                if (!player.getAbilities().mayfly) {
                    var statValue = (int) MathUtils.round(relic.getAbilityValue(stack, "flying", "time"), 0);
                    var time = relic.getTime(stack);

                    if (time >= statValue)
                        return;

                    NetworkHandler.sendToServer(new FlamingoSwimPacket(!relic.getToggled(stack)));

                    if (!relic.getToggled(stack) && !player.isInWater())
                        player.setDeltaMovement(player.getDeltaMovement().add(player.getLookAngle().scale(0.6F)));
                } else {
                    if (relic.getToggled(stack))
                        NetworkHandler.sendToServer(new FlamingoSwimPacket(false));
                }
            }
        }
    }

    @Mod.EventBusSubscriber
    public static class HeliumFlamingoEvent {
        @SubscribeEvent
        public static void onSwimAir(PlayerSwimEvent event) {
            Player player = event.getEntity();
            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.HELIUM_FLAMINGO.get());

            if (!(stack.getItem() instanceof HeliumFlamingoItem relic) || !relic.canUseAbility(stack, "flying"))
                return;

            if (relic.getToggled(stack)) {
                event.setResult(Event.Result.ALLOW);
                player.setSprinting(true);

                EntityUtils.applyAttribute(player, stack, ForgeMod.SWIM_SPEED.get(), (float) relic.getAbilityValue(stack, "flying", "speed"), AttributeModifier.Operation.MULTIPLY_TOTAL);
            } else {
                event.setResult(Event.Result.DEFAULT);

                EntityUtils.removeAttribute(player, stack, ForgeMod.SWIM_SPEED.get(), AttributeModifier.Operation.MULTIPLY_TOTAL);
            }
        }
    }
}