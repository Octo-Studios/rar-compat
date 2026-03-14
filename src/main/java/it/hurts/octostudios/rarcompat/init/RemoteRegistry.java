package it.hurts.octostudios.rarcompat.init;

import artifacts.registry.ModItems;
import it.hurts.octostudios.octolib.module.particle.trail.EntityTrailRegistry;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.entities.SparkEntity;
import it.hurts.sskirillss.relics.api.relics.IRelicItem;
import it.hurts.sskirillss.relics.client.renderer.entities.NullRenderer;
import it.hurts.sskirillss.relics.client.style.base.RelicStyle;
import it.hurts.sskirillss.relics.init.RelicsRelicStyles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = RARCompat.MODID, value = Dist.CLIENT)
public class RemoteRegistry {
    @SubscribeEvent
    public static void setupClient(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            EntityTrailRegistry.registerProvider(EntityRegistry.SPARK.get(), SparkEntity.TrailProvider::new);

            for (var entry : ModItems.ITEMS.getEntries()) {
                var item = entry.get();

                if (!(item instanceof IRelicItem))
                    continue;

                RelicsRelicStyles.register(item, RelicStyle::new);
            }

            RelicsRelicStyles.init();
        });
    }

    @SubscribeEvent
    public static void entityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.SPARK.get(), NullRenderer::new);
        event.registerEntityRenderer(EntityRegistry.WHOOPEE_CLOUD.get(), NullRenderer::new);
    }
}
