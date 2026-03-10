package it.hurts.octostudios.rarcompat.mixin.accessor;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CampfireBlockEntity.class)
public interface CampfireBlockEntityAccessor {
    @Accessor("items")
    NonNullList<ItemStack> rarcompat$getItems();

    @Accessor("cookingProgress")
    int[] rarcompat$getCookingProgress();

    @Accessor("cookingTime")
    int[] rarcompat$getCookingTime();
}