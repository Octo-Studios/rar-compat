package it.hurts.octostudios.rarcompat.init;

import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.entities.SparkEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class EntityRegistry {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, RARCompat.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SparkEntity>> SPARK = ENTITIES.register("spark", () ->
            EntityType.Builder.of(SparkEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .build("spark"));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
