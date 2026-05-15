package dev.tggamesyt.chalkboard.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.tggamesyt.chalkboard.ChalkboardBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class ChalkboardBlockEntityRenderer implements BlockEntityRenderer<ChalkboardBlockEntity, ChalkboardRenderState> {

    public ChalkboardBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public ChalkboardRenderState createRenderState() {
        return new ChalkboardRenderState();
    }

    @Override
    public void extractRenderState(ChalkboardBlockEntity be, ChalkboardRenderState state, float partial, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partial, cameraPos, breakProgress);

        int[] source = be.getPixels();
        if (state.pixels == null || state.pixels.length != source.length) {
            state.pixels = new int[source.length];
        }
        System.arraycopy(source, 0, state.pixels, 0, source.length);

        boolean hasContent = false;
        int hash = 1;
        for (int p : state.pixels) {
            hash = 31 * hash + p;
            if (p != 0) hasContent = true;
        }
        state.hasContent = hasContent;
        state.contentHash = hash;
        state.facing = be.getBlockState().getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        state.lightCoords = LevelRenderer.getLightCoords(be.getLevel(), be.getBlockPos());

        state.textureId = hasContent
                ? ChalkboardTextureCache.getOrUpload(be.getBlockPos(), state.pixels, hash)
                : null;
    }

    @Override
    public void submit(ChalkboardRenderState state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.hasContent || state.textureId == null) return;

        collector.submitCustomGeometry(ps, RenderTypes.entityCutout(state.textureId), (pose, vc) -> {
            Matrix4f m4 = new Matrix4f(pose.pose());

            m4.translate(0.5f, 0.5f, 0.5f);

            float yaw = switch (state.facing) {
                case SOUTH -> 180f;
                case WEST  -> 90f;
                case EAST  -> -90f;
                default    -> 0f;
            };

            m4.rotate(Axis.YP.rotationDegrees(yaw));
            m4.translate(-0.5f, -0.5f, -0.5f);

            final float Z = (6f / 16f) - 0.001f;
            final int light = state.lightCoords;

            vert(vc, m4, pose, 0f, 1f, Z, 0f, 0f, light);
            vert(vc, m4, pose, 1f, 1f, Z, 1f, 0f, light);
            vert(vc, m4, pose, 1f, 0f, Z, 1f, 1f, light);
            vert(vc, m4, pose, 0f, 0f, Z, 0f, 1f, light);
        });
    }

    private static void vert(VertexConsumer v, Matrix4f m4, PoseStack.Pose pose,
                             float x, float y, float z,
                             float u, float w, int light) {
        v.addVertex(m4, x, y, z)
                .setColor(1f, 1f, 1f, 1f)
                .setUv(u, w)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0f, 0f, 1f);
    }
}
