package it.hurts.octostudios.rarcompat;


import it.hurts.octostudios.rarcompat.init.SoundRegistry;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.octostudios.rarcompat.network.NetworkHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.List;

@Mod(RARCompat.MODID)
public class RARCompat {
    public static final String MODID = "rarcompat";

    public RARCompat(IEventBus bus) {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setupCommon);
        MinecraftForge.EVENT_BUS.addListener(RARCompat::onItemTooltip);
        //ItemRegistry.register(bus);
        SoundRegistry.register(bus);
        //EntityRegistry.register(bus);
    }

    private void setupCommon(final FMLCommonSetupEvent event) {
        NetworkHandler.register();
    }

    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack itemStack = event.getItemStack();

        if (itemStack.getItem() instanceof WearableRelicItem) {
            List<Component> tooltip = event.getToolTip();

            tooltip.add(Component.empty());
            tooltip.add(Component.literal("Hold [Shift] to research...").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.empty());

            tooltip.remove(1);
        }
    }

}