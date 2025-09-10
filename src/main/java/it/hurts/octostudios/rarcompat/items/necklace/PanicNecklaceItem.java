package it.hurts.octostudios.rarcompat.items.necklace;

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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class PanicNecklaceItem extends WearableRelicItem {

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("panic")
                                .stat(StatData.builder("movement")
                                        .initialValue(0.1D, 0.2D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> MathUtils.round(value * 10, 1))
                                        .build())
                                .stat(StatData.builder("radius")
                                        .initialValue(6D, 8D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xffdc291b)
                                .borderBottom(0xff57000d)
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

    @Override
    public void onUnequip(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player))
            return;

        EntityUtils.removeAttribute(player, stack, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.MULTIPLY_BASE);
    }

    public static void addSpeed(ItemStack stack, double val) {
        setSpeed(stack, getSpeed(stack) + val);
    }

    public static double getSpeed(ItemStack stack) {
        return NBTUtils.getDouble(stack, "speed", 0D);
    }

    public static void setSpeed(ItemStack stack, double val) {
        NBTUtils.setDouble(stack, "speed", Math.max(val, 0D));
    }

    @Mod.EventBusSubscriber
    public static class PanicNecklaceEvent {
        @SubscribeEvent
        public static void onPlayerDamage(LivingHurtEvent event) {
            if (!(event.getEntity() instanceof Player player) || !(event.getSource().getEntity() instanceof Mob))
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.PANIC_NECKLACE.get());

            if (!(stack.getItem() instanceof PanicNecklaceItem relic) || !relic.canUseAbility(stack, "panic"))
                return;

            relic.spreadExperience(player, stack, 1);
        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            Player player = event.player;
            if (event.phase != TickEvent.Phase.END || player.getCommandSenderWorld().isClientSide())
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.PANIC_NECKLACE.get());
            if (!(stack.getItem() instanceof PanicNecklaceItem relic) || !relic.canPlayerUseActiveAbility(player, stack, "panic"))
                return;

            var radius = relic.getAbilityValue(stack, "panic", "radius");

            double target = player.getCommandSenderWorld().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(radius)).stream().filter(mob -> mob.getTarget() == player).count() * relic.getAbilityValue(stack, "panic", "movement")
                    + player.getCommandSenderWorld().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(radius)).stream().filter(player1 -> !player1.getUUID().equals(player.getUUID())).count();

            double speed = getSpeed(stack);
            double step = 0.01D;
            double modifier = speed < target ? step : speed > target ? -step : 0D;

            if (modifier != 0D)
                addSpeed(stack, modifier);


            EntityUtils.resetAttribute(player, stack, Attributes.MOVEMENT_SPEED, (float) getSpeed(stack), AttributeModifier.Operation.MULTIPLY_BASE);
        }
    }
}