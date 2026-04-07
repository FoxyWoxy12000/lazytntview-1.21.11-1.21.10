// v1_21_11/src/main/java/net/orbitalstrike/mixin/WorldRendererMixin.java
package net.orbitalstrike.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import net.orbitalstrike.client.TntOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.render.WorldRenderer;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(
            method = "renderEntities",
            at = @At("TAIL")
    )
    private void onAfterEntities(VertexConsumerProvider.Immediate immediate,
                                 MatrixStack matrices,
                                 boolean renderBlockOutline,
                                 WorldRenderState renderStates,
                                 CallbackInfo ci) {
        Camera camera = renderStates.camera;
        Vec3d cam = camera.getPos();
        TntOverlayRenderer.render(matrices, immediate, cam);
    }
}