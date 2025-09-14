package it.hurts.octostudios.rarcompat.items.feet;

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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class RunningShoesItem extends WearableRelicItem {

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("runner")
                                .stat(StatData.builder("speed")
                                        .initialValue(0.5D, 0.8D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.2D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xffd53828)
                                .borderBottom(0xffb2120d)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.VILLAGE)
                        .entry(LootCollections.JUNGLE)
                        .build())
                .build();
    }

    @Override
    public void onUnequip(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player))
            return;

        AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);

        if (speedAttribute == null)
            return;

        speedAttribute.setBaseValue(0.1);
    }

    @Mod.EventBusSubscriber
    public static class RunningShoesEvent {
        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            Player player = event.player;
            if (event.phase != TickEvent.Phase.END || player.getCommandSenderWorld().isClientSide())
                return;

            AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttribute == null || player.isSwimming())
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.RUNNING_SHOES.get());
            if (!(stack.getItem() instanceof RunningShoesItem relic) || !relic.canUseAbility(stack, "runner")) {
                return;
            }


            double speedIncrement = relic.getAbilityValue(stack, "runner", "speed") / 1000.0;
            double currentSpeed = NBTUtils.getDouble(stack, "speed", 0.1);
            double newSpeed;

            if (!player.onGround()) {
                speedAttribute.setBaseValue(currentSpeed);
                return;
            }

            if (player.isSprinting()) {
                newSpeed = Math.min(currentSpeed + speedIncrement, 3 * 0.1);

                if (player.tickCount % 20 == 0)
                    relic.spreadExperience(player, stack, 1);
            } else {
                newSpeed = Math.max(currentSpeed - speedIncrement * 4.0, 0.1);
            }
            speedAttribute.setBaseValue(newSpeed);
            NBTUtils.setDouble(stack, "speed", newSpeed);
        }
    }
}