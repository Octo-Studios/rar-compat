package it.hurts.octostudios.rarcompat.items.charm;

import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;

public class CloudInBottleItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder().build();
    }

    public void addCount(net.minecraft.world.item.ItemStack stack, int val) {
    }
}
