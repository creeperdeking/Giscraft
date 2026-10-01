package doc.fasterminecarts;

import java.util.ArrayList;
import java.util.Arrays;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;

final class OceanBoundaryConfig {
    static boolean enabled = true;
    static int centerX;
    static int centerZ;
    static final int DEFAULT_LAND = 4000;
    static final int DEFAULT_FADE = 1000;
    static final int DEFAULT_SHALLOW = 500;
    static final int DEFAULT_DEEP = 1536;
    static final int DEFAULT_ICE = 256;
    static final int DEFAULT_FLOWER = 256;
    static final int DEFAULT_FROZEN = 200;
    static final int DEFAULT_ICEBERG = 600;
    static final int DEFAULT_PYRAMID = 768;

    static int landRadius = DEFAULT_LAND;
    static int coastFade = DEFAULT_FADE;
    static int shallowOcean = DEFAULT_SHALLOW;
    static int deepOcean = DEFAULT_DEEP;
    static int iceBeyond = DEFAULT_ICE;
    static int flowerBiome = DEFAULT_FLOWER;
    static int frozenLead = DEFAULT_FROZEN;
    static int icebergReach = DEFAULT_ICEBERG;
    static int pyramidInset = DEFAULT_PYRAMID;
    static int transitionStart = 4000;
    static int fullOceanRadius = 5000;
    static int seaLevel = 64;
    static int oceanFloor = 48;
    static int oceanFloorVariation = 5;
    static boolean coastlineNoise = true;
    static int coastlineAmplitude = 200;
    static double coastlineScale = 0.0015D;
    static boolean circular = true;
    static boolean useDeepOcean = true;
    static int deepOceanStart = 5500;
    static int deepOceanFloor = 33;
    static int deepOceanTransition = 20;
    static int iceWallGap = 1536;
    static int iceSnowLead = 200;
    static int icebergLead = 600;
    static int iceShelfLength = 256;
    static int bedrockRun = 256;
    static int iceWallHeight = 30;
    static int bedrockExtra = 12;
    static int iceWaveAmplitude = 48;
    static int iceHeightJitter = 8;
    static boolean affectOres = true;
    static boolean debugLogging;
    static float betaEndPercent = ZoneScale.BETA_END;
    static float continentEndPercent = ZoneScale.CONTINENT_END;
    static float climateStartPercent = ZoneScale.CLIMATE_START;
    static float climateEndPercent = ZoneScale.CLIMATE_END;
    static float dryStartPercent = ZoneScale.DRY_START;
    static float dryEndPercent = ZoneScale.DRY_END;
    static int continentalRadius = 5000;
    static int betaEnd = 450;
    static int continentEnd = 1500;
    static int climateStart = 600;
    static int climateEnd = 2600;
    static int dryStart = 2000;
    static int dryEnd = 3200;
    static final int DEFAULT_SEA = 800;
    static final int DEFAULT_RIVER = 64;
    static int centralSea = DEFAULT_SEA;
    static int riverWidth = DEFAULT_RIVER;

    private OceanBoundaryConfig() {
    }

    static OceanBoundaryMath.Settings settings() {
        OceanBoundaryMath.Settings settings = new OceanBoundaryMath.Settings();
        settings.centerX = centerX;
        settings.centerZ = centerZ;
        settings.transitionStart = transitionStart;
        settings.fullOceanRadius = fullOceanRadius;
        settings.seaLevel = seaLevel;
        settings.oceanFloor = oceanFloor;
        settings.oceanFloorVariation = oceanFloorVariation;
        settings.coastlineNoise = coastlineNoise;
        settings.coastlineAmplitude = coastlineAmplitude;
        settings.coastlineScale = coastlineScale;
        settings.circular = circular;
        settings.useDeepOcean = useDeepOcean;
        settings.deepOceanStart = deepOceanStart;
        settings.deepOceanFloor = deepOceanFloor;
        settings.deepOceanTransition = deepOceanTransition;
        settings.iceWallGap = iceWallGap;
        settings.iceSnowLead = iceSnowLead;
        settings.icebergLead = icebergLead;
        settings.pyramidInset = pyramidInset;
        settings.iceShelfLength = iceShelfLength;
        settings.bedrockRun = bedrockRun;
        settings.iceWallHeight = iceWallHeight;
        settings.bedrockExtra = bedrockExtra;
        settings.iceWaveAmplitude = iceWaveAmplitude;
        settings.iceHeightJitter = iceHeightJitter;
        return settings;
    }

