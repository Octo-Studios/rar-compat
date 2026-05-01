package it.hurts.shatterbyte.reliquified_artifacts.items.base;

import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
import it.hurts.sskirillss.relics.items.relics.base.WearableRelicItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public abstract class RAWearableRelicItem extends WearableRelicItem {
    @Override
    public String getConfigRoute() {
        return ReliquifiedArtifacts.MODID;
    }

    @Override
    public @Nullable String getURI(LivingEntity entity, ItemStack stack) {
        return "https://shatterbyte.com/docs/mods/reliquified_artifacts/relics/" + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath() + "/";
    }
}