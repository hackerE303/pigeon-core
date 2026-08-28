package software.hacker_E303.pigeon_core.client.gun.renderer;

import java.util.HashSet;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtils;
import software.hacker_E303.pigeon_core.client.gun.animation.AnimationManager;
import software.hacker_E303.pigeon_core.client.gun.animation.AnimationUtils;
import software.hacker_E303.pigeon_core.geo.item.gun.EGun;
import software.hacker_E303.pigeon_core.gun.gear.GunTracker;

/**
 * Custom {@link GeoItemRenderer} for {@link EGun} items, handling bone hiding,
 * arm rendering in first-person, and emissive part rendering.
 *
 * @param <T> the gun type
 */
public class GunRenderer<T extends EGun> extends GeoItemRenderer<T> {

	protected final Set<String> hiddenBones = new HashSet<>();
	
	protected RenderType renderType;
	protected MultiBufferSource currentBuffer;
	protected ItemDisplayContext transformType;
	protected T animatable;

	/**
	 * Constructs a new GunRenderer with the specified GeoModel.
	 * 
	 * @param model The GeoModel for rendering the gun
	 */
	public GunRenderer(GeoModel<T> model) {
		super(model);
	}

	/**
	 * Gets the render type to use for the gun.
	 * 
	 * @param animatable The gun instance being rendered
	 * @param texture The ResourceLocation of the texture
	 * @param bufferSource The MultiBufferSource for rendering
	 * @param partialTick Partial tick for animation smoothing
	 * @return The RenderType to use (entityTranslucent)
	 */
	@Override
	public RenderType getRenderType(T animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(getTextureLocation(animatable));
	}

