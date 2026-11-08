package dev.xmas.overlay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** All user settings. Saved to config/xmasoverlay.json. */
public final class XmasConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("xmasoverlay.json");
	private static XmasConfig instance = new XmasConfig();

	// ---- knife ----
	public boolean customKnife = true;        // use the 3D butterfly knife + skins for the totem item
	public String skin = "candy_cane";
	public boolean tricksEnabled = true;      // hold the trick key to do tricks
	public boolean autoTricks = false;        // do tricks by themselves, no key needed
	public boolean showHand = true;           // draw your hand when holding the knife
	public int knifeSize = 100;               // 50..200 %
	public int gripX = 0, gripY = 0, gripZ = 0;   // -50..50 : nudge the knife inside your hand
	// ---- effects ----
	public boolean overlayEnabled = true;
	public int overlayOpacity = 40;           // 5..100
	public boolean miningParticles = true;
	public int particleDensity = 5;           // 1..10
	public boolean knifeParticles = true;
	public boolean galaxySky = true;
	public int starBoost = 200;               // 100..300 %
	// ---- camera / hands ----
	public int zoomOut = 100;                 // 40..100 %  (100 = vanilla, 50 = like a 0.5x ultra-wide lens)
	public boolean itemShiftEnabled = false;  // push every held item towards the screen centre
	public int itemShift = 30;                // 0..100 %
	public int itemSize = 100;                // 1..200 %  size of the item in your hand (100 = normal)
	public int swingSpeed = 100;              // 1..100 %  (100 = vanilla) visual only, only for you

	public static XmasConfig get() { return instance; }

	public static void load() {
		try {
			if (Files.exists(FILE)) {
				XmasConfig c = GSON.fromJson(Files.readString(FILE), XmasConfig.class);
				if (c != null) instance = c;
			}
		} catch (Exception e) {
			System.err.println("[xmasoverlay] could not read config, using defaults: " + e);
		}
		instance.clamp();
		save();
	}

	public static void save() {
		try {
			instance.clamp();
			Files.writeString(FILE, GSON.toJson(instance));
		} catch (IOException e) {
			System.err.println("[xmasoverlay] could not save config: " + e);
		}
	}

	public static void reset() { instance = new XmasConfig(); save(); }

	private static int c(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

	private void clamp() {
		overlayOpacity = c(overlayOpacity, 5, 100);
		particleDensity = c(particleDensity, 1, 10);
		starBoost = c(starBoost, 100, 300);
		zoomOut = c(zoomOut, 40, 100);
		itemShift = c(itemShift, 0, 100);
		swingSpeed = c(swingSpeed, 1, 100);
		itemSize = c(itemSize, 1, 200);
		knifeSize = c(knifeSize, 50, 200);
		gripX = c(gripX, -50, 50); gripY = c(gripY, -50, 50); gripZ = c(gripZ, -50, 50);
		if (skin == null) skin = "candy_cane";
	}
}
