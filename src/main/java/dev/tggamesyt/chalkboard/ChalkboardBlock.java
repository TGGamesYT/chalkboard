package dev.tggamesyt.chalkboard;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

    private static final VoxelShape SH_NORTH = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape SH_SOUTH = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape SH_WEST  = Block.box(6, 0, 0, 10, 16, 16);
    private static final VoxelShape SH_EAST  = Block.box(6, 0, 0, 10, 16, 16);

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

    @Override
    public @Nullable BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
        return this.defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override public @Nullable BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return new ChalkboardBlockEntity(p, s);
    }

    static void handleInteraction(Level level, BlockPos pos, BlockState state,
                                  Player player, net.minecraft.world.InteractionHand hand,
                                  ItemStack stack,
                                  ChalkboardBlockEntity cbe,
                                  int px, int py) {

        Item item = stack.getItem();

        // 🎨 CHALK
        if (item instanceof ChalkItem) {
            int newColor = ChalkItem.getColor(stack);
            int oldColor = cbe.getPixels()[py * 16 + px];

            if (oldColor == newColor) return;

            cbe.drawPixel(px, py, newColor);
            stack.hurtAndBreak(1, player, hand);

            level.playSound(null, pos,
                    SoundEvents.CALCITE_PLACE,
                    SoundSource.BLOCKS,
                    1.0f, 1.0f);
        }

        else if (item == Items.SPONGE || item == Items.WET_SPONGE) {

            if (item == Items.WET_SPONGE) {
                cbe.clearRadius(px, py, 1);
            } else {
                cbe.clearPixel(px, py);
            }

            level.playSound(null, pos,
                    item == Items.SPONGE ? SoundEvents.SPONGE_PLACE : SoundEvents.WET_SPONGE_PLACE,
                    SoundSource.BLOCKS,
                    1.0f, 1.0f);
        }

        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state,
                                          Level level, BlockPos pos, Player player,
                                          net.minecraft.world.InteractionHand hand,
                                          BlockHitResult hit) {

        Item item = stack.getItem();

        if (!(item instanceof ChalkItem ||
                item == Items.SPONGE ||
                item == Items.WET_SPONGE)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof ChalkboardBlockEntity cbe)) {
            return InteractionResult.CONSUME;
        }

        Direction facing = state.getValue(FACING);
        Vec3 relativeHit = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());

        double u = switch (facing) {
            case SOUTH -> 1.0 - relativeHit.x;
            case WEST  -> 1.0 - relativeHit.z;
            case EAST  -> relativeHit.z;
            default    -> relativeHit.x;
        };

        double v = relativeHit.y;

        int px = Math.max(0, Math.min(15, (int)(u * 16)));
        int py = 15 - Math.max(0, Math.min(15, (int)(v * 16)));

        handleInteraction(level, pos, state, player, hand, stack, cbe, px, py);

        return InteractionResult.SUCCESS;
    }
}
