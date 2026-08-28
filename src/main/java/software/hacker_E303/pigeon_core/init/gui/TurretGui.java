package software.hacker_E303.pigeon_core.init.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.hacker_E303.pigeon_core.common.PigeGui;
import software.hacker_E303.pigeon_core.common.gui.GuiContext;
import software.hacker_E303.pigeon_core.common.gui.LayoutBounds;
import software.hacker_E303.pigeon_core.entity.ETurret;
import software.hacker_E303.pigeon_core.main.AutoRegister;
import software.hacker_E303.pigeon_core.util.locator.Location;
import software.hacker_E303.pigeon_core.util.locator.Path;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

@AutoRegister("turret_gui")
public final class TurretGui extends PigeGui {

    private static final Location NONE = Location.create(Path.TEXTURE.MISC, "none");

    private static final int    MAX_NAME_LENGTH = 22;
    private static final String DEFAULT_NAME    = "Manual Target Selection";

    /** Current page — 0: stats, 1: name search. Tracked client- and server-side. */
    private int page = 0;

    @Override
    public boolean hasReceiverInventory() { return false; }

    @Override
    public Background getBackground(LayoutBounds bounds) {
        return Background.create(LayoutBounds.create(-1, -1, 234, 148), false);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Location screen(String name) { return Location.create(Path.TEXTURE.GUI, name); }

    private void prevPage() { if (page > 0) page--; }
    private void nextPage() { if (page < 1) page++; }

    private static void appendChar(ETurret t, char c) {
        String cur  = t.getNameTarget();
        String base = (cur.equals(DEFAULT_NAME) || cur.isEmpty()) ? "" : cur;
        if (base.length() < MAX_NAME_LENGTH)
            t.setNameTarget(base + c);
    }

    private static void backspaceName(ETurret t) {
        String cur = t.getNameTarget();
        if (!cur.equals(DEFAULT_NAME) && !cur.isEmpty())
            t.setNameTarget(cur.substring(0, cur.length() - 1));
    }

    private static void resetName(ETurret t) {
        if (!t.getNameTarget().equals(DEFAULT_NAME)) t.setNameTarget(DEFAULT_NAME);
    }

    private static void findTarget(ETurret turret) {
        if (turret.level().isClientSide() || turret.isAlwaysTarget()) return;
        String name = turret.getNameTarget();
        if (name.equals(DEFAULT_NAME) || name.isEmpty()) return;
        Vec3 center = turret.position();
        double range = turret.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
        List<Entity> nearby = turret.level()
                .getEntitiesOfClass(Entity.class, AABB.ofSize(center, range * 2, range * 2, range * 2), e -> true)
                .stream()
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(center)))
                .toList();
        for (Entity e : nearby) {
            if (e.getDisplayName().getString().equalsIgnoreCase(name)
                    && e instanceof net.minecraft.world.entity.LivingEntity living) {
                turret.setTarget(living);
                break;
            }
        }
    }

    // ── Keyboard input ────────────────────────────────────────────────────────

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean onCharTyped(char c) {
        return page != 1; // consume (forward to server) only on page 1
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (page != 1) return true;
        return keyCode != 259; // consume Backspace (GLFW 259) on page 1
    }

    @Override
    public void handleCharInput(char c, int keyCode, @Nullable Entity sender, Player player) {
        if (page != 1 || !(sender instanceof ETurret turret)) return;
        if (c != 0) {
            appendChar(turret, c);
        } else if (keyCode == 259) { // GLFW_KEY_BACKSPACE
            backspaceName(turret);
        }
    }

    // ── renderInterface ───────────────────────────────────────────────────────

    @Override
    public void renderInterface(GuiContext ctx, Player receiver, Entity sender) {
        if (!(sender instanceof ETurret turret)) return;

        boolean page0 = page == 0;
        boolean page1 = page == 1;

        // ── Always: main background + side panels ────────────────────────────
        ctx.renderImage(screen("turret_general_gui1"),        LayoutBounds.create(29,   0), () -> true);
        ctx.renderImage(screen("turret_general_gui3"),        LayoutBounds.create(0,    0), () -> true);
        ctx.renderImage(screen("turret_general_gui3"),        LayoutBounds.create(208,  0), () -> true);
        ctx.renderImage(screen("damage"),                     LayoutBounds.create(5,   10), () -> true);
        ctx.renderImage(screen("health"),                     LayoutBounds.create(213, 10), () -> true);

        // ── Always: navigation bar ────────────────────────────────────────────
        ctx.renderImage(screen("turret_general_gui4"), LayoutBounds.create(-10, 123), () -> true);
        ctx.renderImage(screen("turret_general_gui4"), LayoutBounds.create(198, 123), () -> true);
        ctx.renderImage(screen("page_left"),      LayoutBounds.create(8,   127), () -> true);
        ctx.renderImage(screen("page_right"),     LayoutBounds.create(216, 127), () -> true);

        // ── Always: target-type area ──────────────────────────────────────────
        ctx.renderImage(screen("slide1_turrets_gui"), LayoutBounds.create(29,  123), () -> true);
        ctx.renderImage(screen("checked"),            LayoutBounds.create(39,  127), turret::canTargetPlayers);
        ctx.renderImage(screen("checked"),            LayoutBounds.create(61,  127), turret::canTargetAnimals);
        ctx.renderImage(screen("checked"),            LayoutBounds.create(83,  127), turret::canTargetMonsters);
        ctx.renderImage(screen("checked"),            LayoutBounds.create(105, 127), turret::canTargetAttackers);

        // ── Always: lock and power ────────────────────────────────────────────
        ctx.renderImage(screen("turret_general_gui2"), LayoutBounds.create(169, 123), () -> true);
        ctx.renderImage(screen("turret_general_gui2"), LayoutBounds.create(129, 123), () -> true);
        ctx.renderImage(screen("lock1"),  LayoutBounds.create(182, 127), turret::isAccessible);
        ctx.renderImage(screen("lock2"),  LayoutBounds.create(182, 127), () -> !turret.isAccessible());
        ctx.renderImage(screen("power1"), LayoutBounds.create(142, 127), turret::isPowered);
        ctx.renderImage(screen("power2"), LayoutBounds.create(142, 127), () -> !turret.isPowered());

        // ── Page 0: damage/health modules ────────────────────────────────────
        int dmgModules = turret.getDamageModules();
        int hpModules  = turret.getHealthModules();
        for (int i = 0; i < dmgModules; i++) {
            final int row = i;
            ctx.renderImage(screen("damage_module"), LayoutBounds.create(5,   34 + 16 * row), () -> page0);
        }
        for (int i = 0; i < hpModules; i++) {
            final int row = i;
            ctx.renderImage(screen("health_module"), LayoutBounds.create(213, 34 + 16 * row), () -> page0);
        }

        // ── Page 0: stat icons ────────────────────────────────────────────────
        ctx.renderImage(screen("power_img"),    LayoutBounds.create(179, 25), () -> page0);
        ctx.renderImage(screen("ammo_img"),     LayoutBounds.create(179, 59), () -> page0);
        ctx.renderImage(screen("health_img"),   LayoutBounds.create(179, 92), () -> page0);

        // ── Page 0: bars (energy / ammo / health) — 0–9 indexed ──────────────
        for (int i = 0; i <= 9; i++) {
            final int idx = i;
            ctx.renderImage(screen("bar" + i), LayoutBounds.create(37, 31), () -> page0 && turret.getBarFuel()   == idx);
            ctx.renderImage(screen("bar" + i), LayoutBounds.create(37, 65), () -> page0 && turret.getBarAmmo()   == idx);
            ctx.renderImage(screen("bar" + i), LayoutBounds.create(37, 98), () -> page0 && turret.getBarHealth() == idx);
        }

        // ── Page 1: search area ───────────────────────────────────────────────
        ctx.renderImage(screen("text_imput_slide3"), LayoutBounds.create(36,  24), () -> page1);
        ctx.renderImage(screen("find_button"),       LayoutBounds.create(175, 26), () -> page1);
        ctx.renderImage(screen("reset"),             LayoutBounds.create(189, 104), () -> page1);
        ctx.renderImage(screen("backspace"),         LayoutBounds.create(171, 104), () -> page1);
        ctx.renderImage(screen("always_attack"), LayoutBounds.create(36, 104), () -> page1 &&  turret.isAlwaysTarget());
        ctx.renderImage(screen("attack_once"),   LayoutBounds.create(36, 104), () -> page1 && !turret.isAlwaysTarget());

        // ── Page 1: name display (placeholder §3 / typed text §7) ────────────
        String rawName   = turret.getNameTarget();
        boolean isDefault = rawName.equals(DEFAULT_NAME) || rawName.isEmpty();
        String placeholder = Component.translatable("gui.pigeon_core.turret_gui.placeholder_name").getString();
        String nameDisplay = isDefault ? "§8" + placeholder : "§7" + rawName;
        ctx.renderLiteralText(nameDisplay, LayoutBounds.create(40, 30), () -> page1);

        // ── Always: owner and level (§8, pulled from lang) ───────────────────
        String ownerLabel = Component.translatable("gui.pigeon_core.turret_gui.label_owner").getString();
        String levelLabel = Component.translatable("gui.pigeon_core.turret_gui.label_level").getString();
        ctx.renderLiteralText(ownerLabel + turret.getOwnerName(), LayoutBounds.create(37,  8), () -> true);
        ctx.renderLiteralText(levelLabel + turret.getLevel(),     LayoutBounds.create(153, 8), () -> true);

        // ── Tooltips ──────────────────────────────────────────────────────────
        ctx.renderTooltip("gui.pigeon_core.turret_gui.tooltip_players",
            LayoutBounds.create(31,  120, 24, 24), () -> true);
        ctx.renderTooltip("gui.pigeon_core.turret_gui.tooltip_creatures",
            LayoutBounds.create(54,  120, 24, 24), () -> true);
        ctx.renderTooltip("gui.pigeon_core.turret_gui.tooltip_monsters",
            LayoutBounds.create(77,  120, 24, 24), () -> true);
        ctx.renderTooltip("gui.pigeon_core.turret_gui.tooltip_attacker_entitys",
            LayoutBounds.create(100, 120, 24, 24), () -> true);
        ctx.renderTooltip("gui.pigeon_core.turret_gui.tooltip_public",
            LayoutBounds.create(175, 120, 24, 24), turret::isAccessible);
        ctx.renderTooltip("gui.pigeon_core.turret_gui.tooltip_private",
            LayoutBounds.create(175, 120, 24, 24), () -> !turret.isAccessible());
        ctx.renderTooltip("gui.pigeon_core.turret_gui.tooltip_power_on",
            LayoutBounds.create(135, 120, 24, 24), turret::isPowered);
        ctx.renderTooltip("gui.pigeon_core.turret_gui.tooltip_power_off",
            LayoutBounds.create(135, 120, 24, 24), () -> !turret.isPowered());
        ctx.renderTooltip("gui.pigeon_core.turret_gui.tooltip_damage_modules",
            LayoutBounds.create(1,   6,   24, 24), () -> true);
        ctx.renderTooltip("gui.pigeon_core.turret_gui.tooltip_health_modules",
            LayoutBounds.create(209, 6,   24, 24), () -> true);
        ctx.renderTooltip("gui.pigeon_core.turret_gui.tooltip_next_page",
            LayoutBounds.create(209, 120, 24, 24), () -> true);
        ctx.renderTooltip("gui.pigeon_core.turret_gui.tooltip_previous_page",
            LayoutBounds.create(1,   120, 24, 24), () -> true);
        ctx.renderTooltip("gui.pigeon_core.turret_gui.tooltip_energy",
            LayoutBounds.create(175, 21, 24, 24), () -> page0);
        ctx.renderTooltip("gui.pigeon_core.turret_gui.tooltip_ammo",
            LayoutBounds.create(175, 55, 24, 24), () -> page0);
        ctx.renderTooltip("gui.pigeon_core.turret_gui.tooltip_health",
            LayoutBounds.create(175, 88, 24, 24), () -> page0);
        ctx.renderTooltip("gui.pigeon_core.turret_gui.tooltip_reset",
            LayoutBounds.create(185, 95, 24, 24), () -> page1);
        ctx.renderTooltip("gui.pigeon_core.turret_gui.tooltip_backspace",
            LayoutBounds.create(162, 95, 24, 24), () -> page1);
        ctx.renderTooltip("gui.pigeon_core.turret_gui.tooltip_always_attack",
            LayoutBounds.create(29, 96, 24, 24), () -> page1 && turret.isAlwaysTarget());
        ctx.renderTooltip("gui.pigeon_core.turret_gui.tooltip_attack_once",
            LayoutBounds.create(29, 96, 24, 24), () -> page1 && !turret.isAlwaysTarget());

        // ── Buttons: target toggles ───────────────────────────────────────────
        ctx.renderButton("", NONE,  LayoutBounds.create(36,  124, 16, 16),
            a -> turret.switchValueOf(ETurret.Data.TARGET_PLAYERS),   () -> true);
        ctx.renderButton("", NONE, LayoutBounds.create(58,  124, 16, 16),
            a -> turret.switchValueOf(ETurret.Data.TARGET_ANIMALS),   () -> true);
        ctx.renderButton("", NONE, LayoutBounds.create(80,  124, 16, 16),
            a -> turret.switchValueOf(ETurret.Data.TARGET_MONSTERS),  () -> true);
        ctx.renderButton("", NONE, LayoutBounds.create(102, 124, 16, 16),
            a -> turret.switchValueOf(ETurret.Data.TARGET_ATTACKERS), () -> true);
        ctx.renderButton("", NONE, LayoutBounds.create(179, 124, 16, 16),
            a -> turret.switchValueOf(ETurret.Data.ACESSIBILITY),     () -> true);
        ctx.renderButton("", NONE, LayoutBounds.create(139, 124, 16, 16),
            a -> turret.switchValueOf(ETurret.Data.ALIMENTATION),     () -> true);

        // ── Buttons: page navigation ──────────────────────────────────────────
        ctx.renderButton("", NONE, LayoutBounds.create(5,   124, 16, 16),
            a -> prevPage(), () -> true);
        ctx.renderButton("", NONE,       LayoutBounds.create(213, 124, 16, 16),
            a -> nextPage(), () -> true);

        // ── Buttons: page 1 action keys ──────────────────────────────────────
        ctx.renderButton("", NONE, LayoutBounds.create(186, 101, 16, 16),
            a -> { if (!a.isClientSide()) resetName(turret); }, () -> page1);
        ctx.renderButton("", NONE, LayoutBounds.create(168, 101, 16, 16),
            a -> { if (!a.isClientSide()) backspaceName(turret); }, () -> page1);
        ctx.renderButton("", NONE, LayoutBounds.create(33, 101, 16, 16),
            a -> { if (!a.isClientSide()) turret.setAlwaysTarget(!turret.isAlwaysTarget()); }, () -> page1);
        ctx.renderButton("", screen("find_button"), LayoutBounds.create(175, 26, 24, 16),
            a -> { if (!a.isClientSide()) findTarget(turret); }, () -> page1);
    }
}