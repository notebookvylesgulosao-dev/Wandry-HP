package com.wandryhp.client;

import com.wandryhp.SpellProjectileEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public final class SpellProjectileRenderer extends EntityRenderer<SpellProjectileEntity, SpellProjectileRenderer.State> {
    public static final class State extends EntityRenderState {
        int model;
        float yaw, pitch;
    }

    public SpellProjectileRenderer(EntityRendererFactory.Context context) { super(context); }

    @Override public State createRenderState() { return new State(); }

    @Override public void updateRenderState(SpellProjectileEntity entity, State state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.model = MathHelper.clamp(entity.modelIndex(), 0, QuadModel.NAMES.length - 1);
        state.yaw = MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw());
        state.pitch = MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch());
    }

    @Override public void render(State state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.translate(0, 0.15, 0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(state.yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-state.pitch));
        float scale = state.model < 7 ? 1.6f : 2.2f; // props são menores que as esferas dos feitiços
        matrices.scale(scale, scale, scale);
        QuadModel.get(QuadModel.NAMES[state.model]).render(matrices, vertexConsumers, light);
        matrices.pop();
        super.render(state, matrices, vertexConsumers, light);
    }
}
