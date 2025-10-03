package it.hurts.octostudios.rarcompat.items.hands;

import artifacts.registry.ModItems;
import com.mojang.datafixers.util.Pair;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.CastData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastType;
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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Optional;


public class PickaxeHeaterItem extends WearableRelicItem {
    private static final Container container = new SimpleContainer(3);
    @Nullable
    private static ResourceLocation lastRecipe;

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("heater")
                                .active(CastData.builder()
                                        .type(CastType.TOGGLEABLE)
                                        .build())
                                .stat(StatData.builder("capacity")
                                        .initialValue(20D, 25D)
                                        .upgradeModifier(UpgradeOperation.ADD, 5D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(StatData.builder("duration")
                                        .initialValue(140D, 120D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, -0.083D)
                                        .formatValue(value -> MathUtils.round(value / 20, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff939ca2)
                                .borderBottom(0xff696b7c)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.ANTHROPOGENIC)
                        .build())
                .build();
    }

    @Override
    public void wornTick(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player) || player.tickCount % Math.round(this.getAbilityValue(stack, "heater", "duration")) != 0
                || getCharges(stack) >= Math.round(getAbilityValue(stack, "heater", "capacity")))
            return;

        addCharges(stack, 1);
    }

    public static void addCharges(ItemStack stack, int amount) {
        NBTUtils.setInt(stack, "charge", getCharges(stack) + amount);
    }

    public static int getCharges(ItemStack stack) {
        return NBTUtils.getInt(stack, "charge", 0);
    }

    public static ObjectArrayList<ItemStack> getModifiedBlockDrops(ObjectArrayList<ItemStack> items, LootContext context) {
        if (context.hasParam(LootContextParams.BLOCK_STATE)
                && context.hasParam(LootContextParams.THIS_ENTITY)
                && context.hasParam(LootContextParams.ORIGIN)
                && context.getParam(LootContextParams.THIS_ENTITY) instanceof Player player) {

            var stack = EntityUtils.findEquippedCurio(player, ModItems.PICKAXE_HEATER.get());
            var level = player.getCommandSenderWorld();
            if (level.isClientSide() || !(stack.getItem() instanceof PickaxeHeaterItem relic)
                    || !relic.isAbilityTicking(stack, "heater") || getCharges(stack) <= 1)
                return items;

            var serverLevel = (ServerLevel) level;

            ObjectArrayList<ItemStack> result = new ObjectArrayList<>(items.size());
            boolean hasSmeltableItems = false;

            for (ItemStack item : items) {
                ItemStack resultItem = item;
                Optional<AbstractCookingRecipe> recipe = getRecipeFor(item, serverLevel);
                if (recipe.isPresent()) {
                    resultItem = recipe.get().assemble(container, serverLevel.registryAccess());
                    resultItem.setCount(resultItem.getCount() * item.getCount());
                    hasSmeltableItems = true;
                }
                result.add(resultItem);
            }

            if (hasSmeltableItems) {
                var center = context.getParam(LootContextParams.ORIGIN);
                var random = level.getRandom();

                if (!level.isClientSide()) {
                    serverLevel.sendParticles(ParticleUtils.constructSimpleSpark(new Color(150 + random.nextInt(106), random.nextInt(50), 50 + random.nextInt(51), 255),
                            0.6F, 20, 0.85F), center.x(), center.y(), center.z(), 25, 0.3, 0.3, 0.3, 0.01);
                }

                addCharges(stack, -1);
                relic.spreadExperience(player, stack, 1);

                return result;
            }
        }

        return items;
    }

    public static Optional<AbstractCookingRecipe> getRecipeFor(ItemStack item, Level level) {
        container.clearContent();
        container.setItem(0, item);
        RecipeManager recipeManager = level.getRecipeManager();
        Optional<Pair<ResourceLocation, SmeltingRecipe>> optional = recipeManager.getRecipeFor(RecipeType.SMELTING, container, level, lastRecipe);
        if (optional.isPresent()) {
            Pair<ResourceLocation, SmeltingRecipe> pair = optional.get();
            lastRecipe = pair.getFirst();
            return Optional.of(pair.getSecond());
        } else {
            return Optional.empty();
        }
    }
}