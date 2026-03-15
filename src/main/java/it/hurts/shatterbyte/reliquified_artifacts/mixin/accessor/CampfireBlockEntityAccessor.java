package it.hurts.shatterbyte.reliquified_artifacts.mixin.accessor;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CampfireBlockEntity.class)
public interface CampfireBlockEntityAccessor {
    @Accessor("items")
    NonNullList<ItemStack> reliquified_artifacts$getItems();

    @Accessor("cookingProgress")
    int[] reliquified_artifacts$getCookingProgress();

    @Accessor("cookingTime")
    int[] reliquified_artifacts$getCookingTime();
}