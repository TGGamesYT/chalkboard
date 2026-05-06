package dev.tggamesyt.chalkboard;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class ChalkboardBlockEntity extends BlockEntity {

    public static BlockEntityType<ChalkboardBlockEntity> TYPE;

    private final int[] pixels = new int[256];

    public ChalkboardBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    public void drawPixel(int x, int y, int argb) {
        if (x < 0 || x > 15 || y < 0 || y > 15) return;
        pixels[y * 16 + x] = argb;
        setChanged();
    }

    public boolean clearPixel(int x, int y) {
        if (x < 0 || x > 15 || y < 0 || y > 15) return false;
        int idx = y * 16 + x;
        if (pixels[idx] == 0) return false;
        pixels[idx] = 0;
        setChanged();
        return true;
    }

    public boolean clearRadius(int cx, int cy, int radius) {
        boolean cleared = false;

        var level = getLevel();
        if (level == null) return false;

        var state = getBlockState();
        var origin = getBlockPos();
        var facing = state.getValue(ChalkboardBlock.FACING);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {

                int x = cx + dx;
                int y = cy + dy;

                int offsetX = 0;
                int offsetY = 0;

                // ── wrap X ──
                while (x < 0) {
                    x += 16;
                    offsetX--;
                }
                while (x > 15) {
                    x -= 16;
                    offsetX++;
                }

                // ── wrap Y ──
                while (y < 0) {
                    y += 16;
                    offsetY--;
                }
                while (y > 15) {
                    y -= 16;
                    offsetY++;
                }

                BlockPos targetPos = origin;

                // ── horizontal (relative to facing) ──
                if (offsetX != 0) {
                    Direction dir = switch (facing) {
                        case NORTH -> (offsetX < 0 ? Direction.WEST : Direction.EAST);
                        case SOUTH -> (offsetX < 0 ? Direction.EAST : Direction.WEST);
                        case WEST  -> (offsetX < 0 ? Direction.SOUTH : Direction.NORTH);
                        case EAST  -> (offsetX < 0 ? Direction.NORTH : Direction.SOUTH);
                        default    -> Direction.EAST;
                    };
                    targetPos = targetPos.relative(dir);
                }

                // ── vertical (fixed: pixel space → world space) ──
                if (offsetY != 0) {
                    targetPos = targetPos.relative(offsetY < 0
                            ? Direction.UP
                            : Direction.DOWN);
                }

                // ── ONLY affect matching chalkboards ──
                var targetState = level.getBlockState(targetPos);
                if (!(targetState.getBlock() instanceof ChalkboardBlock)) continue;
                if (targetState.getValue(ChalkboardBlock.FACING) != facing) continue;

                if (level.getBlockEntity(targetPos) instanceof ChalkboardBlockEntity other) {
                    if (other.clearPixel(x, y)) {
                        level.sendBlockUpdated(targetPos, targetState, targetState, Block.UPDATE_ALL);
                        cleared = true;
                    }
                }
            }
        }

        return cleared;
    }

    public int[] getPixels() {
        return pixels;
    }

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

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider reg) {
        return saveWithoutMetadata(reg);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public int getComparatorOutput() {
        int filled = 0;

        for (int p : pixels) {
            if (p != 0) filled++;
        }

        float ratio = filled / 256f;

        return (int)(ratio * 15);
    }
}
