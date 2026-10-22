package com.dbtenkaichi.mixin.client;

import com.dbtenkaichi.client.EntityVisual;
import com.dbtenkaichi.client.anim.PoseApplier;
import com.dbtenkaichi.client.render.RenderGuard;
import com.dbtenkaichi.client.render.TenkaichiLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void dbtenkaichi$applyPose(AvatarRenderState state, CallbackInfo ci) {
        try {
            EntityVisual visual = TenkaichiLayers.visual(state);
            PlayerModel self = (PlayerModel) (Object) this;
            com.dbtenkaichi.client.render.MajinBody body = visual == null ? null : visual.majinBody();
            if (visual != null) {
                PoseApplier.apply(self, visual, state.ageInTicks);
            }
            if (!(self instanceof TenkaichiLayers.HairModel)) {
                boolean shown = body == null;
                self.body.visible = shown;
                self.rightArm.visible = shown;
                self.leftArm.visible = shown;
                self.rightLeg.visible = shown;
                self.leftLeg.visible = shown;
            }
            if (body != null && body.headScale != 1.0f) {
                self.head.xScale = body.headScale;
                self.head.yScale = body.headScale;
                self.head.zScale = body.headScale;
            }
        } catch (RuntimeException e) {
            RenderGuard.report("pose", e);
        }
    }
}
