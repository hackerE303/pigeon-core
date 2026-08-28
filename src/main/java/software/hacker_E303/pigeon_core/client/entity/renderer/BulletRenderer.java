package software.hacker_E303.pigeon_core.client.entity.renderer;

import net.minecraft.resources.ResourceLocation;
import software.hacker_E303.pigeon_core.entity.EBullet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Renders {@link EBullet} entities by delegating to the bullet's own
 * trail rendering logic.
 */
public class BulletRenderer extends EntityRenderer<EBullet> {

    /**
     * Creates a new {@link BulletRenderer}.
     *
     * @param context the entity renderer provider context
     */
    public BulletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /**
     * Renders the bullet by delegating to {@link EBullet#renderTrail}.
     *
     * @param bullet the bullet entity to render
     * @param yaw the yaw rotation
     * @param partialTicks the partial tick for animation smoothing
     * @param poseStack the pose stack for matrix transformations
     * @param bufferSource the buffer source for rendering
     * @param packedLight the packed lighting information
     */
    @Override
    public void render(EBullet bullet, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        bullet.renderTrail(poseStack, bufferSource);
    }

    /**
     * Always returns {@code true} so that bullets are always rendered,
     * regardless of frustum culling.
     *
     * @param bullet the bullet entity
     * @param frustum the frustum to test against
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return {@code true} always
     */
    @Override
    public boolean shouldRender(EBullet bullet, Frustum frustum, double x, double y, double z) { 
        return true;
    }

    /**
     * Returns the texture location for the bullet.
     * <p>
     * Bullets are rendered via their trail rather than a texture, so this
     * always returns {@code null}.
     *
     * @param bullet the bullet entity
     * @return {@code null}
     */
    @Override
    public ResourceLocation getTextureLocation(EBullet bullet) {
        return null;
    }
}
