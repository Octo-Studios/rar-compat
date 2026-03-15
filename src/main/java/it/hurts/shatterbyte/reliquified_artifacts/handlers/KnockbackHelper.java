package it.hurts.shatterbyte.reliquified_artifacts.handlers;

import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.network.packets.S2CSetEntityMotion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class KnockbackHelper {
    public static void apply(LivingEntity target, double strength, Vec3 direction) {
        if (strength <= 0D)
            return;

        if (direction.lengthSqr() <= 1.0E-6D)
            return;

        var impulse = direction.normalize().scale(strength);
        var motion = target.getDeltaMovement().add(-impulse.x, -impulse.y, -impulse.z);

        target.setDeltaMovement(motion);
        target.hasImpulse = true;

        if (target instanceof ServerPlayer serverPlayer)
            NetworkHandler.sendToClient(new S2CSetEntityMotion(target.getId(), motion.toVector3f()), serverPlayer);
    }
}
