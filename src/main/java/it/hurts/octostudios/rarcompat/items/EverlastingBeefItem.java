package it.hurts.octostudios.rarcompat.items;

import net.minecraft.world.food.FoodProperties;

public class EverlastingBeefItem extends EverlastingFoodRelicItem {
    public EverlastingBeefItem() {
        super(new FoodProperties.Builder()
                        .nutrition(3)
                        .saturationModifier(0.3F)
                        .build(),
                64D, 96D,
                12D, 5D,
                1D, 3D,
                0.08D, 0.3D,
                0.1D, 0.4D);
    }
}