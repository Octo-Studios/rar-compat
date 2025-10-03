package it.hurts.octostudios.rarcompat;

import it.hurts.octostudios.rarcompat.items.UmbrellaItem;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.init.CreativeTabRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CreativeTabModifier {

    @SubscribeEvent
    public static void fillCreativeTabs(final BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == CreativeTabRegistry.RELICS_TAB.get()) {
            for (Item item : BuiltInRegistries.ITEM.stream().toList()) {
                if (item instanceof WearableRelicItem) {
                    ItemStack stack = new ItemStack(item);

                    if (item instanceof UmbrellaItem relicUmbrella)
                        relicUmbrella.setCharges(stack, relicUmbrella.getMaxCharges(stack));

                    event.accept(stack);
                }
            }
        }
    }

}
