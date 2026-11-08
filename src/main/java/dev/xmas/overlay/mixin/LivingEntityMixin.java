package dev.xmas.overlay.mixin;

import dev.xmas.overlay.XmasConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Visual-only swing speed for the local player (hit, mine, use, place). Cooldowns and the server are untouched. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true)
	private void xmas$swing(CallbackInfoReturnable<Integer> cir) {
		int sp = XmasConfig.get().swingSpeed;
		if (sp == 100) return;
		if ((Object) this != MinecraftClient.getInstance().player) return;
		cir.setReturnValue(Math.max(1, Math.round(cir.getReturnValue() / (sp / 100f))));
	}
}
