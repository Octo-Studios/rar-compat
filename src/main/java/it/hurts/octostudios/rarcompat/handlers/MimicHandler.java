package it.hurts.octostudios.rarcompat.handlers;

import artifacts.entity.MimicEntity;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.sskirillss.relics.items.relics.base.IRelicItem;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber
public class MimicHandler {
    public static final List<Item> ITEMS = new ArrayList<>();

    @SubscribeEvent
    public static void onStartedServer(ServerStartedEvent event) {
        var entries = BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RARCompat.MODID, "mimic_drops")))
                .stream().flatMap(holderSet -> holderSet.stream().map(Holder::value)).toList();

        MimicHandler.ITEMS.clear();

        if (entries.isEmpty())
            return;

        for (var entry : entries) {
            var id = ResourceLocation.parse(String.valueOf(entry));

            if (!BuiltInRegistries.ITEM.containsKey(id) || !(BuiltInRegistries.ITEM.get(id) instanceof IRelicItem))
                continue;

            MimicHandler.ITEMS.add(BuiltInRegistries.ITEM.get(id));
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (ITEMS.isEmpty() || !(event.getEntity() instanceof MimicEntity entity))
            return;

        var level = entity.getCommandSenderWorld();
        var random = level.getRandom();
        var persistentData = entity.getPersistentData();

        for (int i = 0; i < (persistentData.getInt("relicCount") == 0 ? 1 : persistentData.getInt("relicCount")); i++)
            event.getDrops().add(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), ITEMS.get(random.nextInt(ITEMS.size())).getDefaultInstance()));
    }
}