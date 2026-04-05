package net.orbitalstrike.client;
//what
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class TntOverlayRenderer {

    private TntOverlayRenderer() {}

    private static final double HS  = 0.49;
    private static final double HGT = 0.98;

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            VertexConsumerProvider consumers = context.consumers();
            if (consumers == null) return;
            MatrixStack matrices = context.matrixStack();
            if (matrices == null) return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null) return;

            Vec3d cam = context.camera().getPos();

            for (ClientTntStorage.TntState state : ClientTntStorage.getAll()) {
                if (entityExistsInWorld(client, state.uuid)) continue;
                renderPhantom(matrices, consumers, cam, state);
            }
        });
    }

    private static boolean entityExistsInWorld(MinecraftClient client, UUID uuid) {
        for (Entity e : client.world.getEntities()) {
            if (e instanceof TntEntity && e.getUuid().equals(uuid)) return true;
        }
        return false;
    }

    private static void renderPhantom(MatrixStack matrices,
                                      VertexConsumerProvider consumers,
                                      Vec3d cam,
                                      ClientTntStorage.TntState state) {
        int  fuse  = state.fuse;
        long now   = System.currentTimeMillis();
        long period;

        if      (fuse <= 10) period = 80L;
        else if (fuse <= 40) period = 160L;
        else                 period = 400L;

        boolean bright = (now / period) % 2 == 0;

        float r = 0.0f;
        float g = bright ? 1.0f : 0.5f;
        float b = bright ? 1.0f : 0.8f;

        matrices.push();
        matrices.translate(
                state.x - cam.x,
                state.y - cam.y,
                state.z - cam.z
        );

        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
        Box box = new Box(-HS, 0.0, -HS, HS, HGT, HS);
        WorldRenderer.drawBox(matrices, lines, box, r, g, b, 1.0f);

        matrices.pop();
    }
}