package it.hurts.octostudios.rarcompat.items.hat;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.init.DataComponentRegistry;
import it.hurts.sskirillss.relics.items.relics.base.IRelicItem;
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
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import top.theillusivec4.curios.api.SlotContext;

import java.awt.*;

import static it.hurts.sskirillss.relics.utils.EntityUtils.rayTraceEntity;

public class CowboyHatItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("cowboy")
                                .stat(StatData.builder("speed")
                                        .initialValue(0.2D, 0.3D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.15D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())
                                .research(ResearchData.builder()
                                        .star(0, 5, 8).star(1, 7, 22).star(2, 16, 22).star(3, 19, 18).star(4, 12, 17)
                                        .link(4, 0).link(4, 1).link(4, 2).link(4, 3)
                                        .build())
                                .build())
                        .ability(AbilityData.builder("overlord")
                                .active(CastData.builder().type(CastType.INSTANTANEOUS)
                                        .predicate("overlord", PredicateType.CAST, (player, stack) -> rayTraceEntity(player, entity -> entity instanceof Mob
                                                && checkMob(entity, EnderDragon.class, WitherBoss.class, Warden.class, ElderGuardian.class)
                                                && !player.isPassenger(), player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE).getValue()) != null)
                                        .build())
                                .requiredLevel(5)
                                .stat(StatData.builder("time")
                                        .initialValue(2D, 5D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.15D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff572814)
                                .borderBottom(0xff473626)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFF624d2e)
                                .endColor(0x00481f14)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(15)
                        .step(100)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData.abilityBuilder("cowboy")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.ORANGE)
                                        .build())
                                .source(LevelingSourceData.abilityBuilder("overlord")
                                        .initialValue(1)
                                        .gem(GemShape.SQUARE, GemColor.BLUE)
                                        .build())
                                .build())
                        .build())
                .loot(LootData.builder()
                        .entry(LootEntries.VILLAGE, LootEntries.SAVANNA)
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || !(player.getRootVehicle() instanceof Mob beingMounted)
                || !getToggled(stack) || !checkMob(player, EnderDragon.class, WitherBoss.class, Warden.class, ElderGuardian.class))
            return;

        if (isAbilityOnCooldown(stack, "overlord") || !isAbilityUnlocked(stack, "overlord"))
            player.stopRiding();
        else {
            var random = player.getRandom();
            var level = player.getCommandSenderWorld();

            if (getTime(stack) >= getStatValue(stack, "overlord", "time") * 20) {
                player.stopRiding();
                player.playSound(SoundEvents.WOOL_HIT, 1.0F, 0.9F + player.getRandom().nextFloat() * 0.2F);

                setTime(stack, 0);

                for (int i = 0; i < 50; i++)
                    level.addParticle(ParticleUtils.constructSimpleSpark(new Color(150 + random.nextInt(106), 50 + random.nextInt(100), 50 + random.nextInt(100)),
                                    0.5F, 60, 0.95F),
                            player.getX(), player.getY() + 1.0, player.getZ(),
                            (random.nextDouble() - 0.5) * 3.0,
                            random.nextDouble() * 1.5,
                            (random.nextDouble() - 0.5) * 3.0);
            } else {
                if (level.isClientSide() && player instanceof LocalPlayer localPlayer && localPlayer.input.jumping && beingMounted.onGround()
                        && !isWaterOrFlyingMob(beingMounted))
                    beingMounted.addDeltaMovement(new Vec3(0, 0.8, 0));

                var knownMovement = beingMounted.getKnownMovement();

                if ((knownMovement.x != 0 || knownMovement.z != 0) && random.nextFloat() <= 0.25F && player.tickCount % 20 == 0)
                    spreadRelicExperience(player, stack, 1);

                if (isAbilityUnlocked(stack, "cowboy"))
                    changeAttributes(beingMounted, stack, true, Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH, Attributes.SAFE_FALL_DISTANCE);

                addTime(stack, 1);
            }
        }
    }

    @Override
    public void castActiveAbility(ItemStack stack, Player player, String ability, CastType type, CastStage stage) {
        if (player.getCommandSenderWorld().isClientSide() || !ability.equals("overlord"))
            return;

        EntityHitResult result = rayTraceEntity(player, entity -> entity instanceof Mob, player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE).getValue());

        if (result == null)
            return;

        setToggled(stack, true);

        spreadRelicExperience(player, stack, 1);

        player.startRiding(result.getEntity());
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || newStack.getItem() == stack.getItem()
                || !(player.getRootVehicle() instanceof Mob mob))
            return;

        changeAttributes(mob, stack, false, Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH, Attributes.SAFE_FALL_DISTANCE);

        if (!(player.getControlledVehicle() instanceof Mob))
            player.stopRiding();
    }

    @SafeVarargs
    public final void changeAttributes(Mob beingMounted, ItemStack stack, boolean flag, Holder<Attribute>... attributeHolder) {
        for (Holder<Attribute> attributes : attributeHolder)
            if (flag)
                EntityUtils.applyAttribute(beingMounted, stack, attributes, (float) getStatValue(stack, "cowboy", "speed"), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            else
                EntityUtils.removeAttribute(beingMounted, stack, attributes, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    @SafeVarargs
    private boolean checkMob(Entity entity, Class<? extends Mob>... mobClasses) {
        for (Class<? extends Mob> mobClass : mobClasses)
            if (mobClass.isInstance(entity))
                return false;

        return true;
    }

    public boolean isWaterOrFlyingMob(Mob mounted) {
        return mounted instanceof FlyingAnimal || mounted instanceof FlyingMob || mounted instanceof WaterAnimal || mounted instanceof AmbientCreature;
    }

    public void addTime(ItemStack stack, int val) {
        setTime(stack, getTime(stack) + val);
    }

    public int getTime(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.TIME, 0);
    }

    public void setTime(ItemStack stack, int val) {
        stack.set(DataComponentRegistry.TIME, Math.max(val, 0));
    }

    public void setToggled(ItemStack stack, boolean val) {
        stack.set(DataComponentRegistry.TOGGLED, val);
    }

    public boolean getToggled(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.TOGGLED, false);
    }

    @EventBusSubscriber
    public static class CowboyEvent {
        @SubscribeEvent
        public static void onEntityMount(EntityMountEvent event) {
            if (!(event.getEntity() instanceof Player player))
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.COWBOY_HAT.value());

            if (player.getCommandSenderWorld().isClientSide() || !(stack.getItem() instanceof CowboyHatItem relic) || !event.isDismounting()
                    || !(event.getEntityBeingMounted() instanceof Mob mount) || !relic.getToggled(stack))
                return;

            relic.addAbilityCooldown(stack, "overlord", 600);
            relic.setTime(stack, 0);
            relic.setToggled(stack, false);
            relic.changeAttributes(mount, stack, false, Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH, Attributes.SAFE_FALL_DISTANCE);
        }
    }
}