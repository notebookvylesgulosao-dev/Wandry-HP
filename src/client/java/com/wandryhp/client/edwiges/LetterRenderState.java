package com.wandryhp.client.edwiges;

import net.minecraft.client.render.entity.state.EntityRenderState;

public class LetterRenderState extends EntityRenderState {
	public String houseId;
	public float yaw;
	public float pitch;
	/** 0 = em pe (caindo, como preso), 1 = deitado no chao. */
	public float flat;
}
