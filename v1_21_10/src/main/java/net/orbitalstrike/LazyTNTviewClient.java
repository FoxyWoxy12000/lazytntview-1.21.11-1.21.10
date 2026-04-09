package net.orbitalstrike;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.orbitalstrike.client.ClientTntStorage;
import net.orbitalstrike.client.TntOverlayRenderer;
import net.orbitalstrike.network.TntUpdatePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LazyTNTviewClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("lazytntview-client");

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
                TntUpdatePayload.ID,
                (payload, context) -> context.client().execute(() -> {
                    if (context.client().world == null) return;
                    LOGGER.info("Received TNT packet with {} entries", payload.entries().size());
                    ClientTntStorage.updateAll(
                            payload.entries(),
                            context.client().world.getTime()
                    );
                })
        );

        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> ClientTntStorage.clear()
        );

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            LOGGER.info("WorldRenderEvents.AFTER_ENTITIES fired!");
            if (context.matrixStack() == null || context.consumers() == null) {
                LOGGER.info("Context null — matrixStack={} consumers={}",
                        context.matrixStack(), context.consumers());
                return;
            }
            TntOverlayRenderer.render(
                    context.matrixStack(),
                    context.consumers(),
                    context.camera().getPos()
            );
        });
    }
}