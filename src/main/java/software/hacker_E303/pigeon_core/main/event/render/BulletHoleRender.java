package software.hacker_E303.pigeon_core.main.event.render;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;

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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.hacker_E303.pigeon_core.PigeonCore;
import software.hacker_E303.pigeon_core.util.locator.Location;
import software.hacker_E303.pigeon_core.util.locator.Path;

/**
 * Renders temporary bullet-hole decals on block faces.
 */
@Mod.EventBusSubscriber(modid = PigeonCore.MOD_ID, value = Dist.CLIENT)
public class BulletHoleRender {

    private static final ResourceLocation TEXTURE = Location.create(Path.TEXTURE.PARTICLES, "bullet_hole").from("pigeon_core");
    private static final List<BulletHole> holes = new ArrayList<>();

    private static final int LIFETIME = 12;
    private static final int EMISSIVE_MAX_BRIGHTNESS = 200;

    private static final long LIFETIME_MS = LIFETIME * 1000;
    private static final long DARKEN_DURATION_MS   = Math.round(LIFETIME_MS * 0.33);
    private static final long EMISSIVE_DURATION_MS = Math.round(LIFETIME_MS * 0.15);

    /**
     * Adds a new bullet hole to the render list.
     *
     * @param x    world x position
     * @param y    world y position
     * @param z    world z position
     * @param face the hit face direction
     */
    public static void add(double x, double y, double z, Direction face) {
        holes.add(new BulletHole(x, y, z, face, System.currentTimeMillis()));
    }

    /**
     * Removes expired bullet holes each client tick.
     *
     * @param event the client tick event
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            holes.removeIf(h -> {

                if (System.currentTimeMillis() - h.time > LIFETIME_MS) return true;
                Minecraft mc = Minecraft.getInstance();

                if (mc.level == null) return false;

                Vec3 normal = Vec3.atLowerCornerOf(h.face.getNormal());
                BlockPos pos = BlockPos.containing(h.x - normal.x * 0.5, h.y - normal.y * 0.5, h.z - normal.z * 0.5);

                return mc.level.getBlockState(pos).isAir();
            });
        }
    }

    /**
     * Renders all active bullet holes after translucent blocks.
     *
     * @param event the render level stage event
     */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (holes.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(-4.0f, -4.0f);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, TEXTURE);

        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f matrix = poseStack.last().pose();

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        long now = System.currentTimeMillis();
        for (BulletHole h : holes) {

            long age = now - h.time;
            if (age > LIFETIME_MS) continue;

            float fadeStart = LIFETIME_MS - 3000;
            int alpha = age < fadeStart ? 255 : (int) (255 * (1f - (age - fadeStart) / 3000f));
            if (alpha < 0) alpha = 0;

            Vec3 normal = Vec3.atLowerCornerOf(h.face.getNormal());
            BlockPos pos = BlockPos.containing(h.x - normal.x * 0.5, h.y - normal.y * 0.5, h.z - normal.z * 0.5);
            float skyDarken = mc.level.getSkyDarken(0f);
            int blockL = mc.level.getLightEngine().getLayerListener(LightLayer.BLOCK).getLightValue(pos);
            int skyL = mc.level.getLightEngine().getLayerListener(LightLayer.SKY).getLightValue(pos);
            float light = Math.max(blockL / 15.0f, skyL / 15.0f * (1.0f - skyDarken * 0.8f));
            light = Math.max(0.06f, light);
            int shade = (int)(light * 255);

            float darkT = Math.min(age / (float) DARKEN_DURATION_MS, 1f);
            shade = (int)(shade * (1f - darkT));

            float emissiveT = age < EMISSIVE_DURATION_MS ? (1f - age / (float) EMISSIVE_DURATION_MS) : 0f;
            int emissive = (int)(EMISSIVE_MAX_BRIGHTNESS * emissiveT);

            shade = Math.min(255, shade + emissive);
            renderHole(buf, matrix, h, alpha, shade);
        }

        BufferBuilder.RenderedBuffer buffer = buf.end();
        BufferUploader.drawWithShader(buffer);

        poseStack.popPose();
        RenderSystem.disablePolygonOffset();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /**
     * Renders a single bullet-hole quad aligned to the given face.
     *
     * @param buf     the active buffer builder
     * @param matrix  the current pose matrix
     * @param h       the bullet hole data
     * @param alpha   current alpha value (0-255)
     * @param shade   current brightness value (0-255)
     */
    private static void renderHole(BufferBuilder buf, Matrix4f matrix, BulletHole h, int alpha, int shade) {

        Vec3 normal = Vec3.atLowerCornerOf(h.face.getNormal());
        Vec3 tangent, bitangent;
        
        switch (h.face) {
            case UP, DOWN       -> { tangent = new Vec3(1, 0, 0); bitangent = new Vec3(0, 0, 1); }
            case NORTH, SOUTH   -> { tangent = new Vec3(1, 0, 0); bitangent = new Vec3(0, 1, 0); }
            case EAST, WEST     -> { tangent = new Vec3(0, 0, 1); bitangent = new Vec3(0, 1, 0); }
            default             -> { tangent = new Vec3(1, 0, 0); bitangent = new Vec3(0, 1, 0); }
        }
        double off = 0.0015;
        double cx = h.x + normal.x * off;
        double cy = h.y + normal.y * off;
        double cz = h.z + normal.z * off;
        double s = 0.09375;

        float x1 = (float) (cx - tangent.x * s - bitangent.x * s);
        float y1 = (float) (cy - tangent.y * s - bitangent.y * s);
        float z1 = (float) (cz - tangent.z * s - bitangent.z * s);

        float x2 = (float) (cx + tangent.x * s - bitangent.x * s);
        float y2 = (float) (cy + tangent.y * s - bitangent.y * s);
        float z2 = (float) (cz + tangent.z * s - bitangent.z * s);

        float x3 = (float) (cx + tangent.x * s + bitangent.x * s);
        float y3 = (float) (cy + tangent.y * s + bitangent.y * s);
        float z3 = (float) (cz + tangent.z * s + bitangent.z * s);

        float x4 = (float) (cx - tangent.x * s + bitangent.x * s);
        float y4 = (float) (cy - tangent.y * s + bitangent.y * s);
        float z4 = (float) (cz - tangent.z * s + bitangent.z * s);

        buf.vertex(matrix, x1, y1, z1).uv(0, 1).color(shade, shade, shade, alpha).endVertex();
        buf.vertex(matrix, x2, y2, z2).uv(1, 1).color(shade, shade, shade, alpha).endVertex();
        buf.vertex(matrix, x3, y3, z3).uv(1, 0).color(shade, shade, shade, alpha).endVertex();
        buf.vertex(matrix, x4, y4, z4).uv(0, 0).color(shade, shade, shade, alpha).endVertex();
    }

    private record BulletHole(double x, double y, double z, Direction face, long time) {}
}