    static void load(FMLPreInitializationEvent event) {
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();

        String category = "ocean_boundary";
        config.setCategoryComment(
                category,
                "Each ring is a width in blocks added onto the ring inside it. "
                        + "The first value is the radius where the continent is still whole. "
                        + "Chunks already saved on disk are not rewritten.");
        readRings(config, category);

        enabled = config.getBoolean(
                "enabled", category, true, "Master switch for the ocean boundary.");
        centerX = config.getInt("centerX", category, 0, Integer.MIN_VALUE, Integer.MAX_VALUE,
                "World center X, in blocks.");
        centerZ = config.getInt("centerZ", category, 0, Integer.MIN_VALUE, Integer.MAX_VALUE,
                "World center Z, in blocks.");
        seaLevel = config.getInt(
                "seaLevel", category, 64, 2, 255,
                "First block Y that stays air above the ocean. Old World Gen beta uses 64.");
        oceanFloor = config.getInt(
                "oceanFloor", category, 48, 1, 250,
                "Target seabed height, in blocks.");
        oceanFloorVariation = config.getInt(
                "oceanFloorVariation", category, 5, 0, 40,
                "How many blocks the seabed can rise or fall.");
        coastlineNoise = config.getBoolean(
                "coastlineNoise", category, true,
                "Shift the boundary with low-frequency noise so the coast is not a perfect circle.");
        coastlineAmplitude = config.getInt(
                "coastlineAmplitude", category, 200, 0, 5000,
                "Maximum coastline shift, in blocks.");
        coastlineScale = config.getFloat(
                "coastlineScale", category, 0.0015F, 0.00001F, 1.0F,
                "Coastline noise frequency. Smaller values make wider bends.");
        circular = config.getBoolean(
                "circular", category, true,
                "Use a circular boundary. When false, the boundary is a square.");
        useDeepOcean = config.getBoolean(
                "useDeepOcean", category, true,
                "Drop the seabed to deepOceanFloor across the deep ocean ring.");
        deepOceanFloor = config.getInt(
                "deepOceanFloor", category, 33, 1, 250,
                "Deep seabed height. At sea level 64, 33 leaves 30 blocks of water.");
        int slopeDefault = 20;
        if (config.hasKey(category, "deepOceanTransition")) {
            slopeDefault = config.get(category, "deepOceanTransition", 20).getInt();
            config.getCategory(category).remove("deepOceanTransition");
        }
        deepOceanTransition = config.getInt(
                "deepSlope", category, slopeDefault, 0, 30000000,
                "Blocks of slope at the outer end of the shallow ocean, where the seabed drops to the deep floor.");
        iceWallHeight = config.getInt(
                "iceWallHeight", category, 30, 1, 200,
                "How many blocks the ice wall rises above sea level.");
        bedrockExtra = config.getInt(
                "bedrockExtra", category, 12, 0, 64,
                "Blocks of bedrock above the ice once the shelf ends.");
        iceWaveAmplitude = config.getInt(
                "iceWaveAmplitude", category, 48, 0, 500,
                "How far the ice wall wanders in and out, in blocks.");
        iceHeightJitter = config.getInt(
                "iceHeightJitter", category, 8, 0, 48,
                "Jagged variation of the ice wall top, in blocks.");
        affectOres = config.getBoolean(
                "affectOres", category, true,
                "Keep ores under the permanent ocean. When false, those ores are replaced with stone.");
        debugLogging = config.getBoolean(
                "debugLogging", category, false,
                "Log each newly generated chunk that the boundary modifies.");

        resolveRings(landRadius, coastFade, shallowOcean, deepOcean, iceBeyond, flowerBiome,
                frozenLead, icebergReach, pyramidInset);
        if (oceanFloor >= seaLevel) {
            oceanFloor = seaLevel - 1;
        }
        if (deepOceanFloor >= seaLevel) {
            deepOceanFloor = seaLevel - 2;
        }
        if (deepOceanFloor < 2) {
            deepOceanFloor = 2;
        }
        if (iceWallHeight < 1) {
            iceWallHeight = 1;
        }
        orderOcean(config, category);

        String continent = "continent";
        config.setCategoryComment(
                continent,
                "Biome zones inside the main landmass, as a percentage of its radius. "
                        + "The radius is the coastline, where the continent has faded to sea. "
                        + "100 is that shore. Ocean, ice, and the flower meadow past it are unchanged.");
        betaEndPercent = config.getFloat(
                "betaEnd", continent, ZoneScale.BETA_END, 0.0F, 100.0F,
                "Percent of the continental radius where pure beta terrain ends. "
                        + "The circle is centered on the world origin, not the continent center.");
        continentEndPercent = config.getFloat(
                "continentEnd", continent, ZoneScale.CONTINENT_END, 0.0F, 100.0F,
                "Percent of the continental radius where the wider landmasses are complete.");
        climateStartPercent = config.getFloat(
                "climateStart", continent, ZoneScale.CLIMATE_START, 0.0F, 100.0F,
                "Percent of the continental radius where north starts cooling and south starts warming.");
        climateEndPercent = config.getFloat(
                "climateEnd", continent, ZoneScale.CLIMATE_END, 0.0F, 100.0F,
                "Percent of the continental radius where north is fully cold and south is fully hot.");
        dryStartPercent = config.getFloat(
                "dryStart", continent, ZoneScale.DRY_START, 0.0F, 100.0F,
                "Percent of the continental radius where the far north and south start drying.");
        dryEndPercent = config.getFloat(
                "dryEnd", continent, ZoneScale.DRY_END, 0.0F, 100.0F,
                "Percent of the continental radius where the far north and south are fully dry.");
        ZoneScale.resolve(fullOceanRadius);

        String inland = "inland";
        config.setCategoryComment(
                inland,
                "A sea at the world center and four rivers from it to the outer ocean. "
                        + "The rivers wind toward the northeast, southeast, southwest, and northwest pyramids. "
                        + "Chunks already saved on disk are not rewritten.");
        centralSea = config.getInt(
                "centralSea", inland, DEFAULT_SEA, 0, 30000000,
                "Diameter of the sea at the world center, in blocks. Zero removes the sea.");
        riverWidth = config.getInt(
                "riverWidth", inland, DEFAULT_RIVER, 8, 400,
                "Width of each river, in blocks.");

        if (config.hasChanged()) {
            config.save();
        }
    }

