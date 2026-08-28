package software.hacker_E303.pigeon_core.entity.common.stats;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import software.hacker_E303.pigeon_core.PigeonCore;

/**
 * Base stats for living entities, extending {@link MutableStats}.
 * <p>
 * Provides access to vanilla and custom attributes and handles attribute
 * lookup, validation, and mutation.
 */
public class LivingStats extends MutableStats {

    public static final String MAX_HEALTH                  = "health";
    public static final String FOLLOW_RANGE                = "range.follow";

    public static final String ARMOR                       = "armor";
    public static final String ARMOR_TOUGHNESS             = "armor.toughness";
    public static final String KNOCKBACK_RESISTANCE        = "knockback.resistence";

    public static final String MOVEMENT_SPEED              = "speed";
    public static final String FLYING_SPEED                = "speed.flying";

    public static final String ATTACK_SPEED                = "attack.speed";
    public static final String ATTACK_DAMAGE               = "attack.damage";
    public static final String ATTACK_KNOCKBACK            = "knockback.attack";

    public static final String LUCK                        = "luck";
    public static final String JUMP_STRENGTH               = "jump_strenght";
    public static final String SPAWN_REINFORCEMENTS_CHANCE = "reinforcements_chance";

    public static final String SHOOT_RANGE                 = "shoot_range";
    public static final String POWER_DURATION              = "power_duration";

    private static final Map<String, Attribute> VANILLA_ATTRIBUTES = new HashMap<>();
    static {
        VANILLA_ATTRIBUTES.put("health",                Attributes.MAX_HEALTH);
        VANILLA_ATTRIBUTES.put("range.follow",          Attributes.FOLLOW_RANGE);
        
        VANILLA_ATTRIBUTES.put("armor",                 Attributes.ARMOR);
        VANILLA_ATTRIBUTES.put("armor.toughness",       Attributes.ARMOR_TOUGHNESS);
        VANILLA_ATTRIBUTES.put("knockback.resistence",  Attributes.KNOCKBACK_RESISTANCE);

        VANILLA_ATTRIBUTES.put("speed",                 Attributes.MOVEMENT_SPEED);
        VANILLA_ATTRIBUTES.put("speed.flying",          Attributes.FLYING_SPEED);

        VANILLA_ATTRIBUTES.put("attack.speed",          Attributes.ATTACK_SPEED);
        VANILLA_ATTRIBUTES.put("attack.damage",         Attributes.ATTACK_DAMAGE);
        VANILLA_ATTRIBUTES.put("knockback.attack",      Attributes.ATTACK_KNOCKBACK);

        VANILLA_ATTRIBUTES.put("luck",                  Attributes.LUCK);
        VANILLA_ATTRIBUTES.put("jump_strenght",         Attributes.JUMP_STRENGTH);
        VANILLA_ATTRIBUTES.put("reinforcements_chance", Attributes.SPAWN_REINFORCEMENTS_CHANCE);
    }

    protected final String modid;
    protected LivingEntity living;

    /**
     * Constructs living stats for the given modid and key.
     *
     * @param modid the mod id
     * @param key   the stats identifier
     */
    protected LivingStats(String modid, String key) {
        super(modid, key);

        this.modid = modid;
    }

    /**
     * @return the max health attribute value
     */
    public double getHealth() {
        return this.get("health");
    }

    /**
     * Sets the max health attribute value.
     *
     * @param value the new health value
     */
    public void setHealth(double value) {
        this.set("health", value);
    }

    /**
     * Injects the living entity owner into this stats instance.
     *
     * @param living the owning living entity
     * @return this stats instance for chaining
     */
    protected LivingStats injectOwner(LivingEntity living) {
        this.living = living;
        return this;
    }
    
    /**
     * Sets an attribute base value on the owning living entity.
     *
     * @param attribute the attribute key (vanilla or custom)
     * @param value     the new base value
     */
    public void set(String attribute, double value) {

        Attribute attr = VANILLA_ATTRIBUTES.get(attribute);
        if (attr == null) attr = PigeonCore.getAttribute(this.modid, attribute);

        if (attr == null) this.throwError(attribute, 0);
        if (living == null || living.getAttribute(attr) == null) this.throwError(attribute, 1);

        living.getAttribute(attr).setBaseValue(value);
    }

    /**
     * Gets an attribute base value from the owning living entity.
     *
     * @param attribute the attribute key (vanilla or custom)
     * @return the current base value
     */
    public double get(String attribute) {

        Attribute attr = VANILLA_ATTRIBUTES.get(attribute);
        if (attr == null) attr = PigeonCore.getAttribute(this.modid, attribute);

        if (attr == null) this.throwError(attribute, 2);
        if (living == null || living.getAttribute(attr) == null) this.throwError(attribute, 3);
        
        return living.getAttribute(attr).getBaseValue();
    }

    private void throwError(String attribute, int index) {

        switch (index) {
            case 1:
                throw new IllegalStateException(String.format(
                    "Cannot set value for attribute '%s'. The entity is either null or does not support this attribute.", 
                    attribute
                ));
            case 2:
                throw new IllegalArgumentException(String.format(
                    "Unknown attribute '%s' for mod '%s'. Cannot retrieve value.", 
                    attribute, this.modid
                ));
            case 3:
                throw new IllegalStateException(String.format(
                    "Cannot get value for attribute '%s'. The entity is either null or does not support this attribute.", 
                    attribute
                ));
            default:
                throw new IllegalArgumentException(String.format(
                    "Unknown attribute '%s' for mod '%s'. Make sure it is properly registered.", 
                    attribute, this.modid
                ));
        }
    }

    /**
     * @param attribute the attribute key
     * @return {@code true} if the attribute is a vanilla attribute
     */
    public static boolean isVanillaAttribute(String attribute) {
        return VANILLA_ATTRIBUTES.containsKey(attribute);
    }

    /**
     * @param attribute the attribute key
     * @return the vanilla {@link Attribute}, or {@code null} if not found
     */
    public static Attribute getVanillaAttribute(String attribute) {
        return VANILLA_ATTRIBUTES.get(attribute);
    }

    /**
     * Creates living stats for the given living entity.
     *
     * @param living the living entity
     * @param modid  the mod id
     * @param key    the stats identifier
     * @return a new {@link LivingStats} instance bound to the entity
     */
    public static LivingStats create(LivingEntity living, String modid, String key) {
        return new LivingStats(modid, key).injectOwner(living);
    }
}
