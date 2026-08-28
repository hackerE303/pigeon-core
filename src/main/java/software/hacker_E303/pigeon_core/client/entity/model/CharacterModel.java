package software.hacker_E303.pigeon_core.client.entity.model;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import software.hacker_E303.pigeon_core.entity.animation.gear.IAnimatableEntity;
import software.hacker_E303.pigeon_core.entity.animation.gear.IAnimatableModel;
import software.hacker_E303.pigeon_core.entity.animation.gear.AnimatableParts;
import software.hacker_E303.pigeon_core.geo.item.gun.EGun;
import software.hacker_E303.pigeon_core.util.locator.Location;
import software.hacker_E303.pigeon_core.util.locator.Path;

/**
 * Humanoid player model with cape physics and animatable part support.
 */
public class CharacterModel<T extends LivingEntity & IAnimatableEntity<T>> extends HumanoidModel<T> implements IAnimatableModel {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Location.create(Path.NONE, "humanoid").from("pigeon_core"), "main");

    private final ModelPart CAPE;
    private static final Map<Integer, float[]> CAPE_STATE = new HashMap<>();

	private final AnimatableParts ANIMABLE_PARTS;

    public CharacterModel(ModelPart root) {
        super(root);

        this.CAPE = this.body.getChild("cape");
		this.ANIMABLE_PARTS = new AnimatableParts()
    		.register("head", this.head)
    		.register("body", this.body)
    		.register("right_arm", this.rightArm)
    		.register("left_arm", this.leftArm)
    		.register("right_leg", this.rightLeg)
    		.register("left_leg", this.leftLeg);
    }

	@Override
	public AnimatableParts getAnimatableParts() {
		return this.ANIMABLE_PARTS;
	}

    @Override
    public final void setupAnim(T animable, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

        this.resetParts();

        super.setupAnim(animable, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        this.handleAnimations(animable);

        int id = animable.getId();
        float[] state = CAPE_STATE.get(id);
        
        double dx = animable.getDeltaMovement().x;
        double dz = animable.getDeltaMovement().z;

        float speed = (float) Math.sqrt(dx * dx + dz * dz);
        
        float move = limbSwingAmount * 0.7f;
        float targetRot = 0.24f + (move * 0.8f) + Math.min(speed * 3.5f, 0.9f);
        
        targetRot += (float) Math.sin(ageInTicks * 0.06f) * 0.05f;
        targetRot = Mth.clamp(targetRot, 0.05f, 1.2f);
        
        if (state == null) {
                state = new float[]{targetRot, 0f, targetRot};
                CAPE_STATE.put(id, state);
                return;
        }
        float prevRot = state[0];
        float capeVel = state[1];
        float currentRot = state[2];
        
        float partialTick = Minecraft.getInstance().getFrameTime();
        
        float stiffness = 0.0017f;
        float damping = 0.85f;
        
        float force = (targetRot - currentRot) * stiffness;
        capeVel += force;
        capeVel *= damping;
        
        float nextRot = currentRot + capeVel;
        
        state[0] = currentRot;
        state[1] = capeVel;
        state[2] = nextRot;
        
        this.CAPE.xRot = Mth.lerp(partialTick, prevRot, nextRot);
        this.CAPE.yRot = netHeadYaw * 0.007f;

        if (animable.hasAnyAnim()) return;
        float sneak = animable.isShiftKeyDown() ? 1.0f : 0.0f;

        this.body.y = sneak * 4.5f;
        this.body.z = sneak * -2.7f;

        this.head.y = sneak * 5.3f;
        this.head.z = sneak * -2.3f;
        this.head.zRot = 0.0f;

        this.hat.y = this.head.y;
        this.hat.z = this.head.z;

        this.CAPE.y = sneak * 2.1f;

        this.rightArm.y = sneak * 4.6f + 2.0f;
        this.leftArm.y  = this.rightArm.y;

        this.rightLeg.x = this.getAnimatableParts().getParts().get("right_leg").basePx;
        this.leftLeg.x  = this.getAnimatableParts().getParts().get("left_leg").basePx;

        this.rightArm.z = sneak * -2.3f;
        this.leftArm.z  = this.rightArm.z;

        this.rightLeg.z = sneak * 3.0f;
        this.leftLeg.z  = this.rightLeg.z;

        this.body.xRot += sneak * 0.7f;
        this.body.yRot  = 0.0f;
        this.body.zRot  = 0.0f;

        if (EGun.from(animable.getMainHandItem()) != null) {

            this.rightArmPose = CharacterModel.ArmPose.BOW_AND_ARROW;

            this.rightArm.z += sneak * 2.7f;
            this.rightArm.xRot += sneak * 0.2f;
            this.rightArm.yRot -= sneak * 0.2f;

            this.leftArm.xRot += sneak * 0.4f;
            this.leftArm.yRot += sneak * 0.4f;

        } else {
            this.rightArm.xRot += sneak * 0.2f;
            this.leftArm.xRot  += sneak * 0.2f;
        }
    }

	@SuppressWarnings("unused")
	public static LayerDefinition createBodyLayer() {
		
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition hat = partdefinition.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 14).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(32, 14).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.5F))
		.texOffs(46, 8).addBox(-4.0F, -5.0F, -5.0F, 8.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-8, 0).addBox(-4.0F, -7.999F, -4.0F, 8.0F, 0.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.5F))
        .texOffs(24, 2).addBox(-1.0F, -3.0F, -6.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(40, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(16, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.25F))
		.texOffs(54, 4).addBox(-3.0F, 8.1F, 1.75F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.1F))
		.texOffs(58, 0).addBox(1.7F, 8.1F, -2.975F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.1F))
		.texOffs(58, 0).addBox(-3.7F, 8.1F, -2.975F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.1F))
		.texOffs(58, 0).addBox(-1.0F, 8.1F, -2.975F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.1F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition chain = body.addOrReplaceChild("chain", CubeListBuilder.create().texOffs(28, 4).addBox(-1.0F, -8.0F, -2.5F, 2.0F, 14.0F, 4.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.425F, 7.0F, 0.5F, 0.0F, 0.0F, -0.6109F));
		PartDefinition cape = body.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(0, 32).addBox(-4.0F, 0.0F, 0.0F, 8.0F, 16.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 2.25F, 0.0873F, 0.0F, 0.0F));

		PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(48, 48).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(32, 48).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.24F)), PartPose.offset(-5.0F, 2.0F, 0.0F));

		PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(48, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.24F)), PartPose.offset(5.0F, 2.0F, 0.0F));

		PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 48).addBox(-2.0F, 0.0F, -2.001F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(16, 48).addBox(-2.0F, 0.0F, -2.001F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)), PartPose.offset(-1.9F, 12.0F, 0.0F));

		PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 48).addBox(-2.05F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)), PartPose.offset(1.9F, 12.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}
}