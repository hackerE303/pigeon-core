package software.hacker_E303.pigeon_core.client.gui;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.hacker_E303.pigeon_core.util.locator.Location;
import software.hacker_E303.pigeon_core.util.locator.Path;

/**
 * ResourceLocation constants for all config GUI textures.
 * Path: assets/pigeon_core/textures/guis/configs/<name>.png
 *
 * Every interactive element has two textures: normal and _enabled (hover).
 * Textures must be exactly the size listed — they are blitted 1:1 (no scaling).
 *
 *  back / back_enabled          16×16   — breadcrumb back button
 *  folder                       16×16   — static folder icon (no hover variant)
 *  button_delete / _enabled     18×18   — × delete in list editor
 *  button_add    / _enabled     18×18   — + add in list editor
 *  button       / button_enabled 100×20 — Save, Cancel, Done, Edit List buttons
 *  field        / field_enabled  40×18  — text fields and boolean toggles
 */
@OnlyIn(Dist.CLIENT)
final class PigeConfigTextures {

    static final ResourceLocation ICON_BACK             = tex("back");                  // 16×16
    static final ResourceLocation ICON_BACK_ENABLED     = tex("back_enabled");          // 16×16
    static final ResourceLocation ICON_FOLDER           = tex("folder");                // 16×16
    static final ResourceLocation BTN_DELETE            = tex("trash");                 // 18×18
    static final ResourceLocation BTN_DELETE_ENABLED    = tex("trash_enabled");         // 18×18
    static final ResourceLocation BTN_ADD               = tex("add");                   // 18×18
    static final ResourceLocation BTN_ADD_ENABLED       = tex("add_enabled");           // 18×18
    static final ResourceLocation BTN_WIDE              = texV2("button");              // 100×20
    static final ResourceLocation BTN_WIDE_ENABLED      = texV2("button_enabled");      // 100×20
    static final ResourceLocation LIST_BTW              = texV2("list_button");         // 80×16
    static final ResourceLocation LIST_BTN_ENABLED      = texV2("list_button_enabled"); // 80×16
    static final ResourceLocation FIELD_X1              = texV2("field_x1");           // 40×16
    static final ResourceLocation FIELD_ENABLED_X1      = texV2("field_enabled_x1");   // 40×16
    static final ResourceLocation FIELD_X2              = texV2("field_x2");           // 60×16
    static final ResourceLocation FIELD_ENABLED_X2      = texV2("field_enabled_x2");   // 60×16
    static final ResourceLocation FIELD_X4              = texV2("field_x4");           // 80×16
    static final ResourceLocation FIELD_ENABLED_X4      = texV2("field_enabled_x4");   // 80×16

    private static ResourceLocation tex(String name) {
        return Location.create(Path.create("textures/guis/configs/", ".png"), name).from("pigeon_core");
    }

    private static ResourceLocation texV2(String name) {
        return Location.create(Path.create("textures/guis/buttons/", ".png"), name).from("pigeon_core");
    }

    private PigeConfigTextures() {}
}
