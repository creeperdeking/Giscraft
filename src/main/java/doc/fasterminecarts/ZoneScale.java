package doc.fasterminecarts;

/**
 * Biome zones are a percentage of the continental radius. That radius is the
 * coastline, {@code fullOceanRadius}. One hundred percent is the shore of the
 * main landmass. The ocean, ice, and void beyond it are not part of this scale.
 */
final class ZoneScale {
    static final float BETA_END = 9.0F;
    static final float CONTINENT_END = 30.0F;
    static final float CLIMATE_START = 12.0F;
    static final float CLIMATE_END = 52.0F;
    static final float DRY_START = 40.0F;
    static final float DRY_END = 64.0F;

    private ZoneScale() {
    }

    static int blocks(float percent, int radius) {
        if (percent < 0.0F) {
            percent = 0.0F;
        }
        if (percent > 100.0F) {
            percent = 100.0F;
        }
        if (radius < 1) {
            radius = 1;
        }
        return (int) Math.round((double) percent / 100.0D * radius);
    }

    static void resolve(int radius) {
        if (radius < 1) {
            radius = 1;
        }
        float betaEnd = OceanBoundaryConfig.betaEndPercent;
        float continentEnd = OceanBoundaryConfig.continentEndPercent;
        float climateStart = OceanBoundaryConfig.climateStartPercent;
        float climateEnd = OceanBoundaryConfig.climateEndPercent;
        float dryStart = OceanBoundaryConfig.dryStartPercent;
        float dryEnd = OceanBoundaryConfig.dryEndPercent;
        if (continentEnd < betaEnd) {
            continentEnd = betaEnd;
            OceanBoundaryConfig.continentEndPercent = continentEnd;
        }
        if (climateEnd < climateStart) {
            climateEnd = climateStart;
            OceanBoundaryConfig.climateEndPercent = climateEnd;
        }
        if (dryEnd < dryStart) {
            dryEnd = dryStart;
            OceanBoundaryConfig.dryEndPercent = dryEnd;
        }
        OceanBoundaryConfig.continentalRadius = radius;
        OceanBoundaryConfig.betaEnd = blocks(betaEnd, radius);
        OceanBoundaryConfig.continentEnd = blocks(continentEnd, radius);
        OceanBoundaryConfig.climateStart = blocks(climateStart, radius);
        OceanBoundaryConfig.climateEnd = blocks(climateEnd, radius);
        OceanBoundaryConfig.dryStart = blocks(dryStart, radius);
        OceanBoundaryConfig.dryEnd = blocks(dryEnd, radius);
    }
}
