package it.hurts.shatterbyte.reliquified_artifacts.items.base;

import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
import it.hurts.sskirillss.relics.init.RelicsCreativeTabs;
import it.hurts.sskirillss.relics.items.misc.CreativeContentConstructor;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public abstract class RARelicItem extends RelicItem {
    public RARelicItem(Item.Properties properties) {
        super(properties);
    }

    public RARelicItem() {
        super(new Item.Properties()
                .rarity(Rarity.EPIC)
                .stacksTo(1));
    }

    @Override
    public void gatherCreativeTabContent(CreativeContentConstructor constructor) {
        constructor.entry(RelicsCreativeTabs.RELICS_TAB.get(), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY, this);
    }

    @Override
    public String getConfigRoute() {
        return ReliquifiedArtifacts.MODID;
    }
}
