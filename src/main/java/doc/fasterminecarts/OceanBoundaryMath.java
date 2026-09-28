package doc.fasterminecarts;

final class OceanBoundaryMath {
    static final class Settings {
        int centerX;
        int centerZ;
        int transitionStart;
        int fullOceanRadius;
        int seaLevel;
        int oceanFloor;
        int oceanFloorVariation;
        boolean coastlineNoise;
        int coastlineAmplitude;
        double coastlineScale;
        boolean circular;
        boolean useDeepOcean;
        int deepOceanStart;
        int deepOceanFloor;
        int deepOceanTransition;
        int iceWallGap;
        int iceSnowLead;
        int iceShelfLength;
        int bedrockRun;
        int iceWallHeight;
        int bedrockExtra;
        int iceWaveAmplitude;
        int iceHeightJitter;
    }

    static final int BAND_OCEAN = 0;
    static final int BAND_FROZEN = 1;
    static final int BAND_ICE = 2;
    static final int BAND_BEDROCK = 3;
    static final int BAND_VOID = 4;

    private static final int ICEBERG_LEAD = 400;
    private static final int PYRAMID_RISE = 50;

    private OceanBoundaryMath() {
    }

    static double distanceAt(int x, int z, Settings settings) {
        return distance(x, z, settings.centerX, settings.centerZ, settings.circular);
    }

    static double transitionAt(int x, int z, long seed, Settings settings) {
        double wobble = 0.0D;
        if (settings.coastlineNoise && settings.coastlineAmplitude > 0) {
            wobble = noise(x * settings.coastlineScale, z * settings.coastlineScale, seed)
                    * settings.coastlineAmplitude;
        }
        return transition(distanceAt(x, z, settings), settings.transitionStart, settings.fullOceanRadius, wobble);
    }

    static int oceanFloorAt(int x, int z, long seed, Settings settings) {
        return clampFloor(
                settings.oceanFloor,
                floorWobble(x, z, seed),
                settings.seaLevel,
                settings.oceanFloorVariation);
    }

    static int seabedAt(int x, int z, long seed, Settings settings) {
        double wobble = floorWobble(x, z, seed);
        int shallow = clampFloor(
                settings.oceanFloor, wobble, settings.seaLevel, settings.oceanFloorVariation);
        if (!settings.useDeepOcean) {
            return shallow;
        }
        int deep = clampFloor(
                settings.deepOceanFloor, wobble, settings.seaLevel, settings.oceanFloorVariation);
        int width = settings.deepOceanTransition;
        if (width < 0) {
            width = 0;
        }
        double amount = transition(
                distanceAt(x, z, settings),
                settings.deepOceanStart - width,
                settings.deepOceanStart,
                0.0D);
        return blendHeight(shallow, deep, amount);
    }

    static int columnFloor(int x, int z, long seed, Settings settings) {
        return seabedAt(x, z, seed, settings);
    }

    static int deepEdge(Settings settings) {
        int edge = settings.useDeepOcean ? settings.deepOceanStart : settings.fullOceanRadius;
        if (edge < settings.fullOceanRadius) {
            return settings.fullOceanRadius;
        }
        return edge;
    }

    static double iceWallRadius(Settings settings) {
        return deepEdge(settings) + settings.iceWallGap;
    }

    static double intoWall(int x, int z, long seed, Settings settings) {
        return distanceAt(x, z, settings) - wallAt(x, z, seed, settings);
    }

    static double intoBowl(int x, int z, Settings settings) {
        return distanceAt(x, z, settings) - (deepEdge(settings) + settings.iceWallGap);
    }

    static int bandAt(int x, int z, long seed, Settings settings) {
        double into = intoWall(x, z, seed, settings);
        if (into < -settings.iceSnowLead) {
            return BAND_OCEAN;
        }
        if (into < 0.0D) {
            return BAND_FROZEN;
        }
        double bowl = intoBowl(x, z, settings);
        if (bowl < settings.iceShelfLength) {
            return BAND_ICE;
        }
        if (bowl < settings.iceShelfLength + settings.bedrockRun) {
            return BAND_BEDROCK;
        }
        return BAND_VOID;
    }

