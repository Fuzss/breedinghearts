package com.fuzs.respawnableanimals.mixin;

import com.fuzs.respawnableanimals.config.ConfigBuildHandler;
import com.fuzs.respawnableanimals.mixin.accessor.IOcelotEntityAccessor;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@SuppressWarnings("unused")
@Mixin(MobEntity.class)
public abstract class MobEntityMixin extends LivingEntity {

    protected MobEntityMixin(EntityType<? extends LivingEntity> type, World worldIn) {

        super(type, worldIn);
    }

    @Shadow
    public abstract void enablePersistence();

    @Shadow
    public abstract boolean canDespawn(double distanceToClosestPlayer);

    @Redirect(method = "checkDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/MobEntity;canDespawn(D)Z"))
    public boolean canAnimalDespawn(MobEntity entity, double distanceToClosestPlayer) {

        return entity instanceof AnimalEntity || entity.canDespawn(distanceToClosestPlayer);
    }

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "preventDespawn", at = @At("HEAD"), cancellable = true)
    public void preventDespawn(CallbackInfoReturnable<Boolean> callbackInfo) {

        MobEntity entity = (MobEntity) (Object) this;
        if (entity instanceof AnimalEntity) {

            if (ConfigBuildHandler.animalBlacklist.contains(entity.getType()) && canDespawnBePrevented(entity)) {

                callbackInfo.setReturnValue(true);
            }

            if (ConfigBuildHandler.oldChunkPersistence && entity.world.getChunkAt(entity.getPosition()).getInhabitedTime() > 72000L) {

                callbackInfo.setReturnValue(true);
            }

            if (entity instanceof TameableEntity && ((TameableEntity) entity).isTamed() || entity instanceof OcelotEntity && ((IOcelotEntityAccessor) entity).callIsTrusting()) {

                callbackInfo.setReturnValue(true);
            }
        }
    }

    @Unique
    private static boolean canDespawnBePrevented(MobEntity entity) {

        Entity closestPlayer = entity.world.getClosestPlayer(entity, -1.0);
        if (closestPlayer != null) {

            double distanceToPlayer = entity.getDistanceSq(entity);
            return !entity.canDespawn(distanceToPlayer);
        }

        return true;
    }

    @Inject(method = "onInitialSpawn", at = @At("HEAD"))
    public void onInitialSpawn(IServerWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag, CallbackInfoReturnable<ILivingEntityData> callbackInfo) {

        if (ConfigBuildHandler.summonedMobPersistence) {

            // affects all mobs, not just animals
            if (reason == SpawnReason.COMMAND || reason == SpawnReason.SPAWN_EGG || reason == SpawnReason.DISPENSER) {

                this.enablePersistence();
            }
        }
    }

}