package com.dbtenkaichi.mixin.client;

import com.dbtenkaichi.client.EntityVisual;
import com.dbtenkaichi.client.anim.PoseApplier;
import com.dbtenkaichi.client.render.CustomSkins;
import com.dbtenkaichi.client.render.RenderGuard;
import com.dbtenkaichi.client.render.TenkaichiLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    private static final float[] DBTENKAICHI_BUILD_WIDTH = {0.93f, 1.0f, 1.09f};

    @Inject(method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Lnet/minecraft/resources/Identifier;", at = @At("HEAD"), cancellable = true)
    private void dbtenkaichi$customSkin(AvatarRenderState state, CallbackInfoReturnable<Identifier> cir) {
        try {
            EntityVisual visual = TenkaichiLayers.visual(state);
            if (visual != null && !visual.appearance.useOwnSkin) {
                cir.setReturnValue(visual.skinTexture());
            }
        } catch (RuntimeException e) {
            RenderGuard.report("skin", e);
        }
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void dbtenkaichi$outfitLayers(net.minecraft.world.entity.Avatar avatar, AvatarRenderState state, float partial, CallbackInfo ci) {
        try {
            EntityVisual visual = TenkaichiLayers.visual(state);
            if (visual != null && !visual.appearance.useOwnSkin) {
                state.showHat = true;
                state.showJacket = true;
                state.showLeftPants = true;
                state.showRightPants = true;
                state.showLeftSleeve = true;
                state.showRightSleeve = true;
            }
        } catch (RuntimeException e) {
            RenderGuard.report("outfit", e);
        }
    }

    private static EntityVisual dbtenkaichi$localCustomVisual() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return null;
        }
        EntityVisual visual = com.dbtenkaichi.client.ClientState.forEntity(mc.player);
        return visual.created && !visual.appearance.useOwnSkin ? visual : null;
    }

    @ModifyVariable(method = "renderHand", at = @At("HEAD"), argsOnly = true)
    private Identifier dbtenkaichi$handSkin(Identifier texture) {
        try {
            EntityVisual visual = dbtenkaichi$localCustomVisual();
            if (visual != null) {
                return visual.skinTexture();
            }
        } catch (RuntimeException e) {
            RenderGuard.report("hand_skin", e);
        }
        return texture;
    }

    private static net.minecraft.client.model.geom.ModelPart dbtenkaichi$outfitModel;
    private static final java.util.Map<com.dbtenkaichi.client.render.MajinBody, net.minecraft.client.model.geom.ModelPart> DBTENKAICHI_MAJIN_ARMS = new java.util.EnumMap<>(com.dbtenkaichi.client.render.MajinBody.class);

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void dbtenkaichi$majinHand(PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, int light, Identifier texture,
                                       net.minecraft.client.model.geom.ModelPart arm, boolean sleeve, CallbackInfo ci) {
        try {
            EntityVisual visual = dbtenkaichi$localCustomVisual();
            com.dbtenkaichi.client.render.MajinBody body = visual == null ? null : visual.majinBody();
            if (body == null) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            net.minecraft.client.model.geom.ModelPart root = DBTENKAICHI_MAJIN_ARMS.computeIfAbsent(body, b -> mc.getEntityModels().bakeLayer(b.layer));
            boolean right = arm.x < 0.0f;
            arm.resetPose();
            arm.zRot = right ? 0.1f : -0.1f;
            net.minecraft.client.model.geom.ModelPart limb = root.getChild(right ? "right_arm" : "left_arm");
            limb.loadPose(arm.storePose());
            limb.visible = true;
            collector.submitModelPart(limb, poseStack, net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(body.texture(visual)), light,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, null);
            ci.cancel();
        } catch (RuntimeException e) {
            RenderGuard.report("majin_hand", e);
        }
    }

    @Inject(method = "renderHand", at = @At("TAIL"))
    private void dbtenkaichi$outfitHand(PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, int light, Identifier texture,
                                        net.minecraft.client.model.geom.ModelPart arm, boolean sleeve, CallbackInfo ci) {
        try {
            EntityVisual visual = dbtenkaichi$localCustomVisual();
            Minecraft mc = Minecraft.getInstance();
            if (visual == null || mc.player == null) {
                return;
            }
            net.minecraft.world.item.ItemStack chest = mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
            int style = com.dbtenkaichi.item.Outfits.style(chest, com.dbtenkaichi.item.Outfits.TOP);
            if (style < 0) {
                return;
            }
            int[] spec = {style, com.dbtenkaichi.item.Outfits.primary(chest, visual.appearance.outfitPrimary),
                    com.dbtenkaichi.item.Outfits.secondary(chest, visual.appearance.outfitSecondary), -1, 0, 0, -1, 0, 0};
            Identifier outfit = CustomSkins.outfitTexture(visual.race, visual.appearance, spec);
            if (dbtenkaichi$outfitModel == null) {
                dbtenkaichi$outfitModel = mc.getEntityModels().bakeLayer(com.dbtenkaichi.client.render.TenkaichiModels.OUTFIT);
            }
            net.minecraft.client.model.geom.ModelPart wide = dbtenkaichi$outfitModel.getChild(arm.x > 0.0f ? "left_arm" : "right_arm");
            wide.loadPose(arm.storePose());
            wide.visible = true;
            for (String child : new String[]{"left_sleeve", "right_sleeve"}) {
                if (wide.hasChild(child)) {
                    wide.getChild(child).visible = true;
                }
            }
            collector.submitModelPart(wide, poseStack, net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(outfit), light,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, null);
        } catch (RuntimeException e) {
            RenderGuard.report("outfit_hand", e);
        }
    }

    @ModifyVariable(method = "renderHand", at = @At("HEAD"), argsOnly = true)
    private boolean dbtenkaichi$handSleeve(boolean sleeve) {
        return sleeve || dbtenkaichi$localCustomVisual() != null;
    }

    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", at = @At("TAIL"))
    private void dbtenkaichi$bodyTransform(AvatarRenderState state, PoseStack poseStack, float bodyRot, float entityScale, CallbackInfo ci) {
        try {
            Minecraft mc = Minecraft.getInstance();
            Entity entity = mc.level == null ? null : mc.level.getEntity(state.id);
            if (entity == null || state.isFallFlying || state.swimAmount > 0.0f) {
                return;
            }
            EntityVisual visual = TenkaichiLayers.visual(state);
            if (visual != null) {
                PoseApplier.applyBodyTransform(poseStack, entity, visual, bodyRot, entity.getBbHeight() / Math.max(0.01f, entityScale));
            }
        } catch (RuntimeException e) {
            RenderGuard.report("body", e);
        }
    }

    @Inject(method = "scale(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("TAIL"))
    private void dbtenkaichi$build(AvatarRenderState state, PoseStack poseStack, CallbackInfo ci) {
        EntityVisual visual = TenkaichiLayers.visual(state);
        if (visual != null) {
            if (visual.majinBody() != null) {
                com.dbtenkaichi.client.render.MajinBody.applyScale(poseStack, visual);
                return;
            }
            float width = DBTENKAICHI_BUILD_WIDTH[Math.max(0, Math.min(2, visual.appearance.build))] * (visual.appearance.presentation == 1 ? 0.93f : 1.0f);
            if (width != 1.0f) {
                poseStack.scale(width, 1.0f, width);
            }
        }
    }
}
