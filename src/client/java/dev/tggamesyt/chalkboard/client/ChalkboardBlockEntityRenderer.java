package dev.tggamesyt.chalkboard.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.tggamesyt.chalkboard.chalkboard.ChalkboardBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
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

        state.hasContent = false;
        for (int pixel : state.pixels) {
            if (pixel != 0) {
                state.hasContent = true;
                break;
            }
        }
        // ChalkboardBlock.FACING is assumed to be the property name
        state.facing = be.getBlockState().getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        state.lightCoords = LevelRenderer.getLightCoords(be.getLevel(), be.getBlockPos());
    }

    @Override
    public void submit(ChalkboardRenderState state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.hasContent) return;

        // Use the moving block type - it's designed to stay in world coordinates
        collector.submitCustomGeometry(ps, RenderTypes.beaconBeam(Identifier.fromNamespaceAndPath("chalkboard", "textures/etc/white.png"), false), (pose, vc) -> {
            Matrix4f m4 = new Matrix4f(pose.pose()); // copy the snapshot

// apply transforms manually to the matrix
            m4.translate(0.5f, 0.5f, 0.5f);

            float yaw = switch (state.facing) {
                case SOUTH -> 180f;
                case WEST  -> 90f;
                case EAST  -> -90f;
                default    -> 0f;
            };

            m4.rotate(com.mojang.math.Axis.YP.rotationDegrees(yaw));
            m4.translate(-0.5f, -0.5f, -0.5f);

// keep normals from original pose
            PoseStack.Pose currentPose = pose;

            // Z-offset to sit on the face (1.0 is the front edge)
            final float Z = 0.999f - (1f /16f);
            final float SZ = 1f / 16f;

            for (int row = 0; row < 16; row++) {
                for (int col = 0; col < 16; col++) {
                    int argb = state.pixels[row * 16 + col];
                    if (argb == 0) continue;

                    float a = 1.0f;
                    float r = ((argb >> 16) & 0xFF) / 255f;
                    float g = ((argb >> 8) & 0xFF) / 255f;
                    float b = (argb & 0xFF) / 255f;

                    float x0 = col * SZ, x1 = x0 + SZ;
                    float y0 = 1f - (row + 1) * SZ, y1 = y0 + SZ;

                    // IMPORTANT: The 'moving block' type expects FULL vertex data
                    vert(vc, m4, currentPose, x0, y1, Z, r, g, b, a, state.lightCoords);
                    vert(vc, m4, currentPose, x1, y1, Z, r, g, b, a, state.lightCoords);
                    vert(vc, m4, currentPose, x1, y0, Z, r, g, b, a, state.lightCoords);
                    vert(vc, m4, currentPose, x0, y0, Z, r, g, b, a, state.lightCoords);
                }
            }
        });
    }

    private static void vert(VertexConsumer v, Matrix4f m4, PoseStack.Pose pose,
                             float x, float y, float z,
                             float r, float g, float b, float a, int light) {
        v.addVertex(m4, x, y, z)
                .setColor(r, g, b, a)
                .setUv(0f, 0f)         // Blocks need UVs even if they are solid colors
                .setLight(light)       // This keeps it from being pitch black or "full bright"
                .setNormal(pose, 0f, 0f, 1f); // Faces the player
    }
}