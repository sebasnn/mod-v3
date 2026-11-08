package dev.xmas.overlay.client;

import dev.xmas.overlay.Safe;
import dev.xmas.overlay.XmasConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class XmasOverlayClient implements ClientModInitializer {
	public static final String MOD_ID = "xmasoverlay";

	public static KeyBinding trickKey;
	public static KeyBinding configKey;
	private static long lastWarn;

	@Override
	public void onInitializeClient() {
		XmasConfig.load();

		KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
		trickKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.xmasoverlay.trick", GLFW.GLFW_KEY_G, category));
		configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.xmasoverlay.config", GLFW.GLFW_KEY_J, category));

		HudElementRegistry.attachElementBefore(VanillaHudElements.MISC_OVERLAYS,
				Identifier.of(MOD_ID, "mining_overlay"), new MiningOverlay());

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			try {
				while (configKey.wasPressed()) {
					if (mc.currentScreen == null) mc.setScreen(new ConfigScreen(null));
				}
				while (trickKey.wasPressed()) {
					if (mc.player == null || mc.currentScreen != null) continue;
					if (!TrickController.holdingKnife(mc)) {
						long now = System.currentTimeMillis();
						if (now - lastWarn > 1500) {
							lastWarn = now;
							mc.player.sendMessage(Text.literal("You can't do that - hold the knife in your hand to do tricks!"), false);
						}
					}
				}
			} catch (Throwable t) {
				Safe.fail("keys", t);
			}
			ChristmasParticles.tick(mc);
		});

		// Built-in pack, switched on by default: the galaxy sky texture.
		FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(container -> {
			ResourceLoader.registerBuiltinPack(Identifier.of(MOD_ID, "xmas_galaxy_sky"), container,
					Text.translatable("xmasoverlay.pack.sky"), PackActivationType.DEFAULT_ENABLED);
		});
	}
}
