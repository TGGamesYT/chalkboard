package dev.tggamesyt.chalkboard.client;

import dev.tggamesyt.chalkboard.ChalkboardBlock;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public class ChalkboardRenderState extends BlockEntityRenderState {
    public int[] pixels;
    public Direction facing = Direction.NORTH;
    public boolean hasContent = false;
}