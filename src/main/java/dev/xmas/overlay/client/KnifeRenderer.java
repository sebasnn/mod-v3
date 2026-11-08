package dev.xmas.overlay.client;

import dev.xmas.overlay.Skin;
import dev.xmas.overlay.XmasConfig;
import dev.xmas.overlay.mixin.HeldItemRendererInvoker;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Draws the first-person butterfly knife (blade + two handles) together with your hand. */
public final class KnifeRenderer {
	private KnifeRenderer() {}

	private static final float PIVOT_SHIFT = 0.25f;   // model pivot sits 4 units above the model centre

	private static final class StackOps implements TrickController.Ops {
		final MatrixStack m;
		StackOps(MatrixStack m) { this.m = m; }
		public void translate(float x, float y, float z) { m.translate(x, y, z); }
		public void rotX(float d) { m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(d)); }
		public void rotY(float d) { m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(d)); }
		public void rotZ(float d) { m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(d)); }
		public void scale(float s) { m.scale(s, s, s); }
	}

	private static final class JomlOps implements TrickController.Ops {
		final Matrix4f m = new Matrix4f();
		public void translate(float x, float y, float z) { m.translate(x, y, z); }
		public void rotX(float d) { m.rotateX((float) Math.toRadians(d)); }
		public void rotY(float d) { m.rotateY((float) Math.toRadians(d)); }
		public void rotZ(float d) { m.rotateZ((float) Math.toRadians(d)); }
		public void scale(float s) { m.scale(s); }
	}

	public static void render(HeldItemRenderer hir, AbstractClientPlayerEntity player, Hand hand, float swing, float equip,
			MatrixStack m, OrderedRenderCommandQueue q, int light) {
		XmasConfig cfg = XmasConfig.get();
		HeldItemRendererInvoker inv = (HeldItemRendererInvoker) hir;
		Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
		float side = arm == Arm.RIGHT ? 1f : -1f;
		long now = System.currentTimeMillis();
		TrickController.update(now, XmasOverlayClient.trickKey != null && XmasOverlayClient.trickKey.isPressed());
		TrickController.Pose pose = TrickController.pose();
		Skin skin = Skin.current();

		if (cfg.showHand && !player.isInvisible()) {
			m.push();
			try {
				// the hand follows the knife: it moves and turns a bit with every trick
				float f = TrickController.isRunning() ? 1f : 0f;
				float wob = (float) Math.sin(now / 130.0) * 0.012f * f;
				m.translate(side * pose.dx * 0.55f, pose.dy * 0.55f + wob, pose.dz * 0.55f);
				m.translate(side * TrickController.REST_X, TrickController.REST_Y, TrickController.REST_Z);
				m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pose.rx * 0.12f));
				m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * pose.ry * 0.10f));
				m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * (pose.rz * 0.16f + (pose.close - 0.5f) * 10f * f)));
				m.translate(-side * TrickController.REST_X, -TrickController.REST_Y, -TrickController.REST_Z);
				inv.xmas$renderArmHoldingItem(m, q, light, equip, swing, arm);
			} finally { m.pop(); }
		}

		m.push();
		try {
			inv.xmas$applyEquipOffset(m, arm, equip);
			inv.xmas$applySwingOffset(m, arm, swing);
			if (cfg.itemShiftEnabled) {
				float d = cfg.itemShift / 100f;
				m.translate(-side * 0.45f * d, 0.30f * d, -0.25f * d);
			}
			TrickController.apply(new StackOps(m), side, pose, now);
			ItemDisplayContext ctx = arm == Arm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
			float angle = 180f * pose.close;
			// blade + pins
			m.push();
			m.translate(0, -PIVOT_SHIFT, 0);
			inv.xmas$renderItem(player, skin.part(0), ctx, m, q, light);
			m.pop();
			// handle A swings round one side, handle B round the other
			for (int i = 1; i <= 2; i++) {
				m.push();
				m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i == 1 ? angle : -angle));
				m.translate(0, -PIVOT_SHIFT, 0);
				inv.xmas$renderItem(player, skin.part(i), ctx, m, q, light);
				m.pop();
			}
		} finally {
			m.pop();
		}
	}

	/** World position of a point of the knife, given in model units relative to the hinge (y up the blade). */
	public static Vec3d worldPoint(AbstractClientPlayerEntity player, float ux, float uy, float uz) {
		XmasConfig cfg = XmasConfig.get();
		float side = player instanceof net.minecraft.client.network.ClientPlayerEntity cp ? TrickController.sideOf(cp, TrickController.activeHand(cp)) : 1f;
		long now = System.currentTimeMillis();
		JomlOps ops = new JomlOps();
		ops.m.translate(side * TrickController.REST_X, TrickController.REST_Y, TrickController.REST_Z);
		if (cfg.itemShiftEnabled) {
			float d = cfg.itemShift / 100f;
			ops.m.translate(-side * 0.45f * d, 0.30f * d, -0.25f * d);
		}
		TrickController.apply(ops, side, TrickController.pose(), now);
		Vector3f v = new Vector3f(ux / 16f, uy / 16f, uz / 16f);
		ops.m.transformPosition(v);
		Vec3d eye = player.getEyePos();
		Vec3d fwd = player.getRotationVec(1f);
		Vec3d right = fwd.crossProduct(new Vec3d(0, 1, 0));
		if (right.lengthSquared() < 1e-6) right = new Vec3d(1, 0, 0);
		right = right.normalize();
		Vec3d up = right.crossProduct(fwd).normalize();
		return eye.add(right.multiply(v.x)).add(up.multiply(v.y)).add(fwd.multiply(-v.z));
	}
}
