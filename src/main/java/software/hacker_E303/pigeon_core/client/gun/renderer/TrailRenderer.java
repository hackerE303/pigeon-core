package software.hacker_E303.pigeon_core.client.gun.renderer;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import software.hacker_E303.pigeon_core.util.locator.Location;
import software.hacker_E303.pigeon_core.util.locator.Path;

/**
 * Renders a particle-like trail behind a projectile entity by chaining
 * short {@link TrailSegment} instances along the projectile's path.
 * <p>
 * The trail fades out over {@link #maxAge} seconds and is capped at 100 segments.
 * New segments are added when the projectile moves at least {@link #segmentLength} blocks
 * or when enough time has elapsed since the last segment was spawned.
 */
public class TrailRenderer {

    public Vec3 DIRECTION = Vec3.ZERO;
    
    /**
     * A single point along the trail path, recording its world position and
     * the time it was spawned so it can fade and expire.
     */
    private static class TrailSegment {

        public final Vec3 position;
        public final long spawnTime;
        
        /**
         * Creates a trail segment at the given position.
         *
         * @param pos the world position of this segment
         */
        public TrailSegment(Vec3 pos) {
            this.position = pos;
            this.spawnTime = System.currentTimeMillis();
        }
        
        /**
         * Returns the alpha (opacity) for this segment based on its age.
         * <p>
         * The segment is fully opaque for the first 70% of its life and then
         * fades out linearly to fully transparent at {@code maxAge}.
         *
         * @param maxAge the maximum age of a segment in seconds
         * @return the alpha value in the range 0.0–1.0
         */
        public float getAlpha(float maxAge) {
            float age = (System.currentTimeMillis() - spawnTime) / 1000f;
            float normalizedAge = age / maxAge;

            if (normalizedAge < 0.7f) {
                return 1.0f - (normalizedAge * 0.3f);
            } else {
                return 1.0f - ((normalizedAge - 0.7f) * 3.33f);
            }
        }
        
        /**
         * Returns whether this segment has lived past its maximum age.
         *
         * @param maxAge the maximum age of a segment in seconds
         * @return {@code true} if the segment should be removed
         */
        public boolean isDead(float maxAge) {
            return (System.currentTimeMillis() - spawnTime) / 1000f >= maxAge;
        }
    }
    
    private final List<TrailSegment> segments = new ArrayList<>();
    private final float maxAge = 5;
    private final float segmentLength = 0.1f;
    private Vec3 lastPosition = null;
    private long lastSegmentTime = 0;
    
    /**
     * Updates the trail geometry following the given projectile's current position.
     * <p>
     * New segments are added when the projectile has moved at least
     * {@link #segmentLength} blocks or when enough time has passed since the
     * last segment was created. Expired segments are removed.
     *
     * @param projectile the projectile entity whose trail to follow
     */
    public void updateTrail(Entity projectile) {
        Vec3 currentPos = projectile.position();
        
        if (lastPosition == null) {
            lastPosition = currentPos;
            lastSegmentTime = System.currentTimeMillis();
            return;
        }

        long currentTime = System.currentTimeMillis();
        boolean timeBasedTrigger = (currentTime - lastSegmentTime) >= 16;
        boolean distanceBasedTrigger = currentPos.distanceToSqr(lastPosition) > (segmentLength * segmentLength);
        
        if (segments.size() < 3 || timeBasedTrigger || distanceBasedTrigger) {
            segments.add(new TrailSegment(currentPos.add(0, projectile.getEyeHeight() / 2, 0)));
            lastPosition = currentPos;
            lastSegmentTime = currentTime;
        }
        
        segments.removeIf(segment -> segment.isDead(maxAge));
        
        while (segments.size() > 100) {
            segments.remove(0);
        }
    }

    private static final ResourceLocation texture = Location.create(Path.TEXTURE.MISC, "bullet_trail").from("pigeon_core");
    
    /**
     * Renders the trail as a continuous beam using the beacon beam render type.
     * <p>
     * The trail faces the camera and is stretched between valid segments.
     *
     * @param poseStack the pose stack for matrix transformations
     * @param bufferSource the buffer source for rendering
     */
    public void renderTrail(PoseStack poseStack, MultiBufferSource bufferSource) {
        if (segments.isEmpty() || DIRECTION == Vec3.ZERO) return;
        
        Minecraft mc = Minecraft.getInstance();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        
        poseStack.pushPose();
        
        RenderType renderType = RenderType.beaconBeam(texture, false);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
        Matrix4f pose = poseStack.last().pose();
        
        List<TrailSegment> validSegments = new ArrayList<>();
        for (TrailSegment segment : segments) {
            if (segment.getAlpha(maxAge) > 0) {
                validSegments.add(segment);
            }
        }
        
        if (validSegments.size() < 2) {
            poseStack.popPose();
            return;
        }
        
        TrailSegment firstSegment = validSegments.get(0);
        Vec3 projectileDirection = DIRECTION.normalize();
        Vec3 right = calculateProjectileRightVector(projectileDirection, cameraPos.subtract(firstSegment.position));
        
        renderContinuousTrail(validSegments, pose, vertexConsumer, cameraPos, projectileDirection, right);
        
        poseStack.popPose();
    }
    
