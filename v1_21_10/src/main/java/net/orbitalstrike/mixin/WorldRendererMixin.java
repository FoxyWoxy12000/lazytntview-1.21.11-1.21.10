// v1_21_10/src/main/java/net/orbitalstrike/mixin/WorldRendererMixin.java
package net.orbitalstrike.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
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

    // Adjust the method signature here to match what actually exists in 1.21.10
    @Inject(
            method = "renderEntities", // verify this name in your 1.21.10 jar
            at = @At("TAIL")
    )
    private void onAfterEntities(VertexConsumerProvider.Immediate immediate,
                                 MatrixStack matrices,
                                 boolean renderBlockOutline,
                                 CallbackInfo ci) {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        Vec3d cam = camera.getPos();
        TntOverlayRenderer.render(matrices, immediate, cam);
    }
}