	/**
	 * Handles rendering the gun by item stack.
	 * 
	 * @param stack The ItemStack representing the gun
	 * @param transformType The ItemDisplayContext for transformation
	 * @param matrixStack The PoseStack for matrix transformations
	 * @param bufferIn The MultiBufferSource for rendering
	 * @param combinedLightIn The combined lighting information
	 * @param value Additional rendering value
	 */
	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack matrixStack, MultiBufferSource bufferIn, int combinedLightIn, int value) {
		this.transformType = transformType;
		super.renderByItem(stack, transformType, matrixStack, bufferIn, combinedLightIn, value);
	}

	/**
	 * Handles actual rendering of the gun.
	 * 
	 * @param matrixStackIn The PoseStack for matrix transformations
	 * @param animatable The gun instance being rendered
	 * @param model The BakedGeoModel to render
	 * @param renderType The RenderType to use
	 * @param renderTypeBuffer The MultiBufferSource for rendering
	 * @param vertexBuilder The VertexConsumer for vertex data
	 * @param isRenderer Whether this is the primary renderer
	 * @param partialTicks Partial tick for animation smoothing
	 * @param packedLightIn The packed lighting information
	 * @param packedOverlayIn The packed overlay information
	 * @param red Red color component (0-1)
	 * @param green Green color component (0-1)
	 * @param blue Blue color component (0-1)
	 * @param alpha Alpha transparency component (0-1)
	 */
	@Override
	public void actuallyRender(PoseStack matrixStackIn, T animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource renderTypeBuffer, VertexConsumer vertexBuilder, boolean isRenderer, float partialTicks, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
		this.currentBuffer = renderTypeBuffer;
		this.renderType = renderType;
		this.animatable = animatable;
		super.actuallyRender(matrixStackIn, animatable, model, renderType, renderTypeBuffer, vertexBuilder, isRenderer, partialTicks, packedLightIn, packedOverlayIn, red, green, blue, alpha);
	}

	/**
	 * Handles recursive rendering of geo bones.
	 * 
	 * This method manages bone visibility, arm rendering, and emissive parts.
	 * 
	 * @param stack The PoseStack for matrix transformations
	 * @param animatable The gun instance being rendered
	 * @param bone The current GeoBone being rendered
	 * @param renderType The RenderType to use
	 * @param buffer The MultiBufferSource for rendering
	 * @param bufferIn The VertexConsumer for vertex data
	 * @param isReRender Whether this is a re-render
	 * @param partialTick Partial tick for animation smoothing
	 * @param packedLightIn The packed lighting information
	 * @param packedOverlayIn The packed overlay information
	 * @param red Red color component (0-1)
	 * @param green Green color component (0-1)
	 * @param blue Blue color component (0-1)
	 * @param alpha Alpha transparency component (0-1)
	 */
	@Override
	public void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType, MultiBufferSource buffer, VertexConsumer bufferIn, boolean isReRender, float partialTick, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {

		var stack = getCurrentItemStack();
		if (!isAnimatableVisible(stack)) {
			return;
		}
		String boneName = bone.getName();
		ResourceLocation texture = getTextureLocation(this.animatable);

		boolean renderingArms = false;

		if (boneName.equals("LeftArm") || boneName.equals("RightArm")) {
			bone.setHidden(true);
			renderingArms = true;
		} else {
			bone.setHidden(this.hiddenBones.contains(boneName));
		}

		renderArms(poseStack, bone, this.transformType, this.currentBuffer, texture, packedLightIn, renderingArms);
		super.renderRecursively(poseStack, animatable, bone, renderType, buffer, renderEmissiveParts(animatable, bone, buffer, texture), isReRender, partialTick, packedLightIn, packedOverlayIn, red, green, blue, alpha);
	}

	/**
	 * Gets the texture location for the gun.
	 * 
	 * @param instance The gun instance being rendered
	 * @return The ResourceLocation of the texture
	 */
	@Override
	public ResourceLocation getTextureLocation(T instance) {
		return super.getTextureLocation(instance);
	}

	/**
	 * Adds a bone to the hidden bones set to hide it from rendering.
	 * 
	 * @param boneName The name of the bone to hide
	 */
	protected void hideBone(String boneName) {
		this.hiddenBones.add(boneName);
	}

	/**
	 * Removes a bone from the hidden bones set to show it again.
	 * 
	 * @param boneName The name of the bone to show
	 */
	protected void showBone(String boneName) {
		this.hiddenBones.remove(boneName);
	}

	/**
	 * Clears all hidden bones to show everything.
	 */
	protected void clearHiddenBones() {
		this.hiddenBones.clear();
	}

	/** Constant for converting pixel coordinates to model scale (1/16) */
	private static final float SCALE_RECIPROCAL = 1.0f / 16.0f;

	/**
	 * Renders player arms over gun bones in first-person view.
	 * 
	 * This method handles the rendering of player arms when holding guns,
	 * ensuring proper positioning and transparency for invisible players.
	 * 
	 * @param stack The PoseStack for matrix transformations
	 * @param bone The GeoBone to render arms over
	 * @param context The ItemDisplayContext (e.g., first-person, third-person)
	 * @param thisBuffer The MultiBufferSource for rendering
	 * @param texture The ResourceLocation of the gun texture
	 * @param packedLightIn The packed lighting information
	 * @param renderingArms Whether arms should be rendered
	 */
	public static void renderArms(PoseStack stack, GeoBone bone, ItemDisplayContext context, MultiBufferSource thisBuffer, ResourceLocation texture, int packedLightIn, boolean renderingArms) {

		Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;

		if (context.firstPerson() && renderingArms) {

        	PlayerRenderer playerRenderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(player);
        	PlayerModel<AbstractClientPlayer> model = playerRenderer.getModel();

			float armsAlpha = player.isInvisible() ? 0.15f : 1.0f;
			String boneName = bone.getName();

        	stack.pushPose();
			
        	RenderUtils.translateMatrixToBone(stack, bone);
        	RenderUtils.translateToPivotPoint(stack, bone);
        	RenderUtils.rotateMatrixAroundBone(stack, bone);
        	RenderUtils.scaleMatrixForBone(stack, bone);
        	RenderUtils.translateAwayFromPivotPoint(stack, bone);

        	ResourceLocation location = player.getSkinTextureLocation();
        	VertexConsumer armBuilder = thisBuffer.getBuffer(RenderType.entitySolid(location));
        	VertexConsumer sleeveBuilder = thisBuffer.getBuffer(RenderType.entityTranslucent(location));

        	if (boneName.equals("LeftArm")) {
            	stack.translate(-1.0f * SCALE_RECIPROCAL, 2.0f * SCALE_RECIPROCAL, 0.0f);

            	renderPartOverBone(model.leftArm, bone, stack, armBuilder, packedLightIn, OverlayTexture.NO_OVERLAY, armsAlpha);
            	renderPartOverBone(model.leftSleeve, bone, stack, sleeveBuilder, packedLightIn, OverlayTexture.NO_OVERLAY, armsAlpha);
        	} else if (boneName.equals("RightArm")) {
         		stack.translate(1.0f * SCALE_RECIPROCAL, 2.0f * SCALE_RECIPROCAL, 0.0f);
			
            	renderPartOverBone(model.rightArm, bone, stack, armBuilder, packedLightIn, OverlayTexture.NO_OVERLAY, armsAlpha);
            	renderPartOverBone(model.rightSleeve, bone, stack, sleeveBuilder, packedLightIn, OverlayTexture.NO_OVERLAY, armsAlpha);
        	}
			
			thisBuffer.getBuffer(RenderType.entityTranslucent(texture));
			stack.popPose();
		}
	}

	/**
	 * Renders emissive parts of a gun with special lighting effects.
	 * 
	 * This method determines if a bone is an emissive part and applies
	 * appropriate rendering types for glowing effects.
	 * 
	 * @param animatable The GeoAnimatable object (gun) being rendered
	 * @param bone The GeoBone to check for emissive rendering
	 * @param thisBuffer The MultiBufferSource for rendering
	 * @param texture The ResourceLocation of the texture to use
	 * @return The appropriate VertexConsumer for rendering the emissive part
	 */
	public static VertexConsumer renderEmissiveParts(GeoAnimatable animatable, GeoBone bone, MultiBufferSource thisBuffer, ResourceLocation texture) {
		
		if (!(animatable instanceof EGun gun)) return null;
		String boneName = bone.getName().toLowerCase();

		if (boneName.equals("spark"))
			return thisBuffer.getBuffer(RenderType.beaconBeam(texture, false));

    	boolean found = false;
    	for (String bones : gun.getLightParts())
    		if (bones.toLowerCase().equals(boneName)) {
    			found = true;
    			break;
    		}

    	RenderType renderType;
    	if (found) renderType = RenderType.entityTranslucentEmissive(texture, false);
    	else renderType = RenderType.entityTranslucent(texture);

		return thisBuffer.getBuffer(renderType);
	}

	/**
	 * Determines if a gun animatable should be visible based on player state.
	 * 
	 * This method checks if the gun is held by the current player instance
	 * and whether the player is currently holding a gun.
	 * 
	 * @param renderizedStack The ItemStack representing the gun being rendered
	 * @return true if the animatable should be visible, false otherwise
	 */
	public static boolean isAnimatableVisible(ItemStack renderizedStack) {

		if (!EGun.process(renderizedStack, gun -> gun.hasGeckoId(), true)) return false;
		if (EGun.from(renderizedStack) != null && !GunTracker.isHeldByInstancePlayer(renderizedStack)) return true;

		boolean[] isHolding = {false};
		EGun.process(renderizedStack, gun -> {

			isHolding[0] = gun.isHolding();
			if (!gun.isHolding()) AnimationManager.IS_HOLDING_STARTED = false;
		});

		if (AnimationManager.IS_CLIENT_GUN_VISIBLE && AnimationManager.IS_CLIENT_HOLDING_GUN &&
			AnimationManager.GUN_VISIBILITY_SINCH_TIME == 0 && AnimationManager.IS_HOLDING_STARTED && isHolding[0]) return true;
		return false;
	}

	/**
	 * Renders a model part over a bone with default color values.
	 * 
	 * This is a convenience method that calls the full renderPartOverBone
	 * method with white color (1.0f, 1.0f, 1.0f) and the specified alpha.
	 * 
	 * @param model The ModelPart to render
	 * @param bone The GeoBone to render the part over
	 * @param stack The PoseStack for matrix transformations
	 * @param buffer The VertexConsumer for rendering
	 * @param packedLightIn The packed lighting information
	 * @param packedOverlayIn The packed overlay information
	 * @param alpha The alpha transparency value
	 */
	public static void renderPartOverBone(ModelPart model, GeoBone bone, PoseStack stack, VertexConsumer buffer, int packedLightIn, int packedOverlayIn, float alpha) {
		renderPartOverBone(model, bone, stack, buffer, packedLightIn, packedOverlayIn, 1.0f, 1.0f, 1.0f, alpha);
	}

	/**
	 * Renders a model part over a bone with custom color values.
	 * 
	 * This method sets up the model from the bone's position and rotation,
	 * then renders it with the specified color and transparency.
	 * 
	 * @param model The ModelPart to render
	 * @param bone The GeoBone to render the part over
	 * @param stack The PoseStack for matrix transformations
	 * @param buffer The VertexConsumer for rendering
	 * @param packedLightIn The packed lighting information
	 * @param packedOverlayIn The packed overlay information
	 * @param r Red color component (0.0f to 1.0f)
	 * @param g Green color component (0.0f to 1.0f)
	 * @param b Blue color component (0.0f to 1.0f)
	 * @param a Alpha transparency component (0.0f to 1.0f)
	 */
	public static void renderPartOverBone(ModelPart model, GeoBone bone, PoseStack stack, VertexConsumer buffer, int packedLightIn, int packedOverlayIn, float r, float g, float b, float a) {
		setupModelFromBone(model, bone);
		model.render(stack, buffer, packedLightIn, packedOverlayIn, r, g, b, a);
	}
   
	/**
	 * Sets up a model part to match the position and rotation of a bone.
	 * 
	 * This method positions the model part at the bone's pivot point and
	 * resets its rotation to align with the bone's orientation.
	 * 
	 * @param model The ModelPart to set up
	 * @param bone The GeoBone to match position and rotation from
	 */
	public static void setupModelFromBone(ModelPart model, GeoBone bone) {
		model.setPos(bone.getPivotX(), bone.getPivotY(), bone.getPivotZ());
		model.xRot = 0.0f;
		model.yRot = 0.0f;
		model.zRot = 0.0f;
	}

    /**
     * Default {@link GeoModel} for {@link EGun} items, delegating to the gun's
     * animation, model, and texture resource locations while triggering
     * per-frame animation updates via {@link AnimationUtils#animateGun}.
     */
    public static class Model extends GeoModel<EGun> {

        /**
         * {@inheritDoc}
         * @return the gun's animation resource location
         */
        @Override
        public ResourceLocation getAnimationResource(EGun gun) { return gun.ANIMS_LOCATION; }

        /**
         * {@inheritDoc}
         * @return the gun's model resource location
         */
        @Override
        public ResourceLocation getModelResource(EGun gun) { return gun.MODEL_LOCATION; }

        /**
         * {@inheritDoc}
         * @return the gun's texture resource location
         */
        @Override
        public ResourceLocation getTextureResource(EGun gun) { return gun.getTextureLocation(null); }

        /**
         * {@inheritDoc}
         * <p>Triggers {@link AnimationUtils#animateGun} using the item stack,
         * the {@code "Control"} bone, and the GeckoLib instance id.
         */
        @Override
        public void setCustomAnimations(EGun gun, long instanceId, AnimationState<EGun> animationState) {
            super.setCustomAnimations(gun, instanceId, animationState);
            AnimationUtils.animateGun(animationState.getData(DataTickets.ITEMSTACK), this, "Control", instanceId);
        }
    }
}