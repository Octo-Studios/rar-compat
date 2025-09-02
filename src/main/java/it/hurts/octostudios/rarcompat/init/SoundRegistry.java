package it.hurts.octostudios.rarcompat.init;

import it.hurts.octostudios.rarcompat.RARCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RARCompat.MODID);

    public static final RegistryObject<SoundEvent> NIGHT_VISION_TOGGLE = SOUNDS.register("night_vision_toggle", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(RARCompat.MODID, "night_vision_toggle")));

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}