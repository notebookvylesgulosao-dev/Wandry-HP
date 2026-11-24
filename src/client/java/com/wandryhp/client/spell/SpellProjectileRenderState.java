package com.wandryhp.client.spell;

import net.minecraft.client.render.entity.state.EntityRenderState;

public class SpellProjectileRenderState extends EntityRenderState {
	public String modelId = "green";
	/** Direção de voo: yaw/pitch em radianos que levam o eixo +Z do modelo (nariz) para a velocidade. */
	public float dirYaw;
	public float dirPitch;
}
