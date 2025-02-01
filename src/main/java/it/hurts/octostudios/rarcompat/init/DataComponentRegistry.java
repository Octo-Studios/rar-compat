package it.hurts.octostudios.rarcompat.init;

import com.mojang.serialization.Codec;
import it.hurts.octostudios.rarcompat.RARCompat;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class DataComponentRegistry {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, RARCompat.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MODE = DATA_COMPONENTS.register("mode",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .build()
    );

    public static void register(IEventBus bus) {
        DATA_COMPONENTS.register(bus);
    }

}
