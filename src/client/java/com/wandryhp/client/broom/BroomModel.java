package com.wandryhp.client.broom;

import com.wandryhp.edwiges.model.BbModel;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/** Le o witch_broom.bbmodel original (sem converter nada) uma unica vez. */
final class BroomModel {
	private static final String PATH = "/assets/wandryhp/bbmodels/broom/witch_broom.bbmodel";
	private static BbModel model;
	private static boolean loaded;

	private BroomModel() {}

	/** O modelo, ou null se nao puder ser lido. */
	static synchronized BbModel get() {
		if (!loaded) {
			loaded = true;
			try (InputStream in = BroomModel.class.getResourceAsStream(PATH)) {
				if (in != null) {
					try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
						model = BbModel.parse(reader);
					}
				} else {
					System.err.println("[wandryhp] bbmodel nao encontrado: " + PATH);
				}
			} catch (Exception e) {
				System.err.println("[wandryhp] Falha ao ler " + PATH + ": " + e);
			}
		}
		return model;
	}
}
