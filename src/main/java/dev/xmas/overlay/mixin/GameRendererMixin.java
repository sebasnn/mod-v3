package dev.xmas.overlay.mixin;

import dev.xmas.overlay.XmasConfig;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * "Zoom out": only the world gets the wider field of view. The hand/item pass is rebuilt with the ORIGINAL
 * field of view, so your hand and items stay exactly where they were.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
	private static float xmas$origWorld = -1f, xmas$origHand = -1f;

	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void xmas$zoomOut(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Float> cir) {
		float orig = cir.getReturnValue();
		if (changingFov) xmas$origWorld = orig; else xmas$origHand = orig;
		if (!changingFov) return;
		int z = XmasConfig.get().zoomOut;
		if (z >= 100) return;
		double out = 2.0 * Math.toDegrees(Math.atan(Math.tan(Math.toRadians(orig) / 2.0) / (z / 100.0)));
		cir.setReturnValue((float) Math.min(170.0, out));
	}

	@ModifyVariable(method = "renderHand", at = @At("HEAD"), argsOnly = true)
	private Matrix4f xmas$handProjection(Matrix4f original) {
		if (XmasConfig.get().zoomOut >= 100) return original;
		float f = xmas$origHand > 0 ? xmas$origHand : xmas$origWorld;
		if (f <= 0) return original;
		try {
			return ((GameRendererInvoker) (Object) this).xmas$projection(f);
		} catch (Throwable t) {
			return original;
		}
	}
}
