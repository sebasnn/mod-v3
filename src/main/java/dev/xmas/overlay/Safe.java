package dev.xmas.overlay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Set;

/**
 * Crash guard: any exception in one of the mod's features turns only that feature off and tells you in
 * chat what went wrong, instead of crashing the game.
 */
public final class Safe {
	private static final Set<String> DISABLED = new HashSet<>();

	private Safe() {}

	public static boolean ok(String feature) { return !DISABLED.contains(feature); }

	public static void enableAll() { DISABLED.clear(); }

	public static void fail(String feature, Throwable t) {
		if (!DISABLED.add(feature)) return;
		System.err.println("[xmasoverlay] feature '" + feature + "' failed and was switched off:");
		t.printStackTrace();
		try {
			MinecraftClient mc = MinecraftClient.getInstance();
			if (mc != null && mc.player != null) {
				mc.player.sendMessage(Text.literal("[Christmas Overlay] '" + feature + "' hit an error and was switched off: "
						+ t.getClass().getSimpleName() + ": " + t.getMessage()), false);
			}
		} catch (Throwable ignored) { }
	}

	public static void run(String feature, Runnable r) {
		if (DISABLED.contains(feature)) return;
		try { r.run(); } catch (Throwable t) { fail(feature, t); }
	}
}
