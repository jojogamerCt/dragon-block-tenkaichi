package com.dbtenkaichi.mixin.client;

import com.dbtenkaichi.client.anim.FirstPersonCombat;
import com.dbtenkaichi.client.render.RenderGuard;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsMixin {
    @Shadow
    private void renderPlayerArm(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, float inverseArmHeight, float attackValue,
                                 HumanoidArm arm, PlayerRenderState playerState) {
        throw new AssertionError();
    }

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void dbtenkaichi$combatArms(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state, float partialTicks, float xRot, InteractionHand hand,
                                        float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                                        CallbackInfo ci) {
        try {
            if (com.dbtenkaichi.client.cinematic.CinematicDirector.localActive()) {
                ci.cancel();
                return;
            }
            if (state.isScoping || !itemStack.isEmpty() || !FirstPersonCombat.active()) {
                return;
            }
            AvatarRenderState avatar = playerState.avatarRenderState;
            if (avatar == null || avatar.isInvisible) {
                return;
            }
            HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? avatar.mainArm : avatar.mainArm.getOpposite();
            poseStack.pushPose();
            FirstPersonCombat.transform(poseStack, arm, partialTicks);
            renderPlayerArm(poseStack, collector, lightCoords, inverseArmHeight, 0.0f, FirstPersonCombat.renderedArm(arm), playerState);
            poseStack.popPose();
            ci.cancel();
        } catch (RuntimeException e) {
            RenderGuard.report("first_person", e);
        }
    }
}
