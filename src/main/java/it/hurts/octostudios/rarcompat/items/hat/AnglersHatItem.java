package it.hurts.octostudios.rarcompat.items.hat;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.octostudios.rarcompat.utils.MathBaseUtils;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class AnglersHatItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("catch")
                                .stat(StatData.builder("chance")
                                        .initialValue(0.1D, 0.2D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.25)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())

                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xffa09088)
                                .borderBottom(0xff524742)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.AQUATIC)
                        .entry(LootCollections.VILLAGE)
                        .build())
                .build();
    }

    @Mod.EventBusSubscriber
    public static class AnglersHatEvent {
        @SubscribeEvent
        public static void onItemFished(ItemFishedEvent event) {
            var player = event.getEntity();
            var level = player.getCommandSenderWorld();

            var stack = EntityUtils.findEquippedCurio(player, ModItems.ANGLERS_HAT.get());

            if (level.isClientSide() || !(stack.getItem() instanceof AnglersHatItem relic) || !relic.canPlayerUseActiveAbility(player, stack, "catch"))
                return;

            var serverLevel = (ServerLevel) level;
            var random = serverLevel.getRandom();

            var rolls = MathBaseUtils.multicast(random, relic.getAbilityValue(stack, "catch", "chance"), 1F);

            if (rolls > 0)
                relic.spreadExperience(player, stack, random.nextInt(rolls) + 1);

            LootTable loottable = serverLevel.getServer().getLootData().getLootTable(BuiltInLootTables.FISHING);

            LootParams lootparams = new LootParams.Builder(serverLevel)
                    .withParameter(LootContextParams.ORIGIN, player.position())
                    .withParameter(LootContextParams.TOOL, stack)
                    .withParameter(LootContextParams.THIS_ENTITY, player)
                    .create(LootContextParamSets.FISHING);

            var fishingHook = event.getHookEntity();

            for (int i = 0; i < rolls; i++)
                for (ItemStack itemstack : loottable.getRandomItems(lootparams)) {
                    ItemEntity itementity = new ItemEntity(serverLevel, fishingHook.getX(), fishingHook.getY(), fishingHook.getZ(), itemstack);

                    double x = player.getX() - fishingHook.getX();
                    double y = player.getY() - fishingHook.getY();
                    double z = player.getZ() - fishingHook.getZ();

                    itementity.setDeltaMovement(x * 0.1, y * 0.1 + Math.sqrt(Math.sqrt(x * x + y * y + z * z)) * 0.08, z * 0.1);

                    serverLevel.addFreshEntity(itementity);
                    serverLevel.addFreshEntity(new ExperienceOrb(serverLevel, player.getX(), player.getY() + 0.5, player.getZ() + 0.5, random.nextInt(6) + 1));
                }
        }
    }
}