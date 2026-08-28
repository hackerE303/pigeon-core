package software.hacker_E303.pigeon_core.common;

import java.util.function.Consumer;

import net.minecraft.world.item.Item;
import software.hacker_E303.pigeon_core.PigeonCore;

/**
 * Declares a creative-mode tab populated with registered items.
 */
public final class Tab {

    public static final Tab EMPTY = new Tab("empty");
         
    private final String ID;
    private final String[] KEYS;

    private String icon = null;
    private String modid = "~none";

    private boolean creative = false;

    private Tab(String id, String... keys) {
        this.ID = id;
        this.KEYS = keys;
    }

    public String getId() {
        return this.ID;
    }

    public String getIcon() {
        return this.icon;
    }

    public Tab setModid(String modid) {
        this.modid = modid;
        return this;
    }

    public static Tab create(String id, String... keys) {

        Tab tab = new Tab(id, keys);
        return tab;
    }

    public Tab isCreative(String icon) {
        this.icon = icon;
        this.creative = true;
        return this;
    }

    public boolean isCreative() {
        return this.creative;
    }

    public boolean hasKey(String key) {
        for (String k : KEYS) if (k.equals(key)) return true;
        return false;
    }

    public void forEach(Consumer<Item> action) {
        for (String key : KEYS) {

            Item item = PigeonCore.getItem(modid, key);
            if (item != null) action.accept(item);
        }
    }
}