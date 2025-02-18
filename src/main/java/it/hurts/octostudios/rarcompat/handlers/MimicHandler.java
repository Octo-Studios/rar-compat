package it.hurts.octostudios.rarcompat.handlers;

import artifacts.entity.MimicEntity;
import it.hurts.octostudios.rarcompat.RARCompat;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber
public class MimicHandler {
    public static final List<Item> MIMIC_LOOT = new ArrayList<>();
    public static final List<Item> MIMIFICABLE = new ArrayList<>();

    @SubscribeEvent
    public static void onStartedServer(TagsUpdatedEvent event) {
        var mimicLoot = BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RARCompat.MODID, "mimic_loot")))
                .stream().flatMap(holderSet -> holderSet.stream().map(Holder::value)).toList();
        var mimificable = BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RARCompat.MODID, "mimificable")))
                .stream().flatMap(holderSet -> holderSet.stream().map(Holder::value)).toList();

        MIMIC_LOOT.clear();
        MIMIFICABLE.clear();

        if (mimicLoot.isEmpty() || mimificable.isEmpty())
            return;

        mimicLoot.stream().map(entry -> ResourceLocation.parse(String.valueOf(entry))).filter(BuiltInRegistries.ITEM::containsKey).map(BuiltInRegistries.ITEM::get).forEach(MIMIC_LOOT::add);
        mimificable.stream().map(entry -> ResourceLocation.parse(String.valueOf(entry))).filter(BuiltInRegistries.ITEM::containsKey).map(BuiltInRegistries.ITEM::get).forEach(MIMIFICABLE::add);
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (MIMIC_LOOT.isEmpty() || !(event.getEntity() instanceof MimicEntity entity))
            return;

        var level = entity.getCommandSenderWorld();
        var random = level.getRandom();
        var persistentData = entity.getPersistentData();

        for (int i = 0; i < (persistentData.getInt("relicCount") == 0 ? 1 : persistentData.getInt("relicCount")); i++)
            event.getDrops().add(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), MIMIC_LOOT.get(random.nextInt(MIMIC_LOOT.size())).getDefaultInstance()));
    }
}