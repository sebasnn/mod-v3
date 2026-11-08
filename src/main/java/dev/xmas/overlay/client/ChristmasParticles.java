package dev.xmas.overlay.client;

import dev.xmas.overlay.Safe;
import dev.xmas.overlay.Skin;
import dev.xmas.overlay.XmasConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.Arm;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/**
 * Particles for mining and for the knife. Everything uses the colours/types of the selected skin:
 * a shower from the top of the block, a spinning helix, sparks on the face you hit, and a big
 * starburst + ground ring + fountain when the block breaks. The knife gets orbiting glints, and a
 * glowing trail behind the blade tip while you do tricks.
 */
public final class ChristmasParticles {
	private static final Random RNG = Random.create();
	private static boolean wasBreaking = false;
	private static BlockPos lastPos = null;
	private static Vec3d lastTip = null;
	private static Vec3d lastEndA = null, lastEndB = null;

	private ChristmasParticles() {}

	private static ParticleEffect pick(ParticleEffect[] a) { return a[RNG.nextInt(a.length)]; }
	private static double rnd() { return RNG.nextDouble() - 0.5; }

	private static void spawn(MinecraftClient mc, ParticleEffect e, double x, double y, double z, double vx, double vy, double vz) {
		mc.particleManager.addParticle(e, x, y, z, vx, vy, vz);
	}

	public static void tick(MinecraftClient mc) {
		Safe.run("particles", () -> doTick(mc));
	}

	private static void doTick(MinecraftClient mc) {
		ClientWorld world = mc.world;
		ClientPlayerEntity player = mc.player;
		if (world == null || player == null || mc.interactionManager == null) { wasBreaking = false; lastTip = null; return; }
		XmasConfig cfg = XmasConfig.get();
		Skin skin = Skin.current();
		int d = cfg.particleDensity;

		// ------------------------------------------------------------ mining
		boolean breaking = MiningOverlay.isMining(mc);
		if (cfg.miningParticles) {
			if (breaking && mc.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
				lastPos = hit.getBlockPos();
				Vec3d c = Vec3d.ofCenter(lastPos);
				long t = world.getTime();
				// 1) shower falling from the top of the block
				int shower = 1 + d / 2;
				for (int i = 0; i < shower; i++)
					spawn(mc, pick(skin.ambient), c.x + rnd() * 0.9, c.y + 0.55 + RNG.nextDouble() * 0.25, c.z + rnd() * 0.9, rnd() * 0.01, -0.02 - RNG.nextDouble() * 0.02, rnd() * 0.01);
				// 2) helix spinning around the block
				int strands = d >= 8 ? 3 : d >= 4 ? 2 : 1;
				for (int k = 0; k < strands; k++) {
					double ang = t * 0.42 + k * (Math.PI * 2 / strands);
					double h = ((t * 0.045 + k / (double) strands) % 1.0) * 1.3 - 0.65;
					double r = 0.85;
					spawn(mc, pick(skin.burst), c.x + Math.cos(ang) * r, c.y + h, c.z + Math.sin(ang) * r,
							-Math.sin(ang) * 0.025, 0.012, Math.cos(ang) * 0.025);
				}
				// 3) sparks flying off the face you are hitting
				Direction side = hit.getSide();
				Vec3d p = hit.getPos();
				int sparks = 1 + d / 3;
				for (int i = 0; i < sparks; i++) {
					double sx = side.getAxis() == Direction.Axis.X ? 0 : rnd() * 0.9;
					double sy = side.getAxis() == Direction.Axis.Y ? 0 : rnd() * 0.9;
					double sz = side.getAxis() == Direction.Axis.Z ? 0 : rnd() * 0.9;
					spawn(mc, pick(skin.ambient), p.x + side.getOffsetX() * 0.03 + sx, p.y + side.getOffsetY() * 0.03 + sy, p.z + side.getOffsetZ() * 0.03 + sz,
							side.getOffsetX() * 0.06 + rnd() * 0.03, 0.03 + side.getOffsetY() * 0.06 + RNG.nextDouble() * 0.03, side.getOffsetZ() * 0.06 + rnd() * 0.03);
				}
			}
			if (wasBreaking && !breaking && lastPos != null && world.getBlockState(lastPos).isAir()) breakBurst(mc, skin, d);
		}
		wasBreaking = breaking;

		// ------------------------------------------------------------ knife
		boolean holding = player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING);
		boolean offhand = !holding && player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
		if (!cfg.knifeParticles || (!holding && !offhand)) { lastTip = null; return; }
		boolean custom = cfg.customKnife && mc.options.getPerspective().isFirstPerson();
		long t = world.getTime();
		float chance = 0.25f + 0.05f * d;

