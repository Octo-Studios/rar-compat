package it.hurts.octostudios.rarcompat.init;

import com.mojang.serialization.Codec;
import it.hurts.octostudios.rarcompat.RARCompat;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Map;


public class DataComponentRegistry {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, RARCompat.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> UMBRELLA_BOUNCE_COUNT = construct("umbrella/bounce_count", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> UMBRELLA_MAX_BOUNCES = construct("umbrella/max_bounces", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> UMBRELLA_BOUNCE_RECHARGE_TIMER = construct("umbrella/bounce_recharge_timer", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> UMBRELLA_FALL_TICKS = construct("umbrella/fall_ticks", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> UMBRELLA_SLOW_FALL_PROCESSED_TICK = construct("umbrella/slow_fall_processed_tick", Codec.LONG);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> UMBRELLA_SLOW_FALL_POS_TICK = construct("umbrella/slow_fall_pos_tick", Codec.LONG);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> UMBRELLA_SLOW_FALL_POS_X = construct("umbrella/slow_fall_pos_x", Codec.DOUBLE);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> UMBRELLA_SLOW_FALL_POS_Y = construct("umbrella/slow_fall_pos_y", Codec.DOUBLE);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> UMBRELLA_SLOW_FALL_POS_Z = construct("umbrella/slow_fall_pos_z", Codec.DOUBLE);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> UMBRELLA_BOUNCE_NEEDS_LANDING = construct("umbrella/bounce_needs_landing", Codec.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> UMBRELLA_SHIELD_HITS = construct("umbrella/shield_hits", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> UMBRELLA_SHIELD_MAX_HITS = construct("umbrella/shield_max_hits", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> UMBRELLA_SHOW_SHIELD_BAR = construct("umbrella/show_shield_bar", Codec.BOOL);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> SNORKEL_RESERVE_TRIGGERED = construct("snorkel/reserve_triggered", Codec.BOOL);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> COUNT = construct("count", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TIME = construct("time", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> TOGGLED = construct("toggled", Codec.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> HELIUM_FLAMINGO_WATER_TAKEOFF_READY = construct("helium_flamingo/water_takeoff_ready", Codec.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CLOUD_IN_BOTTLE_SLOW_FALL_TICKS = construct("cloud_in_bottle/slow_fall_ticks", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> COWBOY_HAT_LAST_MOUNT_ID = construct("cowboy_hat/last_mount_id", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> COWBOY_HAT_ABSORPTION_REMAINING = construct("cowboy_hat/absorption_remaining", Codec.DOUBLE);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> COWBOY_HAT_RIDING_DISTANCE_REMAINDER = construct("cowboy_hat/riding_distance_remainder", Codec.DOUBLE);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> LUCKY_SCARF_PITY_CHANCE_BONUS = construct("lucky_scarf/pity_chance_bonus", Codec.DOUBLE);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LUCKY_SCARF_NEXT_MAX_CASTS_BONUS = construct("lucky_scarf/next_max_casts_bonus", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> LUCKY_SCARF_LAST_BLOCK_POS = construct("lucky_scarf/last_block_pos", Codec.LONG);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LUCKY_SCARF_LAST_FORTUNE_BONUS = construct("lucky_scarf/last_fortune_bonus", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LUCKY_SCARF_PENDING_FORTUNE_BONUS = construct("lucky_scarf/pending_fortune_bonus", Codec.INT);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> SUPERSTITIOUS_HAT_LAST_KILL_TICK = construct("superstitious_hat/last_kill_tick", Codec.LONG);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SUPERSTITIOUS_HAT_STREAK_COUNT = construct("superstitious_hat/streak_count", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SUPERSTITIOUS_HAT_LAST_TARGET_UUID = construct("superstitious_hat/last_target_uuid", Codec.STRING);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SUPERSTITIOUS_HAT_LAST_LOOTING_BONUS = construct("superstitious_hat/last_looting_bonus", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DIGGING_CLAWS_STREAK_COUNT = construct("digging_claws/streak_count", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> DIGGING_CLAWS_LAST_BREAK_TICK = construct("digging_claws/last_break_tick", Codec.LONG);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PICKAXE_HEATER_SMELT_PITY_STACKS = construct("pickaxe_heater/smelt_pity_stacks", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PICKAXE_HEATER_PENDING_SMELT_RESULT = construct("pickaxe_heater/pending_smelt_result", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PICKAXE_HEATER_PENDING_FORTUNE_BONUS = construct("pickaxe_heater/pending_fortune_bonus", Codec.INT);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FERAL_CLAWS_CHARGES = construct("feral_claws/charges", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FERAL_CLAWS_TIMEOUT_TICKS = construct("feral_claws/timeout_ticks", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FERAL_CLAWS_DECAY_TICKS = construct("feral_claws/decay_ticks", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> POWER_GLOVE_HIT_COUNTER = construct("power_glove/hit_counter", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> POWER_GLOVE_FORCE_NEXT = construct("power_glove/force_next", Codec.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> POWER_GLOVE_POWER_STRIKE_ACTIVE = construct("power_glove/power_strike_active", Codec.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> VAMPIRIC_GLOVE_STREAK_COUNT = construct("vampiric_glove/streak_count", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> VAMPIRIC_GLOVE_LAST_ATTACK_TICK = construct("vampiric_glove/last_attack_tick", Codec.LONG);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ONION_RING_FOOD_BOOST_TICKS = construct("onion_ring/food_boost_ticks", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> BUNNY_HOPPERS_USED_MAX_DURATION = construct("bunny_hoppers/used_max_duration", Codec.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BUNNY_HOPPERS_LANDING_STRIKE_TICKS = construct("bunny_hoppers/landing_strike_ticks", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> BUNNY_HOPPERS_JUMP_START_Y = construct("bunny_hoppers/jump_start_y", Codec.DOUBLE);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> BUNNY_HOPPERS_JUMP_PEAK_Y = construct("bunny_hoppers/jump_peak_y", Codec.DOUBLE);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> BUNNY_HOPPERS_JUMP_LOCKED = construct("bunny_hoppers/jump_locked", Codec.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> KITTY_SLIPPERS_DODGE_READY = construct("kitty_slippers/dodge_ready", Codec.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> RUNNING_SHOES_CHARGE = construct("running_shoes/charge", Codec.DOUBLE);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> SNOWSHOES_SPEED_CHARGE = construct("snowshoes/speed_charge", Codec.DOUBLE);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STRIDER_SHOES_RECOVERY_TICKS = construct("strider_shoes/recovery_ticks", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> AQUA_DASHERS_RECOVERY_TICKS = construct("aqua_dashers/recovery_ticks", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SNOWSHOES_LINGER_TICKS = construct("snowshoes/linger_ticks", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ROOTED_BOOTS_COOLDOWN_TICKS = construct("rooted_boots/cooldown_ticks", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ROOTED_BOOTS_BONEMEAL_TICKS = construct("rooted_boots/bonemeal_ticks", Codec.INT);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> EVERLASTING_FOOD_REGEN_TICKS = construct("everlasting_food/regen_ticks", Codec.INT);


    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> CHARM_OF_SHRINKING_CURRENT_SCALE = construct("charm_of_shrinking/current_scale", Codec.DOUBLE);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHARM_OF_SHRINKING_MOVING_TICKS = construct("charm_of_shrinking/moving_ticks", Codec.INT);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> OBSIDIAN_SKULL_LAVA_TICKS = construct("obsidian_skull/lava_ticks", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> OBSIDIAN_SKULL_HEAT_SURGE_ACTIVE = construct("obsidian_skull/heat_surge_active", Codec.BOOL);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CRYSTAL_HEART_COOLDOWN_TICKS = construct("crystal_heart/cooldown_ticks", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> CRYSTAL_HEART_LAST_HEALTH = construct("crystal_heart/last_health", Codec.DOUBLE);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHORUS_TOTEM_COOLDOWN_TICKS = construct("chorus_totem/cooldown_ticks", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHORUS_TOTEM_REGEN_BOOST_TICKS = construct("chorus_totem/regen_boost_ticks", Codec.INT);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Map<ResourceLocation, Long>>> ANTIDOTE_VESSEL_IMMUNITY_UNTIL = construct("antidote_vessel/immunity_until", Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SCARF_OF_INVISIBILITY_STATIONARY_TICKS = construct("scarf_of_invisibility/stationary_ticks", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SCARF_OF_INVISIBILITY_COOLDOWN = construct("scarf_of_invisibility/cooldown", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> SCARF_OF_INVISIBILITY_ACTIVE = construct("scarf_of_invisibility/active", Codec.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> SCARF_OF_INVISIBILITY_NEEDS_OUT_OF_SIGHT = construct("scarf_of_invisibility/needs_out_of_sight", Codec.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SCARF_OF_INVISIBILITY_STRIKE_TICKS = construct("scarf_of_invisibility/strike_ticks", Codec.INT);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> PANIC_NECKLACE_SPEED_BONUS = construct("panic_necklace/speed_bonus", Codec.DOUBLE);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHARM_OF_SINKING_STATIONARY_TICKS = construct("charm_of_sinking/stationary_ticks", Codec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> CHARM_OF_SINKING_IMMORTALITY_ACTIVE = construct("charm_of_sinking/immortality_active", Codec.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> CHARM_OF_SINKING_AIR_RESTORE_PROGRESS = construct("charm_of_sinking/air_restore_progress", Codec.DOUBLE);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> SHOCK_PENDANT_LAST_LIGHTNING_XP_TICK = construct("shock_pendant/last_lightning_xp_tick", Codec.LONG);

    public static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> construct(String name, Codec<T> codec) {
        return DATA_COMPONENTS.register(name, () -> DataComponentType.<T>builder().persistent(codec).build());
    }

    public static void register(IEventBus bus) {
        DATA_COMPONENTS.register(bus);
    }
}

