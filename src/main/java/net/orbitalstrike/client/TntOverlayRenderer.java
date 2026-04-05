package net.orbitalstrike.client;

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
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class TntOverlayRenderer {

    private TntOverlayRenderer() {}

    private static final double HS  = 0.49;
    private static final double HGT = 0.98;

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null) return;

            VertexConsumerProvider consumers = context.consumers();
            if (consumers == null) return;

            Vec3d       cam      = context.camera().getPos();
            MatrixStack matrices = context.matrixStack();

            for (Map.Entry<UUID, ClientTntStorage.TntState> e : ClientTntStorage.getAll()) {
                UUID                      uuid  = e.getKey();
                ClientTntStorage.TntState state = e.getValue();

                if (entityExistsInWorld(client, uuid)) continue;

                int cx = (int) Math.floor(state.x) >> 4;
                int cz = (int) Math.floor(state.z) >> 4;
                if (client.world.isChunkLoaded(cx, cz)) continue;

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
        int  fuse   = state.fuse;
        long now    = System.currentTimeMillis();
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

        VertexConsumer lines = consumers.getBuffer(RenderLayer.LINES);
        WorldRenderer.drawBox(matrices, lines,
                -HS, 0.0, -HS,
                HS, HGT,  HS,
                r, g, b, 1.0f);

        matrices.pop();
    }
}