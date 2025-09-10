package it.hurts.octostudios.rarcompat.items.necklace;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootData;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootCollections;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.TooltipData;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;

public class ShockPendantItem extends WearableRelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("passive")
                                .maxLevel(0)
                                .build())
                        .ability(AbilityData.builder("lightning")
                                .stat(StatData.builder("damage")
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.2D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(StatData.builder("chance")
                                        .initialValue(0.1D, 0.2D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100, 1))
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xff0090cd)
                                .borderBottom(0xff0e356e)
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .maxLevel(10)
                        .step(100)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.COLD)
                        .build())
                .build();
    }

    @Mod.EventBusSubscriber
    public static class ShockPendantEvent {

        @SubscribeEvent
        public static void onLivingAttack(LivingAttackEvent event) {
            if (!(event.getEntity() instanceof Player player) || !event.getSource().is(DamageTypeTags.IS_LIGHTNING))
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.SHOCK_PENDANT.get());

            if (!(stack.getItem() instanceof ShockPendantItem relic) || !relic.canPlayerUseActiveAbility(player, stack, "passive"))
                return;

            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onReceivingDamage(LivingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player))
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.SHOCK_PENDANT.get());
            var level = player.getCommandSenderWorld();

            if (level.isClientSide() || !(stack.getItem() instanceof ShockPendantItem relic)
                    || !relic.canPlayerUseActiveAbility(player, stack, "lightning"))
                return;

            var random = level.getRandom();
            var attacker = event.getSource().getEntity();

            if (attacker == null || random.nextDouble() > relic.getAbilityValue(stack, "lightning", "chance"))
                return;

            relic.spreadExperience(player, stack, 1);

            var bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);

            bolt.setCause((ServerPlayer) player);
            bolt.setPos(attacker.position());
            bolt.setVisualOnly(true);

            level.addFreshEntity(bolt);

            attacker.invulnerableTime = 0;
            attacker.hurt(bolt.damageSources().lightningBolt(), (float) relic.getAbilityValue(stack, "lightning", "damage"));

            ((ServerLevel) level).sendParticles(ParticleUtils.constructSimpleSpark(new Color(random.nextInt(50), random.nextInt(50), 50 + random.nextInt(55)), 0.4F, 30, 0.95F),
                    attacker.getX(), attacker.getY() + attacker.getBbHeight() / 2F, attacker.getZ(), 10, attacker.getBbWidth() / 2F, attacker.getBbHeight() / 2F, attacker.getBbWidth() / 2F, 0.025F);
        }
    }
}