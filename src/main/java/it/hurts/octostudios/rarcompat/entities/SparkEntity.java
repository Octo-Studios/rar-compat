package it.hurts.octostudios.rarcompat.entities;

import it.hurts.octostudios.octolib.module.particle.trail.EntityTrailProvider;
import it.hurts.octostudios.rarcompat.items.hands.FireGauntletItem;
import it.hurts.sskirillss.relics.entities.misc.ITargetableEntity;
import it.hurts.sskirillss.relics.utils.MathUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class SparkEntity extends ThrowableProjectile implements ITargetableEntity {
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(SparkEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FIRE_DURATION = SynchedEntityData.defineId(SparkEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<ItemStack> RELIC_STACK = SynchedEntityData.defineId(SparkEntity.class, EntityDataSerializers.ITEM_STACK);

    @Getter
    @Setter
    private LivingEntity target;

    public SparkEntity(EntityType<? extends ThrowableProjectile> type, Level worldIn) {
        super(type, worldIn);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.isInWaterRainOrBubble()) {
            this.discard();
            return;
        }

        var level = getCommandSenderWorld();
        var particleCenter = this.getPosition(1).add(0, 0.5, 0);

        level.addParticle(ParticleUtils.constructSimpleSpark(new Color(200 + random.nextInt(56), 100 + random.nextInt(156), 0), 0.01F + random.nextFloat() * 0.1F, 5 + random.nextInt(3), 0.9F),
                particleCenter.x() + MathUtils.randomFloat(random) * 0.05F, particleCenter.y() + MathUtils.randomFloat(random) * 0.05F, particleCenter.z() + MathUtils.randomFloat(random) * 0.05F, 0F, 0F, 0F);

        if (level.isClientSide())
            return;

        if (target == null || target.isDeadOrDying() || tickCount >= 120) {
            this.discard();

            return;
        }

        if (this.distanceTo(target) <= 1)
            hitEntity(target);

        Vec3 targetPos = new Vec3(target.getX(), target.getY() + target.getBbHeight() / 2F, target.getZ());
        Vec3 direction = targetPos.subtract(this.position()).normalize();

        var motion = this.getDeltaMovement();
        var factor = Math.clamp(tickCount * 0.05F, 0F, 1F);

        var deltaX = motion.x + (direction.x * factor - motion.x) * factor;
        var deltaZ = motion.z + (direction.z * factor - motion.z) * factor;

        this.setDeltaMovement(new Vec3(deltaX, direction.scale(this.position().distanceTo(targetPos) * (this.tickCount * 0.01F)).y, deltaZ));
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);

        if (result instanceof EntityHitResult entityResult && entityResult.getEntity() instanceof LivingEntity entity)
            hitEntity(entity);
    }

    public void hitEntity(LivingEntity target) {
        if (this.getOwner() instanceof Player player && !target.getStringUUID().equals(player.getStringUUID())) {
            target.invulnerableTime = 0;

            if (target.hurt(getCommandSenderWorld().damageSources().mobProjectile(this, player), getDamage())) {
                var fireDuration = Math.max(0F, getFireDuration());

                if (fireDuration <= 0F && getRelicStack().getItem() instanceof FireGauntletItem relic) {
                    var ability = relic.getRelicData(player, getRelicStack()).getAbilitiesData().getAbilityData("flame");

                    if (ability.canPlayerUse(player))
                        fireDuration = (float) Math.max(0D, ability.getStatData("spark_fire_duration").getValue());
                }

                if (fireDuration > 0F)
                    FireGauntletItem.igniteFromGauntlet(target, player, fireDuration);
            }
        }

        this.discard();
    }

    public void setDamage(float damage) {
        this.getEntityData().set(DAMAGE, damage);
    }

    public float getDamage() {
        return this.getEntityData().get(DAMAGE);
    }

    public void setFireDuration(float duration) {
        this.getEntityData().set(FIRE_DURATION, Math.max(0F, duration));
    }

    public float getFireDuration() {
        return this.getEntityData().get(FIRE_DURATION);
    }

    @Override
    public boolean hurt(DamageSource p_19946_, float p_19947_) {
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    public void setRelicStack(ItemStack stack) {
        this.getEntityData().set(RELIC_STACK, stack);
    }

    public ItemStack getRelicStack() {
        return this.getEntityData().get(RELIC_STACK);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        tag.put("relic_stack", getRelicStack().save(this.registryAccess()));
        tag.putFloat("damage", getDamage());
        tag.putFloat("fire_duration", getFireDuration());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        setRelicStack(ItemStack.parseOptional(this.registryAccess(), tag.getCompound("relic_stack")));
        setDamage(tag.getFloat("damage"));
        setFireDuration(tag.getFloat("fire_duration"));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DAMAGE, 1F);
        builder.define(FIRE_DURATION, 0F);
        builder.define(RELIC_STACK, ItemStack.EMPTY);
    }

    @OnlyIn(Dist.CLIENT)
    public static class TrailProvider extends EntityTrailProvider<SparkEntity> {
        public TrailProvider(SparkEntity entity) {
            super(entity);
        }

        @Override
        public Vec3 getTrailPosition(float partialTicks) {
            return entity.getPosition(partialTicks).add(0, 0.5, 0);
        }

        @Override
        public int getTrailUpdateFrequency() {
            return 1;
        }

        @Override
        public boolean isTrailAlive() {
            return entity.isAlive();
        }

        @Override
        public boolean isTrailGrowing() {
            return entity.tickCount > 2;
        }

        @Override
        public int getTrailMaxLength() {
            return 4;
        }

        @Override
        public int getTrailFadeInColor() {
            return 0xFFB22222;
        }

        @Override
        public int getTrailFadeOutColor() {
            return 0x80FF8C00;
        }

        @Override
        public double getTrailScale() {
            return 0.05F;
        }
    }
}