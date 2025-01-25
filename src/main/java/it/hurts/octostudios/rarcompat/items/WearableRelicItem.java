package it.hurts.octostudios.rarcompat.items;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.sskirillss.relics.init.CreativeTabRegistry;
import it.hurts.sskirillss.relics.items.misc.CreativeContentConstructor;
import it.hurts.sskirillss.relics.items.relics.base.IRelicItem;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastStage;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class WearableRelicItem extends RelicItem implements IRelicItem, ICurioItem {
    @Override
    public void gatherCreativeTabContent(CreativeContentConstructor constructor) {
        constructor.entry(CreativeTabRegistry.RELICS_TAB.get(), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY, this);
        constructor.entry(ModItems.CREATIVE_TAB.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS, this);
    }

    @Override
    public List<Component> getAttributesTooltip(List<Component> tooltips, ItemStack stack) {
        return new ArrayList<>();
    }

    @Override
    public String getConfigRoute() {
        return RARCompat.MODID;
    }
}