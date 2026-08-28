package software.hacker_E303.pigeon_core.entity.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import software.hacker_E303.pigeon_core.entity.common.stats.IStats;
import software.hacker_E303.pigeon_core.entity.common.stats.MutableStats;

/**
 * Custom bounding box definition that can be synced to clients and persisted to NBT.
 */
public final class BoundingBox {
    
    private Runnable listener;

    private double width  = 0.5;
    private double height = 1.9;
    private double scale  = 1.0;
    private double shadow = 0.5;

    private BoundingBox() {
    }

    private BoundingBox(double width, double height, double scale, double shadow) {
        this.width  = width;
        this.height = height;
        this.scale  = scale;
        this.shadow = shadow;
    }

    private BoundingBox(BoundingBox source) {
        this.width  = source.width;
        this.height = source.height;
        this.scale  = source.scale;
        this.shadow = source.shadow;
    }

    /**
     * Creates a new default bounding box.
     *
     * @return a new {@link BoundingBox} with default values
     */
    public static BoundingBox create() {
        return new BoundingBox();
    }

    /**
     * Creates a new bounding box with the specified dimensions.
     *
     * @param width   the hitbox width
     * @param height  the hitbox height
     * @param scale   the model scale
     * @param shadow  the shadow scale
     * @return a new {@link BoundingBox}
     */
    public static BoundingBox create(double width, double height, double scale, double shadow) {
        return new BoundingBox(width, height, scale, shadow);
    }

    /**
     * Creates a copy of this bounding box.
     *
     * @return a new {@link BoundingBox} with identical values
     */
    public BoundingBox copy() {
        return new BoundingBox(this);
    }

    /**
     * @return the hitbox width
     */
    public double getWidth() {
        return this.width;
    }

    /**
     * @return the hitbox height
     */
    public double getHeight() {
        return this.height;
    }

    /**
     * @return the model scale
     */
    public double getScale() {
        return this.scale;
    }

    /**
     * @return the shadow scale
     */
    public double getShadow() {
        return this.shadow;
    }

    /**
     * Creates a serializer for saving this bounding box to NBT.
     *
     * @return a new {@link BoundingBoxSerializer}
     */
    public BoundingBoxSerializer serialization() {
        return new BoundingBoxSerializer(this);
    }

    /**
     * Creates a changer for modifying this bounding box.
     *
     * @return a new {@link BoundingBoxChanger}
     */
    public BoundingBoxChanger modify() {
        return new BoundingBoxChanger(this);
    }

    /**
     * Handles serialization and deserialization of a {@link BoundingBox} to/from NBT.
     */
    public final static class BoundingBoxSerializer {

        private final BoundingBox box;
        private static final String TAG_KEY = "CustomBoundingBox";

        private BoundingBoxSerializer(BoundingBox box) {
            this.box = box;
        }

        /**
         * Saves the bounding box data into the given compound tag.
         *
         * @param tag the tag to save into
         * @return the updated tag
         */
        public CompoundTag save(CompoundTag tag) {
            CompoundTag boxTag = new CompoundTag();

            boxTag.putDouble("Width", box.width);
            boxTag.putDouble("Height", box.height);
            boxTag.putDouble("Scale", box.scale);
            boxTag.putDouble("Shadow", box.shadow);
            
            tag.put(TAG_KEY, boxTag);
            return tag;
        }

        /**
         * Loads bounding box data from the given compound tag.
         *
         * @param tag the tag to load from
         */
        public void load(CompoundTag tag) {
            if (tag.contains(TAG_KEY, 10)) {

                CompoundTag boxTag = tag.getCompound(TAG_KEY);
                box.width  = boxTag.getDouble("Width");
                box.height = boxTag.getDouble("Height");
                box.scale  = boxTag.getDouble("Scale");
                box.shadow = boxTag.getDouble("Shadow");

                if (box.listener != null) box.listener.run();
            }
        }

        /**
         * Registers a listener that syncs bounding box changes to the entity's
         * entity data and refreshes dimensions.
         *
         * @param stats the entity stats
         * @param entity the entity
         * @param <T> the entity type bound to {@link IStats}
         */
        public static <T extends Entity & IStats> void registerListener(MutableStats stats, T entity) {
            if (stats == null ||
                entity == null) return;

            BoundingBox box = stats.getBoundingBox();
            box.serialization().box.listener = () -> {

                CompoundTag compound = new CompoundTag();
                box.serialization().save(compound);

                if (!entity.level().isClientSide()) entity.getEntityData()
                    .set(BoundingBoxManager.BOUNDING_BOXES.get(entity.getClass()), compound);
                
                entity.refreshDimensions();
            };
        }
    }

