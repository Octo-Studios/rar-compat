package it.hurts.shatterbyte.reliquified_artifacts.init;

import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
import it.hurts.shatterbyte.reliquified_artifacts.items.MimiDustItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RAItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, ReliquifiedArtifacts.MODID);

    public static final DeferredHolder<Item, Item> MIMI_DUST = ITEMS.register("mimi_dust", MimiDustItem::new);

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
