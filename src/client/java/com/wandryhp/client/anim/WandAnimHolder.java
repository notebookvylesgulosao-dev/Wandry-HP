package com.wandryhp.client.anim;

/** Acessor (duck) injetado no PlayerEntityRenderState: carrega a pose da varinha até o modelo/feature renderer. */
public interface WandAnimHolder {
	/** Pose atual da conjuração ou null quando não há animação. */
	WandPose wandryhp$getPose();

	void wandryhp$setPose(WandPose pose);
}
