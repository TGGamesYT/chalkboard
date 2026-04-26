package dev.tggamesyt.chalkboard.chalkboard;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ChalkboardBlock extends BaseEntityBlock {

    public static final MapCodec<ChalkboardBlock> CODEC = simpleCodec(ChalkboardBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    // One-pixel-deep flat panel facing each cardinal direction
    private static final VoxelShape SH_NORTH = Block.box(0,  0, 15, 16, 16, 16);
    private static final VoxelShape SH_SOUTH = Block.box(0,  0,  0, 16, 16,  1);
    private static final VoxelShape SH_WEST  = Block.box(15, 0,  0, 16, 16, 16);
    private static final VoxelShape SH_EAST  = Block.box(0,  0,  0,  1, 16, 16);

    public ChalkboardBlock(BlockBehaviour.Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter w, BlockPos p, CollisionContext c) {
        return switch (s.getValue(FACING)) {
            case SOUTH -> SH_SOUTH;
            case WEST  -> SH_WEST;
            case EAST  -> SH_EAST;
            default    -> SH_NORTH;
        };
    }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override public @Nullable BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return new ChalkboardBlockEntity(p, s);
    }

    /**
     * Called every tick the player holds right-click on this block face.
     * Maps the sub-block hit position to a pixel on the 16x16 board surface
     * and writes that pixel into the block entity.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state,
                                          Level level, BlockPos pos, Player player,
                                          net.minecraft.world.InteractionHand hand,
                                          BlockHitResult hit) {
        // 1. Check if we are actually holding chalk
        if (!(stack.getItem() instanceof ChalkItem)) return InteractionResult.PASS;

        // 2. We MUST return SUCCESS on client to allow the server call to happen
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        // 3. Server-side drawing logic
        if (level.getBlockEntity(pos) instanceof ChalkboardBlockEntity cbe) {
            int color = ChalkItem.getColor(stack);
            Direction facing = state.getValue(FACING);

            // Use the 'hit' location relative to the block position
            Vec3 relativeHit = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());

            double u, v;
            // Map the 3D hit point to 2D U/V based on which way the board faces
            u = switch (facing) {
                case SOUTH -> 1.0 - relativeHit.x;
                case WEST  -> 1.0 - relativeHit.z;
                case EAST  -> relativeHit.z;
                default    -> relativeHit.x; // NORTH
            };
            v = relativeHit.y;

            int px = Math.max(0, Math.min(15, (int)(u * 16)));
            int py = 15 - Math.max(0, Math.min(15, (int)(v * 16)));

            cbe.drawPixel(px, py, color);

            // CRITICAL: In 1.21.4, use the correct block update flags
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.CONSUME;
    }
}
