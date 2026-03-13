package it.hurts.octostudios.rarcompat.items.hat;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.AbilityMetricTemplate;
import it.hurts.sskirillss.relics.api.relics.AbilityStatisticTemplate;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.VisibilityState;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.ExperienceSourceTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.ExperienceSourcesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;

import java.util.List;

public class AnglersHatItem extends WearableRelicItem {
    private static final TagKey<Item> ANGLERS_HAT_VALUABLES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RARCompat.MODID, "anglers_hat_valuables"));

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("catch")
                                .rankModifier(1, "quick_bite")
                                .rankModifier(3, "healing")
                                .rankModifier(5, "treasure")
                                .stat(AbilityStatTemplate.builder("chance")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.1D, 0.25D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("heal")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("treasure_chance")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.08D, 0.2D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("max_casts")
                                        .thresholdValue(1D, Double.MAX_VALUE)
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("catch").build())
                                        .source(ExperienceSourceTemplate.builder("fish_eaten").build())
                                        .source(ExperienceSourceTemplate.builder("treasure_catch")
                                                .rankModifierVisibilityState("treasure", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("fish_caught")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("fish_eaten")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("healing_restored")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 1)))
                                                .rankModifierVisibilityState("healing", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("treasures_caught")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("treasure", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @EventBusSubscriber
    public static class CommonEvents {
        @SubscribeEvent
        public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide())
                return;

            var item = event.getItem();

            if (!item.is(ItemTags.FISHES) || item.getUseAnimation() != UseAnim.EAT)
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.ANGLERS_HAT.value());

            if (!(stack.getItem() instanceof AnglersHatItem relic))
                return;

            var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("catch");

            if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("quick_bite"))
                return;

            var duration = event.getDuration();

            if (duration > 1)
                event.setDuration(Math.max(1, duration / 2));
        }

        @SubscribeEvent
        public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide())
                return;

            var item = event.getItem();

            if (!item.is(ItemTags.FISHES) || item.getUseAnimation() != UseAnim.EAT)
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.ANGLERS_HAT.value());

            if (!(stack.getItem() instanceof AnglersHatItem relic))
                return;

            var relicData = relic.getRelicData(player, stack);
            var ability = relicData.getAbilitiesData().getAbilityData("catch");

            if (!ability.canPlayerUse(player))
                return;

            ability.getStatisticData().getMetricData("fish_eaten").addValue(1D);
            relicData.getLevelingData().addExperience("catch", "fish_eaten", 1D);

            if (!ability.isRankModifierUnlocked("healing"))
                return;

            var heal = (float) Math.max(0D, ability.getStatData("heal").getValue());

            if (heal <= 0F)
                return;

            var beforeHealth = player.getHealth();

            player.heal(heal);

            var restoredHealth = Math.max(0F, player.getHealth() - beforeHealth);

            if (restoredHealth > 0F)
                ability.getStatisticData().getMetricData("healing_restored").addValue(restoredHealth);
        }

        @SubscribeEvent
        public static void onItemFished(ItemFishedEvent event) {
            var player = event.getEntity();
            var level = player.getCommandSenderWorld();

            var stack = EntityUtils.findEquippedCurio(player, ModItems.ANGLERS_HAT.value());

            if (level.isClientSide() || !(stack.getItem() instanceof AnglersHatItem relic))
                return;

            var relicData = relic.getRelicData(player, stack);
            var ability = relicData.getAbilitiesData().getAbilityData("catch");

            if (!ability.canPlayerUse(player))
                return;

            var serverLevel = (ServerLevel) level;
            var random = serverLevel.getRandom();
            var catches = 0;
            var treasures = 0;
            var treasureChance = 0D;
            var valuables = List.<Item>of();

            if (ability.isRankModifierUnlocked("treasure")) {
                treasureChance = Math.max(0D, Math.min(1D, ability.getStatData("treasure_chance").getValue()));

                if (treasureChance > 0D) {
                    valuables = BuiltInRegistries.ITEM.getTag(ANGLERS_HAT_VALUABLES)
                            .stream()
                            .flatMap(holderSet -> holderSet.stream().map(Holder::value))
                            .filter(item -> !item.getDefaultInstance().is(ItemTags.FISHES))
                            .toList();

                    if (!valuables.isEmpty())
                        for (var i = 0; i < event.getDrops().size(); i++) {
                            var drop = event.getDrops().get(i);

                            if (!drop.is(ItemTags.FISHES) || random.nextDouble() > treasureChance)
                                continue;

                            var replacement = valuables.get(random.nextInt(valuables.size())).getDefaultInstance();

                            if (replacement.is(ItemTags.FISHES))
                                continue;

                            replacement.setCount(drop.getCount());
                            event.getDrops().set(i, replacement);
                            treasures++;
                        }
                }
            }

            catches += event.getDrops().size();

            var chance = Math.max(0D, Math.min(1D, ability.getStatData("chance").getValue()));
            var maxCasts = Math.max(0, (int) MathUtils.round(ability.getStatData("max_casts").getValue(), 0));

            if (chance > 0D && maxCasts > 0) {
                var rolls = MathUtils.multicast(random, chance, maxCasts);

                if (rolls > 0) {
                    LootTable loottable = serverLevel.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);

                    LootParams lootparams = new LootParams.Builder(serverLevel)
                            .withParameter(LootContextParams.ORIGIN, player.position())
                            .withParameter(LootContextParams.TOOL, stack)
                            .withParameter(LootContextParams.THIS_ENTITY, player)
                            .create(LootContextParamSets.FISHING);

                    var fishingHook = event.getHookEntity();

                    for (int i = 0; i < rolls; i++)
                        for (ItemStack itemstack : loottable.getRandomItems(lootparams)) {
                            catches++;

                            var reward = itemstack;

                            if (!valuables.isEmpty() && reward.is(ItemTags.FISHES) && random.nextDouble() <= treasureChance) {
                                var replacement = valuables.get(random.nextInt(valuables.size())).getDefaultInstance();

                                if (!replacement.is(ItemTags.FISHES)) {
                                    replacement.setCount(reward.getCount());
                                    reward = replacement;
                                    treasures++;
                                }
                            }

                            ItemEntity itementity = new ItemEntity(serverLevel, fishingHook.getX(), fishingHook.getY(), fishingHook.getZ(), reward);

                            double x = player.getX() - fishingHook.getX();
                            double y = player.getY() - fishingHook.getY();
                            double z = player.getZ() - fishingHook.getZ();

                            itementity.setDeltaMovement(x * 0.1, y * 0.1 + Math.sqrt(Math.sqrt(x * x + y * y + z * z)) * 0.08, z * 0.1);

                            serverLevel.addFreshEntity(itementity);
                            serverLevel.addFreshEntity(new ExperienceOrb(serverLevel, player.getX(), player.getY() + 0.5, player.getZ() + 0.5, random.nextInt(6) + 1));
                        }
                }
            }

            if (catches > 0) {
                ability.getStatisticData().getMetricData("fish_caught").addValue(catches);
                relicData.getLevelingData().addExperience("catch", "catch", catches);
            }

            if (treasures > 0) {
                ability.getStatisticData().getMetricData("treasures_caught").addValue(treasures);
                relicData.getLevelingData().addExperience("catch", "treasure_catch", treasures);
            }
        }
    }
}
