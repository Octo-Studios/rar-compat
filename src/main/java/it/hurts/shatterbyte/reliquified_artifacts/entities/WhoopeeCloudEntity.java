package it.hurts.shatterbyte.reliquified_artifacts.entities;

import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.UUID;

public class WhoopeeCloudEntity extends Entity {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(WhoopeeCloudEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> INITIAL_RADIUS = SynchedEntityData.defineId(WhoopeeCloudEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(WhoopeeCloudEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;

    public WhoopeeCloudEntity(EntityType<? extends WhoopeeCloudEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public void configure(@Nullable Player owner, float radius, int durationTicks) {
        this.ownerUuid = owner == null ? null : owner.getUUID();
        setInitialRadius(Math.max(0.5F, radius));
        setRadius(Math.max(0.5F, radius));
        setDuration(Math.max(1, durationTicks));
    }

    @Override
    public void tick() {
        super.tick();

        var radius = getRadius();

        if (radius <= 0.05F || this.tickCount >= getDuration()) {
            discard();
            return;
        }

        if (this.level().isClientSide()) {
            spawnParticles(radius);
            return;
        }

        var duration = Math.max(1, getDuration());
        var progress = Math.min(1F, this.tickCount / (float) duration);
        var nextRadius = Math.max(0F, getInitialRadius() * (1F - progress));

        setRadius(nextRadius);

        if (nextRadius <= 0.05F) {
            discard();
            return;
        }

        if (this.tickCount % 5 == 0)
            applyEffects(nextRadius);
    }

    private void spawnParticles(float radius) {
        var centerX = this.getX();
        var centerY = this.getY() + 0.35D;
        var centerZ = this.getZ();

        var count = Math.ceil(radius * 10F);

        for (var i = 0; i < count; i++) {
            var theta = this.random.nextDouble() * Math.PI * 2D;
            var phi = Math.acos(2D * this.random.nextDouble() - 1D);

            var r = Math.cbrt(this.random.nextDouble()) * radius;

            var x = Math.sin(phi) * Math.cos(theta) * r;
            var y = Math.cos(phi) * r;
            var z = Math.sin(phi) * Math.sin(theta) * r;

            this.level().addParticle(
                    ParticleUtils.constructSimpleSpark(
                            new Color(120 + this.random.nextInt(50), 255, 80 + this.random.nextInt(40)),
                            0.34F + this.random.nextFloat() * 0.14F,
                            14 + this.random.nextInt(6),
                            0.9F),
                    centerX + x,
                    centerY + y,
                    centerZ + z,
                    0D,
                    0.003D,
                    0D
            );
        }
    }

    private void applyEffects(float radius) {
        var radiusSq = radius * radius;
        var owner = getOwnerPlayer();

        for (var target : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius),
                living -> living.isAlive() && living != owner)) {
            var centerY = target.getY() + target.getBbHeight() * 0.5D;
            var dx = target.getX() - this.getX();
            var dy = centerY - (this.getY() + 0.35D);
            var dz = target.getZ() - this.getZ();

            if (dx * dx + dy * dy + dz * dz > radiusSq)
                continue;

            if (owner != null && target instanceof Mob mob && mob.getTarget() == owner) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }

            target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, true), this);
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, true), this);
        }
    }

    @Nullable
    private Player getOwnerPlayer() {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.ownerUuid == null || serverLevel.getServer() == null)
            return null;

        return serverLevel.getServer().getPlayerList().getPlayer(this.ownerUuid);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    private void setRadius(float value) {
        this.entityData.set(RADIUS, Math.max(0F, value));
    }

    public float getInitialRadius() {
        return this.entityData.get(INITIAL_RADIUS);
    }

    private void setInitialRadius(float value) {
        this.entityData.set(INITIAL_RADIUS, Math.max(0F, value));
    }

    public int getDuration() {
        return this.entityData.get(DURATION);
    }

    private void setDuration(int value) {
        this.entityData.set(DURATION, Math.max(1, value));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 0.5F);
        builder.define(INITIAL_RADIUS, 0.5F);
        builder.define(DURATION, 120);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setRadius(tag.getFloat("radius"));
        setInitialRadius(tag.getFloat("initial_radius"));
        setDuration(tag.getInt("duration"));

        if (tag.hasUUID("owner"))
            this.ownerUuid = tag.getUUID("owner");
        else
            this.ownerUuid = null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("radius", getRadius());
        tag.putFloat("initial_radius", getInitialRadius());
        tag.putInt("duration", getDuration());

        if (this.ownerUuid != null)
            tag.putUUID("owner", this.ownerUuid);
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }
}