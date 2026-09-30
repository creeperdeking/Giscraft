package doc.fasterminecarts;

import java.util.Map;

/**
 * Radial zones are a percentage of the continental diameter. Fifty percent
 * from the center is the coastline, where the ocean is fully ocean.
 */
final class ZoneScale {
    static final int DEFAULT_DIAMETER = 10000;

    static final float BETA_END = 4.5F;
    static final float CONTINENT_END = 15.0F;
    static final float CLIMATE_START = 6.0F;
    static final float CLIMATE_END = 26.0F;
    static final float DRY_START = 20.0F;
    static final float DRY_END = 32.0F;
    static final float COAST_START = 40.0F;
    static final float DEEP_START = 55.0F;
    static final float DEEP_SIZE = 15.36F;
    static final float FROZEN_SIZE = 2.0F;
    static final float ICEBERG_SIZE = 6.0F;
    static final float ICE_SHELF_SIZE = 2.56F;
    static final float BEDROCK_SIZE = 2.56F;

    private ZoneScale() {
    }

    static int blocks(float percent, int diameter) {
        if (percent < 0.0F) {
            percent = 0.0F;
        }
        if (diameter < 2) {
            diameter = 2;
        }
        return (int) Math.round((double) percent / 100.0D * diameter);
    }

    static float percent(int blocks, int diameter) {
        if (diameter < 1) {
            diameter = 1;
        }
        if (blocks < 0) {
            blocks = 0;
        }
        return (float) (Math.round(blocks * 10000.0D / diameter) / 100.0D);
    }

    static void apply(
            int diameter,
            float betaEnd,
            float continentEnd,
            float climateStart,
            float climateEnd,
            float dryStart,
            float dryEnd,
            float coastStart,
            float deepStart,
            float deepSize,
            float frozenSize,
            float icebergSize,
            float iceShelfSize,
            float bedrockSize,
            OceanBoundaryMath.Settings settings) {
        if (diameter < 2) {
            diameter = 2;
        }
        int radius = diameter / 2;
        OceanBoundaryConfig.continentalDiameter = diameter;
        applyClimate(diameter, betaEnd, continentEnd, climateStart, climateEnd, dryStart, dryEnd);

        settings.fullOceanRadius = radius;
        settings.transitionStart = blocks(coastStart, diameter);
        if (settings.transitionStart > radius) {
            settings.transitionStart = radius;
        }
        settings.deepOceanStart = blocks(deepStart, diameter);
        if (settings.deepOceanStart < radius) {
            settings.deepOceanStart = radius;
        }
        settings.iceWallGap = blocks(deepSize, diameter);
        settings.iceSnowLead = blocks(frozenSize, diameter);
        settings.icebergLead = blocks(icebergSize, diameter);
        int shelf = blocks(iceShelfSize, diameter);
        settings.iceShelfLength = shelf < 1 ? 1 : shelf;
        settings.bedrockRun = blocks(bedrockSize, diameter);
    }

    static void applyClimate(
            int diameter,
            float betaEnd,
            float continentEnd,
            float climateStart,
            float climateEnd,
            float dryStart,
            float dryEnd) {
        OceanBoundaryConfig.betaEnd = blocks(betaEnd, diameter);
        OceanBoundaryConfig.continentEnd = blocks(continentEnd, diameter);
        if (OceanBoundaryConfig.continentEnd < OceanBoundaryConfig.betaEnd) {
            OceanBoundaryConfig.continentEnd = OceanBoundaryConfig.betaEnd;
        }
        OceanBoundaryConfig.climateStart = blocks(climateStart, diameter);
        OceanBoundaryConfig.climateEnd = blocks(climateEnd, diameter);
        if (OceanBoundaryConfig.climateEnd < OceanBoundaryConfig.climateStart) {
            OceanBoundaryConfig.climateEnd = OceanBoundaryConfig.climateStart;
        }
        OceanBoundaryConfig.dryStart = blocks(dryStart, diameter);
        OceanBoundaryConfig.dryEnd = blocks(dryEnd, diameter);
        if (OceanBoundaryConfig.dryEnd < OceanBoundaryConfig.dryStart) {
            OceanBoundaryConfig.dryEnd = OceanBoundaryConfig.dryStart;
        }
    }

