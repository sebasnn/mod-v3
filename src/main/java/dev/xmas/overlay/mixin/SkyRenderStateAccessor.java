package dev.xmas.overlay.mixin;

import net.minecraft.client.render.state.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SkyRenderState.class)
public interface SkyRenderStateAccessor {
	@Accessor("starBrightness") float xmas$getStarBrightness();
	@Mutable @Accessor("starBrightness") void xmas$setStarBrightness(float value);
}
