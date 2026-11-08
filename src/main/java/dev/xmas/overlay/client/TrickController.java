package dev.xmas.overlay.client;

import dev.xmas.overlay.XmasConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;

/**
 * Knife trick state machine. While the trick key is HELD (and the knife is in your main hand) it plays a
 * loop of butterfly-knife tricks; when you let go it finishes the current move quickly and stops.
 */
public final class TrickController {
	/** Extra motion added on top of the idle grip. Angles in degrees, offsets in view units. */
	public static final class Pose {
		public float dx, dy, dz, rx, ry, rz, close;   // close: 0 = handles open, 1 = handles folded over the blade
		void clear() { dx = dy = dz = rx = ry = rz = close = 0; }
	}

	private interface Move { void eval(double u, Pose p); }
	private record Def(String name, double ms, Move move) {}

	private static double ease(double t) { t = Math.max(0, Math.min(1, t)); return t * t * (3 - 2 * t); }
	private static double easeInOut(double t) { t = Math.max(0, Math.min(1, t)); return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2; }
	private static double backOut(double t) { t = Math.max(0, Math.min(1, t)); double c1 = 1.9, c3 = c1 + 1; return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2); }
	private static double arc(double u) { return 4 * u * (1 - u); }

	private static final Def[] MOVES = {
			// handles snap open and shut twice
			new Def("Fan", 1500, (u, p) -> {
				double k = (u * 2) % 1.0;
				double c = k < 0.5 ? backOut(k * 2) : 1 - backOut((k - 0.5) * 2);
				p.close = (float) Math.max(0, Math.min(1.06, c));
				p.rz = (float) (9 * Math.sin(u * Math.PI * 4));
				p.dy = (float) (0.02 * Math.sin(u * Math.PI * 4));
			}),
			// knife rolls two times around its own blade axis
			new Def("Rollover", 1700, (u, p) -> {
				p.ry = (float) (720 * easeInOut(u));
				p.rx = (float) (10 * Math.sin(u * Math.PI * 2));
				p.rz = (float) (14 * Math.sin(u * Math.PI));
			}),
			// toss up, double front flip, handles fold mid-air, catch
			new Def("Aerial", 1900, (u, p) -> {
				double a = arc(u);
				p.dy = (float) (0.34 * a);
				p.dz = (float) (0.10 * a);
				p.rx = (float) (-720 * easeInOut(u));
				p.close = (float) ease(0.5 - 0.5 * Math.cos(u * Math.PI * 2));
			}),
			// spins flat in front of you like a propeller, handles pumping
			new Def("Twirl", 2200, (u, p) -> {
				p.rz = (float) (720 * easeInOut(u));
				p.close = (float) (0.5 + 0.5 * Math.sin(u * Math.PI * 6 - Math.PI / 2));
				p.ry = (float) (22 * Math.sin(u * Math.PI * 4));
				p.dz = (float) (0.05 * arc(u));
			}),
			// quick half-turns with the handles flipping every time
			new Def("Chaplin", 1800, (u, p) -> {
				p.ry = (float) (360 * easeInOut(u));
				p.close = (float) Math.abs(Math.sin(u * Math.PI * 4));
				p.rx = (float) (18 * Math.sin(u * Math.PI * 2));
				p.dy = (float) (0.05 * Math.sin(u * Math.PI * 4));
			}),
			// slow cartwheel with a wrist roll
			new Def("Zen roll", 2000, (u, p) -> {
				p.rx = (float) (360 * easeInOut(u));
				p.ry = (float) (180 * Math.sin(u * Math.PI * 2));
				p.close = (float) (0.35 * Math.sin(u * Math.PI) * Math.sin(u * Math.PI));
				p.dz = (float) (0.06 * arc(u));
			}),
	};

	private static final Pose POSE = new Pose();
	private static boolean running;
	private static int index = -1;
	private static double progress;
	private static long lastMs = -1;
	private static boolean lastHeld;
	private static String lastName = "";

	private TrickController() {}

	public static Pose pose() { return POSE; }
	public static boolean isRunning() { return running; }
	public static String currentName() { return running ? MOVES[Math.max(0, index)].name() : ""; }

	/** True when a totem (the knife) is in the main hand OR the off hand. */
	public static boolean holdingKnife(MinecraftClient mc) {
		ClientPlayerEntity p = mc.player;
		return p != null && XmasConfig.get().customKnife
				&& (p.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING) || p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING));
	}

	/** The hand the effects follow: the main hand if it holds the knife, otherwise the off hand. */
	public static Hand activeHand(ClientPlayerEntity p) {
		return p.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING) ? Hand.MAIN_HAND : Hand.OFF_HAND;
	}

	/** +1 for a knife on the right side of the screen, -1 on the left (the off hand is the mirror image). */
	public static float sideOf(ClientPlayerEntity p, Hand hand) {
		Arm arm = hand == Hand.MAIN_HAND ? p.getMainArm() : p.getMainArm().getOpposite();
		return arm == Arm.RIGHT ? 1f : -1f;
	}

	/** Advance the state machine to 'now' (safe to call several times per frame). */
	public static void update(long nowMs, boolean keyDown) {
		MinecraftClient mc = MinecraftClient.getInstance();
		boolean holding = holdingKnife(mc) && XmasConfig.get().tricksEnabled && mc.currentScreen == null;
		boolean want = (keyDown || XmasConfig.get().autoTricks) && holding;
		double dt = lastMs < 0 ? 0 : Math.min(100, nowMs - lastMs);
		if (nowMs == lastMs) return;
		lastMs = nowMs;

		if (!holding) { running = false; progress = 0; }
		if (want && !running) {
			running = true; progress = 0;
			index = (index + 1) % MOVES.length;
		}
		if (running) {
			double speed = want ? 1.0 : 3.2;           // let go of the key -> wrap up the move fast
			progress += dt * speed / MOVES[index].ms();
			if (progress >= 1) {
				progress = 0;
				if (want) index = (index + 1) % MOVES.length; else running = false;
			}
		}
		POSE.clear();
		if (running) MOVES[index].move().eval(progress, POSE);
		lastHeld = want;
	}

	public static void reset() { running = false; progress = 0; lastMs = -1; POSE.clear(); }

	// ------------------------------------------------------------------ transform chain
	public interface Ops {
		void translate(float x, float y, float z);
		void rotX(float deg);
		void rotY(float deg);
		void rotZ(float deg);
		void scale(float s);
	}

	public static final float REST_X = 0.56f, REST_Y = -0.52f, REST_Z = -0.72f;     // vanilla item rest position
	private static final float GRIP_X = -0.10f, GRIP_Y = 0.17f, GRIP_Z = 0.05f;      // where the hand holds the knife
	private static final float IDLE_RX = -32f, IDLE_RZ = 14f;                         // idle tilt: blade up, leaning forward + inward
	public static final float BASE_SIZE = 0.42f;
	public static final float GRIP_LOCAL_Y = 4.5f / 16f;                               // middle of the handles, in blocks

	/** The same chain is replayed on a MatrixStack for drawing and on a JOML matrix for particle positions. */
	public static void apply(Ops o, float side, Pose p, long nowMs) {
		XmasConfig cfg = XmasConfig.get();
		float sway = (float) Math.sin(nowMs / 900.0) * (p.close == 0 && !running ? 1.4f : 0.4f);
		o.translate(side * (GRIP_X + cfg.gripX / 100f + p.dx), GRIP_Y + cfg.gripY / 100f + p.dy, GRIP_Z + cfg.gripZ / 100f + p.dz);
		o.rotZ(side * p.rz);
		o.rotX(p.rx);
		o.rotZ(side * IDLE_RZ + sway);
		o.rotX(IDLE_RX);
		o.rotY(side * p.ry);
		o.scale(BASE_SIZE * cfg.knifeSize / 100f * cfg.itemSize / 100f);
		o.translate(0, GRIP_LOCAL_Y, 0);
	}
}
