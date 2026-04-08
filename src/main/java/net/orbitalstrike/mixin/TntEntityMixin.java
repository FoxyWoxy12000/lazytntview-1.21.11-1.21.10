package net.orbitalstrike.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.TntEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkStatus;
import net.orbitalstrike.client.ClientTntStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(TntEntity.class)
public class TntEntityMixin {

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void onTick(CallbackInfo ci) {
		TntEntity self = (TntEntity)(Object)this;
		if (!self.getEntityWorld().isClient()) return;

		ClientTntStorage.TntState state = ClientTntStorage.get(self.getUuid());
		// Only interfere if the server told us this TNT is lazy
		if (state == null || !state.lazy) return;

		// Double-check: if the chunk IS loaded on the client, let vanilla handle it.
		// This catches carpet creative mode where client may have the chunk.
		int cx = (int)Math.floor(state.x) >> 4;
		int cz = (int)Math.floor(state.z) >> 4;
		boolean chunkLoadedOnClient = self.getEntityWorld()
				.getChunk(cx, cz, ChunkStatus.FULL, false) != null;
		if (chunkLoadedOnClient) return;

		// Chunk is not loaded on client — freeze this entity at the server position.
		self.lastX = state.x;
		self.lastY = state.y;
		self.lastZ = state.z;
		self.setPos(state.x, state.y, state.z);
		self.setVelocity(Vec3d.ZERO);
		self.setFuse(state.fuse);
		ci.cancel();
	}
}