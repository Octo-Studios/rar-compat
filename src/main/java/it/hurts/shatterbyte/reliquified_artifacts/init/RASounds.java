package it.hurts.shatterbyte.reliquified_artifacts.init;

import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RASounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, ReliquifiedArtifacts.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> NIGHT_VISION_TOGGLE = SOUNDS.register("night_vision_toggle", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReliquifiedArtifacts.MODID, "night_vision_toggle")));

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}
