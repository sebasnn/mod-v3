package dev.xmas.overlay.client;

import dev.xmas.overlay.Safe;
import dev.xmas.overlay.XmasConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.hit.HitResult;

/**
 * Transparent animated Christmas frame on the HUD while a block is being mined: coloured edge glow,
 * a string of twinkling lights, falling snow and holly. Plain rectangles only; guarded so that an error
 * switches the overlay off instead of crashing the game.
 */
public final class MiningOverlay implements HudElement {
	private float level = 0f;
	private long lastMs = System.currentTimeMillis();
	private static final int[] BULBS = { 0xFF3B47, 0x3DDC6B, 0xFFE066, 0x5AB8FF, 0xFFFFFF };

	@Override
	public void render(DrawContext ctx, RenderTickCounter tick) {
		if (!Safe.ok("overlay")) return;
		try { draw(ctx); } catch (Throwable t) { Safe.fail("overlay", t); }
	}

	/** True while you are mining: vanilla says so, or you hold the attack button while looking at a block. */
	public static boolean isMining(MinecraftClient mc) {
		if (mc.player == null || mc.interactionManager == null) return false;
		if (mc.interactionManager.isBreakingBlock()) return true;
		return mc.currentScreen == null && mc.options.attackKey.isPressed() && mc.crosshairTarget != null
				&& mc.crosshairTarget.getType() == HitResult.Type.BLOCK;
	}

	private void draw(DrawContext ctx) {
		MinecraftClient mc = MinecraftClient.getInstance();
		XmasConfig cfg = XmasConfig.get();
		long now = System.currentTimeMillis();
		float dt = Math.min(0.1f, (now - lastMs) / 1000f);
		lastMs = now;

		boolean mining = cfg.overlayEnabled && mc.player != null && mc.currentScreen == null && !mc.options.hudHidden
				&& isMining(mc);
		level += ((mining ? 1f : 0f) - level) * Math.min(1f, dt * (mining ? 7f : 4f));
		if (level < 0.02f) return;

		float a = level * cfg.overlayOpacity / 100f;
		int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
		if (w < 40 || h < 40) return;
		float t = now / 1000f;

		// coloured edge glow
		int edge = Math.max(18, h / 7);
		ctx.fillGradient(0, 0, w, edge, argb(a * 0.55f, 0xE8202A), argb(0f, 0xE8202A));
		ctx.fillGradient(0, h - edge, w, h, argb(0f, 0x1FA84F), argb(a * 0.55f, 0x1FA84F));
		int strips = 12, sw = Math.max(2, w / 70);
		for (int i = 0; i < strips; i++) {
			float f = 1f - i / (float) strips;
			int al = (int) (a * 0.20f * f * f * 255);
			if (al <= 0) continue;
			ctx.fill(i * sw, edge, (i + 1) * sw, h - edge, (al << 24) | 0xE8202A);
			ctx.fill(w - (i + 1) * sw, edge, w - i * sw, h - edge, (al << 24) | 0x1FA84F);
		}

		// string of lights
		int n = Math.max(10, w / 16);
		float step = w / (float) n;
		int prevX = 0, prevY = 4;
		for (int i = 0; i <= n; i++) {
			int x = (int) (i * step);
			float sag = (float) Math.sin((x / (float) w) * Math.PI * 4.0) * 0.5f + 0.5f;
			int y = 3 + (int) (sag * 9 + Math.sin(t * 1.3 + i * 0.7) * 1.2);
			ctx.fill(prevX, Math.min(prevY, y), Math.max(prevX + 1, x + 1), Math.max(prevY, y) + 1, argb(a * 0.8f, 0x16361F));
			prevX = x; prevY = y;
			if (i % 2 == 0) continue;
			float tw = 0.55f + 0.45f * (float) Math.sin(t * 4.5 + i * 1.9);
			int rgb = BULBS[(i + (int) (t * 1.5)) % BULBS.length];
			ctx.fill(x - 1, y + 1, x + 2, y + 5, argb(a * tw, rgb));
			ctx.fill(x - 2, y + 2, x + 3, y + 4, argb(a * tw * 0.35f, rgb));
		}

		// holly in the lower corners (blocks of 2x2)
		holly(ctx, 6, h - 6, 1, a, t);
		holly(ctx, w - 8, h - 6, -1, a, t);

		// falling snow
		for (int i = 0; i < 45; i++) {
			float r1 = hash(i * 12.9898f), r2 = hash(i * 78.233f), r3 = hash(i * 37.719f);
			float y = (t * (14f + r3 * 26f) + r2 * h) % (h + 6) - 3;
			float x = r1 * w + (float) Math.sin(t * (0.7f + r3) + i) * 9f;
			int size = r3 > 0.78f ? 2 : 1;
			float al = a * (0.45f + 0.55f * r2);
			int xi = (int) x, yi = (int) y;
			ctx.fill(xi, yi, xi + size, yi + size, argb(al, 0xFFFFFF));
			if (size == 2) {
				ctx.fill(xi - 1, yi, xi + 3, yi + 1, argb(al * 0.4f, 0xDDEEFF));
				ctx.fill(xi, yi - 1, xi + 1, yi + 3, argb(al * 0.4f, 0xDDEEFF));
			}
		}
	}

	private static void holly(DrawContext ctx, int ox, int oy, int dir, float a, float t) {
		for (int i = 0; i < 5; i++) {
			int len = 9 - i;
			for (int k = 0; k < len; k++) {
				int x = ox + dir * (k * 2 + i * 2);
				int y = oy - (int) (k * 2 * (0.25f + i * 0.22f)) - i;
				ctx.fill(x, y, x + 2, y + 2, argb(a * 0.85f, (k % 3 == 0) ? 0x0F6B30 : 0x1FA84F));
			}
		}
		float pulse = 0.8f + 0.2f * (float) Math.sin(t * 3f);
		for (int b = 0; b < 3; b++) {
			int bx = ox + dir * (4 + b * 3), by = oy - 6 - b * 2;
			ctx.fill(bx, by, bx + 3, by + 3, argb(a * pulse, 0xE8202A));
		}
	}

	private static float hash(float v) { float s = (float) Math.sin(v) * 43758.5453f; return s - (float) Math.floor(s); }

	private static int argb(float alpha, int rgb) {
		int al = Math.max(0, Math.min(255, (int) (alpha * 255f)));
		return (al << 24) | (rgb & 0xFFFFFF);
	}
}
