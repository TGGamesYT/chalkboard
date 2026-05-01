package dev.tggamesyt.chalkboard;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public record ChalkboardUsePayload(BlockPos pos, Vec3 hit) implements CustomPacketPayload {

    public static final Type<ChalkboardUsePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("chalkboard", "use"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChalkboardUsePayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeBlockPos(payload.pos);
                        buf.writeDouble(payload.hit.x);
                        buf.writeDouble(payload.hit.y);
                        buf.writeDouble(payload.hit.z);
                    },
                    buf -> new ChalkboardUsePayload(
                            buf.readBlockPos(),
                            new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}