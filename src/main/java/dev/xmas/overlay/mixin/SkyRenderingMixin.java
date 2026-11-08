package dev.xmas.overlay.mixin;

import dev.xmas.overlay.Safe;
import dev.xmas.overlay.XmasConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.world.MoonPhase;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Galaxy night sky. At night, just before the sun/moon/stars are drawn, the End-style sky box is drawn
 * (its texture is replaced by the galaxy picture of the built-in "Galaxy night sky" pack). The moon and
 * the stars then draw on top of it, and the day sky is not touched.
 */
@Mixin(SkyRendering.class)
public abstract class SkyRenderingMixin {
	@Shadow protected abstract void renderEndSky();

	private static boolean xmas$told;

	@Inject(method = "renderCelestialBodies", at = @At("HEAD"))
	private void xmas$galaxy(MatrixStack matrices, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase,
			float alpha, float starBrightness, CallbackInfo ci) {
		XmasConfig cfg = XmasConfig.get();
		if (!cfg.galaxySky || !Safe.ok("sky") || starBrightness < 0.08f) return;
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.world == null || mc.world.getRegistryKey() != World.OVERWORLD) return;
		try {
			renderEndSky();
			if (!xmas$told && mc.player != null) {
				xmas$told = true;
				mc.player.sendMessage(Text.literal("[Christmas Overlay] Galaxy sky is drawing (night). If you still see the normal sky, tell me."), false);
			}
		} catch (Throwable t) {
			Safe.fail("sky", t);
		}
	}

	@Inject(method = "updateRenderState", at = @At("TAIL"))
	private void xmas$stars(ClientWorld world, float tickProgress, Camera camera, SkyRenderState state, CallbackInfo ci) {
		XmasConfig cfg = XmasConfig.get();
		if (!cfg.galaxySky || !Safe.ok("sky") || world.getRegistryKey() != World.OVERWORLD) return;
		try {
			SkyRenderStateAccessor acc = (SkyRenderStateAccessor) state;
			float stars = acc.xmas$getStarBrightness();
			if (stars > 0f) acc.xmas$setStarBrightness(Math.min(1f, stars * (cfg.starBoost / 100f)));
		} catch (Throwable t) {
			Safe.fail("sky", t);
		}
	}
}
