package it.hurts.shatterbyte.reliquified_artifacts.items.base;

import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
import it.hurts.sskirillss.relics.items.relics.base.WearableRelicItem;

public abstract class RAWearableRelicItem extends WearableRelicItem {
    @Override
    public String getConfigRoute() {
        return ReliquifiedArtifacts.MODID;
    }
}