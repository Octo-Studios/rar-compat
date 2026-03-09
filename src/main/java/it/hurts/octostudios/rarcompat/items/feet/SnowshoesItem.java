package it.hurts.octostudios.rarcompat.items.feet;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.init.DataComponentRegistry;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

public class SnowshoesItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("snow")
                                .rankModifier(1, "powder_walk")
                                .rankModifier(3, "frost_immunity")
                                .rankModifier(5, "linger")
                                .stat(AbilityStatTemplate.builder("speed_bonus")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.1D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("linger_duration")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(1D, 6D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide())
            return;

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("snow");

        if (!ability.canPlayerUse(player)) {
            setLingerTicks(stack, 0);
            EntityUtils.removeAttribute(player, stack, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

            return;
        }

        var onSnow = isOnSnowTerrain(player);
        var active = onSnow;

        if (onSnow) {
            if (ability.isRankModifierUnlocked("linger"))
                setLingerTicks(stack, getLingerDurationTicks(ability.getStatData("linger_duration").getValue()));
            else
                setLingerTicks(stack, 0);
        } else if (ability.isRankModifierUnlocked("linger")) {
            var ticks = getLingerTicks(stack);

            if (ticks > 0) {
                setLingerTicks(stack, ticks - 1);
                active = true;
            } else {
                active = false;
            }
        } else {
            setLingerTicks(stack, 0);
        }

        if (ability.isRankModifierUnlocked("frost_immunity") && player.getTicksFrozen() > 0)
            player.setTicksFrozen(0);

        var speedBonus = active ? Math.max(0D, ability.getStatData("speed_bonus").getValue()) : 0D;

        if (speedBonus <= 0D)
            EntityUtils.removeAttribute(player, stack, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        else
            EntityUtils.resetAttribute(player, stack, Attributes.MOVEMENT_SPEED, (float) speedBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean canWalkOnPowderedSnow(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player))
            return false;

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("snow");

        return ability.canPlayerUse(player) && ability.isRankModifierUnlocked("powder_walk");
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (stack.getItem() == newStack.getItem() || !(slotContext.entity() instanceof Player player))
            return;

        setLingerTicks(stack, 0);
        EntityUtils.removeAttribute(player, stack, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static boolean isOnSnowTerrain(Player player) {
        var level = player.level();
        var feetPos = player.blockPosition();

        return player.isInPowderSnow()
                || level.getBlockState(feetPos).is(BlockTags.SNOW)
                || level.getBlockState(feetPos.below()).is(BlockTags.SNOW);
    }

    private static int getLingerDurationTicks(double seconds) {
        return Math.max(0, (int) Math.round(Math.max(0D, seconds) * 20D));
    }

    private int getLingerTicks(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(DataComponentRegistry.SNOWSHOES_LINGER_TICKS.get(), 0));
    }

    private void setLingerTicks(ItemStack stack, int ticks) {
        stack.set(DataComponentRegistry.SNOWSHOES_LINGER_TICKS.get(), Math.max(0, ticks));
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || !event.getSource().is(DamageTypes.FREEZE))
                return;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.SNOWSHOES.value())) {
                if (!(stack.getItem() instanceof SnowshoesItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("snow");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("frost_immunity"))
                    continue;

                event.setAmount(0F);
                event.setCanceled(true);
                player.setTicksFrozen(0);

                return;
            }
        }
    }
}
