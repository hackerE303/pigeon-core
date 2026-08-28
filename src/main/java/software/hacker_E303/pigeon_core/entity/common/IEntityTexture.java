package software.hacker_E303.pigeon_core.entity.common;

import net.minecraft.resources.ResourceLocation;
import software.hacker_E303.pigeon_core.util.locator.Path;

/**
 * Defines how an entity's texture is resolved and applied.
 */
public interface IEntityTexture {

    /**
     * Returns the texture identifier for this entity.
     *
     * @return the texture name
     */
    String getTexture();

    /**
     * Sets the texture identifier for this entity.
     *
     * @param name the texture name to apply
     */
    void setTexture(String name);

    /**
     * Returns the texture path used for resolving the texture location.
     *
     * @return the texture {@link Path}
     */
    Path getTexturePath();

    /**
     * Returns the fully resolved texture location.
     *
     * @return the texture {@link ResourceLocation}
     */
    ResourceLocation getTextureLocation();

    /**
     * Returns the emissive texture identifier, if any.
     *
     * @return the emissive texture name, or an empty string by default
     */
    default String getTextureEmissive() {
        return "";
    }

    /**
     * Sets the emissive texture identifier.
     *
     * @param name the emissive texture name to apply
     */
    default void setTextureEmissive(String name) {
    }
}