    static void copyBoundary(OceanBoundaryMath.Settings settings) {
        settings.transitionStart = transitionStart;
        settings.fullOceanRadius = fullOceanRadius;
        settings.deepOceanStart = deepOceanStart;
        settings.deepOceanTransition = deepOceanTransition;
        settings.iceWallGap = iceWallGap;
        settings.iceSnowLead = iceSnowLead;
        settings.icebergLead = icebergLead;
        settings.pyramidInset = pyramidInset;
        settings.iceShelfLength = iceShelfLength;
        settings.bedrockRun = bedrockRun;
    }

    static void resolveRings(
            int land,
            int fade,
            int shallow,
            int deep,
            int ice,
            int flowers,
            int frozen,
            int bergs,
            int pyramid) {
        landRadius = land < 0 ? 0 : land;
        coastFade = fade < 0 ? 0 : fade;
        shallowOcean = shallow < 0 ? 0 : shallow;
        deepOcean = deep < 0 ? 0 : deep;
        iceBeyond = ice < 1 ? 1 : ice;
        flowerBiome = flowers < 0 ? 0 : flowers;
        frozenLead = frozen < 0 ? 0 : frozen;
        icebergReach = bergs < 0 ? 0 : bergs;
        pyramidInset = pyramid < 0 ? 0 : pyramid;
        if (deepOceanTransition > shallowOcean) {
            deepOceanTransition = shallowOcean;
        }
        transitionStart = landRadius;
        fullOceanRadius = landRadius + coastFade;
        deepOceanStart = fullOceanRadius + shallowOcean;
        iceWallGap = deepOcean;
        iceShelfLength = iceBeyond;
        bedrockRun = flowerBiome;
        iceSnowLead = frozenLead;
        icebergLead = icebergReach;
    }