    static void resolve(Map<String, String> values, OceanBoundaryMath.Settings settings) {
        if (values.containsKey("continentalDiameter")) {
            apply(
                    parseInt(values, "continentalDiameter", DEFAULT_DIAMETER),
                    parseFloat(values, "betaEnd", BETA_END),
                    parseFloat(values, "continentEnd", CONTINENT_END),
                    parseFloat(values, "climateStart", CLIMATE_START),
                    parseFloat(values, "climateEnd", CLIMATE_END),
                    parseFloat(values, "dryStart", DRY_START),
                    parseFloat(values, "dryEnd", DRY_END),
                    parseFloat(values, "coastStart", COAST_START),
                    parseFloat(values, "deepStart", DEEP_START),
                    parseFloat(values, "deepOceanSize", DEEP_SIZE),
                    parseFloat(values, "frozenSize", FROZEN_SIZE),
                    parseFloat(values, "icebergSize", ICEBERG_SIZE),
                    parseFloat(values, "iceShelfSize", ICE_SHELF_SIZE),
                    parseFloat(values, "bedrockSize", BEDROCK_SIZE),
                    settings);
            return;
        }
        settings.transitionStart = parseInt(values, "transitionStart", settings.transitionStart);
        settings.fullOceanRadius = parseInt(values, "fullOceanRadius", settings.fullOceanRadius);
        settings.deepOceanStart = parseInt(values, "deepOceanStart", settings.deepOceanStart);
        settings.iceWallGap = parseInt(values, "iceWallGap", settings.iceWallGap);
        settings.iceSnowLead = parseInt(values, "iceSnowLead", settings.iceSnowLead);
        settings.icebergLead = parseInt(values, "icebergLead", settings.icebergLead);
        settings.iceShelfLength = parseInt(values, "iceShelfLength", settings.iceShelfLength);
        settings.bedrockRun = parseInt(values, "bedrockRun", settings.bedrockRun);
        int diameter = settings.fullOceanRadius * 2;
        if (diameter < 2) {
            diameter = 2;
        }
        OceanBoundaryConfig.continentalDiameter = diameter;
        applyClimate(diameter, BETA_END, CONTINENT_END, CLIMATE_START, CLIMATE_END, DRY_START, DRY_END);
    }

    static void scale(OceanBoundaryMath.Settings settings, int diameter) {
        int previous = settings.fullOceanRadius * 2;
        if (previous < 2) {
            previous = 2;
        }
        if (diameter < 2) {
            diameter = 2;
        }
        double scale = diameter / (double) previous;
        settings.transitionStart = scaleInt(settings.transitionStart, scale);
        settings.fullOceanRadius = diameter / 2;
        settings.deepOceanStart = scaleInt(settings.deepOceanStart, scale);
        if (settings.deepOceanStart < settings.fullOceanRadius) {
            settings.deepOceanStart = settings.fullOceanRadius;
        }
        if (settings.transitionStart > settings.fullOceanRadius) {
            settings.transitionStart = settings.fullOceanRadius;
        }
        settings.iceWallGap = scaleInt(settings.iceWallGap, scale);
        settings.iceSnowLead = scaleInt(settings.iceSnowLead, scale);
        settings.icebergLead = scaleInt(settings.icebergLead, scale);
        int shelf = scaleInt(settings.iceShelfLength, scale);
        settings.iceShelfLength = shelf < 1 ? 1 : shelf;
        settings.bedrockRun = scaleInt(settings.bedrockRun, scale);
        OceanBoundaryConfig.continentalDiameter = diameter;
        OceanBoundaryConfig.betaEnd = scaleInt(OceanBoundaryConfig.betaEnd, scale);
        OceanBoundaryConfig.continentEnd = scaleInt(OceanBoundaryConfig.continentEnd, scale);
        OceanBoundaryConfig.climateStart = scaleInt(OceanBoundaryConfig.climateStart, scale);
        OceanBoundaryConfig.climateEnd = scaleInt(OceanBoundaryConfig.climateEnd, scale);
        OceanBoundaryConfig.dryStart = scaleInt(OceanBoundaryConfig.dryStart, scale);
        OceanBoundaryConfig.dryEnd = scaleInt(OceanBoundaryConfig.dryEnd, scale);
    }

    private static int scaleInt(int value, double scale) {
        if (value < 0) {
            value = 0;
        }
        return (int) Math.round(value * scale);
    }

    private static int parseInt(Map<String, String> values, String key, int fallback) {
        String value = values.get(key);
        return value == null ? fallback : Integer.parseInt(value);
    }

    private static float parseFloat(Map<String, String> values, String key, float fallback) {
        String value = values.get(key);
        return value == null ? fallback : Float.parseFloat(value);
    }
}
