package it.hurts.shatterbyte.reliquified_artifacts.init;

import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
import it.hurts.shatterbyte.reliquified_artifacts.entities.SparkEntity;
import it.hurts.shatterbyte.reliquified_artifacts.entities.WhoopeeCloudEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RAEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, ReliquifiedArtifacts.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SparkEntity>> SPARK = ENTITIES.register("spark", () ->
            EntityType.Builder.of(SparkEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .build("spark"));

    public static final DeferredHolder<EntityType<?>, EntityType<WhoopeeCloudEntity>> WHOOPEE_CLOUD = ENTITIES.register("whoopee_cloud", () ->
            EntityType.Builder.of(WhoopeeCloudEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .build("whoopee_cloud"));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
