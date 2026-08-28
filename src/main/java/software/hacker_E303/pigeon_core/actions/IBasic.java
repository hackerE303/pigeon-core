package software.hacker_E303.pigeon_core.actions;

import net.minecraft.resources.ResourceLocation;
import software.hacker_E303.pigeon_core.init.PigeUtils;
import software.hacker_E303.pigeon_core.util.locator.Location;
import software.hacker_E303.pigeon_core.util.locator.Path;

/**
 * Provides basic identifiers and location utilities for entities and items.
 */
public interface IBasic {
    
    /**
     * Returns the pigeon ID for this instance.
     */
    default String pigeid() {
        return PigeUtils.pigeidFrom(this);
    }

    /**
     * Returns the mod ID for this instance.
     */
    default String modid() {
        return PigeUtils.modidFrom(this);
    }

    /**
     * Creates a quick resource location from a path using this instance's identifiers.
     */
    default ResourceLocation createQuickLocation(Path path) {
        return Location.create(path, this.pigeid()).from(this.modid());
    }
}