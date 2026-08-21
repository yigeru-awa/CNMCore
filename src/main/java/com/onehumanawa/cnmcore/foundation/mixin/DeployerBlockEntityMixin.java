package com.onehumanawa.cnmcore.foundation.mixin;

import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeployerBlockEntity.class)
public class DeployerBlockEntityMixin {

    @Shadow
    protected int timer;

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/kinetics/deployer/DeployerBlockEntity;timer:I", shift = At.Shift.AFTER, ordinal = 0, opcode = Opcodes.GETFIELD))
    private void removeRetractDelay(CallbackInfo ci) {
        timer = 0;
    }
}