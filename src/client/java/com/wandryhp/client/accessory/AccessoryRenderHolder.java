package com.wandryhp.client.accessory;

import com.wandryhp.accessory.CapeType;

/** Duck injetado no PlayerEntityRenderState: carrega a capa equipada ate o feature renderer. */
public interface AccessoryRenderHolder {
	CapeType wandryhp$getCape();

	void wandryhp$setCape(CapeType cape);
}
