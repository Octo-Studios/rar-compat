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
import it.hurts.sskirillss.relics.utils.WorldUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

public class SnowshoesItem extends WearableRelicItem {

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("passive")
                                .maxLevel(0)
                                .build())
                        .ability(AbilityData.builder("speed")
                                .stat(StatData.builder("amount")
                                        .initialValue(0.2D, 0.4D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xffae894e)
                                .borderBottom(0xff614126)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.COLD)
                        .build())
                .build();
    }

    public static void addSpeed(ItemStack stack, double val) {
        if (getSpeed(stack) <= ((SnowshoesItem) stack.getItem()).getAbilityValue(stack, "speed", "amount") || val < 0)
            setSpeed(stack, getSpeed(stack) + val);
    }

    public static double getSpeed(ItemStack stack) {
        return NBTUtils.getDouble(stack, "speed", 0D);
    }

    public static void setSpeed(ItemStack stack, double val) {
        NBTUtils.setDouble(stack, "speed", Math.max(val, 0D));
    }

    private static boolean isStandingOnSnow(Player player) {
        for (int i = 0; i < 8; i++)
            if (player.getCommandSenderWorld().getBlockState(player.blockPosition().atY((int) Math.floor(WorldUtils.getGroundHeight(player, player.position().add(0, 0.1, 0), 8))).below(i)).is(BlockTags.SNOW))
                return true;

        return false;
    }


    @Override
    public boolean canWalkOnPowderedSnow() {
        Player player = Minecraft.getInstance().player;

        if (player == null)
            return super.canWalkOnPowderedSnow();

        var stack = EntityUtils.findEquippedCurio(player, ModItems.SNOWSHOES.get());

        return canPlayerUseActiveAbility(player, stack, "passive");
    }

    @Override
    public void onUnequip(LivingEntity entity, ItemStack stack) {
        EntityUtils.removeAttribute(entity, stack, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.MULTIPLY_BASE);
    }

    @Mod.EventBusSubscriber
    public static class SnowshoesEvent {
        private static final Map<Player, Vec3> lastPos = new HashMap<>();

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            Player player = event.player;

            if (event.phase != TickEvent.Phase.END || player.getCommandSenderWorld().isClientSide())
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.SNOWSHOES.get());
            if (!(stack.getItem() instanceof SnowshoesItem relic) || !relic.canPlayerUseActiveAbility(player, stack, "speed"))
                return;

            var prev = lastPos.getOrDefault(player, player.position());
            var pPos = player.position();

            var moved = pPos.distanceToSqr(prev) > 0.0001;

            double step = 0.01D;
            double modifier = isStandingOnSnow(player) ? step : -step;

            if (modifier != 0D)
                addSpeed(stack, modifier);

            EntityUtils.resetAttribute(player, stack, Attributes.MOVEMENT_SPEED, (float) getSpeed(stack), AttributeModifier.Operation.MULTIPLY_BASE);

            if (isStandingOnSnow(player) && player.tickCount % 60 == 0 && moved) {
                relic.spreadExperience(player, stack, 1);
            }
            lastPos.put(player, pPos);
        }

    }
}