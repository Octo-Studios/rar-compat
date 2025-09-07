package it.hurts.octostudios.rarcompat.items.necklace;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.init.EffectRegistry;
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
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class ScarfOfInvisibilityItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("invisible")
                                .stat(StatData.builder("time")
                                        .initialValue(140D, 100D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, -0.05D)
                                        .formatValue(value -> MathUtils.round(value / 20, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff51a4df)
                                .borderBottom(0xff2c3b70)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.END)
                        .build())
                .build();
    }

    @Override
    public void wornTick(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player) || player.getCommandSenderWorld().isClientSide()
                || !canUseAbility(stack, "invisible"))
            return;

        var time = getTime(stack);

        if (time == 0 && player.getCommandSenderWorld().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(15)).stream().noneMatch(mob -> mob.getTarget() == player)) {
            if (!player.hasEffect(EffectRegistry.VANISHING.get()))
                spreadExperience(player, stack, 1);

            player.addEffect(new MobEffectInstance(EffectRegistry.VANISHING.get(), 4, 0, true, false));
        } else if (time > 0)
            addTime(stack, -1);
    }

    public void addTime(ItemStack stack, int time) {
        setTime(stack, getTime(stack) + time);
    }

    public void setTime(ItemStack stack, int time) {
        NBTUtils.setInt(stack, "time", Math.max(time, 0));
    }

    public int getTime(ItemStack stack) {
        return NBTUtils.getInt(stack, "time", 0);
    }

    @Mod.EventBusSubscriber
    public static class ScarfOfInvisibilityEvent {
        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            onInteract(event.getEntity());
        }

        @SubscribeEvent
        public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
            onInteract(event.getEntity());
        }

        @SubscribeEvent
        public static void onHarvestCheck(PlayerEvent.BreakSpeed event) {
            onInteract(event.getEntity());
        }

        @SubscribeEvent
        public static void onAttacking(AttackEntityEvent event) {
            onInteract(event.getEntity());
        }

        private static void onInteract(Player player) {
            if (player.getCommandSenderWorld().isClientSide())
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.SCARF_OF_INVISIBILITY.get());

            if (!(stack.getItem() instanceof ScarfOfInvisibilityItem relic))
                return;

            relic.setTime(stack, (int) relic.getAbilityValue(stack, "invisible", "time"));
        }
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT)
    public static class ScarfOfInvisibilityClientEvent {
        @SubscribeEvent
        public static void onRenderHand(RenderHandEvent event) {
            Player player = Minecraft.getInstance().player;

            if (player == null)
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.SCARF_OF_INVISIBILITY.get());

            if (!(stack.getItem() instanceof ScarfOfInvisibilityItem relic) || !relic.canUseAbility(stack, "invisible"))
                return;

            if (player.hasEffect(EffectRegistry.VANISHING.get())) {
                event.setCanceled(true);
            }
        }
    }
}