    /**
     * Fluent builder for modifying a {@link BoundingBox}.
     */
    public final static class BoundingBoxChanger {

        private final BoundingBox box;

        private BoundingBoxChanger(BoundingBox box) {
            this.box = box;
        }

        /**
         * Sets the hitbox width.
         *
         * @param value the new width
         * @return this changer for chaining
         */
        public BoundingBoxChanger setWidth(double value) {
            box.width = value;
            return this.dirt();
        }

        /**
         * Sets the hitbox height.
         *
         * @param value the new height
         * @return this changer for chaining
         */
        public BoundingBoxChanger setHeight(double value) {
            box.height = value;
            return this.dirt();
        }

        /**
         * Sets the model scale.
         *
         * @param value the new scale
         * @return this changer for chaining
         */
        public BoundingBoxChanger setScale(double value) {
            box.scale = value;
            return this.dirt();
        }

        /**
         * Sets the shadow scale.
         *
         * @param value the new shadow scale
         * @return this changer for chaining
         */
        public BoundingBoxChanger setShadow(double value) {
            box.shadow = value;
            return this.dirt();
        }

        /**
         * Multiplies width, height, scale, and shadow by the given value.
         *
         * @param value the resize factor
         * @return this changer for chaining
         */
        public BoundingBoxChanger resize(float value) {
            box.width *= value;
            box.height *= value;
            box.scale *= value;
            box.shadow *= value;
            return this.dirt();
        }

        /**
         * Runs the change listener if set.
         *
         * @return this changer for chaining
         */
        private BoundingBoxChanger dirt() {
            if (box.listener != null) box.listener.run();
            return this;
        }
    }

    /**
     * Manages synchronized bounding box data on entities.
     */
    public final static class BoundingBoxManager {

        private static final Map<Class<? extends Entity>,
            EntityDataAccessor<CompoundTag>> BOUNDING_BOXES = new ConcurrentHashMap<>();

        /**
         * Defines the synchronized bounding box data field for the given entity.
         *
         * @param entity the entity
         * @param <T> the entity type bound to {@link IStats}
         */
        public static <T extends Entity & IStats> void defineSynchedData(T entity) {

            entity.getEntityData().define(BOUNDING_BOXES.computeIfAbsent(entity.getClass(),
                clazz -> SynchedEntityData.defineId(clazz, EntityDataSerializers.COMPOUND_TAG)), new CompoundTag());
        }

        /**
         * Saves the entity's bounding box to the given compound tag.
         *
         * @param entity the entity
         * @param compound the tag to save into
         * @param <T> the entity type bound to {@link IStats}
         */
        public static <T extends Entity & IStats> void addAdditionalSaveData(T entity, CompoundTag compound) {

            BoundingBox box = entity.getStats().getBoundingBox();
            if (box != null) box.serialization().save(compound);
        }

        /**
         * Reads the entity's bounding box from the given compound tag.
         *
         * @param entity the entity
         * @param compound the tag to load from
         * @param <T> the entity type bound to {@link IStats}
         */
        public static <T extends Entity & IStats> void readAdditionalSaveData(T entity, CompoundTag compound) {

            BoundingBox box = entity.getStats().getBoundingBox();
            if (box != null) box.serialization().load(compound);
        }

        /**
         * Called when synched entity data is updated; reloads the bounding box
         * from entity data on the client.
         *
         * @param entity the entity
         * @param key the updated data key
         * @param <T> the entity type bound to {@link IStats}
         */
        public static <T extends Entity & IStats> void onSyncedDataUpdated(T entity, EntityDataAccessor<?> key) {
            EntityDataAccessor<CompoundTag> boxData = BOUNDING_BOXES.get(entity.getClass());

            if (boxData.equals(key) && entity.level().isClientSide()) {
                BoundingBox box = entity.getStats().getBoundingBox();
                
                if (box != null) {
                    CompoundTag compound = entity.getEntityData().get(boxData);
                    box.serialization().load(compound);
                }
            }
        }
    }
}
