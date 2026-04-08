package net.orbitalstrike.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class TntOverlayRenderer {

    private TntOverlayRenderer() {}

    private static final float HS  = 0.49f;
    private static final float HGT = 0.98f;

    public static void register() {}

    public static void render(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d cam) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        for (Map.Entry<UUID, ClientTntStorage.TntState> entry : ClientTntStorage.getAll()) {
            ClientTntStorage.TntState state = entry.getValue();

            // Only draw ghost if client doesn't have this chunk loaded.
            // If the chunk IS loaded, the real entity is already rendering.
            int cx = (int) Math.floor(state.x) >> 4;
            int cz = (int) Math.floor(state.z) >> 4;
            boolean chunkLoaded = client.world
                    .getChunk(cx, cz, ChunkStatus.FULL, false) != null;
            if (chunkLoaded) continue;

            renderPhantom(matrices, consumers, cam, state);
        }
    }

    private static void renderPhantom(MatrixStack matrices,
                                      VertexConsumerProvider consumers,
                                      Vec3d cam,
                                      ClientTntStorage.TntState state) {
        int fuse = state.fuse;
        long now = System.currentTimeMillis();
        long period = fuse <= 10 ? 80L : fuse <= 40 ? 160L : 400L;
        boolean bright = (now / period) % 2 == 0;

        float r = 0.0f;
        float g = bright ? 1.0f : 0.5f;
        float b = bright ? 1.0f : 0.8f;

        float x0 = (float)(state.x - cam.x);
        float y0 = (float)(state.y - cam.y);
        float z0 = (float)(state.z - cam.z);

        VertexConsumer vc = consumers.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry e = matrices.peek();

        line(vc, e, x0-HS, y0,     z0-HS, x0+HS, y0,     z0-HS, r, g, b);
        line(vc, e, x0+HS, y0,     z0-HS, x0+HS, y0,     z0+HS, r, g, b);
        line(vc, e, x0+HS, y0,     z0+HS, x0-HS, y0,     z0+HS, r, g, b);
        line(vc, e, x0-HS, y0,     z0+HS, x0-HS, y0,     z0-HS, r, g, b);
        line(vc, e, x0-HS, y0+HGT, z0-HS, x0+HS, y0+HGT, z0-HS, r, g, b);
        line(vc, e, x0+HS, y0+HGT, z0-HS, x0+HS, y0+HGT, z0+HS, r, g, b);
        line(vc, e, x0+HS, y0+HGT, z0+HS, x0-HS, y0+HGT, z0+HS, r, g, b);
        line(vc, e, x0-HS, y0+HGT, z0+HS, x0-HS, y0+HGT, z0-HS, r, g, b);
        line(vc, e, x0-HS, y0,     z0-HS, x0-HS, y0+HGT, z0-HS, r, g, b);
        line(vc, e, x0+HS, y0,     z0-HS, x0+HS, y0+HGT, z0-HS, r, g, b);
        line(vc, e, x0+HS, y0,     z0+HS, x0+HS, y0+HGT, z0+HS, r, g, b);
        line(vc, e, x0-HS, y0,     z0+HS, x0-HS, y0+HGT, z0+HS, r, g, b);
    }

    private static void line(VertexConsumer vc, MatrixStack.Entry e,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float r, float g, float b) {
        float dx = x2-x1, dy = y2-y1, dz = z2-z1;
        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len > 0) { dx /= len; dy /= len; dz /= len; }
        vc.vertex(e, x1, y1, z1).color(r, g, b, 1f).normal(e, dx, dy, dz);
        vc.vertex(e, x2, y2, z2).color(r, g, b, 1f).normal(e, dx, dy, dz);
    }
}