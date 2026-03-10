package it.hurts.octostudios.rarcompat.mixin;

import artifacts.registry.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {
    @Shadow
    private static boolean canBurn(RegistryAccess registryAccess, @Nullable RecipeHolder<?> recipe, NonNullList<ItemStack> inventory, int maxStackSize, AbstractFurnaceBlockEntity furnace) {
        return false;
    }

    @Inject(method = "burn", at = @At("HEAD"), cancellable = true)
    private static void rarcompat$preserveRelicData(RegistryAccess registryAccess, @Nullable RecipeHolder<?> recipe, NonNullList<ItemStack> inventory, int maxStackSize, AbstractFurnaceBlockEntity furnace, CallbackInfoReturnable<Boolean> cir) {
        if (recipe == null || !canBurn(registryAccess, recipe, inventory, maxStackSize, furnace))
            return;

        var input = inventory.getFirst();

        if (!input.is(ModItems.EVERLASTING_BEEF.value()))
            return;

        if (!(recipe.value() instanceof AbstractCookingRecipe cookingRecipe))
            return;

        var assembled = cookingRecipe.assemble(new SingleRecipeInput(input), registryAccess);

        if (!assembled.is(ModItems.ETERNAL_STEAK.value()))
            return;

        var output = input.transmuteCopy(ModItems.ETERNAL_STEAK.value(), assembled.getCount());
        var outputSlot = inventory.get(2);

        if (outputSlot.isEmpty())
            inventory.set(2, output);
        else if (outputSlot.is(output.getItem()))
            outputSlot.grow(output.getCount());

        input.shrink(1);

        cir.setReturnValue(true);
    }
}