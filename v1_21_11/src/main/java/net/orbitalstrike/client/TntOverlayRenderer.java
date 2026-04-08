package net.orbitalstrike.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderPipelines;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.util.HashSet;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class TntOverlayRenderer {

    private TntOverlayRenderer() {}

    private static final float HS  = 0.49f;
    private static final float HGT = 0.98f;

    private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(
            RenderPipelines.DEBUG_LINE_STRIP.getVertexFormat().getVertexSize() * 512
    );
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET    = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX  = new Matrix4f();
    private static GpuBuffer vertexBuffer = null;

    public static void register() {}

    public static void render(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d cam) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        // Build UUID set once — O(n) — instead of scanning all entities per TNT entry.
        Set<UUID> clientTntUuids = new HashSet<>();
        for (Entity e : client.world.getEntities()) {
            if (e instanceof TntEntity) clientTntUuids.add(e.getUuid());
        }

        BufferBuilder buffer = new BufferBuilder(
                ALLOCATOR,
                RenderPipelines.DEBUG_LINE_STRIP.getVertexFormatMode(),
                RenderPipelines.DEBUG_LINE_STRIP.getVertexFormat()
        );

        boolean anyDrawn = false;

        for (Map.Entry<UUID, ClientTntStorage.TntState> entry : ClientTntStorage.getAll()) {
            ClientTntStorage.TntState state = entry.getValue();

            // Only draw ghost boxes for lazy TNT.
            // Non-lazy TNT is in a loaded chunk and vanilla renders it normally.
            if (!state.lazy) continue;

            // If the entity is already in the client world, vanilla handles rendering.
            if (clientTntUuids.contains(entry.getKey())) continue;

            addPhantomToBuffer(buffer, matrices, cam, state);
            anyDrawn = true;
        }

        if (!anyDrawn) {
            ALLOCATOR.reset();
            return;
        }

        MeshData mesh = buffer.buildOrThrow();
        drawMesh(client, mesh);
        mesh.close();
    }

    private static void addPhantomToBuffer(BufferBuilder buffer,
                                           MatrixStack matrices,
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

        Matrix4fc pose = matrices.peek().getPositionMatrix();

        addLine(buffer, pose, x0-HS, y0,     z0-HS, x0+HS, y0,     z0-HS, r, g, b);
        addLine(buffer, pose, x0+HS, y0,     z0-HS, x0+HS, y0,     z0+HS, r, g, b);
        addLine(buffer, pose, x0+HS, y0,     z0+HS, x0-HS, y0,     z0+HS, r, g, b);
        addLine(buffer, pose, x0-HS, y0,     z0+HS, x0-HS, y0,     z0-HS, r, g, b);
        addLine(buffer, pose, x0-HS, y0+HGT, z0-HS, x0+HS, y0+HGT, z0-HS, r, g, b);
        addLine(buffer, pose, x0+HS, y0+HGT, z0-HS, x0+HS, y0+HGT, z0+HS, r, g, b);
        addLine(buffer, pose, x0+HS, y0+HGT, z0+HS, x0-HS, y0+HGT, z0+HS, r, g, b);
        addLine(buffer, pose, x0-HS, y0+HGT, z0+HS, x0-HS, y0+HGT, z0-HS, r, g, b);
        addLine(buffer, pose, x0-HS, y0,     z0-HS, x0-HS, y0+HGT, z0-HS, r, g, b);
        addLine(buffer, pose, x0+HS, y0,     z0-HS, x0+HS, y0+HGT, z0-HS, r, g, b);
        addLine(buffer, pose, x0+HS, y0,     z0+HS, x0+HS, y0+HGT, z0+HS, r, g, b);
        addLine(buffer, pose, x0-HS, y0,     z0+HS, x0-HS, y0+HGT, z0+HS, r, g, b);
    }

    private static void addLine(BufferBuilder buffer, Matrix4fc pose,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float r, float g, float b) {
        float dx = x2-x1, dy = y2-y1, dz = z2-z1;
        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len > 0) { dx /= len; dy /= len; dz /= len; }
        buffer.addVertex(pose, x1, y1, z1).setColor(r, g, b, 1f).setNormal(dx, dy, dz);
        buffer.addVertex(pose, x2, y2, z2).setColor(r, g, b, 1f).setNormal(dx, dy, dz);
    }

    private static void drawMesh(MinecraftClient client, MeshData mesh) {
        MeshData.DrawState draw = mesh.drawState();
        VertexFormat format = draw.format();
        int size = draw.vertexCount() * format.getVertexSize();

        if (vertexBuffer == null || vertexBuffer.size() < size) {
            if (vertexBuffer != null) vertexBuffer.close();
            vertexBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "lazytntview line buffer",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE,
                    size
            );
        }

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView view = encoder.mapBuffer(
                vertexBuffer.slice(0, mesh.vertexBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(mesh.vertexBuffer(), view.data());
        }

        RenderSystem.AutoStorageIndexBuffer indexBuf = RenderSystem.getSequentialBuffer(draw.mode());
        GpuBuffer indices = indexBuf.getBuffer(draw.indexCount());

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

        try (RenderPass pass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                        () -> "lazytntview tnt outline",
                        client.getFramebuffer().getColorAttachment(),
                        OptionalInt.empty(),
                        client.getFramebuffer().getDepthAttachment(),
                        OptionalDouble.empty()
                )) {
            pass.setPipeline(RenderPipelines.DEBUG_LINE_STRIP);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynamicTransforms);
            pass.setVertexBuffer(0, vertexBuffer);
            pass.setIndexBuffer(indices, indexBuf.type());
            pass.drawIndexed(0, 0, draw.indexCount(), 1);
        }
    }

    public static void close() {
        ALLOCATOR.close();
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }
}