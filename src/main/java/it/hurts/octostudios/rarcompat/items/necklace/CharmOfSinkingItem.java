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
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.LivingBreatheEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.SlotContext;

public class CharmOfSinkingItem extends WearableRelicItem {

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("dive")
                                .maxLevel(0)
                                .build())
                        .ability(AbilityData.builder("dipping")
                                .stat(StatData.builder("air")
                                        .initialValue(0.2D, 0.4D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.2)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff3c7090)
                                .borderBottom(0xff1c212d)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.AQUATIC)
                        .build())
                .build();
    }

    @Override
    public void wornTick(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player) || !canUseAbility(stack, "dive"))
            return;

        if (player.isUnderWater()) {
            EntityUtils.applyAttribute(player, stack,ForgeMod.ENTITY_GRAVITY.get(), 2F, AttributeModifier.Operation.MULTIPLY_TOTAL);

            if (!canUseAbility(stack, "dipping"))
                return;

            if (!player.onGround())
                setToggled(stack, true);

            if (player.tickCount % 20 == 0 && player.onGround() && player.getMaxAirSupply() > player.getAirSupply()) {
                player.setAirSupply(Math.min(player.getMaxAirSupply(), player.getAirSupply() + (int) (getAbilityValue(stack, "dipping", "air") * 15)));

                if (getToggled(stack)) {
                    setToggled(stack, false);
                    spreadExperience(player, stack, 1);
                }
            }
        } else {
            EntityUtils.removeAttribute(player, stack, ForgeMod.ENTITY_GRAVITY.get(), AttributeModifier.Operation.MULTIPLY_TOTAL);
        }
    }

    @Override
    public void onUnequip(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player))
            return;

        EntityUtils.removeAttribute(player, stack, ForgeMod.ENTITY_GRAVITY.get(),  AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    public void setToggled(ItemStack stack, boolean val) {
        NBTUtils.setBoolean(stack, "toggled", val);
    }

    public boolean getToggled(ItemStack stack) {
        return NBTUtils.getBoolean(stack, "toggled", true);
    }

    @Mod.EventBusSubscriber
    public static class CharmOfSinkingEvent {
        @SubscribeEvent
        public static void onPlayerBreathe(LivingBreatheEvent event) {
            if (!(event.getEntity() instanceof Player player))
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.CHARM_OF_SINKING.get());

            if (!(stack.getItem() instanceof CharmOfSinkingItem relic) || !player.isUnderWater()
                    || !relic.canUseAbility(stack, "dipping") || !player.onGround() || player.getAirSupply() == player.getMaxAirSupply())
                return;

            event.setConsumeAirAmount(0);
        }
    }
}
