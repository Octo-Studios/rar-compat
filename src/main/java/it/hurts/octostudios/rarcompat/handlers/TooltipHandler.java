package it.hurts.octostudios.rarcompat.handlers;

import artifacts.Artifacts;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class TooltipHandler {
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof WearableRelicItem))
            return;

        var tooltip = event.getToolTip();

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Hold [Shift] to research...").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.remove(1);

        for (int i = 0; i < tooltip.size(); i++) {
            var component = tooltip.get(i);

            if (component.getString().startsWith(Artifacts.MOD_ID)) {
                tooltip.set(i, Component.empty()
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY))
                        .append(component)
                        .append(" | ")
                        .append(Component.translatable("tooltip.rarcompat.modified")));
                break;
            }
        }
    }
}