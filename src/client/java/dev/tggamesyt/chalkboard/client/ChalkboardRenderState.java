package dev.tggamesyt.chalkboard.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

public class ChalkboardRenderState extends BlockEntityRenderState {
    public int[] pixels;
    public Direction facing = Direction.NORTH;
    public boolean hasContent = false;
    public int contentHash;
    public Identifier textureId;
}
