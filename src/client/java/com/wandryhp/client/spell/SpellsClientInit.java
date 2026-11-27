package com.wandryhp.client.spell;

import com.wandryhp.WandryHP;
import com.wandryhp.item.WandItem;
import com.wandryhp.spell.CastSpellPayload;
import com.wandryhp.spell.ModSpellEntities;
import com.wandryhp.spell.Spell;
import com.wandryhp.spell.SpellUnlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

/**
 * Entrypoint cliente dos feitiços (registrado em fabric.mod.json depois de WandryHPClient):
 * renderizador do projétil, modelos 3D, tecla G da roda e botão direito para lançar.
 */
public class SpellsClientInit implements ClientModInitializer {
	/** Tecla G: abre e fecha a roda de feitiços. */
	public static KeyBinding WHEEL_KEY;

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModSpellEntities.SPELL_PROJECTILE, SpellProjectileRenderer::new);
		EntityRendererRegistry.register(ModSpellEntities.PATRONUS, PatronusRenderer::new);

		// Carrega a geometria dos projéteis direto dos .bbmodel originais.
		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
				new SimpleSynchronousResourceReloadListener() {
					@Override
					public Identifier getFabricId() {
						return Identifier.of(WandryHP.MOD_ID, "spell_models");
					}

					@Override
					public void reload(ResourceManager manager) {
						SpellModels.reload(manager);
					}
				});

		WHEEL_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.wandryhp.spell_wheel", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.wandryhp"));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (WHEEL_KEY.wasPressed()) {
				if (client.player != null && client.currentScreen == null) {
					client.setScreen(new SpellWheelScreen());
				}
			}
		});

		// Botão direito com uma varinha na mão principal e um feitiço ativo: lança o feitiço (o servidor valida
		// a recarga) e cancela o uso vanilla. Os três eventos cobrem clicar no ar, em bloco e em entidade.
		// Com a Confusão ativa o KeyBindingMixin já troca os botões, então a inversão vale aqui também.
		UseItemCallback.EVENT.register((player, world, hand) -> tryCast(player, world, hand));
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> tryCast(player, world, hand));
		UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> tryCast(player, world, hand));
	}

	private static ActionResult tryCast(PlayerEntity player, World world, Hand hand) {
		if (!world.isClient || hand != Hand.MAIN_HAND) {
			return ActionResult.PASS;
		}
		if (!(player.getMainHandStack().getItem() instanceof WandItem)) {
			return ActionResult.PASS;
		}
		Spell spell = SpellClientState.selected();
		if (spell == null || !SpellUnlocks.isUnlocked(player, spell)) {
			return ActionResult.PASS;
		}
		ClientPlayNetworking.send(new CastSpellPayload(spell.ordinal()));
		return ActionResult.FAIL;
	}
}
