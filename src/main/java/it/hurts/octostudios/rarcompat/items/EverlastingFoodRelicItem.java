package it.hurts.octostudios.rarcompat.items;

import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.init.DataComponentRegistry;
import it.hurts.sskirillss.relics.init.RelicsCreativeTabs;
import it.hurts.sskirillss.relics.items.misc.CreativeContentConstructor;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import top.theillusivec4.curios.api.SlotContext;

import java.util.ArrayList;
import java.util.List;

public abstract class EverlastingFoodRelicItem extends RelicItem {
    protected EverlastingFoodRelicItem(FoodProperties foodProperties) {
        super(new Item.Properties()
                .rarity(Rarity.EPIC)
                .food(foodProperties)
                .setNoRepair());
    }

    @Override
    public void gatherCreativeTabContent(CreativeContentConstructor constructor) {
        constructor.entry(RelicsCreativeTabs.RELICS_TAB.get(), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY, this);
    }

    @Override
    public List<Component> getAttributesTooltip(List<Component> tooltips, Item.TooltipContext context, ItemStack stack) {
        return new ArrayList<>();
    }

    @Override
    public String getConfigRoute() {
        return RARCompat.MODID;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return false;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);

        if (isExhausted(player, stack))
            return InteractionResultHolder.fail(stack);

        if (!player.canEat(stack.getFoodProperties(player).canAlwaysEat()))
            return InteractionResultHolder.fail(stack);

        player.startUsingItem(hand);

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (stack.has(DataComponents.FOOD))
            livingEntity.eat(level, stack.copy());

        if (!level.isClientSide)
            syncDurability(livingEntity, stack);

        if (level.isClientSide || !stack.isDamageableItem())
            return stack;

        var ability = this.getRelicData(livingEntity, stack).getAbilitiesData().getAbilityData("meal");

        if (ability.canPlayerUse(livingEntity)) {
            ability.getStatisticData().getMetricData("consumes").addValue(1D);
            ability.getStatisticData().getMetricData("consume_duration").addValue(getConsumeDurationSeconds(livingEntity, stack));
            this.getRelicData(livingEntity, stack).getLevelingData().addExperience("meal", "consume", 1D);
        }

        if (ability.canPlayerUse(livingEntity) && ability.isRankModifierUnlocked("restoration")) {
            var heal = (float) Math.max(0D, ability.getStatData("healing").getValue());

            if (heal > 0F) {
                var beforeHealth = livingEntity.getHealth();

                livingEntity.heal(heal);

                var restored = Math.max(0F, livingEntity.getHealth() - beforeHealth);

                if (restored > 0F) {
                    ability.getStatisticData().getMetricData("healed").addValue(restored);
                    this.getRelicData(livingEntity, stack).getLevelingData().addExperience("meal", "healing", restored);
                }
            }
        }

        var consumeDurability = true;

        if (ability.canPlayerUse(livingEntity) && ability.isRankModifierUnlocked("preservation")) {
            var chance = Math.max(0D, Math.min(1D, ability.getStatData("preservation_chance").getValue()));

            if (chance > 0D)
                consumeDurability = livingEntity.getRandom().nextDouble() > chance;
        }

        if (!consumeDurability)
            return stack;

        stack.setDamageValue(Math.min(stack.getMaxDamage(), stack.getDamageValue() + 1));

        if (stack.getDamageValue() > 0)
            stack.set(DataComponentRegistry.EVERLASTING_FOOD_REGEN_TICKS.get(), getRegenerationTicks(livingEntity, stack));

        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide || !stack.isDamageableItem())
            return;

        if (!(entity instanceof LivingEntity livingEntity))
            return;

        syncDurability(livingEntity, stack);

        if (stack.getDamageValue() <= 0) {
            stack.set(DataComponentRegistry.EVERLASTING_FOOD_REGEN_TICKS.get(), 0);
            return;
        }

        var regenerationTicks = getRegenerationTicks(livingEntity, stack);
        var timer = Math.max(0, stack.getOrDefault(DataComponentRegistry.EVERLASTING_FOOD_REGEN_TICKS.get(), regenerationTicks));

        if (timer <= 0)
            timer = regenerationTicks;

        timer--;

        if (timer <= 0) {
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
            timer = stack.getDamageValue() > 0 ? regenerationTicks : 0;
        }

        stack.set(DataComponentRegistry.EVERLASTING_FOOD_REGEN_TICKS.get(), timer);
    }

    protected boolean isExhausted(ItemStack stack) {
        return stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage();
    }

    protected boolean isExhausted(LivingEntity livingEntity, ItemStack stack) {
        syncDurability(livingEntity, stack);

        return isExhausted(stack);
    }

    private int getRegenerationTicks(LivingEntity livingEntity, ItemStack stack) {
        var ability = this.getRelicData(livingEntity, stack).getAbilitiesData().getAbilityData("meal");
        var regenerationSeconds = Math.max(0.05D, ability.getStatData("regeneration").getValue());

        return Math.max(1, (int) Math.round(regenerationSeconds * 20D));
    }

    private double getConsumeDurationSeconds(LivingEntity livingEntity, ItemStack stack) {
        var durationTicks = Math.max(1, this.getUseDuration(stack, livingEntity));
        var ability = this.getRelicData(livingEntity, stack).getAbilitiesData().getAbilityData("meal");

        if (ability.canPlayerUse(livingEntity) && ability.isRankModifierUnlocked("quick_meal")) {
            var speed = Math.max(0D, Math.min(0.95D, ability.getStatData("consume_speed").getValue()));

            if (speed > 0D)
                durationTicks = Math.max(1, (int) Math.round(durationTicks / (1D + speed)));
        }

        return durationTicks / 20D;
    }

    private int getMaxDurability(LivingEntity livingEntity, ItemStack stack) {
        var ability = this.getRelicData(livingEntity, stack).getAbilitiesData().getAbilityData("meal");
        var durability = Math.max(1D, ability.getStatData("durability").getValue());

        return Math.max(1, (int) Math.round(durability));
    }

    private void syncDurability(LivingEntity livingEntity, ItemStack stack) {
        if (!stack.isDamageableItem())
            return;

        var maxDurability = getMaxDurability(livingEntity, stack);
        stack.set(DataComponents.MAX_DAMAGE, maxDurability);

        if (stack.getDamageValue() > maxDurability)
            stack.setDamageValue(maxDurability);
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
            if (event.getEntity().level().isClientSide())
                return;

            var stack = event.getItem();

            if (!(stack.getItem() instanceof EverlastingFoodRelicItem relic) || relic.isExhausted(event.getEntity(), stack))
                return;

            var ability = relic.getRelicData(event.getEntity(), stack).getAbilitiesData().getAbilityData("meal");

            if (!ability.canPlayerUse(event.getEntity()) || !ability.isRankModifierUnlocked("quick_meal"))
                return;

            var speed = Math.max(0D, Math.min(0.95D, ability.getStatData("consume_speed").getValue()));

            if (speed <= 0D)
                return;

            var duration = event.getDuration();

            if (duration > 1)
                event.setDuration(Math.max(1, (int) Math.round(duration / (1D + speed))));
        }
    }
}