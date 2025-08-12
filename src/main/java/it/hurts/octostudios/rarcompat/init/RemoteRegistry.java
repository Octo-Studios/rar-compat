package it.hurts.octostudios.rarcompat.init;

import it.hurts.octostudios.octolib.module.particle.trail.EntityTrailRegistry;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.entities.SparkEntity;
import it.hurts.sskirillss.relics.client.renderer.entities.NullRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = RARCompat.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RemoteRegistry {
    @SubscribeEvent
    public static void setupClient(final FMLClientSetupEvent event) {
        EntityTrailRegistry.registerProvider(EntityRegistry.SPARK.get(), SparkEntity.TrailProvider::new);
    }

    @SubscribeEvent
    public static void entityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.SPARK.get(), NullRenderer::new);
    }
}
