package it.hurts.octostudios.rarcompat.items.hat;

import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;

public class SuperstitiousHatItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder().build();
    }

//    @Override
//    public int getLootingLevel(SlotContext slotContext, @Nullable LootContext lootContext, ItemStack stack) {
//        if (!(slotContext.entity() instanceof Player player))
//            return super.getLootingLevel(slotContext, lootContext, stack);
//
//        var random = player.getRandom();
//        var amount = MathUtils.multicast(random, getStatValue(stack, "looting", "chance"));
//
//        if (amount > 0)
//            spreadRelicExperience(player, stack, random.nextInt(amount) + 1);
//
//        return amount;
//    }
}