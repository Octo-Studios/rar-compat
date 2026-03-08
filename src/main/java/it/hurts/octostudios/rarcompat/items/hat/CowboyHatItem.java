package it.hurts.octostudios.rarcompat.items.hat;

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
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import top.theillusivec4.curios.api.SlotContext;

public class CowboyHatItem extends WearableRelicItem {
    private static final int NO_MOUNT = -1;

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("riding")
                                .rankModifier(1, "taming")
                                .rankModifier(3, "reach")
                                .rankModifier(5, "absorption")
                                .stat(AbilityStatTemplate.builder("amount")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(0.1D, 0.25D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("reach")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(0.15D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("absorption")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(4D, 10D)
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

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("riding");

        if (!ability.canPlayerUse(player)) {
            clearMountBuff(player, stack);
            removeRiderReachBuff(player, stack);
            removeRiderAbsorptionBonus(player, stack);
            return;
        }

        if (!(player.getVehicle() instanceof LivingEntity mounted)) {
            clearMountBuff(player, stack);
            removeRiderReachBuff(player, stack);
            removeRiderAbsorptionBonus(player, stack);
            return;
        }

        var amount = Math.max(0F, (float) ability.getStatData("amount").getValue());

        if (amount > 0F) {
            syncMountedEntity(player, stack, mounted);
            applyMountedBuff(stack, mounted, amount);
        } else {
            clearMountBuff(player, stack);
        }

        if (!ability.isRankModifierUnlocked("reach")) {
            removeRiderReachBuff(player, stack);
        } else {
            var reach = Math.max(0F, (float) ability.getStatData("reach").getValue());

            if (reach > 0F)
                applyRiderReachBuff(player, stack, reach);
            else
                removeRiderReachBuff(player, stack);
        }

        if (!ability.isRankModifierUnlocked("absorption")) {
            removeRiderAbsorptionBonus(player, stack);
            return;
        }

        var absorption = Math.max(0D, ability.getStatData("absorption").getValue());

        if (absorption > 0D)
            applyRiderAbsorptionBonus(player, stack, absorption);
        else
            removeRiderAbsorptionBonus(player, stack);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide() || stack.getItem() == newStack.getItem())
            return;

        clearMountBuff(player, stack);
        removeRiderReachBuff(player, stack);
        removeRiderAbsorptionBonus(player, stack);
    }

    private void syncMountedEntity(Player player, ItemStack stack, LivingEntity mounted) {
        var lastMountId = getLastMountId(stack);

        if (lastMountId == mounted.getId())
            return;

        removeMountBuffById(player, stack, lastMountId);
        setLastMountId(stack, mounted.getId());
    }

    private void clearMountBuff(Player player, ItemStack stack) {
        removeMountBuffById(player, stack, getLastMountId(stack));
        setLastMountId(stack, NO_MOUNT);
    }

    private void removeMountBuffById(Player player, ItemStack stack, int entityId) {
        if (entityId == NO_MOUNT)
            return;

        if (player.level().getEntity(entityId) instanceof LivingEntity mounted)
            removeMountedBuff(stack, mounted);
    }

    private void applyMountedBuff(ItemStack stack, LivingEntity mounted, float amount) {
        resetLivingAttribute(mounted, stack, Attributes.MAX_HEALTH, amount);
        resetLivingAttribute(mounted, stack, Attributes.MOVEMENT_SPEED, amount);
        resetLivingAttribute(mounted, stack, Attributes.JUMP_STRENGTH, amount);
        resetLivingAttribute(mounted, stack, Attributes.ATTACK_DAMAGE, amount);
        resetLivingAttribute(mounted, stack, Attributes.ATTACK_KNOCKBACK, amount);
        resetLivingAttribute(mounted, stack, Attributes.FLYING_SPEED, amount);
        resetLivingAttribute(mounted, stack, Attributes.ARMOR, amount);
        resetLivingAttribute(mounted, stack, Attributes.ARMOR_TOUGHNESS, amount);
        resetLivingAttribute(mounted, stack, Attributes.KNOCKBACK_RESISTANCE, amount);
    }

    private void removeMountedBuff(ItemStack stack, LivingEntity mounted) {
        removeLivingAttribute(mounted, stack, Attributes.MAX_HEALTH);
        removeLivingAttribute(mounted, stack, Attributes.MOVEMENT_SPEED);
        removeLivingAttribute(mounted, stack, Attributes.JUMP_STRENGTH);
        removeLivingAttribute(mounted, stack, Attributes.ATTACK_DAMAGE);
        removeLivingAttribute(mounted, stack, Attributes.ATTACK_KNOCKBACK);
        removeLivingAttribute(mounted, stack, Attributes.FLYING_SPEED);
        removeLivingAttribute(mounted, stack, Attributes.ARMOR);
        removeLivingAttribute(mounted, stack, Attributes.ARMOR_TOUGHNESS);
        removeLivingAttribute(mounted, stack, Attributes.KNOCKBACK_RESISTANCE);
    }

    private void applyRiderReachBuff(Player player, ItemStack stack, float amount) {
        resetLivingAttribute(player, stack, Attributes.ENTITY_INTERACTION_RANGE, amount);
        resetLivingAttribute(player, stack, Attributes.BLOCK_INTERACTION_RANGE, amount);
    }

    private void removeRiderReachBuff(Player player, ItemStack stack) {
        removeLivingAttribute(player, stack, Attributes.ENTITY_INTERACTION_RANGE);
        removeLivingAttribute(player, stack, Attributes.BLOCK_INTERACTION_RANGE);
    }

    private void applyRiderAbsorptionBonus(Player player, ItemStack stack, double bonus) {
        var remaining = getRiderAbsorptionRemaining(stack);

        if (remaining <= 0D) {
            player.setAbsorptionAmount((float) (Math.max(0D, player.getAbsorptionAmount()) + bonus));
            setRiderAbsorptionRemaining(stack, bonus);
            return;
        }

        var current = Math.max(0D, player.getAbsorptionAmount());

        setRiderAbsorptionRemaining(stack, Math.min(remaining, current));
    }

    private void removeRiderAbsorptionBonus(Player player, ItemStack stack) {
        var remaining = getRiderAbsorptionRemaining(stack);

        if (remaining <= 0D)
            return;

        var current = Math.max(0D, player.getAbsorptionAmount());

        player.setAbsorptionAmount((float) Math.max(0D, current - remaining));
        setRiderAbsorptionRemaining(stack, 0D);
    }

    private void resetLivingAttribute(LivingEntity entity, ItemStack stack, Holder<Attribute> attribute, float amount) {
        if (entity.getAttribute(attribute) != null)
            EntityUtils.resetAttribute(entity, stack, attribute, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private void removeLivingAttribute(LivingEntity entity, ItemStack stack, Holder<Attribute> attribute) {
        if (entity.getAttribute(attribute) != null)
            EntityUtils.removeAttribute(entity, stack, attribute, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private int getLastMountId(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.COWBOY_HAT_LAST_MOUNT_ID.get(), NO_MOUNT);
    }

    private void setLastMountId(ItemStack stack, int id) {
        stack.set(DataComponentRegistry.COWBOY_HAT_LAST_MOUNT_ID.get(), id);
    }

    private double getRiderAbsorptionRemaining(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.COWBOY_HAT_ABSORPTION_REMAINING.get(), 0D);
    }

    private void setRiderAbsorptionRemaining(ItemStack stack, double value) {
        stack.set(DataComponentRegistry.COWBOY_HAT_ABSORPTION_REMAINING.get(), Math.max(0D, value));
    }

    public static boolean canUseTamingModifier(Player player) {
        var stack = EntityUtils.findEquippedCurio(player, ModItems.COWBOY_HAT.value());

        if (!(stack.getItem() instanceof CowboyHatItem relic))
            return false;

        return relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("riding").canPlayerUse(player)
                && relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("riding").isRankModifierUnlocked("taming");
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
            tryInstantTame(event.getEntity(), event.getTarget());
        }

        @SubscribeEvent
        public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
            tryInstantTame(event.getEntity(), event.getTarget());
        }

        private static void tryInstantTame(Player player, Entity target) {
            if (player.level().isClientSide() || !(target instanceof AbstractHorse horse) || horse.isTamed())
                return;

            if (!CowboyHatItem.canUseTamingModifier(player))
                return;

            horse.tameWithName(player);
        }
    }
}