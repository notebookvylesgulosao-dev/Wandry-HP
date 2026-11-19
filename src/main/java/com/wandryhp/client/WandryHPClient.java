package com.wandryhp.client;

import com.wandryhp.BaseAttackPayload;
import com.wandryhp.BroomEntity;
import com.wandryhp.BroomInputPayload;
import com.wandryhp.MagicWandItem;
import com.wandryhp.WandryHP;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class WandryHPClient implements ClientModInitializer {
    private static KeyBinding wheelKey;
    private static int lastBroomFlags;

    @Override public void onInitializeClient() {
        wheelKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.wandry_hp.spell_wheel", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.wandry_hp"));

        EntityRendererRegistry.register(WandryHP.SPELL_PROJECTILE, SpellProjectileRenderer::new);
        EntityRendererRegistry.register(WandryHP.BROOM, BroomRenderer::new);

        // botão esquerdo com varinha = ataque base (no lugar de quebrar bloco/bater)
        ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
            if (client.currentScreen == null && player.getMainHandStack().getItem() instanceof MagicWandItem) {
                ClientPlayNetworking.send(new BaseAttackPayload());
                return true;
            }
            return false;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (wheelKey.wasPressed()) if (client.currentScreen == null) client.setScreen(new SpellWheelScreen());

            // controles da vassoura: W/A/S/D, Espaço sobe, Ctrl (tecla de correr) desce
            int flags = 0;
            if (client.player != null && client.player.getVehicle() instanceof BroomEntity) {
                GameOptions o = client.options;
                if (o.forwardKey.isPressed()) flags |= BroomEntity.FORWARD;
                if (o.backKey.isPressed()) flags |= BroomEntity.BACK;
                if (o.leftKey.isPressed()) flags |= BroomEntity.LEFT;
                if (o.rightKey.isPressed()) flags |= BroomEntity.RIGHT;
                if (o.jumpKey.isPressed()) flags |= BroomEntity.UP;
                if (o.sprintKey.isPressed()) flags |= BroomEntity.DOWN;
            }
            if (flags != lastBroomFlags) {
                lastBroomFlags = flags;
                if (client.player != null) ClientPlayNetworking.send(new BroomInputPayload(flags));
            }
        });
    }
}
