package it.hurts.octostudios.rarcompat.items.charm;

import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;

public class HeliumFlamingoItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder().build();
    }

    public void setToggled(net.minecraft.world.item.ItemStack stack, boolean val) {
    }

    public void setTime(net.minecraft.world.item.ItemStack stack, int val) {
    }
}
