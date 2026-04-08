package net.orbitalstrike.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.TntEntity;
import net.minecraft.util.math.Vec3d;
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
		// Only interfere if the server explicitly told us this TNT is lazy.
		if (state == null || !state.lazy) return;

		// Server confirmed this chunk has no entity-ticking ticket.
		// Freeze the entity at the server-reported position — no gravity, no fuse tick.
		self.setPos(state.x, state.y, state.z);
		self.setVelocity(Vec3d.ZERO);
		self.setFuse(state.fuse);
		ci.cancel();
	}
}