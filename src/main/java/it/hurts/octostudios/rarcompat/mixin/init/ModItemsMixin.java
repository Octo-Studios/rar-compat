package it.hurts.octostudios.rarcompat.mixin.init;

import artifacts.registry.ModItems;
import artifacts.registry.RegistrySupplier;
import it.hurts.octostudios.rarcompat.items.necklace.*;
import it.hurts.octostudios.rarcompat.items.hat.*;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Mixin(ModItems.class)
public class ModItemsMixin {

    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lartifacts/registry/ModItems;register(Ljava/lang/String;Ljava/util/function/Supplier;)Lartifacts/registry/RegistrySupplier;"))
    private static <T extends Item> RegistrySupplier<T> redirectWearableItem(String name, Supplier<T> supplier) {
        return switch (name) {
            case "plastic_drinking_hat" -> register(name, DrinkingHatItem::new);
            case "novelty_drinking_hat" -> register(name, DrinkingHatItem::new);
            case "snorkel" -> register(name, SnorkelItem::new);
            case "villager_hat" -> register(name, VillagerHatItem::new);
            case "superstitious_hat" -> register(name, SuperstitiousHatItem::new);
            case "anglers_hat" -> register(name, AnglersHatItem::new);
            case "lucky_scarf" -> register(name, LuckyScarfItem::new);
            case "scarf_of_invisibility" -> register(name, ScarfOfInvisibilityItem::new);
            case "cowboy_hat" -> register(name, CowboyHatItem::new);

            default -> register(name, supplier);
        };
    }

    @Shadow
    private static <T extends Item> RegistrySupplier register(String name, Supplier<T> supplier) {
        return null;
    }
}