    static int iceCrest(int x, int z, long seed, Settings settings) {
        double jag = noise(x * 0.045D, z * 0.045D, seed ^ 0xA11CEL) * 0.65D
                + noise(x * 0.11D, z * 0.11D, seed ^ 0xB0A7L) * 0.35D;
        int top = settings.seaLevel + settings.iceWallHeight + (int) Math.round(jag * settings.iceHeightJitter);
        top -= lipBite(x, z, seed, settings);
        int low = settings.seaLevel + 4;
        if (top < low) {
            return low;
        }
        if (top > 240) {
            return 240;
        }
        return top;
    }

    static int icebergHeight(int x, int z, long seed, Settings settings) {
        double into = intoWall(x, z, seed, settings);
        if (into >= 0.0D || into < -settings.iceSnowLead - ICEBERG_LEAD - 24.0D) {
            return 0;
        }
        int height = bergGrid(x, z, seed, settings, 26, 0x1000, 3, 8, 2, 4, 0.16D, 0);
        int medium = bergGrid(x, z, seed, settings, 60, 0x2000, 7, 18, 4, 8, 0.09D, 1);
        if (medium > height) {
            height = medium;
        }
        int large = bergGrid(x, z, seed, settings, 98, 0x3000, 28, 60, 14, 30, 0.09D, 2);
        if (large > height) {
            height = large;
        }
        return height;
    }

    static int pyramidTop(int x, int z, Settings settings) {
        if (!settings.useDeepOcean) {
            return -1;
        }
        int peak = pyramidPeak(settings);
        int radius = pyramidRadius(settings);
        int best = -1;
        for (int index = 0; index < 4; index++) {
            int span = Math.max(
                    Math.abs(x - pyramidCenterX(index, settings)),
                    Math.abs(z - pyramidCenterZ(index, settings)));
            if (span > radius) {
                continue;
            }
            int top = peak - span;
            if (top > best) {
                best = top;
            }
        }
        return best;
    }

