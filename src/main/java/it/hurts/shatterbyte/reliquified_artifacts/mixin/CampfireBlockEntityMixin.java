package it.hurts.shatterbyte.reliquified_artifacts.mixin;

import artifacts.registry.ModItems;
import it.hurts.shatterbyte.reliquified_artifacts.mixin.accessor.CampfireBlockEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CampfireBlockEntity.class)
public class CampfireBlockEntityMixin {
    @Redirect(
            method = "cookTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Containers;dropItemStack(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V")
    )
    private static void reliquified_artifacts$preserveEverlastingFoodData(Level level, double x, double y, double z, ItemStack output,
                                                               Level cookLevel, BlockPos pos, BlockState state, CampfireBlockEntity blockEntity) {
        var stackToDrop = output;

        if (output.is(ModItems.ETERNAL_STEAK.value())) {
            var accessor = (CampfireBlockEntityAccessor) blockEntity;
            var items = accessor.reliquified_artifacts$getItems();
            var progress = accessor.reliquified_artifacts$getCookingProgress();
            var cookingTime = accessor.reliquified_artifacts$getCookingTime();

            for (var i = 0; i < items.size(); i++) {
                var input = items.get(i);

                if (!input.is(ModItems.EVERLASTING_BEEF.value()) || progress[i] < cookingTime[i])
                    continue;

                stackToDrop = input.transmuteCopy(ModItems.ETERNAL_STEAK.value(), output.getCount());
                break;
            }
        }

        Containers.dropItemStack(level, x, y, z, stackToDrop);
    }
}