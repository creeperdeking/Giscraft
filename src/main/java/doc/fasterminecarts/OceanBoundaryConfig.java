package doc.fasterminecarts;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.config.Configuration;

final class OceanBoundaryConfig {
    static boolean enabled = true;
    static int centerX;
    static int centerZ;
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
    static int continentalDiameter = 10000;
    static int betaEnd = 450;
    static int continentEnd = 1500;
    static int climateStart = 600;
    static int climateEnd = 2600;
    static int dryStart = 2000;
    static int dryEnd = 3200;

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
                "Turns newly generated Overworld chunks into ocean past a radius, "
                        + "then an ice wall, rising bedrock, and a void. "
                        + "Old World Gen beta terrain inside the radius is left as-is. "
                        + "Chunks already saved on disk are not rewritten.");

        enabled = config.getBoolean(
                "enabled", category, true, "Master switch for the ocean boundary.");
        centerX = config.getInt("centerX", category, 0, Integer.MIN_VALUE, Integer.MAX_VALUE,
                "World center X, in blocks.");
        centerZ = config.getInt("centerZ", category, 0, Integer.MIN_VALUE, Integer.MAX_VALUE,
                "World center Z, in blocks.");
        transitionStart = config.getInt(
                "transitionStart", category, 4000, 0, 30000000,
                "Distance where land starts lowering toward the seabed.");
        fullOceanRadius = config.getInt(
                "fullOceanRadius", category, 5000, 0, 30000000,
                "Distance where terrain is fully ocean. Must be greater than transitionStart.");
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
                "Drop the seabed to deepOceanFloor and use the deep ocean biome past deepOceanStart.");
        deepOceanStart = config.getInt(
                "deepOceanStart", category, 5500, 0, 30000000,
                "Distance where the deep seabed and the deep ocean biome are fully reached.");
        deepOceanFloor = config.getInt(
                "deepOceanFloor", category, 33, 1, 250,
                "Deep seabed height. At sea level 64, 33 leaves 30 blocks of water.");
        deepOceanTransition = config.getInt(
                "deepOceanTransition", category, 20, 0, 30000000,
                "Length of the slope from the shallow seabed down to the deep seabed, in blocks. The slope ends at deepOceanStart.");
        iceWallGap = config.getInt(
                "iceWallGap", category, 1536, 0, 30000000,
                "Blocks of deep ocean before the ice wall.");
        iceSnowLead = config.getInt(
                "iceSnowLead", category, 200, 0, 30000000,
                "Blocks of snowy frozen ocean before the ice wall.");
        icebergLead = config.getInt(
                "icebergLead", category, 600, 0, 30000000,
                "Blocks before the ice wall where icebergs generate. Past the snowy plain this is open ocean.");
        iceShelfLength = config.getInt(
                "iceShelfLength", category, 256, 1, 30000000,
                "Blocks of ice with rising bedrock behind the wall.");
        bedrockRun = config.getInt(
                "bedrockRun", category, 256, 0, 30000000,
                "Blocks of bare bedrock between the ice and the void.");
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

        if (fullOceanRadius < transitionStart) {
            fullOceanRadius = transitionStart;
        }
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

        if (config.hasChanged()) {
            config.save();
        }
    }
}