    static boolean chunkReachesPyramid(int originX, int originZ, Settings settings) {
        if (!settings.useDeepOcean) {
            return false;
        }
        int radius = pyramidRadius(settings);
        for (int index = 0; index < 4; index++) {
            int centerX = pyramidCenterX(index, settings);
            int centerZ = pyramidCenterZ(index, settings);
            if (originX + 15 < centerX - radius || originX > centerX + radius) {
                continue;
            }
            if (originZ + 15 < centerZ - radius || originZ > centerZ + radius) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static int pyramidPeak(Settings settings) {
        int peak = settings.seaLevel - 1 + PYRAMID_RISE;
        if (peak > 250) {
            return 250;
        }
        if (peak < settings.seaLevel) {
            return settings.seaLevel;
        }
        return peak;
    }

    private static int pyramidRadius(Settings settings) {
        int lowest = settings.deepOceanFloor - settings.oceanFloorVariation;
        if (lowest < 2) {
            lowest = 2;
        }
        int radius = pyramidPeak(settings) - lowest;
        if (radius < 1) {
            return 1;
        }
        return radius;
    }

    private static int pyramidCenterDistance(Settings settings) {
        int inner = deepEdge(settings);
        int outer = inner + settings.iceWallGap;
        int mid = inner + (outer - inner) / 2;
        int radius = pyramidRadius(settings);
        int earliestIce = outer - settings.iceWaveAmplitude - settings.iceSnowLead - ICEBERG_LEAD;
        if (mid + radius > earliestIce - 24) {
            mid = earliestIce - 24 - radius;
        }
        int minCenter = inner + radius + 24;
        if (mid < minCenter) {
            mid = minCenter;
        }
        return mid;
    }

    private static int pyramidCenterX(int index, Settings settings) {
        int mid = pyramidCenterDistance(settings);
        if (index == 1) {
            return settings.centerX + mid;
        }
        if (index == 3) {
            return settings.centerX - mid;
        }
        return settings.centerX;
    }

    private static int pyramidCenterZ(int index, Settings settings) {
        int mid = pyramidCenterDistance(settings);
        if (index == 0) {
            return settings.centerZ - mid;
        }
        if (index == 2) {
            return settings.centerZ + mid;
        }
        return settings.centerZ;
    }

    static boolean chunkReachesIcebergs(int originX, int originZ, Settings settings) {
        double farthest = 0.0D;
        farthest = Math.max(farthest, distanceAt(originX, originZ, settings));
        farthest = Math.max(farthest, distanceAt(originX + 15, originZ, settings));
        farthest = Math.max(farthest, distanceAt(originX, originZ + 15, settings));
        farthest = Math.max(farthest, distanceAt(originX + 15, originZ + 15, settings));
        return farthest >= icebergNear(settings);
    }

    static int bedrockPlateau(Settings settings) {
        int top = settings.seaLevel + settings.iceWallHeight + settings.bedrockExtra;
        if (top < 2) {
            return 2;
        }
        if (top > 250) {
            return 250;
        }
        return top;
    }

    static final int MEADOW_GRASS = 0;
    static final int MEADOW_POND = 1;
    static final int MEADOW_FLOWER = 2;
    static final int MEADOW_TOMB = 3;
    static final int FLOWER_KINDS = 14;

    static int meadowCover(int x, int z, long seed) {
        if (tombPart(x, z, seed) != TOMB_NONE) {
            return MEADOW_TOMB;
        }
        if (pondDepth(x, z, seed) > 0) {
            return MEADOW_POND;
        }
        if (flowerKind(x, z, seed) >= 0) {
            return MEADOW_FLOWER;
        }
        return MEADOW_GRASS;
    }

    static int tombPart(int x, int z, long seed) {
        int cell = 72;
        int cellX = Math.floorDiv(x, cell);
        int cellZ = Math.floorDiv(z, cell);
        return tombPartInCell(x, z, seed, cell, cellX, cellZ);
    }

    static int pondDepth(int x, int z, long seed) {
        int cell = 40;
        int cellX = Math.floorDiv(x, cell);
        int cellZ = Math.floorDiv(z, cell);
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                int depth = pondInCell(x, z, seed, cell, cellX + offsetX, cellZ + offsetZ);
                if (depth > 0) {
                    return depth;
                }
            }
        }
        return 0;
    }

