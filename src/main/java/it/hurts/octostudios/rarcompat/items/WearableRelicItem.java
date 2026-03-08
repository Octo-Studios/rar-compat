package it.hurts.octostudios.rarcompat.items;

import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.sskirillss.relics.api.relics.IRelicItem;
import it.hurts.sskirillss.relics.init.RelicsCreativeTabs;
import it.hurts.sskirillss.relics.items.misc.CreativeContentConstructor;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;

public abstract class WearableRelicItem extends RelicItem implements IRelicItem, ICurioItem {
    @Override
    public void gatherCreativeTabContent(CreativeContentConstructor constructor) {
        constructor.entry(RelicsCreativeTabs.RELICS_TAB.get(), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY, this);
    }

    @Override
    public List<Component> getAttributesTooltip(List<Component> tooltips, Item.TooltipContext context, ItemStack stack) {
        return new ArrayList<>();
    }

    @Override
    public String getConfigRoute() {
        return RARCompat.MODID;
    }
}