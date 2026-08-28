package software.hacker_E303.pigeon_core.client.entity.renderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.hacker_E303.pigeon_core.geo.entity.GeoEMob;

/**
 * Renders {@link GeoEMob} entities using their GeoModel definitions.
 */
public class GeoEMobRenderer<T extends GeoEMob> extends GeoEntityRenderer<T> {

    public GeoEMobRenderer(EntityRendererProvider.Context context) {
        super(context, new Model<>());
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    protected float getDeathMaxRotation(T entity) {
        return 0.0F;
    }

    private static class Model<T extends GeoEMob> extends GeoModel<T> {

        @Override
        public ResourceLocation getModelResource(T animatable) {
            return animatable.getModelLocation();
        }

        @Override
        public ResourceLocation getTextureResource(T animatable) {
            return animatable.getTextureLocation();
        }

        @Override
        public ResourceLocation getAnimationResource(T animatable) {
            return animatable.getAnimsLocation();
        }

        @Override
        public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
            CoreGeoBone head = getAnimationProcessor().getBone("Head");

            if (head != null) {
                EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
                head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
                head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
            }
        }
    }
}