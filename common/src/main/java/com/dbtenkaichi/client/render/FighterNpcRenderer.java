package com.dbtenkaichi.client.render;

import com.dbtenkaichi.client.ClientState;
import com.dbtenkaichi.client.EntityVisual;
import com.dbtenkaichi.client.anim.PoseApplier;
import com.dbtenkaichi.entity.FighterNpc;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

public class FighterNpcRenderer extends LivingEntityRenderer<FighterNpc, AvatarRenderState, PlayerModel> {
    public FighterNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(TenkaichiModels.FIGHTER), false), 0.5f);
        for (RenderLayer<AvatarRenderState, PlayerModel> layer : TenkaichiLayers.create(this, context.getModelSet())) {
            this.addLayer(layer);
        }
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public void extractRenderState(FighterNpc entity, AvatarRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTicks, this.itemModelResolver);
        state.skin = DefaultPlayerSkin.getDefaultSkin();
        state.id = entity.getId();
        state.showHat = true;
        state.showJacket = true;
        state.showLeftPants = true;
        state.showRightPants = true;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        state.showCape = false;
        state.isSpectator = false;
    }

    @Override
    protected void scale(AvatarRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        EntityVisual visual = TenkaichiLayers.visual(state);
        if (visual != null && Math.abs(visual.appearance.height - 1.0f) > 0.01f) {
            float h = visual.appearance.height;
            poseStack.scale(h, h, h);
        }
        if (visual != null && visual.majinBody() != null) {
            MajinBody.applyScale(poseStack, visual);
        }
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        EntityVisual visual = TenkaichiLayers.visual(state);
        if (visual == null) {
            return DefaultPlayerSkin.getDefaultSkin().body().texturePath();
        }
        return visual.skinTexture();
    }

    @Override
    protected void setupRotations(AvatarRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        Minecraft mc = Minecraft.getInstance();
        Entity entity = mc.level == null ? null : mc.level.getEntity(state.id);
        if (entity != null) {
            PoseApplier.applyBodyTransform(poseStack, entity, ClientState.forEntity(entity), bodyRot, entity.getBbHeight());
        }
    }
}
