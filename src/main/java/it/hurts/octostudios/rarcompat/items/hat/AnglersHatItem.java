package it.hurts.octostudios.rarcompat.items.hat;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;

public class AnglersHatItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder().build();
    }

    @EventBusSubscriber
    public static class CommonEvents {
        @SubscribeEvent
        public static void onItemFished(ItemFishedEvent event) {
            var player = event.getEntity();
            var level = player.getCommandSenderWorld();

            var stack = EntityUtils.findEquippedCurio(player, ModItems.ANGLERS_HAT.value());

            if (level.isClientSide() || !(stack.getItem() instanceof AnglersHatItem relic))
                return;

            var serverLevel = (ServerLevel) level;
            var random = serverLevel.getRandom();
            var rolls = MathUtils.multicast(random, relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("catch").getStatData("chance").getValue());

            LootTable loottable = serverLevel.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);

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