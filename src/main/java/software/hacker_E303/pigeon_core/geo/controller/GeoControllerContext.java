package software.hacker_E303.pigeon_core.geo.controller;

import java.util.ArrayList;
import java.util.List;

/**
 * Context for managing Geo entity controllers.
 * Provides a simple API for creating and configuring animation controllers.
 * 
 * <h3>Usage Example</h3>
 * <pre>{@code
 * GeoControllerContext context = new GeoControllerContext();
 * 
 * // Add a controller for a Geo entity
 * context.add(GeoController.create("mainController", geoMob))
 *     .idle("idle")
 *     .trigger("shoot");
 * 
 * // Get the list of controllers for registration
 * List<AnimationController<?>> controllers = context.getControllers();
 * for (AnimationController<?> controller : controllers) {
 *     registrar.add(controller);
 * }
 * }</pre>
 */
public final class GeoControllerContext {

    private final List<GeoController> controllers = new ArrayList<>();

    /**
     * Adds a GeoController to the context and returns this context for method chaining.
     * 
     * @param controller The GeoController to add
     * @return This context for method chaining
     */
    public GeoControllerContext add(GeoController controller) {
        controllers.add(controller);
        return this;
    }

    /**
     * Gets the list of built controllers for registration.
     * 
     * @return List of AnimationController instances ready for registration
     */
    public List<GeoController> getControllers() {
        return controllers;
    }
}