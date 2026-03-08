package it.hurts.octostudios.rarcompat.init;

import com.mojang.serialization.Codec;
import it.hurts.octostudios.rarcompat.RARCompat;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class DataComponentRegistry {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, RARCompat.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> UMBRELLA_BOUNCE_COUNT = construct("umbrella/bounce_count", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> UMBRELLA_MAX_BOUNCES = construct("umbrella/max_bounces", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> UMBRELLA_BOUNCE_RECHARGE_TIMER = construct("umbrella/bounce_recharge_timer", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> UMBRELLA_BOUNCE_NEEDS_LANDING = construct("umbrella/bounce_needs_landing", Codec.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> UMBRELLA_SHIELD_HITS = construct("umbrella/shield_hits", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> UMBRELLA_SHIELD_MAX_HITS = construct("umbrella/shield_max_hits", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> UMBRELLA_SHOW_SHIELD_BAR = construct("umbrella/show_shield_bar", Codec.BOOL);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> SNORKEL_RESERVE_TRIGGERED = construct("snorkel/reserve_triggered", Codec.BOOL);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> COWBOY_HAT_LAST_MOUNT_ID = construct("cowboy_hat/last_mount_id", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> COWBOY_HAT_ABSORPTION_REMAINING = construct("cowboy_hat/absorption_remaining", Codec.DOUBLE);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SCARF_OF_INVISIBILITY_STATIONARY_TICKS = construct("scarf_of_invisibility/stationary_ticks", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SCARF_OF_INVISIBILITY_COOLDOWN = construct("scarf_of_invisibility/cooldown", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> SCARF_OF_INVISIBILITY_ACTIVE = construct("scarf_of_invisibility/active", Codec.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> SCARF_OF_INVISIBILITY_NEEDS_OUT_OF_SIGHT = construct("scarf_of_invisibility/needs_out_of_sight", Codec.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SCARF_OF_INVISIBILITY_STRIKE_TICKS = construct("scarf_of_invisibility/strike_ticks", Codec.INT);

    public static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> construct(String name, Codec<T> codec) {
        return DATA_COMPONENTS.register(name, () -> DataComponentType.<T>builder().persistent(codec).build());
    }

    public static void register(IEventBus bus) {
        DATA_COMPONENTS.register(bus);
    }
}
