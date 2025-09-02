package it.hurts.octostudios.rarcompat.mixin;

import artifacts.forge.loot.RollLootTableModifier;
import it.hurts.sskirillss.relics.items.relics.base.IRelicItem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RollLootTableModifier.class)
public class RollLootTableModifierMixin {
    @Final
    @Shadow
    private ResourceLocation lootTable;
    @Final
    @Shadow
    private boolean replace;

    /**
     * @author Amiri163
     * @reason Temp solution til the end of development
     */
    @Overwrite(remap = false)
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (lootTable == null)
            return generatedLoot;

        if (replace)
            generatedLoot.clear();

        context.getResolver().getLootTable(lootTable).getRandomItemsRaw(context, stack -> {
            if (!(stack.getItem() instanceof IRelicItem))
                generatedLoot.add(stack);
        });

        return generatedLoot;
    }
}