    private static void readRings(Configuration config, String category) {
        boolean legacy = config.hasKey(category, "transitionStart")
                && !config.hasKey(category, "landRadius");
        if (legacy) {
            int land = config.get(category, "transitionStart", DEFAULT_LAND).getInt();
            int shore = config.get(category, "fullOceanRadius", land + DEFAULT_FADE).getInt();
            int deepStart = config.get(category, "deepOceanStart", shore + DEFAULT_SHALLOW).getInt();
            landRadius = land;
            coastFade = shore > land ? shore - land : 0;
            shallowOcean = deepStart > shore ? deepStart - shore : 0;
            deepOcean = config.get(category, "iceWallGap", DEFAULT_DEEP).getInt();
            iceBeyond = config.get(category, "iceShelfLength", DEFAULT_ICE).getInt();
            flowerBiome = config.get(category, "bedrockRun", DEFAULT_FLOWER).getInt();
            frozenLead = config.get(category, "iceSnowLead", DEFAULT_FROZEN).getInt();
            icebergReach = config.get(category, "icebergLead", DEFAULT_ICEBERG).getInt();
            pyramidInset = deepOcean / 2;
        }
        ConfigCategory section = config.getCategory(category);
        if (legacy) {
            section.remove("transitionStart");
            section.remove("fullOceanRadius");
            section.remove("deepOceanStart");
            section.remove("iceWallGap");
            section.remove("iceShelfLength");
            section.remove("bedrockRun");
            section.remove("iceSnowLead");
        }
        landRadius = config.getInt(
                "landRadius", category, landRadius, 0, 30000000,
                "Radius where the continent is still whole, before it starts fading to the sea.");
        coastFade = config.getInt(
                "coastFade", category, coastFade, 0, 30000000,
                "Additional blocks until the continent has faded to sea.");
        shallowOcean = config.getInt(
                "shallowOcean", category, shallowOcean, 0, 30000000,
                "Additional blocks of shallow ocean past that shore.");
        deepOcean = config.getInt(
                "deepOcean", category, deepOcean, 0, 30000000,
                "Additional blocks of deep ocean before the ice wall.");
        iceBeyond = config.getInt(
                "iceBeyond", category, iceBeyond, 1, 30000000,
                "Additional blocks of ice wall and the shelf beyond it.");
        flowerBiome = config.getInt(
                "flowerBiome", category, flowerBiome, 0, 30000000,
                "Additional blocks of the outer flower meadow past the ice.");
        frozenLead = config.getInt(
                "frozenLead", category, frozenLead, 0, 30000000,
                "How far the frozen surface reaches inward from the ice wall, across the deep ocean.");
        icebergReach = config.getInt(
                "icebergLead", category, icebergReach, 0, 30000000,
                "How far icebergs reach inward from the ice wall. Past the frozen surface this is open ocean.");
        pyramidInset = config.getInt(
                "pyramidInset", category, pyramidInset, 0, 30000000,
                "Pyramid centers, in blocks inward from the ice wall.");
    }

    private static void orderOcean(Configuration config, String category) {
        config.getCategory(category).setPropertyOrder(new ArrayList<String>(Arrays.asList(
                "landRadius",
                "coastFade",
                "shallowOcean",
                "deepSlope",
                "deepOcean",
                "iceBeyond",
                "flowerBiome",
                "frozenLead",
                "icebergLead",
                "pyramidInset",
                "centerX",
                "centerZ",
                "circular",
                "coastlineNoise",
                "coastlineAmplitude",
                "coastlineScale",
                "seaLevel",
                "oceanFloor",
                "oceanFloorVariation",
                "deepOceanFloor",
                "useDeepOcean",
                "iceWallHeight",
                "iceWaveAmplitude",
                "iceHeightJitter",
                "bedrockExtra",
                "enabled",
                "affectOres",
                "debugLogging")));
    }
}
