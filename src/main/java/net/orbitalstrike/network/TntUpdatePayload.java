package net.orbitalstrike.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record TntUpdatePayload(List<TntEntry> entries) implements CustomPayload {

    public static final CustomPayload.Id<TntUpdatePayload> ID =
            new CustomPayload.Id<>(Identifier.of("orbitalstrike", "tnt_update"));

    public static final PacketCodec<PacketByteBuf, TntUpdatePayload> CODEC =
            PacketCodec.of(TntUpdatePayload::encode, TntUpdatePayload::decode);

    private static void encode(TntUpdatePayload payload, PacketByteBuf buf) {
        buf.writeVarInt(payload.entries.size());
        for (TntEntry e : payload.entries) {
            buf.writeUuid(e.uuid());
            buf.writeDouble(e.x());
            buf.writeDouble(e.y());
            buf.writeDouble(e.z());
            buf.writeFloat(e.yaw());
            buf.writeFloat(e.pitch());
            buf.writeVarInt(e.fuse());
        }
    }

    private static TntUpdatePayload decode(PacketByteBuf buf) {
        int count = buf.readVarInt();
        List<TntEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new TntEntry(
                    buf.readUuid(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readFloat(),  buf.readFloat(),
                    buf.readVarInt()
            ));
        }
        return new TntUpdatePayload(entries);
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }

    public record TntEntry(
            UUID   uuid,
            double x, double y, double z,
            float  yaw, float pitch,
            int    fuse
    ) {}
}