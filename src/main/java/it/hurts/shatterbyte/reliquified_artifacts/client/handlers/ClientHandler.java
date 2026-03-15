package it.hurts.shatterbyte.reliquified_artifacts.client.handlers;

import artifacts.registry.ModItems;
import it.hurts.octostudios.octolib.module.particle.trail.EntityTrailRegistry;
import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
import it.hurts.shatterbyte.reliquified_artifacts.entities.SparkEntity;
import it.hurts.shatterbyte.reliquified_artifacts.init.RAEntities;
import it.hurts.sskirillss.relics.api.relics.IRelicItem;
import it.hurts.sskirillss.relics.client.renderer.entities.NullRenderer;
import it.hurts.sskirillss.relics.client.style.base.RelicStyle;
import it.hurts.sskirillss.relics.init.RelicsRelicStyles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = ReliquifiedArtifacts.MODID, value = Dist.CLIENT)
public class ClientHandler {
    @SubscribeEvent
    public static void setupClient(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            EntityTrailRegistry.registerProvider(RAEntities.SPARK.get(), SparkEntity.TrailProvider::new);

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
        event.registerEntityRenderer(RAEntities.SPARK.get(), NullRenderer::new);
        event.registerEntityRenderer(RAEntities.WHOOPEE_CLOUD.get(), NullRenderer::new);
    }
}
