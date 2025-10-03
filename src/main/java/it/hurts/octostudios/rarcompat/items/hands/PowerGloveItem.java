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
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;

public class PowerGloveItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("power")
                                .stat(StatData.builder("amount")
                                        .initialValue(0.8D, 1D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.4D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 0))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xffd8b200)
                                .borderBottom(0xffb57e00)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.VILLAGE)
                        .build())
                .build();
    }

    @Override
    public void wornTick(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player) || player.tickCount % 20 != 0 || getTime(stack) > 5
                || !canPlayerUseActiveAbility(player, stack, "power"))
            return;

        addTime(stack, 1);
    }

    public void addTime(ItemStack stack, int val) {
        setTime(stack, getTime(stack) + val);
    }

    public int getTime(ItemStack stack) {
        return NBTUtils.getInt(stack, "time", 0);
    }

    public void setTime(ItemStack stack, int val) {
        NBTUtils.setInt(stack, "time", Math.max(val, 0));
    }

    @Mod.EventBusSubscriber
    public static class PowerGloveEvent {
        @SubscribeEvent
        public static void onPlayerAttack(LivingHurtEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player))
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.POWER_GLOVE.get());

            if (!(stack.getItem() instanceof PowerGloveItem relic) || relic.getTime(stack) < 5
                    || !relic.canPlayerUseActiveAbility(player, stack, "power"))
                return;

            relic.spreadExperience(player, stack, 1);
            relic.setTime(stack, 0);

            event.setAmount((float) (event.getAmount() + (event.getAmount() * relic.getAbilityValue(stack, "power", "amount"))));

            var random = player.getRandom();

            for (int i = 0; i < 20; i++) {
                ((ServerLevel) player.getCommandSenderWorld()).sendParticles(ParticleUtils.constructSimpleSpark(new Color(200 + random.nextInt(55), 100 + random.nextInt(100), random.nextInt(50)),
                                0.7F, 60, 0.9F), player.getX(), player.getY() + 1.0, player.getZ(),
                        1, 0.0, 0.5, 0.0, 0.05
                );
            }
        }

    }
}
