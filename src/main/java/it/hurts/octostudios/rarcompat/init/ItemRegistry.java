package it.hurts.octostudios.rarcompat.init;

import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.items.MimiDustItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RARCompat.MODID);

    public static final RegistryObject<Item> MIMI_DUST = ITEMS.register("mimi_dust", MimiDustItem::new);

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