    /**
     * Draws a single vertex with the given position, colour and default UV.
     *
     * @param pose the model-view matrix
     * @param consumer the vertex consumer to write to
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @param color the RGBA colour array
     */
    private void vertex(Matrix4f pose, VertexConsumer consumer, float x, float y, float z, float[] color) {
        consumer.vertex(pose, x, y, z)
                .color(color[0], color[1], color[2], color[3])
                .uv(0, 0)
                .overlayCoords(0, 0)
                .uv2(15728880)
                .normal(0, 1, 0)
                .endVertex();
    }
    
    /**
     * Renders the trail as a single lightning-like spike between the first
     * and last valid segments.
     *
     * @param segments the list of valid trail segments
     * @param pose the model-view matrix
     * @param vertexConsumer the vertex consumer to write to
     * @param cameraPos the camera position
     * @param projectileDirection the normalised direction of the projectile
     * @param right the rightward-facing vector perpendicular to the projectile
     */
    private void renderContinuousTrail(List<TrailSegment> segments, Matrix4f pose, VertexConsumer vertexConsumer, Vec3 cameraPos, Vec3 projectileDirection, Vec3 right) {
        TrailSegment firstSegment = segments.get(0);
        TrailSegment lastSegment = segments.get(segments.size() - 1);
        
        float maxThickness = 0.15f;
        
        renderLightningSpike(firstSegment.position, lastSegment.position, right, maxThickness * 0.1f, maxThickness, maxThickness * 0.2f, pose, vertexConsumer, cameraPos);
    }
    
    /**
     * Renders a tapered, lightning-shaped spike between two points.
     * <p>
     * The spike is subdivided into {@code segments} quads, each varying in
     * thickness along the line.
     *
     * @param start the start point in world space
     * @param end the end point in world space
     * @param right the rightward direction for thickness offset
     * @param startThickness the thickness at the start of the spike
     * @param middleThickness the thickness at the middle of the spike
     * @param endThickness the thickness at the end of the spike
     * @param pose the model-view matrix
     * @param vertexConsumer the vertex consumer to write to
     * @param cameraPos the camera position
     */
    private void renderLightningSpike(Vec3 start, Vec3 end, Vec3 right, float startThickness, float middleThickness, float endThickness, Matrix4f pose, VertexConsumer vertexConsumer, Vec3 cameraPos) {
        int segments = 8;
        
        start = start.subtract(cameraPos);
        end = end.subtract(cameraPos);
        
        List<Vec3> leftPoints = new ArrayList<>();
        List<Vec3> rightPoints = new ArrayList<>();
        
        for (int i = 0; i <= segments; i++) {
            float t = (float)i / segments;
            float thickness = calculateThickness(t, startThickness, middleThickness, endThickness);
            
            Vec3 pointAlongLine = start.lerp(end, t);
            Vec3 offset = right.scale(thickness * 0.5f);
            
            leftPoints.add(pointAlongLine.add(offset));
            rightPoints.add(pointAlongLine.subtract(offset));
        }
        
        float[] color = { 1.0f, 0.9f, 0.4f, 0.9f };
        
        for (int i = 0; i < segments; i++) {
            Vec3 left1 = leftPoints.get(i);
            Vec3 left2 = leftPoints.get(i + 1);
            Vec3 right1 = rightPoints.get(i);
            Vec3 right2 = rightPoints.get(i + 1);
            
            vertex(pose, vertexConsumer, (float)left1.x, (float)left1.y, (float)left1.z, color);
            vertex(pose, vertexConsumer, (float)left2.x, (float)left2.y, (float)left2.z, color);
            vertex(pose, vertexConsumer, (float)right1.x, (float)right1.y, (float)right1.z, color);
            
            vertex(pose, vertexConsumer, (float)left2.x, (float)left2.y, (float)left2.z, color);
            vertex(pose, vertexConsumer, (float)right2.x, (float)right2.y, (float)right2.z, color);
            vertex(pose, vertexConsumer, (float)right1.x, (float)right1.y, (float)right1.z, color);
        }
    }
    
    /**
     * Computes the thickness of the spike at a given interpolation factor.
     *
     * @param t the interpolation factor (0.0–1.0) along the spike
     * @param startThickness the thickness at the start
     * @param middleThickness the thickness at the middle
     * @param endThickness the thickness at the end
     * @return the calculated thickness, never below 0.001
     */
    private float calculateThickness(float t, float startThickness, float middleThickness, float endThickness) {
        float curve = (float)Math.sin(t * Math.PI);
        float thickness = startThickness + (middleThickness - startThickness) * curve;
        
        float endFactor = 1.0f - t;
        thickness = thickness * (1.0f - (1.0f - endFactor) * (1.0f - endThickness/middleThickness));
        
        return Math.max(thickness, 0.001f);
    }
    
    /**
     * Calculates a rightward vector perpendicular to the projectile direction
     * and the vector to the camera, falling back to a world-up cross product
     * when the vectors are nearly parallel.
     *
     * @param projectileDirection the normalised direction of the projectile
     * @param toCamera the vector from the projectile to the camera
     * @return the normalised rightward vector
     */
    private Vec3 calculateProjectileRightVector(Vec3 projectileDirection, Vec3 toCamera) {
        Vec3 right = projectileDirection.cross(toCamera).normalize();
        
        if (right.length() < 0.001) right = projectileDirection.cross(new Vec3(0, 1, 0)).normalize();
        
        return right;
    }
}
