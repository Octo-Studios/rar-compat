package it.hurts.shatterbyte.reliquified_artifacts;

import it.hurts.shatterbyte.reliquified_artifacts.init.RADataComponent;
import it.hurts.shatterbyte.reliquified_artifacts.init.RAEntities;
import it.hurts.shatterbyte.reliquified_artifacts.init.RAItems;
import it.hurts.shatterbyte.reliquified_artifacts.init.RASounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(ReliquifiedArtifacts.MODID)
public class ReliquifiedArtifacts {
    public static final String MODID = "reliquified_artifacts";

    public ReliquifiedArtifacts(IEventBus bus) {
        RAItems.register(bus);
        RASounds.register(bus);
        RAEntities.register(bus);
        RADataComponent.register(bus);
    }
}