package software.hacker_E303.pigeon_core.main.event.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.hacker_E303.pigeon_core.PigeonCore;
import software.hacker_E303.pigeon_core.util.world.BuildUtils;

import java.awt.Color;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Renders the structure tool's selection perimeter as a wireframe box with
 * semi-transparent faces, one per tool instance (keyed by the tool's UUID).
 * Both the faces and the edge outline are tinted with a color derived from
 * the tool's UUID, so each item stack gets a visually distinct box.
 */
@Mod.EventBusSubscriber(modid = PigeonCore.MOD_ID, value = Dist.CLIENT)
public class PerimeterRenderer {

    private static final ConcurrentLinkedQueue<ActivePerimeter> perimeters = new ConcurrentLinkedQueue<>();

    /** Half-thickness, in blocks, of the solid edge outline. */
    private static final float EDGE_HALF_THICKNESS = 0.03f;

    /** Alpha of each of the 6 faces (DOWN, UP, NORTH, SOUTH, WEST, EAST). */
    private static final float[] FACE_ALPHAS = {0.08f, 0.10f, 0.06f, 0.07f, 0.06f, 0.08f};

    public static void addPerimeter(UUID toolUUID, BlockPos corner1, BlockPos corner2) {
        perimeters.removeIf(p -> p.toolUUID.equals(toolUUID));
        perimeters.add(new ActivePerimeter(toolUUID, corner1, corner2));
    }

    public static void removePerimeter(UUID toolUUID) {
        perimeters.removeIf(p -> p.toolUUID.equals(toolUUID));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (perimeters.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Vec3 cameraPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(-1.0f, -1.0f);
        for (ActivePerimeter perimeter : perimeters) {
            float[] color = colorForUUID(perimeter.toolUUID);
            renderFaces(poseStack, Tesselator.getInstance(), perimeter, cameraPos, color);
        }
        // Edges must always win the depth test against this box's own faces (never be
        // partially occluded by them), regardless of viewing angle - so they get a
        // stronger (closer to camera) offset than the faces.
        RenderSystem.polygonOffset(-4.0f, -4.0f);
        for (ActivePerimeter perimeter : perimeters) {
            float[] color = colorForUUID(perimeter.toolUUID);
            renderEdges(poseStack, Tesselator.getInstance(), perimeter, cameraPos, color);
        }
        RenderSystem.disablePolygonOffset();

        RenderSystem.enableCull();
    }

    private static float[] colorForUUID(UUID uuid) {
        int hash = uuid.hashCode();
        float hue = (hash & 0x7FFFFFFF) % 360 / 360.0f;
        int rgb = Color.HSBtoRGB(hue, 0.75f, 1.0f);
        return new float[] {
                ((rgb >> 16) & 0xFF) / 255.0f,
                ((rgb >> 8) & 0xFF) / 255.0f,
                (rgb & 0xFF) / 255.0f
        };
    }

    private static void renderFaces(PoseStack poseStack, Tesselator tesselator,
                                    ActivePerimeter perimeter, Vec3 cameraPos, float[] color) {
        BuildUtils.Vec3[] verts = BuildUtils.getBoxVertices(perimeter.corner1, perimeter.corner2);
        int[][] faces = BuildUtils.getBoxFaces();
        double ox = -cameraPos.x, oy = -cameraPos.y, oz = -cameraPos.z;

        BufferBuilder buf = tesselator.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int faceIdx = 0; faceIdx < faces.length; faceIdx++) {
            int[] face = faces[faceIdx];
            float alpha = FACE_ALPHAS[faceIdx];
            for (int vi = 0; vi < 4; vi++) {
                BuildUtils.Vec3 v = verts[face[vi]];
                buf.vertex(poseStack.last().pose(), (float) (v.x + ox), (float) (v.y + oy), (float) (v.z + oz))
                        .color(color[0], color[1], color[2], alpha)
                        .endVertex();
            }
        }
        BufferUploader.drawWithShader(buf.end());
    }

    /**
     * Draws the box outline as 12 thin solid prisms, one per edge, instead of
     * GL lines. Thick GL lines drawn via the vanilla "rendertype_lines" shader
     * don't miter at corners, which produced visible double edges where two
     * faces meet; solid prisms have no such join artifact and their thickness
     * is exact regardless of distance or driver line-width limits.
     */
    private static void renderEdges(PoseStack poseStack, Tesselator tesselator,
                                     ActivePerimeter perimeter, Vec3 cameraPos, float[] color) {
        BuildUtils.Vec3[] verts = BuildUtils.getBoxVertices(perimeter.corner1, perimeter.corner2);
        int[][] edges = BuildUtils.getBoxEdges();
        double ox = -cameraPos.x, oy = -cameraPos.y, oz = -cameraPos.z;

        BufferBuilder buf = tesselator.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int[] edge : edges) {
            BuildUtils.Vec3 v1 = verts[edge[0]];
            BuildUtils.Vec3 v2 = verts[edge[1]];
            emitEdgePrism(buf, poseStack, v1, v2, ox, oy, oz, color);
        }
        BufferUploader.drawWithShader(buf.end());
    }

    /**
     * Emits a thin axis-aligned box spanning the bounding box of {@code v1}-{@code v2},
     * inflated by {@link #EDGE_HALF_THICKNESS} on every axis. Since every box edge is
     * itself axis-aligned, this uniform inflation naturally produces a prism running
     * along the edge, capped slightly past both endpoints so adjacent edge prisms
     * overlap and fully cover the shared corner.
     */
    private static void emitEdgePrism(BufferBuilder buf, PoseStack poseStack, BuildUtils.Vec3 v1, BuildUtils.Vec3 v2,
                                       double ox, double oy, double oz, float[] color) {
        float ht = EDGE_HALF_THICKNESS;
        float x0 = (float) (Math.min(v1.x, v2.x) - ht + ox);
        float x1 = (float) (Math.max(v1.x, v2.x) + ht + ox);
        float y0 = (float) (Math.min(v1.y, v2.y) - ht + oy);
        float y1 = (float) (Math.max(v1.y, v2.y) + ht + oy);
        float z0 = (float) (Math.min(v1.z, v2.z) - ht + oz);
        float z1 = (float) (Math.max(v1.z, v2.z) + ht + oz);

        float[][] p = {
                {x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0},
                {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}
        };
        int[][] faces = {
                {0, 4, 5, 1}, {3, 2, 6, 7}, {0, 1, 2, 3}, {4, 7, 6, 5}, {0, 3, 7, 4}, {1, 5, 6, 2}
        };

        for (int[] face : faces) {
            for (int idx : face) {
                buf.vertex(poseStack.last().pose(), p[idx][0], p[idx][1], p[idx][2])
                        .color(color[0], color[1], color[2], 1.0f)
                        .endVertex();
            }
        }
    }

    private static final class ActivePerimeter {
        final UUID toolUUID;
        final BlockPos corner1;
        final BlockPos corner2;

        ActivePerimeter(UUID toolUUID, BlockPos corner1, BlockPos corner2) {
            this.toolUUID = toolUUID;
            this.corner1 = corner1;
            this.corner2 = corner2;
        }
    }
}
