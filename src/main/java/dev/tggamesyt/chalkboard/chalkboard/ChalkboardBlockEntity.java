package dev.tggamesyt.chalkboard.chalkboard;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Holds a 16x16 grid of ARGB colours (int[] pixels).
 * 0 = transparent / not drawn.  Persisted in NBT and synced to clients.
 */
public class ChalkboardBlockEntity extends BlockEntity {

    public static final BlockEntityType<ChalkboardBlockEntity> TYPE = register();

    private final int[] pixels = new int[256]; // 16*16

    public ChalkboardBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    public void drawPixel(int x, int y, int argb) {
        if (x < 0 || x > 15 || y < 0 || y > 15) return;
        pixels[y * 16 + x] = argb;
        setChanged();
    }

    public int[] getPixels() { return pixels; }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putIntArray("Pixels", pixels);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        if (input.contains("Pixels")) {
            int[] src = input.getIntArray("Pixels").orElse(new int[0]);
            if (src.length == 256) System.arraycopy(src, 0, pixels, 0, 256);
        }
    }

    // ── Sync ──────────────────────────────────────────────────────────────────

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider reg) {
        return saveWithoutMetadata(reg);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private static BlockEntityType<ChalkboardBlockEntity> register() {
        return Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(ChalkboardMod.ID, "chalkboard_be"),
                FabricBlockEntityTypeBuilder.create(ChalkboardBlockEntity::new,
                        // Use a Supplier or handle the block reference carefully
                        ChalkboardMod.CHALKBOARD
                ).build()
        );
    }

    public static void registerAll() {}
}
