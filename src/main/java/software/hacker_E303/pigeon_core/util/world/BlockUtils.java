package software.hacker_E303.pigeon_core.util.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Block type classification helpers based on {@link SoundType}.
 */
public class BlockUtils {

    /**
     * Checks whether the block at a world position is foliage.
     *
     * @param level the level to query
     * @param pos   the world position
     * @return {@code true} if the block is classified as foliage
     */
    public static boolean isFoliage(Level level, Vec3 pos) {
        return isFoliage(level, BlockPos.containing(pos));
    }

    /**
     * Checks whether the block at a block position is foliage.
     *
     * @param level the level to query
     * @param pos   the block position
     * @return {@code true} if the block is classified as foliage
     */
    public static boolean isFoliage(Level level, BlockPos pos) {

        BlockState state = level.getBlockState(pos);
        SoundType sound = state.getSoundType();

        return isFoliage(sound) && !state.is(Blocks.GRASS_BLOCK);
    }

    /**
     * Determines whether a {@link SoundType} matches known foliage sounds.
     *
     * @param sound the sound type to inspect
     * @return {@code true} if the sound type is foliage-like
     */
    private static boolean isFoliage(SoundType sound) {

        return sound == SoundType.AZALEA_LEAVES || sound == SoundType.CHERRY_LEAVES 
            || sound == SoundType.GRASS || sound == SoundType.LILY_PAD;
    }

    /**
     * Checks whether the block at a world position is stone-like.
     *
     * @param level the level to query
     * @param pos   the world position
     * @return {@code true} if the block is classified as stone
     */
    public static boolean isStone(Level level, Vec3 pos) {
        return isStone(level, BlockPos.containing(pos));
    }

    /**
     * Checks whether the block at a block position is stone-like.
     *
     * @param level the level to query
     * @param pos   the block position
     * @return {@code true} if the block is classified as stone
     */
    public static boolean isStone(Level level, BlockPos pos) {

        BlockState state = level.getBlockState(pos);
        SoundType sound = state.getSoundType();

        return isStone(sound);
    }

    /**
     * Determines whether a {@link SoundType} matches known stone sounds.
     *
     * @param sound the sound type to inspect
     * @return {@code true} if the sound type is stone-like
     */
    private static boolean isStone(SoundType sound) {

        return sound == SoundType.STONE || sound == SoundType.DEEPSLATE || sound == SoundType.DEEPSLATE_BRICKS || sound == SoundType.NETHER_BRICKS ||
            sound == SoundType.BASALT || sound == SoundType.TUFF || sound == SoundType.CALCITE || sound == SoundType.POLISHED_DEEPSLATE;
    }

    /**
     * Checks whether the block at a world position is metal.
     *
     * @param level the level to query
     * @param pos   the world position
     * @return {@code true} if the block is classified as metal
     */
    public static boolean isMetal(Level level, Vec3 pos) {
        return isMetal(level, BlockPos.containing(pos));
    }

    /**
     * Checks whether the block at a block position is metal.
     *
     * @param level the level to query
     * @param pos   the block position
     * @return {@code true} if the block is classified as metal
     */
    public static boolean isMetal(Level level, BlockPos pos) {

        BlockState state = level.getBlockState(pos);
        SoundType sound = state.getSoundType();

        return isMetal(sound);
    }

    /**
     * Determines whether a {@link SoundType} matches known metal sounds.
     *
     * @param sound the sound type to inspect
     * @return {@code true} if the sound type is metal-like
     */
    private static boolean isMetal(SoundType sound) {

        return sound == SoundType.METAL || sound == SoundType.COPPER ||
            sound == SoundType.NETHERITE_BLOCK;
    }

    /**
     * Checks whether the block at a world position is glass.
     *
     * @param level the level to query
     * @param pos   the world position
     * @return {@code true} if the block is classified as glass
     */
    public static boolean isGlass(Level level, Vec3 pos) {
        return isGlass(level, BlockPos.containing(pos));
    }

    /**
     * Checks whether the block at a block position is glass.
     *
     * @param level the level to query
     * @param pos   the block position
     * @return {@code true} if the block is classified as glass
     */
    public static boolean isGlass(Level level, BlockPos pos) {

        BlockState state = level.getBlockState(pos);
        SoundType sound = state.getSoundType();

        return isGlass(sound);
    }

    /**
     * Determines whether a {@link SoundType} matches glass.
     *
     * @param sound the sound type to inspect
     * @return {@code true} if the sound type is glass-like
     */
    private static boolean isGlass(SoundType sound) {
        return sound == SoundType.GLASS;
    }
}