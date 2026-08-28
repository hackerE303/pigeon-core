package software.hacker_E303.pigeon_core.test;

import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import software.hacker_E303.pigeon_core.common.PigeConfig;
import software.hacker_E303.pigeon_core.common.config.ConfigContext;
import software.hacker_E303.pigeon_core.main.AutoRegister;

/**
 * Test config for pigeon_core.
 * Open Mods → pigeon_core → CONFIG to verify the GUI.
 *
 * Description tooltips are seeded as empty strings in en_us.json by the framework
 * (key format: config.<modid>.<id>.description). Fill them in manually.
 */
@AutoRegister("pigeon_config")
public class PigeonConfig extends PigeConfig {

    @Override
    public void build(ConfigContext ctx) {

        ctx.server(f -> {
            f.add("max_turrets",     Integer.class, 5,    null,  null);
            f.add("reload_delay_ms", Float.class, 50.0f, 50.0f, 5000.0f);

            f.folder("combat", sub -> {
                sub.add("damage_mult", Double.class,  1.0, 0.1, 10.0);
                sub.add("pvp_damage",  Boolean.class, true, null, null);
                sub.add("armor_pen",   Integer.class, 0,    0,  100);
                sub.add("fuel_item",   Item.class,    Items.COAL, null, null);

                sub.folder("explosions", exp -> {
                    exp.add("blast_radius", Double.class,  4.0, 0.5, 20.0);
                    exp.add("chain_react",  Boolean.class, false, null, null);
                });
            });

            f.addList("banned_items", Item.class, List.of(Items.TNT, Items.BEDROCK));
        });

        ctx.client(f -> {
            f.add("show_ammo_bar", Boolean.class, true, null, null);
            f.add("hud_scale",     Double.class,  1.0,  0.5, 2.0);

            f.folder("animations", sub -> {
                sub.add("enable_animations", Boolean.class, true,  null, null);
                sub.add("anim_speed",        Double.class,  1.0,   0.25, 3.0);
                sub.add("debug_overlay",     Boolean.class, false, null, null);
            });
        });

        ctx.common(f -> {
            f.add("server_tag",   String.class,  "PigeonServer", null, null);
            f.add("enable_debug", Boolean.class, false, null, null);
            f.addList("whitelist_biomes", String.class,
                      List.of("minecraft:plains", "minecraft:forest"));
        });
    } 
}