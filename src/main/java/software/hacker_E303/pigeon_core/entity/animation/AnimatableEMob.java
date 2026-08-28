package software.hacker_E303.pigeon_core.entity.animation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.hacker_E303.pigeon_core.entity.EMob;
import software.hacker_E303.pigeon_core.entity.animation.gear.IAnimatableEntity;

/**
 * Base mob class that integrates the animation system with standard entity lifecycle.
 * <p>
 * Automatically ticks animation managers, syncs data to NBT, and initializes
 * entity data entries for animations.
 */
public abstract class AnimatableEMob extends EMob implements IAnimatableEntity<AnimatableEMob> {

    /**
     * Constructs an animatable entity.
     *
     * @param mob the entity type
     * @param level the level the entity spawns in
     */
    public AnimatableEMob(EntityType<? extends AnimatableEMob> mob, Level level) {
        super(mob, level);
    }

    /**
     * Advances animation cooldowns and client playback state each tick.
     *
     * @return {@code true} to continue ticking
     */
    @Override
    public boolean tickEvent() {
        this.tickAnimatable();
        return super.tickEvent();
    }

    /**
     * Defines the synched entity data entries used for animations.
     */
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.defineAnimatableData();
    }

    /**
     * Serializes animation state into the entity's saved NBT.
     *
     * @param compound the tag to write into
     */
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        this.addAnimatableData(compound);
    }

    /**
     * Reads animation state back from the entity's saved NBT.
     *
     * @param compound the tag to read from
     */
    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.readAnimatableData(compound);
    }
}