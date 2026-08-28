package software.hacker_E303.pigeon_core.common.gui;

import net.minecraft.world.entity.player.Player;

public final class PressAction {

    /**
     * Thrown by {@link #deny()} to immediately abort the action lambda.
     * Caught by the framework — do not catch it yourself.
     */
    public static final class Abort extends RuntimeException {
        Abort() { super(null, null, true, false); } // lightweight, no stack trace
    }

    private final boolean clientSide;
    private final Player  player;
    private boolean denied = false;

    public PressAction(boolean clientSide, Player player) {
        this.clientSide = clientSide;
        this.player     = player;
    }

    public boolean isClientSide() { return clientSide; }

    public boolean isDenied() { return denied; }

    /**
     * Aborts the action immediately, cancelling any further code in the lambda.
     * Code executed <em>before</em> this call has already run and cannot be rolled back
     * — place {@code deny()} before any side-effects you want to suppress.
     */
    public void deny() {
        denied = true;
        throw new Abort();
    }

    /** Closes the GUI for the player who triggered this action. */
    public void closeInterface() {
        player.closeContainer();
    }
}