		if (custom) {
			TrickController.update(System.currentTimeMillis(), XmasOverlayClient.trickKey != null && XmasOverlayClient.trickKey.isPressed());
			// idle: glints along the blade and two sparks orbiting it
			if (RNG.nextFloat() < chance) {
				float y = -8f + RNG.nextFloat() * 21f;
				Vec3d w = KnifeRenderer.worldPoint(player, (float) rnd() * 3f, y, (float) rnd() * 2f);
				spawn(mc, pick(skin.ambient), w.x, w.y, w.z, rnd() * 0.01, 0.006 + RNG.nextDouble() * 0.01, rnd() * 0.01);
			}
			for (int k = 0; k < 2; k++) {
				double ang = t * 0.5 + k * Math.PI;
				Vec3d w = KnifeRenderer.worldPoint(player, (float) Math.cos(ang) * 3.6f, 4f + (float) Math.sin(t * 0.13 + k) * 6f, (float) Math.sin(ang) * 3.6f);
				if (RNG.nextFloat() < 0.5f) spawn(mc, pick(skin.burst), w.x, w.y, w.z, 0, 0.004, 0);
			}
			// trick: trail behind the blade tip, sparks from the swinging handles
			if (TrickController.isRunning()) {
				Vec3d tip = KnifeRenderer.worldPoint(player, 0f, 13.5f, 0f);
				if (lastTip != null && lastTip.squaredDistanceTo(tip) < 9) {
					int steps = 3 + d / 3;
					for (int i = 0; i < steps; i++) {
						double f = i / (double) steps;
						Vec3d q = lastTip.lerp(tip, f);
						spawn(mc, pick(skin.burst), q.x, q.y, q.z, rnd() * 0.01, rnd() * 0.01, rnd() * 0.01);
					}
				}
				lastTip = tip;
				double th = Math.PI * TrickController.pose().close;
				Vec3d ea = KnifeRenderer.worldPoint(player, (float) Math.sin(th) * 10f, -(float) Math.cos(th) * 10f, 1.7f);
				Vec3d eb = KnifeRenderer.worldPoint(player, -(float) Math.sin(th) * 10f, -(float) Math.cos(th) * 10f, -1.7f);
				if (lastEndA != null && lastEndA.squaredDistanceTo(ea) > 0.0004 && RNG.nextFloat() < 0.7f) {
					spawn(mc, pick(skin.ambient), ea.x, ea.y, ea.z, rnd() * 0.02, rnd() * 0.02, rnd() * 0.02);
					spawn(mc, pick(skin.ambient), eb.x, eb.y, eb.z, rnd() * 0.02, rnd() * 0.02, rnd() * 0.02);
				}
				lastEndA = ea; lastEndB = eb;
			} else { lastTip = null; lastEndA = null; lastEndB = null; }
		} else if (RNG.nextFloat() < chance) {
			// fallback: sparkles near the hand
			Arm arm = holding ? player.getMainArm() : player.getMainArm().getOpposite();
			double side = arm == Arm.RIGHT ? 1 : -1;
			Vec3d eye = player.getEyePos();
			Vec3d fwd = player.getRotationVec(1f);
			Vec3d right = fwd.crossProduct(new Vec3d(0, 1, 0));
			if (right.lengthSquared() < 1e-4) right = new Vec3d(1, 0, 0);
			right = right.normalize();
			Vec3d up = right.crossProduct(fwd).normalize();
			Vec3d hand = eye.add(fwd.multiply(0.7)).add(right.multiply(0.32 * side)).subtract(up.multiply(0.24));
			spawn(mc, pick(skin.ambient), hand.x + rnd() * 0.2, hand.y + rnd() * 0.25, hand.z + rnd() * 0.2, 0, 0.012, 0);
		}
	}

	private static void breakBurst(MinecraftClient mc, Skin skin, int d) {
		Vec3d c = Vec3d.ofCenter(lastPos);
		// starburst: points evenly spread on a sphere (golden-angle spiral)
		int n = 16 + d * 8;
		for (int i = 0; i < n; i++) {
			double y = 1 - 2 * (i + 0.5) / n;
			double r = Math.sqrt(1 - y * y);
			double phi = i * 2.399963;
			double dx = Math.cos(phi) * r, dz = Math.sin(phi) * r;
			double speed = 0.10 + RNG.nextDouble() * 0.18;
			spawn(mc, pick(skin.burst), c.x + dx * 0.3, c.y + y * 0.3, c.z + dz * 0.3, dx * speed, y * speed + 0.04, dz * speed);
		}
		// ring along the ground
		int ring = 12 + d * 2;
		for (int i = 0; i < ring; i++) {
			double a = i * Math.PI * 2 / ring;
			spawn(mc, pick(skin.ambient), c.x + Math.cos(a) * 0.5, c.y - 0.45, c.z + Math.sin(a) * 0.5, Math.cos(a) * 0.14, 0.02, Math.sin(a) * 0.14);
		}
		// slow fountain going up
		for (int i = 0; i < d * 2; i++)
			spawn(mc, pick(skin.burst), c.x + rnd() * 0.5, c.y, c.z + rnd() * 0.5, rnd() * 0.05, 0.16 + RNG.nextDouble() * 0.16, rnd() * 0.05);
	}
}
