package software.hacker_E303.pigeon_core.geo;

import net.minecraft.resources.ResourceLocation;
import software.hacker_E303.pigeon_core.geo.controller.GeoControllerContext;
import software.hacker_E303.pigeon_core.util.locator.Location;

/**
 * Common interface for GeoLib-backed models and animations.
 */
public interface IGeo {
    
    /**
     * Registers animation controllers.
     * 
     * @param ctx the controller context
     */
    void registerControllers(GeoControllerContext ctx);
    /**
     * @return the animations resource location
     */
    ResourceLocation getAnimsLocation();
    /**
     * @return the model resource location
     */
    ResourceLocation getModelLocation();
    /**
     * Sets the animations location.
     * 
     * @param location the animations location
     */
    void setAnims(Location location);
    /**
     * Sets the model location.
     * 
     * @param location the model location
     */
    void setModel(Location location);

    /**
     * Plays an animation.
     * 
     * @param name the animation name
     * @param controller the controller name
     */
    void playAnim(String name, String controller);
    /**
     * Stops an animation.
     * 
     * @param controller the controller name
     */
    void stopAnim(String controller);
    /**
     * Gets the current animation.
     * 
     * @param controller the controller name
     * @return the animation name, or empty string if none
     */
    String getAnim(String controller);
}