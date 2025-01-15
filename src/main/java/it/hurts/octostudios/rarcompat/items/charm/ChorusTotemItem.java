package it.hurts.octostudios.rarcompat.items.charm;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.init.DataComponentRegistry;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.CastData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastStage;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastType;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.PredicateType;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.*;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.GemColor;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.GemShape;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootData;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import it.hurts.sskirillss.relics.items.relics.base.data.research.ResearchData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.BeamsData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.TooltipData;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import it.hurts.sskirillss.relics.utils.data.WorldPosition;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import top.theillusivec4.curios.api.SlotContext;

public class ChorusTotemItem extends WearableRelicItem {

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("past")
                                .active(CastData.builder().type(CastType.INSTANTANEOUS)
                                        .predicate("past", PredicateType.CAST, (player, stack) -> teleportedPlayer(player, player.position(), getWorldPos(stack, player).getPos()))
                                        .build())
                                .stat(StatData.builder("capacity")
                                        .initialValue(14D, 12D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, -0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .research(ResearchData.builder()
                                        .star(0, 11, 19).star(1, 11, 27).star(2, 3, 12).star(3, 19, 12)
                                        .star(4, 13, 8).star(5, 9, 8)
                                        .link(0, 1).link(1, 2).link(0, 2).link(0, 3).link(1, 3).link(4, 0).link(5, 0)
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff9045a6)
                                .borderBottom(0xff258273)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFF7a09b0)
                                .endColor(0x003c0357)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData.abilityBuilder("past")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.PURPLE)
                                        .build())
                                .build())
                        .build())
                .loot(LootData.builder()
                        .entry(LootEntries.WILDCARD, LootEntries.END_LIKE, LootEntries.THE_END)
                        .build())
                .build();
    }

    @Override
    public void castActiveAbility(ItemStack stack, Player player, String ability, CastType type, CastStage stage) {
        Vec3 pos = getWorldPos(stack, player).getPos();

        player.teleportTo(pos.x, pos.y, pos.z);

        spreadRelicExperience(player, stack, 1);

        setAbilityCooldown(stack, "past", (int) getStatValue(stack, "past", "capacity") * 20);

        if (stage == CastStage.END)
            setToggled(stack, true);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player))
            return;

        int tickCount = player.tickCount;

        if (canPlayerUseAbility(player, stack, "past"))
            setToggled(stack, false);

        if (tickCount % 2 == 0 && getToggled(stack))
            setWorldPos(stack, new WorldPosition(player));
        else {
            if (tickCount % 20 == 0) {
                addTime(stack, 1);
            }

            if (getTime(stack) >= 5) {
                setWorldPos(stack, new WorldPosition(player));
                addTime(stack, -getTime(stack));

                setToggled(stack, true);
            }
        }
    }

    public boolean teleportedPlayer(Player player, Vec3 startPosition, Vec3 endPosition) {
        return !(startPosition.distanceTo(endPosition) <= player.getKnownMovement().length() * 10);
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        if (prevStack.getItem() == stack.getItem())
            return;

        setWorldPos(stack, null);
        setToggled(stack, true);
        addTime(stack, -getTime(stack));
    }

    public void setWorldPos(ItemStack stack, WorldPosition worldPosition) {
        stack.set(DataComponentRegistry.WORLD_POSITION, worldPosition);
    }

    public WorldPosition getWorldPos(ItemStack stack, Player player) {
        return stack.getOrDefault(DataComponentRegistry.WORLD_POSITION, new WorldPosition(player));
    }

    public void addTime(ItemStack stack, int val) {
        stack.set(DataComponentRegistry.TIME, getTime(stack) + val);
    }

    public int getTime(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.TIME, 0);
    }

    public void setToggled(ItemStack stack, boolean val) {
        stack.set(DataComponentRegistry.TOGGLED, val);
    }

    public boolean getToggled(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.TOGGLED, true);
    }

    @EventBusSubscriber
    public static class ChorusTotemEvent {
        @SubscribeEvent
        public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
            Player player = event.getEntity();

            ItemStack itemStack = EntityUtils.findEquippedCurio(player, ModItems.CHORUS_TOTEM.value());

            if (!(itemStack.getItem() instanceof ChorusTotemItem relic))
                return;

            relic.setWorldPos(itemStack, null);
        }
    }
}
