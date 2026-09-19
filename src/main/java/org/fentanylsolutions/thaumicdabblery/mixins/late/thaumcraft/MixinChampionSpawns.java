package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import net.minecraft.entity.monster.EntityMob;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

import org.fentanylsolutions.thaumicdabblery.feature.champions.ChampionMobsFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import thaumcraft.common.lib.events.EventHandlerEntity;

@Mixin(value = EventHandlerEntity.class, remap = false)
public abstract class MixinChampionSpawns {

    @Shadow
    private boolean isDangerousLocation(World world, int x, int y, int z) {
        throw new AssertionError();
    }

    @Inject(method = "entitySpawns", at = @At("HEAD"), cancellable = true, require = 1)
    private void td$championPolicy(EntityJoinWorldEvent event, CallbackInfo ci) {
        if (!ChampionMobsFeature.isEnabled() || event.world.isRemote || !(event.entity instanceof EntityMob)) return;
        EntityMob mob = (EntityMob) event.entity;
        ChampionMobsFeature.checkSpawn(
            mob,
            isDangerousLocation(
                event.world,
                MathHelper.floor_double(mob.posX),
                MathHelper.floor_double(mob.posY),
                MathHelper.floor_double(mob.posZ)));
        // EntityMob has only the champion branch in this TC handler. Leave pearl/player handling untouched.
        ci.cancel();
    }
}
