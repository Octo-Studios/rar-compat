package it.hurts.octostudios.rarcompat.items.feet;

import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;

public class BunnyHoppersItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder().build();
    }

    public int getTime(net.minecraft.world.item.ItemStack stack) {
        return 0;
    }

    public void addTime(net.minecraft.world.item.ItemStack stack, int val) {
    }
}
