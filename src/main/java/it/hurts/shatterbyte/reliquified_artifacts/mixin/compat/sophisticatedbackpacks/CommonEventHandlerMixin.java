package it.hurts.shatterbyte.reliquified_artifacts.mixin.compat.sophisticatedbackpacks;

import it.hurts.shatterbyte.reliquified_artifacts.items.charm.UniversalAttractorItem;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedbackpacks.common.CommonEventHandler", remap = false)
public class CommonEventHandlerMixin {
    @Inject(
            method = "onItemPickup(Lnet/neoforged/neoforge/event/entity/player/ItemEntityPickupEvent$Pre;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/event/entity/player/ItemEntityPickupEvent$Pre;setCanPickup(Lnet/neoforged/neoforge/common/util/TriState;)V",
                    remap = false
            ),
            require = 0
    )
    private void reliquified_artifacts$awardAttractorXpOnBackpackPickup(ItemEntityPickupEvent.Pre event, CallbackInfo ci) {
        UniversalAttractorItem.awardPulledItemPickup(event.getPlayer(), event.getItemEntity());
    }
}
