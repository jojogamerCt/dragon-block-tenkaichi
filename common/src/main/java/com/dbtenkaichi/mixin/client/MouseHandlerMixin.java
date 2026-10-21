package com.dbtenkaichi.mixin.client;

import com.dbtenkaichi.client.cinematic.CinematicDirector;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void dbtenkaichi$lockDuringCinematic(double mousea, CallbackInfo ci) {
        if (CinematicDirector.localActive()) {
            ci.cancel();
        }
    }
}
