package com.wandryhp.client.edwiges;

import net.minecraft.client.render.entity.state.EntityRenderState;

public class EdwigesRenderState extends EntityRenderState {
	public int animation;
	/** Segundos desde o inicio da animacao atual. */
	public float animTime;
	public boolean hasLetter;
	public String houseId;
	public float yaw;
	public float pitch;
}
