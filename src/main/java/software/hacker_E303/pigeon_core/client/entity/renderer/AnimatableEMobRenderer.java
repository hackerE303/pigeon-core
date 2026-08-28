package software.hacker_E303.pigeon_core.client.entity.renderer;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import software.hacker_E303.pigeon_core.client.entity.model.CharacterModel;
import software.hacker_E303.pigeon_core.entity.EMob;
import software.hacker_E303.pigeon_core.entity.ETurret;
import software.hacker_E303.pigeon_core.entity.animation.AnimatableEMob;
import software.hacker_E303.pigeon_core.entity.common.stats.MutableStats;
import software.hacker_E303.pigeon_core.entity.common.stats.InitStats;
import software.hacker_E303.pigeon_core.util.locator.Location;
import software.hacker_E303.pigeon_core.util.locator.Path;

/**
 * Entity renderer for {@link AnimatableEMob} using {@link CharacterModel}.
 * <p>
 * Supports dynamic model-layer switching and turret-specific render overrides.
 */
@SuppressWarnings("unused")
public class AnimatableEMobRenderer extends LivingEntityRenderer<AnimatableEMob, CharacterModel<AnimatableEMob>> {

    private static final Map<ModelLayerLocation, CharacterModel<AnimatableEMob>> MODEL_CACHE = new HashMap<>();
    private final EntityRendererProvider.Context context;

    private ModelLayerLocation currentLayer;

    public AnimatableEMobRenderer(EntityRendererProvider.Context context) {
        super(context, new CharacterModel<>(context.bakeLayer(CharacterModel.LAYER_LOCATION)), 0.5f);

        this.context = context;
        this.currentLayer = CharacterModel.LAYER_LOCATION;

        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new HumanoidArmorLayer<>(this,

            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
            context.getModelManager()));
    }

    @Override
    public void render(AnimatableEMob mob, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        if (mob instanceof ETurret) {
            mob.hurtTime  = 0;
            mob.yBodyRot  = 0;
		    mob.yBodyRotO = 0;
        }
        MutableStats stats = mob.getStats();
        this.shadowRadius = (float) stats.getBoundingBox().getShadow();

        ModelLayerLocation layer = mob.getModelLayer();
        if (layer != null && !layer.equals(this.currentLayer)) {

            this.model = MODEL_CACHE.computeIfAbsent(layer, l -> new CharacterModel<>(context.bakeLayer(l)));
            this.currentLayer = layer;
        }
        super.render(mob, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    protected void scale(AnimatableEMob mob, PoseStack poseStack, float partialTickTime) {

        float scale = (float) mob.getStats().getBoundingBox().getScale();
        poseStack.scale(scale, scale, scale);
    }

    @Override
    protected boolean shouldShowName(AnimatableEMob mob) {
        return mob.shouldShowName();
    }

    private static final ResourceLocation TEXTURE_NONE = Location.create(Path.TEXTURE.MISC, "none").from("pigeon_core");

    @Override
    public ResourceLocation getTextureLocation(AnimatableEMob mob) {
        if (!mob.isTextureLoaded()) return TEXTURE_NONE;
        return mob.getTextureLocation();
    }
}