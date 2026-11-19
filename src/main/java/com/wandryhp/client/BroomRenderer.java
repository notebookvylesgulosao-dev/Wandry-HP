package com.wandryhp.client;

import com.wandryhp.BroomEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public final class BroomRenderer extends EntityRenderer<BroomEntity, BroomRenderer.State> {
    public static final class State extends EntityRenderState {
        float yaw;
    }

    public BroomRenderer(EntityRendererFactory.Context context) { super(context); }

    @Override public State createRenderState() { return new State(); }

    @Override public void updateRenderState(BroomEntity entity, State state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.yaw = MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw());
    }

    @Override public void render(State state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.translate(0, 0.3, 0);
        // o modelo aponta para +Z (cabo à frente, cerdas atrás)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-state.yaw));
        QuadModel.get("witch_broom").render(matrices, vertexConsumers, light);
        matrices.pop();
        super.render(state, matrices, vertexConsumers, light);
    }
}
