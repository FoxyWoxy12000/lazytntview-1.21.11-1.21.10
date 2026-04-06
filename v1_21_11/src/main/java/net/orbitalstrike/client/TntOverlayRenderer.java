package net.orbitalstrike.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class TntOverlayRenderer {

    private TntOverlayRenderer() {
    }

    private static final double HS = 0.49;
    private static final double HGT = 0.98;

    public static void render(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d cam) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        for (Map.Entry<UUID, ClientTntStorage.TntState> entry : ClientTntStorage.getAll()) {
            UUID uuid = entry.getKey();
            ClientTntStorage.TntState state = entry.getValue();

            if (entityExistsInWorld(client, uuid)) continue;

            int cx = (int) Math.floor(state.x) >> 4;
            int cz = (int) Math.floor(state.z) >> 4;
            if (client.world.isChunkLoaded(cx, cz)) continue;

            renderPhantom(matrices, consumers, cam, state);
        }
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
        int fuse = state.fuse;
        long now = System.currentTimeMillis();
        long period;

        if      (fuse <= 10) period = 80L;
        else if (fuse <= 40) period = 160L;
        else                 period = 400L;

        boolean bright = (now / period) % 2 == 0;

        float r = 0.0f;
        float g = bright ? 1.0f : 0.5f;
        float b = bright ? 1.0f : 0.8f;

        double x1 = state.x - cam.x - HS;
        double y1 = state.y - cam.y;
        double z1 = state.z - cam.z - HS;
        double x2 = state.x - cam.x + HS;
        double y2 = state.y - cam.y + HGT;
        double z2 = state.z - cam.z + HS;

        VertexConsumer lines = consumers.getBuffer(RenderLayer.LINES);
        VertexRendering.drawBox(matrices, lines, x1, y1, z1, x2, y2, z2, r, g, b, 1.0f);
    }
}