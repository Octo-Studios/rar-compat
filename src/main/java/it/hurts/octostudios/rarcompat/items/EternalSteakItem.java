package it.hurts.octostudios.rarcompat.items;

import net.minecraft.world.food.FoodProperties;

public class EternalSteakItem extends EverlastingFoodRelicItem {
    public EternalSteakItem() {
        super(new FoodProperties.Builder()
                        .nutrition(8)
                        .saturationModifier(0.8F)
                        .build(),
                96,
                9D, 3.5D,
                2D, 4.5D,
                0.12D, 0.4D,
                0.15D, 0.5D);
    }
}