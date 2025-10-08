package it.hurts.octostudios.rarcompat.mixin.init;

import artifacts.registry.ModItems;
import artifacts.registry.RegistrySupplier;
import it.hurts.octostudios.rarcompat.items.MimiDustItem;
import it.hurts.octostudios.rarcompat.items.UmbrellaItem;
import it.hurts.octostudios.rarcompat.items.bracelet.OnionRingItem;
import it.hurts.octostudios.rarcompat.items.charm.*;
import it.hurts.octostudios.rarcompat.items.feet.*;
import it.hurts.octostudios.rarcompat.items.hands.*;
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
            case "plastic_drinking_hat", "novelty_drinking_hat" -> register(name, DrinkingHatItem::new);
            case "snorkel" -> register(name, SnorkelItem::new);
            case "villager_hat" -> register(name, VillagerHatItem::new);
            case "superstitious_hat" -> register(name, SuperstitiousHatItem::new);
            case "anglers_hat" -> register(name, AnglersHatItem::new);
            case "lucky_scarf" -> register(name, LuckyScarfItem::new);
            case "scarf_of_invisibility" -> register(name, ScarfOfInvisibilityItem::new);
            case "cowboy_hat" -> register(name, CowboyHatItem::new);
            case "night_vision_goggles" -> register(name, NightVisionGogglesItem::new);
            case "whoopee_cushion" -> register(name, WhoopeeCushionItem::new);
            case "charm_of_sinking" -> register(name, CharmOfSinkingItem::new);
            case "cross_necklace" -> register(name, CrossNecklaceItem::new);
            case "flame_pendant" -> register(name, FlamePendantItem::new);
            case "panic_necklace" -> register(name, PanicNecklaceItem::new);
            case "shock_pendant" -> register(name, ShockPendantItem::new);
            case "thorn_pendant" -> register(name, ThornPendantItem::new);
            case "bunny_hoppers" -> register(name, BunnyHoppersItem::new);
            case "flippers" -> register(name, FlippersItem::new);
            case "kitty_slippers" -> register(name, KittySlippersItem::new);
            case "rooted_boots" -> register(name, RootedBootsItem::new);
            case "running_shoes" -> register(name, RunningShoesItem::new);
            case "snowshoes" -> register(name, SnowshoesItem::new);
            case "steadfast_spikes" -> register(name, SteadfastSpikesItem::new);
            case "digging_claws" -> register(name, DiggingClawsItem::new);
            case "feral_claws" -> register(name, FeralClawsItem::new);
            case "fire_gauntlet" -> register(name, FireGauntletItem::new);
            case "golden_hook" -> register(name, GoldenHookItem::new);
            case "pickaxe_heater" -> register(name, PickaxeHeaterItem::new);
            case "pocket_piston" -> register(name, PocketPistonItem::new);
            case "power_glove" -> register(name, PowerGloveItem::new);
            case "vampiric_glove" -> register(name, VampiricGloveItem::new);
            case "umbrella" -> register(name, UmbrellaItem::new);
            case "mimi_dust" -> register(name, MimiDustItem::new);
            case "antidote_vessel" -> register(name, AntidoteVesselItem::new);
            case "chorus_totem" -> register(name, ChorusTotemItem::new);
            case "cloud_in_a_bottle" -> register(name, CloudInBottleItem::new);
            case "crystal_heart" -> register(name, CrystalHeartItem::new);
            case "helium_flamingo" -> register(name, HeliumFlamingoItem::new);
            case "obsidian_skull" -> register(name, ObsidianSkullItem::new);
            case "universal_attractor" -> register(name, UniversalAttractorItem::new);
            case "onion_ring" -> register(name, OnionRingItem::new);

            default -> register(name, supplier);
        };
    }

    @Shadow
    private static <T extends Item> RegistrySupplier register(String name, Supplier<T> supplier) {
        return null;
    }
}