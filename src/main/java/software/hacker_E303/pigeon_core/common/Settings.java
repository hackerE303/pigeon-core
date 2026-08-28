package software.hacker_E303.pigeon_core.common;

import net.minecraft.world.item.Item;
import software.hacker_E303.pigeon_core.PigeonCore;
import software.hacker_E303.pigeon_core.init.PigeUtils;

/**
 * Abstract configuration holder for turret, pigeon-item, and gun client/server settings.
 * <p>
 * Each framework mod provides its own subclass; the core supplies a {@link #DEFAULT}
 * fallback.
 */
public abstract class Settings {

    private String modId = "~none";

    public static Settings init(Settings settings, String modId) {
        settings.modId = modId;
        return settings;
    }

    protected abstract PigeItemServer pigeItemServer();
    protected abstract TurretServer turretServer();
    protected abstract GunClient gunClient();
    protected abstract GunServer gunServer();

    public final Item turretFuelItem() {
        return PigeonCore.getItem(modId, this.turretServer().fuelItem());
    }

    public final Item turretMagazineItem() {
        return PigeonCore.getItem(modId, this.turretServer().magazineItem());
    }

    public final Item turretRepairKitItem() {
        return PigeonCore.getItem(modId, this.turretServer().repairKitItem());
    }

    public final Item turretHealthModuleItem() {
        return PigeonCore.getItem(modId, this.turretServer().healthModuleItem());
    }

    public final Item turretDamageModuleItem() {
        return PigeonCore.getItem(modId, this.turretServer().damageModuleItem());
    }

    public final Item turretUpgradeModuleItem() {
        return PigeonCore.getItem(modId, this.turretServer().upgradeModuleItem());
    }

    public final Item cannedFoodEmptyCanItem() {
        return PigeonCore.getItem(modId, this.pigeItemServer().emptyCanItem());
    }

    public final boolean gunAnimationNoise() {
        return this.gunClient().animationNoise();
    }

    public final boolean gunAmmoBarVisible() {
        return this.gunClient().ammoBarVisible();
    }

    public final boolean gunDurability() {
        return this.gunServer().durability();
    }

    public final boolean gunPushPlayer() {
        return this.gunServer().pushPlayer();
    }

    protected static class TurretServer {

        public String fuelItem() {
            return "~none";
        }

        public String magazineItem() {
            return "~none";
        }

        public String repairKitItem() {
            return "~none";
        }

        public String healthModuleItem() {
            return "~none";
        }

        public String damageModuleItem() {
            return "~none";
        }

        public String upgradeModuleItem() {
            return "~none";
        }
    }

    protected static class PigeItemServer {

        public String emptyCanItem() {
            return "~none";
        }
    }

    protected static class GunClient {

        public boolean animationNoise() {
            return true;
        }

        public boolean ammoBarVisible() {
            return true;
        }
    }

    protected static class GunServer {

        public boolean durability() {
            return false;
        };

        public boolean pushPlayer() {
            return false;
        };
    }

    public static final Settings DEFAULT = new Settings() {

        @Override
        protected PigeItemServer pigeItemServer() {
            return new PigeItemServer();
        }

        @Override
        protected TurretServer turretServer() {
            return new TurretServer();
        }

        @Override
        protected GunClient gunClient() {
            return new GunClient();
        }

        @Override
        protected GunServer gunServer() {
            return new GunServer();
        }
    };

    public static Settings from(Object obj) {
        return PigeonCore.settingsFrom(PigeUtils.modidFrom(obj));
    }
}