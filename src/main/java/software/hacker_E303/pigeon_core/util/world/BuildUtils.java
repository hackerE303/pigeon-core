package software.hacker_E303.pigeon_core.util.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class BuildUtils {

    /**
     * Returns an AABB encompassing the two corner positions.
     */
    public static AABB getBoundingBox(BlockPos corner1, BlockPos corner2) {
        double minX = Math.min(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxX = Math.max(corner1.getX(), corner2.getX()) + 1.0;
        double maxY = Math.max(corner1.getY(), corner2.getY()) + 1.0;
        double maxZ = Math.max(corner1.getZ(), corner2.getZ()) + 1.0;
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Returns an array of 8 corner vertices of the box defined by two BlockPos.
     * Order: [minX minY minZ, maxX minY minZ, maxX maxY minZ, minX maxY minZ,
     *         minX minY maxZ, maxX minY maxZ, maxX maxY maxZ, minX maxY maxZ]
     */
    public static Vec3[] getBoxVertices(BlockPos corner1, BlockPos corner2) {
        int x1 = Math.min(corner1.getX(), corner2.getX());
        int y1 = Math.min(corner1.getY(), corner2.getY());
        int z1 = Math.min(corner1.getZ(), corner2.getZ());
        int x2 = Math.max(corner1.getX(), corner2.getX()) + 1;
        int y2 = Math.max(corner1.getY(), corner2.getY()) + 1;
        int z2 = Math.max(corner1.getZ(), corner2.getZ()) + 1;

        return new Vec3[] {
                new Vec3(x1, y1, z1), // 0 - bottom near left
                new Vec3(x2, y1, z1), // 1 - bottom near right
                new Vec3(x2, y2, z1), // 2 - top near right
                new Vec3(x1, y2, z1), // 3 - top near left
                new Vec3(x1, y1, z2), // 4 - bottom far left
                new Vec3(x2, y1, z2), // 5 - bottom far right
                new Vec3(x2, y2, z2), // 6 - top far right
                new Vec3(x1, y2, z2)  // 7 - top far left
        };
    }

    /**
     * Returns the 12 edges of the box as pairs of vertex indices (into getBoxVertices result).
     */
    public static int[][] getBoxEdges() {
        return new int[][] {
                {0, 1}, {1, 2}, {2, 3}, {3, 0}, // near face
                {4, 5}, {5, 6}, {6, 7}, {7, 4}, // far face
                {0, 4}, {1, 5}, {2, 6}, {3, 7}  // connecting edges
        };
    }

    /**
     * Returns 6 quads (face vertex indices) for the box.
     * Each quad is an array of 4 vertex indices.
     * Order: DOWN, UP, NORTH, SOUTH, WEST, EAST
     */
    public static int[][] getBoxFaces() {
        return new int[][] {
                {0, 4, 5, 1}, // DOWN  (y = min)
                {3, 2, 6, 7}, // UP    (y = max)
                {0, 1, 2, 3}, // NORTH (z = min)
                {4, 7, 6, 5}, // SOUTH (z = max)
                {0, 3, 7, 4}, // WEST  (x = min)
                {1, 5, 6, 2}  // EAST  (x = max)
        };
    }

    /**
     * Checks if a block is inside the volume defined by the two corners.
     */
    public static boolean isInside(BlockPos pos, BlockPos corner1, BlockPos corner2) {
        int minX = Math.min(corner1.getX(), corner2.getX());
        int minY = Math.min(corner1.getY(), corner2.getY());
        int minZ = Math.min(corner1.getZ(), corner2.getZ());
        int maxX = Math.max(corner1.getX(), corner2.getX());
        int maxY = Math.max(corner1.getY(), corner2.getY());
        int maxZ = Math.max(corner1.getZ(), corner2.getZ());
        return pos.getX() >= minX && pos.getX() <= maxX &&
               pos.getY() >= minY && pos.getY() <= maxY &&
               pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    /**
     * Counts the non-air blocks within the volume defined by the two corners, grouped by {@link Block}.
     */
    public static Map<Block, Integer> countBlocks(Level level, BlockPos corner1, BlockPos corner2) {
        Map<Block, Integer> counts = new HashMap<>();

        int minX = Math.min(corner1.getX(), corner2.getX());
        int minY = Math.min(corner1.getY(), corner2.getY());
        int minZ = Math.min(corner1.getZ(), corner2.getZ());
        int maxX = Math.max(corner1.getX(), corner2.getX());
        int maxY = Math.max(corner1.getY(), corner2.getY());
        int maxZ = Math.max(corner1.getZ(), corner2.getZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockState state = level.getBlockState(new BlockPos(x, y, z));
                    if (state.isAir()) continue;
                    counts.merge(state.getBlock(), 1, Integer::sum);
                }
            }
        }
        return counts;
    }

    /**
     * Saves the block data within the volume to a .nbt file in .minecraft/pigeon_core/structures/.
     * Returns the file path if successful, null otherwise.
     */
    public static String saveStructure(Level level, String name, BlockPos corner1, BlockPos corner2) {
        try {
            Path structureDir = Paths.get(System.getProperty("user.home"), ".minecraft", "pigeon_core", "structures");
            Files.createDirectories(structureDir);

            int minX = Math.min(corner1.getX(), corner2.getX());
            int minY = Math.min(corner1.getY(), corner2.getY());
            int minZ = Math.min(corner1.getZ(), corner2.getZ());
            int maxX = Math.max(corner1.getX(), corner2.getX());
            int maxY = Math.max(corner1.getY(), corner2.getY());
            int maxZ = Math.max(corner1.getZ(), corner2.getZ());

            int sizeX = maxX - minX + 1;
            int sizeY = maxY - minY + 1;
            int sizeZ = maxZ - minZ + 1;

            net.minecraft.nbt.CompoundTag root = new net.minecraft.nbt.CompoundTag();
            root.putInt("width", sizeX);
            root.putInt("height", sizeY);
            root.putInt("length", sizeZ);

            net.minecraft.nbt.ListTag blocksList = new net.minecraft.nbt.ListTag();

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos currentPos = new BlockPos(x, y, z);
                        net.minecraft.world.level.block.state.BlockState blockState = level.getBlockState(currentPos);

                        if (blockState.isAir()) continue;

                        net.minecraft.nbt.CompoundTag blockTag = new net.minecraft.nbt.CompoundTag();
                        blockTag.putInt("x", x - minX);
                        blockTag.putInt("y", y - minY);
                        blockTag.putInt("z", z - minZ);
                        blockTag.putString("id", ForgeRegistries.BLOCKS.getKey(blockState.getBlock()).toString());

                        // Save block state properties
                        net.minecraft.nbt.CompoundTag propertiesTag = new net.minecraft.nbt.CompoundTag();
                        for (net.minecraft.world.level.block.state.properties.Property<?> prop : blockState.getBlock().getStateDefinition().getProperties()) {
                            String propName = prop.getName();
                            String propValue = blockState.getValue(prop).toString();
                            propertiesTag.putString(propName, propValue);
                        }
                        if (!propertiesTag.isEmpty()) {
                            blockTag.put("properties", propertiesTag);
                        }

                        net.minecraft.nbt.CompoundTag blockEntityTag = level.getBlockEntity(currentPos) != null ? level.getBlockEntity(currentPos).saveWithId() : null;
                        if (blockEntityTag != null) {
                            blockTag.put("block_entity", blockEntityTag);
                        }

                        blocksList.add(blockTag);
                    }
                }
            }

            root.put("blocks", blocksList);

            String fileName = name + ".nbt";
            Path filePath = structureDir.resolve(fileName);
            java.io.DataOutputStream dos = new java.io.DataOutputStream(new java.io.FileOutputStream(filePath.toFile()));
            net.minecraft.nbt.NbtIo.writeCompressed(root, dos);
            dos.close();

            return filePath.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Helper inner class to avoid confusion with net.minecraft.world.phys.Vec3
    public static final class Vec3 {
        public final double x, y, z;
        public Vec3(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}