    static int flowerKind(int x, int z, long seed) {
        int cell = 26;
        int cellX = Math.floorDiv(x, cell);
        int cellZ = Math.floorDiv(z, cell);
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                int kind = flowerInCell(x, z, seed, cell, cellX + offsetX, cellZ + offsetZ);
                if (kind >= 0) {
                    return kind;
                }
            }
        }
        return -1;
    }

    static final int TOMB_NONE = 0;
    static final int TOMB_CORNER = 1;
    static final int TOMB_WALL = 2;
    static final int TOMB_WINDOW = 3;
    static final int TOMB_DOOR = 4;
    static final int TOMB_INNER = 5;
    static final int TOMB_COFFIN = 6;
    static final int TOMB_PORCH = 7;

    static int bedrockTop(double into, Settings settings) {
        int plateau = bedrockPlateau(settings);
        double shelf = settings.iceShelfLength < 1 ? 1.0D : settings.iceShelfLength;
        if (into >= shelf) {
            return plateau;
        }
        if (into <= 0.0D) {
            return 0;
        }
        return (int) Math.round(smoothstep(into / shelf) * plateau);
    }

    static double freezeAmount(double into, Settings settings) {
        double lead = settings.iceSnowLead < 1 ? 1.0D : settings.iceSnowLead;
        return smoothstep((into + lead) / lead);
    }

    static boolean isChunkFullyVoid(int chunkX, int chunkZ, Settings settings) {
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        double closest = closestDistance(originX, originZ, originX + 15, originZ + 15, settings);
        return closest >= voidFar(settings);
    }

    static boolean isChunkPastSnow(int chunkX, int chunkZ, Settings settings) {
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        double closest = closestDistance(originX, originZ, originX + 15, originZ + 15, settings);
        return closest >= snowFar(settings);
    }

    static boolean chunkReachesSnow(int originX, int originZ, Settings settings) {
        double farthest = 0.0D;
        farthest = Math.max(farthest, distanceAt(originX, originZ, settings));
        farthest = Math.max(farthest, distanceAt(originX + 15, originZ, settings));
        farthest = Math.max(farthest, distanceAt(originX, originZ + 15, settings));
        farthest = Math.max(farthest, distanceAt(originX + 15, originZ + 15, settings));
        return farthest >= snowNear(settings);
    }

    private static double wallAt(int x, int z, long seed, Settings settings) {
        return deepEdge(settings) + settings.iceWallGap + wallOffset(x, z, seed, settings);
    }

    private static int lipBite(int x, int z, long seed, Settings settings) {
        double into = intoWall(x, z, seed, settings);
        if (into < 0.0D || into >= 10.0D) {
            return 0;
        }
        double pocket = linearNoise(x * 0.13D, z * 0.13D, seed ^ 0xE20DEL);
        double chip = linearNoise(x * 0.36D, z * 0.36D, seed ^ 0xB17EL);
        double closeness = (10.0D - into) / 10.0D;
        double depth = 4.0D + (pocket * 0.5D + 0.5D) * 8.0D;
        depth += (chip * 0.5D + 0.5D) * 8.0D * closeness;
        depth *= 0.2D + 0.8D * closeness * closeness;
        if (into > 1.5D && pocket < -0.45D) {
            return 0;
        }
        int bite = (int) Math.round(depth);
        if (into < 2.0D && bite < 8) {
            bite = 8;
        }
        if (bite > 18) {
            return 18;
        }
        if (bite < 1) {
            return 0;
        }
        return bite;
    }

    private static double wallOffset(int x, int z, long seed, Settings settings) {
        if (settings.iceWaveAmplitude <= 0) {
            return 0.0D;
        }
        double dx = x - settings.centerX;
        double dz = z - settings.centerZ;
        double angle = Math.atan2(dz, dx);
        double radius = deepEdge(settings) + settings.iceWallGap;
        if (radius < 64.0D) {
            radius = 64.0D;
        }
        int broadCells = sectorsFor(radius, 220.0D);
        int faceCells = sectorsFor(radius, 64.0D);
        int toothCells = sectorsFor(radius, 18.0D);
        double broad = angularLinear(angle, broadCells, seed ^ 0x1CEB00DAL);
        double face = angledRun(angle, faceCells, seed ^ 0x51A7L);
        double tooth = angledNotch(angle, toothCells, seed ^ 0x7E57L);
        return (broad * 0.16D + face * 0.54D + tooth * 0.30D) * settings.iceWaveAmplitude;
    }

    private static double angledRun(double angle, int cells, long seed) {
        double scaled = (angle + Math.PI) / (2.0D * Math.PI) * cells;
        int index = Math.floorMod((int) Math.floor(scaled), cells);
        double t = scaled - Math.floor(scaled);
        double split = 0.16D + unit(index, cells, seed ^ 0x11L) * 0.68D;
        double left = hash(index, cells, seed);
        double corner = hash(index, cells, seed ^ 0x22L);
        double right = hash(Math.floorMod(index + 1, cells), cells, seed);
        if (t < split) {
            return lerp(left, corner, t / split);
        }
        return lerp(corner, right, (t - split) / (1.0D - split));
    }

    private static double angledNotch(double angle, int cells, long seed) {
        double scaled = (angle + Math.PI) / (2.0D * Math.PI) * cells;
        int index = Math.floorMod((int) Math.floor(scaled), cells);
        if (unit(index, cells, seed) < 0.58D) {
            return 0.0D;
        }
        return hash(index, cells, seed ^ 0x33L);
    }

    private static int sectorsFor(double radius, double arc) {
        int sectors = (int) Math.round(2.0D * Math.PI * radius / arc);
        if (sectors < 8) {
            return 8;
        }
        return sectors;
    }

    private static double angularLinear(double angle, int sectors, long seed) {
        double scaled = (angle + Math.PI) / (2.0D * Math.PI) * sectors;
        int index = (int) Math.floor(scaled);
        double t = scaled - index;
        int start = Math.floorMod(index, sectors);
        int end = Math.floorMod(start + 1, sectors);
        return lerp(hash(start, sectors, seed), hash(end, sectors, seed), t);
    }

    private static double bergChance(double into, Settings settings, double rate, int melt) {
        if (into >= 0.0D) {
            return 0.0D;
        }
        double far = -settings.iceSnowLead - ICEBERG_LEAD;
        if (into < far) {
            return 0.0D;
        }
        double span = -far;
        if (span < 1.0D) {
            span = 1.0D;
        }
        double close = (into - far) / span;
        double chance;
        if (melt <= 0) {
            chance = rate * 5.0D * close * close * close;
        } else if (melt == 1) {
            chance = rate * (0.08D + 0.92D * close * close);
        } else {
            double fringe = close < 0.12D ? close / 0.12D : 1.0D;
            chance = rate * (0.90D + 0.10D * close) * fringe;
        }
        if (chance > 0.92D) {
            return 0.92D;
        }
        if (chance < 0.0D) {
            return 0.0D;
        }
        return chance;
    }

    private static int bergGrid(
            int x,
            int z,
            long seed,
            Settings settings,
            int cell,
            int salt,
            int minHeight,
            int maxHeight,
            int minRadius,
            int maxRadius,
            double rate,
            int melt) {
        int cellX = Math.floorDiv(x, cell);
        int cellZ = Math.floorDiv(z, cell);
        int best = 0;
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                int height = bergCell(
                        x,
                        z,
                        seed,
                        settings,
                        cell,
                        salt,
                        minHeight,
                        maxHeight,
                        minRadius,
                        maxRadius,
                        rate,
                        melt,
                        cellX + offsetX,
                        cellZ + offsetZ);
                if (height > best) {
                    best = height;
                }
            }
        }
        return best;
    }

    private static int bergCell(
            int x,
            int z,
            long seed,
            Settings settings,
            int cell,
            int salt,
            int minHeight,
            int maxHeight,
            int minRadius,
            int maxRadius,
            double rate,
            int melt,
            int cellX,
            int cellZ) {
        long cellSeed = seed ^ (salt * 0x9E3779B97L);
        double jitterX = 0.12D + unit(cellX, cellZ, cellSeed ^ 0x11L) * 0.76D;
        double jitterZ = 0.12D + unit(cellX, cellZ, cellSeed ^ 0x22L) * 0.76D;
        int centerX = (int) Math.round(cellX * (double) cell + jitterX * cell);
        int centerZ = (int) Math.round(cellZ * (double) cell + jitterZ * cell);
        double chance = bergChance(intoWall(centerX, centerZ, seed, settings), settings, rate, melt);
        if (chance <= 0.0D || unit(cellX, cellZ, cellSeed) > chance) {
            return 0;
        }
        double size = unit(cellX, cellZ, cellSeed ^ 0x33L);
        double widthRoll = unit(cellX, cellZ, cellSeed ^ 0x44L);
        int peak = minHeight + (int) Math.round(size * (maxHeight - minHeight));
        double radius = minRadius + widthRoll * (maxRadius - minRadius);
        double dx = x - centerX;
        double dz = z - centerZ;
        double angle = Math.atan2(dz, dx);
        double limit = bergLimit(angle, radius, cellX, cellZ, cellSeed);
        if (dx * dx + dz * dz >= limit * limit) {
            return 0;
        }
        double shiftAngle = (unit(cellX, cellZ, cellSeed ^ 0x99L) * 2.0D - 1.0D) * Math.PI;
        double tipReach = bergLimit(shiftAngle, radius, cellX, cellZ, cellSeed);
        double shift = tipReach * (0.28D + 0.24D * unit(cellX, cellZ, cellSeed ^ 0x88L));
        double peakX = centerX + Math.cos(shiftAngle) * shift;
        double peakZ = centerZ + Math.sin(shiftAngle) * shift;
        double peakDist = Math.hypot(x - peakX, z - peakZ);
        if (peakDist >= limit) {
            return 0;
        }
        double point = 1.7D + unit(cellX, cellZ, cellSeed ^ 0xABCL) * 1.3D;
        double profile = Math.pow(1.0D - peakDist / limit, point);
        double across = Math.abs(Math.sin(angle - shiftAngle));
        profile *= 1.0D - 0.58D * Math.pow(across, 0.5D);
        double gouge = linearNoise(x * 0.72D, z * 0.72D, cellSeed ^ 0x66L);
        if (gouge < -0.15D) {
            double cut = (-gouge - 0.15D) / 0.85D;
            if (cut > 1.0D) {
                cut = 1.0D;
            }
            profile *= 1.0D - 0.72D * cut;
        }
        int height = (int) Math.round(peak * profile);
        height += (int) Math.round(linearNoise(x * 0.95D, z * 0.95D, cellSeed ^ 0xB7L) * 2.6D);
        if (height > peak) {
            height = peak;
        }
        if (height < 1) {
            return 0;
        }
        return height;
    }

    private static double bergLimit(double angle, double radius, int cellX, int cellZ, long cellSeed) {
        int teeth = 4 + (int) Math.floor(unit(cellX, cellZ, cellSeed ^ 0x77L) * 5.0D);
        double broad = angularLinear(angle, teeth, cellSeed ^ 0xA1L);
        int chips = teeth * 2;
        double chip = angledNotch(angle, chips, cellSeed ^ 0xC3L);
        double shaped = broad * 0.62D + chip * 0.38D;
        double limit = radius * (0.62D + 0.58D * shaped);
        if (limit < 1.5D) {
            return 1.5D;
        }
        return limit;
    }

    private static double icebergNear(Settings settings) {
        return deepEdge(settings) + settings.iceWallGap - settings.iceWaveAmplitude
                - settings.iceSnowLead - ICEBERG_LEAD;
    }

    private static double unit(int x, int z, long seed) {
        return hash(x, z, seed) * 0.5D + 0.5D;
    }

    private static int tombPartInCell(int x, int z, long seed, int cell, int cellX, int cellZ) {
        long cellSeed = seed ^ 0x70B1L;
        if (unit(cellX, cellZ, cellSeed) > 0.42D) {
            return TOMB_NONE;
        }
        double jitterX = 0.28D + unit(cellX, cellZ, cellSeed ^ 0x11L) * 0.44D;
        double jitterZ = 0.28D + unit(cellX, cellZ, cellSeed ^ 0x22L) * 0.44D;
        int centerX = (int) Math.round(cellX * (double) cell + jitterX * cell);
        int centerZ = (int) Math.round(cellZ * (double) cell + jitterZ * cell);
        int localX = x - centerX;
        int localZ = z - centerZ;
        int rotation = (int) Math.floor(unit(cellX, cellZ, cellSeed ^ 0x33L) * 4.0D);
        for (int turn = 0; turn < rotation; turn++) {
            int swapped = localX;
            localX = -localZ;
            localZ = swapped;
        }
        if (localX == 0 && localZ == 3) {
            return TOMB_PORCH;
        }
        if (localX < -2 || localX > 2 || localZ < -2 || localZ > 2) {
            return TOMB_NONE;
        }
        if (localX == 0 && localZ == 2) {
            return TOMB_DOOR;
        }
        if (localX == 0 && localZ == 0) {
            return TOMB_COFFIN;
        }
        boolean edge = localX == -2 || localX == 2 || localZ == -2 || localZ == 2;
        if (!edge) {
            return TOMB_INNER;
        }
        boolean corner = (localX == -2 || localX == 2) && (localZ == -2 || localZ == 2);
        if (corner) {
            return TOMB_CORNER;
        }
        if ((localX == -2 || localX == 2) && localZ == 0) {
            return TOMB_WINDOW;
        }
        return TOMB_WALL;
    }

    private static int pondInCell(int x, int z, long seed, int cell, int cellX, int cellZ) {
        long cellSeed = seed ^ 0xD0D1L;
        if (unit(cellX, cellZ, cellSeed) > 0.34D) {
            return 0;
        }
        double jitterX = 0.2D + unit(cellX, cellZ, cellSeed ^ 0x11L) * 0.6D;
        double jitterZ = 0.2D + unit(cellX, cellZ, cellSeed ^ 0x22L) * 0.6D;
        double centerX = cellX * (double) cell + jitterX * cell;
        double centerZ = cellZ * (double) cell + jitterZ * cell;
        double dx = x - centerX;
        double dz = z - centerZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        double radius = 2.2D + unit(cellX, cellZ, cellSeed ^ 0x33L) * 2.4D;
        double wobble = 0.72D + 0.35D * noise(x * 0.35D, z * 0.35D, cellSeed);
        if (dist >= radius * wobble) {
            return 0;
        }
        return dist < 1.15D ? 2 : 1;
    }

    private static int flowerInCell(int x, int z, long seed, int cell, int cellX, int cellZ) {
        long cellSeed = seed ^ 0xF10AEL;
        if (unit(cellX, cellZ, cellSeed) > 0.58D) {
            return -1;
        }
        double jitterX = 0.18D + unit(cellX, cellZ, cellSeed ^ 0x11L) * 0.64D;
        double jitterZ = 0.18D + unit(cellX, cellZ, cellSeed ^ 0x22L) * 0.64D;
        double centerX = cellX * (double) cell + jitterX * cell;
        double centerZ = cellZ * (double) cell + jitterZ * cell;
        double dx = x - centerX;
        double dz = z - centerZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        double radius = 6.0D + unit(cellX, cellZ, cellSeed ^ 0x33L) * 7.0D;
        if (dist >= radius) {
            return -1;
        }
        if (noise(x * 0.22D, z * 0.22D, cellSeed ^ 0x44L) < -0.25D) {
            return -1;
        }
        int kindA = (int) Math.floor(unit(cellX, cellZ, cellSeed ^ 0x55L) * FLOWER_KINDS);
        if (unit(cellX, cellZ, cellSeed ^ 0x77L) >= 0.45D) {
            return kindA;
        }
        int kindB = (int) Math.floor(unit(cellX, cellZ, cellSeed ^ 0x66L) * FLOWER_KINDS);
        int kindC = (int) Math.floor(unit(cellX, cellZ, cellSeed ^ 0x88L) * FLOWER_KINDS);
        double blend = noise(x * 0.45D, z * 0.45D, cellSeed ^ 0x99L);
        if (blend < -0.2D) {
            return kindA;
        }
        if (blend < 0.25D) {
            return kindB;
        }
        return kindC;
    }

    private static double snowNear(Settings settings) {
        return deepEdge(settings) + settings.iceWallGap - settings.iceWaveAmplitude - settings.iceSnowLead;
    }

    private static double snowFar(Settings settings) {
        return deepEdge(settings) + settings.iceWallGap + settings.iceWaveAmplitude - settings.iceSnowLead;
    }

    private static double voidFar(Settings settings) {
        return deepEdge(settings) + settings.iceWallGap
                + settings.iceShelfLength + settings.bedrockRun;
    }

    private static double closestDistance(int minX, int minZ, int maxX, int maxZ, Settings settings) {
        double dx = axisGap(settings.centerX, minX, maxX);
        double dz = axisGap(settings.centerZ, minZ, maxZ);
        if (settings.circular) {
            return Math.sqrt(dx * dx + dz * dz);
        }
        return Math.max(dx, dz);
    }

    private static double axisGap(int center, int min, int max) {
        if (center < min) {
            return min - center;
        }
        if (center > max) {
            return center - max;
        }
        return 0.0D;
    }

    private static double floorWobble(int x, int z, long seed) {
        return noise(x * 0.008D, z * 0.008D, seed ^ 0x5DEECE66DL);
    }

    private static int clampFloor(int base, double wobble, int seaLevel, int variation) {
        int floor = (int) Math.round(base + wobble * variation);
        int maxFloor = seaLevel - 2;
        if (floor < 2) {
            return 2;
        }
        if (floor > maxFloor) {
            return maxFloor;
        }
        return floor;
    }

    static double distance(
            double x,
            double z,
            double centerX,
            double centerZ,
            boolean circular) {
        double dx = x - centerX;
        double dz = z - centerZ;
        if (circular) {
            return Math.sqrt(dx * dx + dz * dz);
        }
        return Math.max(Math.abs(dx), Math.abs(dz));
    }

    static double smoothstep(double amount) {
        if (amount <= 0.0D) {
            return 0.0D;
        }
        if (amount >= 1.0D) {
            return 1.0D;
        }
        return amount * amount * (3.0D - 2.0D * amount);
    }

    static double transition(double distance, double start, double end, double wobble) {
        double inner = start + wobble;
        double outer = end + wobble;
        if (distance <= inner) {
            return 0.0D;
        }
        if (outer <= inner || distance >= outer) {
            return 1.0D;
        }
        return smoothstep((distance - inner) / (outer - inner));
    }

    static int blendHeight(int nostalgic, int ocean, double amount) {
        if (amount <= 0.0D) {
            return nostalgic;
        }
        if (amount >= 1.0D) {
            return ocean;
        }
        int blended = (int) Math.round((1.0D - amount) * nostalgic + amount * ocean);
        if (blended < 1) {
            return 1;
        }
        if (blended > 255) {
            return 255;
        }
        return blended;
    }

    static double noise(double x, double z, long seed) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        double fadeX = fade(x - x0);
        double fadeZ = fade(z - z0);
        double n00 = hash(x0, z0, seed);
        double n10 = hash(x0 + 1, z0, seed);
        double n01 = hash(x0, z0 + 1, seed);
        double n11 = hash(x0 + 1, z0 + 1, seed);
        return lerp(lerp(n00, n10, fadeX), lerp(n01, n11, fadeX), fadeZ);
    }

    private static double linearNoise(double x, double z, long seed) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        double tx = x - x0;
        double tz = z - z0;
        double n00 = hash(x0, z0, seed);
        double n10 = hash(x0 + 1, z0, seed);
        double n01 = hash(x0, z0 + 1, seed);
        double n11 = hash(x0 + 1, z0 + 1, seed);
        return lerp(lerp(n00, n10, tx), lerp(n01, n11, tx), tz);
    }

    private static double fade(double amount) {
        return amount * amount * (3.0D - 2.0D * amount);
    }

    private static double lerp(double start, double end, double amount) {
        return start + (end - start) * amount;
    }

    private static double hash(int x, int z, long seed) {
        long value = seed + (long) x * 341873128712L + (long) z * 132897987541L;
        value = (value << 13) ^ value;
        value = value * (value * value * 15731L + 789221L) + 1376312589L;
        int bits = (int) (value & 0x7fffffffL);
        return 1.0D - bits / 1073741824.0D;
    }
}
