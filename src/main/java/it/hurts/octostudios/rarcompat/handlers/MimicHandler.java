package it.hurts.octostudios.rarcompat.handlers;

import artifacts.entity.MimicEntity;
import it.hurts.octostudios.rarcompat.RARCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber
public class MimicHandler {
    public static final List<Item> MIMIC_LOOT = new ArrayList<>();
    public static final List<Item> MIMIFICABLE = new ArrayList<>();

    @SubscribeEvent
    public static void onStartedServer(TagsUpdatedEvent event) {
        var mimicLoot = BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, new ResourceLocation(RARCompat.MODID, "mimic_loot")));
        var mimificable = BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, new ResourceLocation(RARCompat.MODID, "mimificable")));

        MIMIC_LOOT.clear();
        MIMIFICABLE.clear();

        if (!mimicLoot.iterator().hasNext() || !mimificable.iterator().hasNext())
            return;

        mimicLoot.forEach(holder -> MIMIC_LOOT.add(holder.value()));
        mimificable.forEach(holder -> MIMIFICABLE.add(holder.value()));
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (MIMIC_LOOT.isEmpty() || !(event.getEntity() instanceof MimicEntity entity))
            return;

        var level = entity.getCommandSenderWorld();
        var random = level.getRandom();
        var persistentData = entity.getPersistentData();

        event.getDrops().clear();

        for (int i = 0; i < (persistentData.getInt("relicCount") == 0 ? 1 : persistentData.getInt("relicCount")); i++)
            event.getDrops().add(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), MIMIC_LOOT.get(random.nextInt(MIMIC_LOOT.size())).getDefaultInstance()));
    }
}