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
		if (state == null) return;

		// If the client has the chunk loaded, vanilla handles it fine — don't interfere.
		int cx = (int) Math.floor(state.x) >> 4;
		int cz = (int) Math.floor(state.z) >> 4;
		boolean chunkLoaded = self.getEntityWorld()
				.getChunk(cx, cz, ChunkStatus.FULL, false) != null;
		if (chunkLoaded) return;

		// Chunk not loaded on client — freeze at server-reported position.
		self.lastX = state.x;
		self.lastY = state.y;
		self.lastZ = state.z;
		self.setPos(state.x, state.y, state.z);
		self.setVelocity(Vec3d.ZERO);
		self.setFuse(state.fuse);
		ci.cancel();
	}
}