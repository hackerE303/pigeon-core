package software.hacker_E303.pigeon_core.util;

import java.util.Random;

/**
 * Utility class providing enhanced mathematical operations for the Pige Tech Weapons mod.
 * Includes Perlin noise generation, easing functions, interpolation, damping, and random number generation.
 * This class is used extensively for creating smooth animations, realistic movements, and random effects.
 */
public class BetterMath {

    /** Permutation table for Perlin noise generation */
    private static final int[] perm = new int[]{
        151,160,137,91,90,15,131,13,201,95,96,53,194,233,7,225,140,36,103,30,69,142,8,99,37,240,21,10,23,
        190,6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,35,11,32,57,177,33,88,237,149,56,87,174,20,
        125,136,171,168,68,175,74,165,71,134,139,48,27,166,77,146,158,231,83,111,229,122,60,211,133,230,220,
        105,92,41,55,46,245,40,244,102,143,54,65,25,63,161,1,216,80,73,209,76,132,187,208,89,18,169,200,196,
        135,130,116,188,159,86,164,100,109,198,173,186,3,64,52,217,226,250,124,123,5,202,38,147,118,126,255,
        82,85,212,207,206,59,227,47,16,58,17,182,189,28,42,223,183,170,213,119,248,152,2,44,154,163,70,221,
        153,101,155,167,43,172,9,129,22,39,253,19,98,108,110,79,113,224,232,178,185,112,104,218,246,97,228,
        251,34,242,193,238,210,144,12,191,179,162,241,81,51,145,235,249,14,239,107,49,192,214,31,181,199,106,
        157,184,84,204,176,115,121,50,45,127,4,150,254,138,236,205,93,222,114,67,29,24,72,243,141,128,195,78,
        66,215,61,156,180
    };

    /** Doubled permutation table for Perlin noise generation (to avoid index wrapping) */
    private static final int p[] = new int[512];

    /** Random number generator instance */
    public static final Random rand = new Random();

    static {
        for (int i = 0; i < 256; i++) {
            p[256 + i] = p[i] = perm[i];
        }
    }

    /**
     * Gradient function for Perlin noise generation.
     * @param hash Hash value for gradient selection
     * @param x X coordinate
     * @param y Y coordinate
     * @return Gradient value
     */
    private static float grad(int hash, float x, float y) {
        int h = hash & 15;
        float u = h < 8 ? x : y;
        float v = h < 4 ? y : h == 12|| h == 14 ? x : 0;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    /**
     * Generates 2D Perlin noise value at specified coordinates.
     * @param x X coordinate
     * @param y Y coordinate
     * @return Perlin noise value between -1 and 1
     */
    public static float noise(float x, float y) {

        float scale = 3.6f;
        x *= scale;
        y *= scale;

        int X = (int)Math.floor(x) & 255;
        int Y = (int)Math.floor(y) & 255;
        x -= Math.floor(x);
        y -= Math.floor(y);
        float u = fade(x);
        float v = fade(y);
        int A = p[X] + Y, AA = p[A], AB = p[A + 1];
        int B = p[X + 1]+Y, BA = p[B], BB = p[B + 1];

        return lerp(v, lerp(u, grad(p[AA], x, y), grad(p[BA], x - 1, y)), lerp(u, grad(p[AB], x, y - 1), grad(p[BB], x - 1, y - 1)));
    }

    /**
     * Dampens a value towards a target value using exponential easing.
     * @param current Current value
     * @param target Target value
     * @param lambda Damping coefficient
     * @param dt Delta time in seconds
     * @return Dampened value
     */
    public static float damp(float current, float target, float lambda, float dt) {
        return current + (target - current) * (1f - (float) Math.exp(-lambda * dt));
    }

    /**
     * Fade function for Perlin noise interpolation (6t^5 - 15t^4 + 10t^3).
     * @param t Interpolation parameter between 0 and 1
     * @return Faded value
     */
    private static float fade(float t) {
		return t * t * t * (t * (t * 6 - 15) + 10);
	}

    /**
     * Linear interpolation function.
     * @param t Interpolation parameter between 0 and 1
     * @param a Start value
     * @param b End value
     * @return Interpolated value
     */
    private static float lerp(float t, float a, float b) {
		return a + t * (b - a);
	}

    /**
     * Linear interpolation function (alias for lerp).
     * @param start Start value
     * @param end End value
     * @param t Interpolation parameter between 0 and 1
     * @return Interpolated value
     */
	public static float interpolate(float start, float end, float t) {
    	return start + (end - start) * t;
	}

    /**
     * Ease in out quadratic function for smooth animations.
     * @param t Time parameter between 0 and 1
     * @return Eased value
     */
	public static float easeInOutQuad(float t) {
		return t < 0.5f ? 2 * t * t : -1 + (4 - 2 * t) * t;
	}

    /**
     * Ease in out cubic function for smooth animations.
     * @param t Time parameter between 0 and 1
     * @return Eased value
     */
	public static float easeInOutCubic(float t) {
		return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
	}

    /**
     * Ease in out quartic function for smooth animations.
     * @param t Time parameter between 0 and 1
     * @return Eased value
     */
	public static float easeInOutQuart(float t) {
		return t < 0.5f ? 8 * t * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 4) / 2;
	}
	
    /**
     * Ease in out quintic function for smooth animations.
     * @param t Time parameter between 0 and 1
     * @return Eased value
     */
	public static float easeInOutQuint(float t) {
		return t < 0.5f ? 16 * t * t * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 5) / 2;
	}

    /**
     * Generates a random float between two values.
     * @param min Minimum value
     * @param max Maximum value
     * @return Random float between min and max
     */
    public static float randomFrom(float min, float max) {
        return min + rand.nextFloat() * (max - min);
    }

    /**
     * Generates a random rotation angle in radians.
     * @return Random rotation between -90 and 90 degrees (in radians)
     */
    public static float randomRotation() {
        return (rand.nextFloat() * 180.0f - 90.0f) * 0.017453292f;
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    public static float decimalCut(float value, int decimals) {
        float factor = (float) Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}