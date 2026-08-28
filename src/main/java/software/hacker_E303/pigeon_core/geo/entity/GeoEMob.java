package software.hacker_E303.pigeon_core.geo.entity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.hacker_E303.pigeon_core.entity.EMob;
import software.hacker_E303.pigeon_core.geo.IGeo;
import software.hacker_E303.pigeon_core.geo.controller.GeoControllerContext;
import software.hacker_E303.pigeon_core.util.locator.Location;
import software.hacker_E303.pigeon_core.util.locator.Path;

/**
 * Base class for GeoLib-enabled mobs.
 */
public abstract class GeoEMob extends EMob implements GeoEntity, IGeo {

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private ResourceLocation animsLocation = this.createQuickLocation(Path.GEO_ANIMS.ENTITIES);
    private ResourceLocation modelLocation = this.createQuickLocation(Path.GEO_MODEL.ENTITIES);

    /**
     * Creates a new GeoLib-enabled mob.
     * 
     * @param mob the entity type
     * @param level the level
     */
    public GeoEMob(EntityType<? extends GeoEMob> mob, Level level) {
        super(mob, level);
    }

    @Override
    public ResourceLocation getAnimsLocation() {
        return this.animsLocation;
    }

    @Override
    public ResourceLocation getModelLocation() {
        return this.modelLocation;
    }

    @Override
    public void setAnims(Location location) {
        this.animsLocation = location.from(this.modid);
    }

    @Override
    public void setModel(Location location) {
        this.modelLocation = location.from(this.modid);
    }

    /**
     * Plays an animation.
     * 
     * @param name the animation name
     * @param controller the controller name
     */
    @Override
    public void playAnim(String name, String controller) {
        this.triggerAnim(controller, name);
    }

    /**
     * Stops an animation.
     * 
     * @param controller the controller name
     */
    @Override
    public void stopAnim(String controller) {
    }

    /**
     * Gets the current animation.
     * 
     * @param controller the controller name
     * @return the animation name, or empty string if none
     */
    @Override
    public String getAnim(String controller) {
        return "";
    }

    /**
     * Registers GeckoLib animation controllers.
     */
    @Override
    public final void registerControllers(ControllerRegistrar controllers) {
        GeoControllerContext ctx = new GeoControllerContext();
        this.registerControllers(ctx);
        ctx.getControllers().forEach(controller -> controllers.add(controller.build()));
    }

    /**
     * Returns the GeckoLib animatable instance cache.
     */
    @Override
    public final AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /**
     * Returns the model layer location.
     */
    @Override
    public final ModelLayerLocation getModelLayer() {
        return null;
    }
}