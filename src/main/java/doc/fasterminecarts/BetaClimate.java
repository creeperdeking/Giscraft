package doc.fasterminecarts;

/**
 * Beta 1.7.3 terrain stays as generated inside a radius of the world origin,
 * aside from the inland sea and the rivers that leave it. That origin is
 * spawn. Shifting the continent center does not move this radius. Farther
 * out the landmasses widen, north rises into cold plateaus, and south lowers
 * into hot dunes. Humidity follows beta's own chart: wet near the center
 * latitude, dry toward the far north and south.
 */
public final class BetaClimate {
    private static long seed;

    private BetaClimate() {
    }

    static void setSeed(long worldSeed) {
        seed = worldSeed;
        InlandWater.bind(worldSeed);
    }

    public static double temperature(int x, int z, double base) {
        if (pureBeta(x, z)) {
            return clamp01(base);
        }
        int latitude = latitude(x, z);
        double north = northOf(latitude);
        double south = southOf(latitude);
        double temp = base;
        temp += (0.18D - temp) * north;
        temp += (1.0D - temp) * south;
        return clamp01(temp);
    }

    public static double humidity(int x, int z, double base) {
        if (pureBeta(x, z)) {
            return clamp01(base);
        }
        double dry = dryness(latitude(x, z));
        double wet = base * 0.25D + 0.82D;
        if (wet > 1.0D) {
            wet = 1.0D;
        }
        return clamp01(wet + (0.08D - wet) * dry);
    }

    public static double shapeDepth(int x, int z, double depth) {
        return InlandWater.carveDepth(x, z, continentalDepth(x, z, depth));
    }

    public static double shapeHeight(int x, int z, double center) {
        return InlandWater.carveHeight(x, z, continentalHeight(x, z, center));
    }

    private static double continentalDepth(int x, int z, double depth) {
        if (pureBeta(x, z)) {
            return depth;
        }
        double continental = continental(x, z);
        double north = northOf(z);
        double south = southOf(z);
        if (continental <= 0.0D && north <= 0.0D && south <= 0.0D) {
            return depth;
        }
        double broad = OceanBoundaryMath.noise(x * 0.00028D, z * 0.00028D, seed ^ 0xC0A71L);
        double gulf = OceanBoundaryMath.noise(x * 0.0009D, z * 0.0009D, seed ^ 0x61F1L);
        double mask = broad * 0.72D + gulf * 0.28D + 0.12D;
        double shaped = depth * (1.0D - 0.8D * continental);
        shaped += mask * 2.4D * continental;
        shaped += north * 1.6D;
        shaped += south * 0.35D;
        return shaped;
    }

    private static double continentalHeight(int x, int z, double center) {
        if (pureBeta(x, z)) {
            return center;
        }
        double north = northOf(z);
        double south = southOf(z);
        if (north <= 0.0D && south <= 0.0D) {
            return center;
        }
        double plateau = OceanBoundaryMath.noise(x * 0.00065D, z * 0.00065D, seed ^ 0x51A7L) * 0.5D + 0.5D;
        double ridge = Math.abs(OceanBoundaryMath.noise(x * 0.0024D, z * 0.0024D, seed ^ 0xA31L));
        double peak = Math.abs(OceanBoundaryMath.noise(x * 0.006D, z * 0.006D, seed ^ 0x17CL));
        double lift = 1.2D + plateau * 2.2D + ridge * 2.0D + peak * peak * 1.6D;
        double dune = OceanBoundaryMath.noise(x * 0.0018D, z * 0.0065D, seed ^ 0xD11EL);
        return center + north * lift + south * dune * 1.8D;
    }

    /**
     * True inside the pure beta radius. That circle is centered on the world
     * origin. When the continent center is the origin too, the older path in
     * {@link #continental(int, int)} already protects the same circle.
     */
    private static boolean pureBeta(int x, int z) {
        if (OceanBoundaryConfig.centerX == 0 && OceanBoundaryConfig.centerZ == 0) {
            return false;
        }
        double radius = OceanBoundaryConfig.betaEnd;
        return (double) x * x + (double) z * z <= radius * radius;
    }

    private static double continental(int x, int z) {
        double dx = x - OceanBoundaryConfig.centerX;
        double dz = z - OceanBoundaryConfig.centerZ;
        int inner = OceanBoundaryConfig.betaEnd;
        if (OceanBoundaryConfig.centerX != 0 || OceanBoundaryConfig.centerZ != 0) {
            inner = 0;
        }
        return smooth(Math.sqrt(dx * dx + dz * dz), inner, OceanBoundaryConfig.continentEnd);
    }

    private static int latitude(int x, int z) {
        double wander = OceanBoundaryMath.noise(x * 0.002D, z * 0.002D, seed ^ 0xB10CL);
        return z + (int) Math.round(wander * 180.0D);
    }

    private static double northOf(int z) {
        return smooth(OceanBoundaryConfig.centerZ - z, OceanBoundaryConfig.climateStart, OceanBoundaryConfig.climateEnd);
    }

    private static double southOf(int z) {
        return smooth(z - OceanBoundaryConfig.centerZ, OceanBoundaryConfig.climateStart, OceanBoundaryConfig.climateEnd);
    }

    private static double dryness(int z) {
        double dz = z - OceanBoundaryConfig.centerZ;
        if (dz < 0.0D) {
            dz = -dz;
        }
        return smooth(dz, OceanBoundaryConfig.dryStart, OceanBoundaryConfig.dryEnd);
    }

    private static double smooth(double value, double start, double end) {
        if (value <= start) {
            return 0.0D;
        }
        if (value >= end) {
            return 1.0D;
        }
        double t = (value - start) / (end - start);
        return t * t * (3.0D - 2.0D * t);
    }

    private static double clamp01(double value) {
        if (value < 0.0D) {
            return 0.0D;
        }
        if (value > 1.0D) {
            return 1.0D;
        }
        return value;
    }
}
