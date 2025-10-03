package it.hurts.octostudios.rarcompat.items.hands;

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
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class FeralClawsItem extends WearableRelicItem {

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("claws")
                                .stat(StatData.builder("modifier")
                                        .initialValue(0.05D, 0.15D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.12D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff71e00c)
                                .borderBottom(0xff198915)
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

    public static void resetAttribute(Player player, ItemStack stack, FeralClawsItem relic) {
        EntityUtils.resetAttribute(player, stack, Attributes.ATTACK_SPEED, (float) (getAttackCount(stack) * relic.getAbilityValue(stack, "claws", "modifier")), AttributeModifier.Operation.MULTIPLY_BASE);
    }

    public static void addTime(ItemStack stack, int val) {
        NBTUtils.setInt(stack, "time", Math.max(0, NBTUtils.getInt(stack, "time", 0) + val));
    }

    public static int getTime(ItemStack stack) {
        return NBTUtils.getInt(stack, "time", 0);
    }

    public static void addAttackCount(ItemStack stack, int val) {
        NBTUtils.setInt(stack, "count", Math.max(0, getAttackCount(stack) + val));
    }

    public static int getAttackCount(ItemStack stack) {
        return NBTUtils.getInt(stack, "count", 0);
    }

    @Override
    public void onUnequip(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player))
            return;

        ItemStack currentStack = EntityUtils.findEquippedCurio(player, ModItems.FERAL_CLAWS.get());

        if (!currentStack.isEmpty())
            return;

        EntityUtils.removeAttribute(entity, stack, Attributes.ATTACK_SPEED, AttributeModifier.Operation.MULTIPLY_BASE);
    }


    @Mod.EventBusSubscriber
    public static class FeralClawsEvent {

        @SubscribeEvent
        public static void onPlayerAttack(AttackEntityEvent event) {
            Player player = event.getEntity();

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.FERAL_CLAWS.get());

            if (!(stack.getItem() instanceof FeralClawsItem relic) || !relic.canPlayerUseActiveAbility(player, stack, "claws"))
                return;

            if (player.getAttackStrengthScale(0) != 1F)
                relic.spreadExperience(player, stack, 1);

            addAttackCount(stack, 1);
            addTime(stack, -getTime(stack));

            resetAttribute(player, stack, relic);
        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            Player player = event.player;

            if (event.phase != TickEvent.Phase.END || player.getCommandSenderWorld().isClientSide)
                return;

            if (player.tickCount % 20 != 0)
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.FERAL_CLAWS.get());

            if (!(stack.getItem() instanceof FeralClawsItem relic) || !relic.canPlayerUseActiveAbility(player, stack, "claws"))
                return;

            addTime(stack, 1);

            int time = getTime(stack);
            int attackCount = getAttackCount(stack);

            if (player.getAttackStrengthScale(0) != 1F) {
                EntityUtils.removeAttribute(player, stack, Attributes.ATTACK_SPEED, AttributeModifier.Operation.MULTIPLY_BASE);
                addAttackCount(stack, -attackCount);
            }

            if (time >= 3) {
                if (attackCount <= 0) {
                    EntityUtils.removeAttribute(player, stack, Attributes.ATTACK_SPEED, AttributeModifier.Operation.MULTIPLY_BASE);
                }

                if (time % 3 == 0 && attackCount > 0) {
                    addAttackCount(stack, -1);
                    resetAttribute(player, stack, relic);
                }

                addTime(stack, -time);
            }
        }
    }
}