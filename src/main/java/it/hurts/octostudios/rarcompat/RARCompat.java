package it.hurts.octostudios.rarcompat;

import it.hurts.octostudios.rarcompat.handlers.MimicHandler;
import it.hurts.octostudios.rarcompat.init.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(RARCompat.MODID)
public class RARCompat {
    public static final String MODID = "rarcompat";

    public RARCompat(IEventBus bus) {
        bus.addListener(this::setupCommon);

        ItemRegistry.register(bus);
        SoundRegistry.register(bus);
        EntityRegistry.register(bus);
        DataComponentRegistry.register(bus);
    }

    private void setupCommon(final FMLCommonSetupEvent event) {
        ConfigRegistry.register();

        MimicHandler.init();
    }
}