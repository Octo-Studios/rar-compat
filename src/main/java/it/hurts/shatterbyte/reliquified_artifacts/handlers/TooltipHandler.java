package it.hurts.shatterbyte.reliquified_artifacts.handlers;

import artifacts.Artifacts;
import it.hurts.shatterbyte.reliquified_artifacts.items.WearableRelicItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber
public class TooltipHandler {
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof WearableRelicItem))
            return;

        var tooltip = event.getToolTip();

        for (int i = 0; i < tooltip.size(); i++) {
            var component = tooltip.get(i);

            if (component.getString().startsWith(Artifacts.MOD_ID)) {
                tooltip.set(i, Component.empty().setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)).append(component).append(" | ").append(Component.translatable("tooltip.reliquified_artifacts.modified")));

                break;
            }
        }
    }
}