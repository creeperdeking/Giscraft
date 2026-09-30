package doc.fasterminecarts;

import java.util.Arrays;
import java.util.Random;

import owg.noise.NoiseOctavesBeta;
import owg.noise.OldNoiseGeneratorOctaves2;

/**
 * Samples Old World Gen's beta 1.7.3 surface using that mod's noise classes,
 * then applies the same continental and latitude shift as the running game.
 * Each result is the highest stone y and a biome group for one chunk column.
 */
public final class BetaTerrainSampler {
    public static final int SNOW = 0;
    public static final int GRASS = 1;
    public static final int FOREST = 2;
    public static final int DESERT = 3;

    private final OldNoiseGeneratorOctaves2 temperatureNoise;
    private final OldNoiseGeneratorOctaves2 humidityNoise;
    private final OldNoiseGeneratorOctaves2 weirdnessNoise;
    private final NoiseOctavesBeta lowNoise;
    private final NoiseOctavesBeta highNoise;
    private final NoiseOctavesBeta selectorNoise;
    private final NoiseOctavesBeta depthNoise;
    private final NoiseOctavesBeta scaleNoise;

    private double[] depthField;
    private double[] scaleField;
    private double[] selectorField;
    private double[] lowField;
    private double[] highField;
    private double[] temperature = new double[256];
    private double[] humidity = new double[256];
    private double[] rawTemperature;
    private double[] rawHumidity;
    private double[] weirdness;
    private final int[] tops = new int[256];

    public BetaTerrainSampler(long seed) {
        temperatureNoise = new OldNoiseGeneratorOctaves2(new Random(seed * 9871L), 4);
        humidityNoise = new OldNoiseGeneratorOctaves2(new Random(seed * 39811L), 4);
        weirdnessNoise = new OldNoiseGeneratorOctaves2(new Random(seed * 543321L), 2);

        Random random = new Random(seed);
        lowNoise = new NoiseOctavesBeta(random, 16);
        highNoise = new NoiseOctavesBeta(random, 16);
        selectorNoise = new NoiseOctavesBeta(random, 8);
        new NoiseOctavesBeta(random, 4);
        new NoiseOctavesBeta(random, 4);
        depthNoise = new NoiseOctavesBeta(random, 10);
        scaleNoise = new NoiseOctavesBeta(random, 16);
    }

    public void sampleChunk(int chunkX, int chunkZ, int[] height, int[] biome) {
        climate(chunkX << 4, chunkZ << 4, temperature, humidity);

        double[] density = densityField(chunkX << 2, chunkZ << 2, temperature, humidity);
        Arrays.fill(tops, 0);
        fillHeights(density, tops);
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int index = localX * 16 + localZ;
                height[index] = tops[localZ * 16 + localX];
                int blockX = (chunkX << 4) + localX;
                int blockZ = (chunkZ << 4) + localZ;
                biome[index] = category(
                        (float) BetaClimate.temperature(blockX, blockZ, temperature[index]),
                        (float) BetaClimate.humidity(blockX, blockZ, humidity[index]));
            }
        }
    }

    private void climate(int blockX, int blockZ, double[] temperature, double[] humidity) {
        rawTemperature = temperatureNoise.func_4112_a(
                rawTemperature, blockX, blockZ, 16, 16, 0.02500000037252903D, 0.02500000037252903D, 0.25D);
        rawHumidity = humidityNoise.func_4112_a(
                rawHumidity, blockX, blockZ, 16, 16, 0.05000000074505806D, 0.05000000074505806D, 1.0D / 3.0D);
        weirdness = weirdnessNoise.func_4112_a(
                weirdness, blockX, blockZ, 16, 16, 0.25D, 0.25D, 0.5882352941176471D);

        int index = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                double blend = weirdness[index] * 1.1D + 0.5D;
                double temperatureValue = (rawTemperature[index] * 0.15D + 0.7D) * 0.99D + blend * 0.01D;
                double humidityValue = (rawHumidity[index] * 0.15D + 0.5D) * 0.998D + blend * 0.002D;
                temperatureValue = 1.0D - (1.0D - temperatureValue) * (1.0D - temperatureValue);
                if (temperatureValue < 0.0D) {
                    temperatureValue = 0.0D;
                }
                if (humidityValue < 0.0D) {
                    humidityValue = 0.0D;
                }
                if (temperatureValue > 1.0D) {
                    temperatureValue = 1.0D;
                }
                if (humidityValue > 1.0D) {
                    humidityValue = 1.0D;
                }
                temperature[index] = temperatureValue;
                humidity[index] = humidityValue;
                index++;
            }
        }
    }

    private double[] densityField(int x, int z, double[] temperature, double[] humidity) {
        int xSize = 5;
        int ySize = 17;
        int zSize = 5;
        double[] density = new double[xSize * ySize * zSize];
        double horizontal = 684.412D;
        double vertical = 684.412D;
        depthField = depthNoise.generateNoiseOctaves(depthField, x, z, xSize, zSize, 1.121D, 1.121D, 0.5D);
        scaleField = scaleNoise.generateNoiseOctaves(scaleField, x, z, xSize, zSize, 200.0D, 200.0D, 0.5D);
        selectorField = selectorNoise.generateNoiseOctaves(
                selectorField, x, 0.0D, z, xSize, ySize, zSize,
                horizontal / 80.0D, vertical / 160.0D, horizontal / 80.0D);
        lowField = lowNoise.generateNoiseOctaves(
                lowField, x, 0.0D, z, xSize, ySize, zSize, horizontal, vertical, horizontal);
        highField = highNoise.generateNoiseOctaves(
                highField, x, 0.0D, z, xSize, ySize, zSize, horizontal, vertical, horizontal);

        int densityIndex = 0;
        int cellIndex = 0;
        int step = 16 / xSize;
        for (int cellX = 0; cellX < xSize; cellX++) {
            int sampleX = cellX * step + step / 2;
            for (int cellZ = 0; cellZ < zSize; cellZ++) {
                int sampleZ = cellZ * step + step / 2;
                double climate = humidity[sampleX * 16 + sampleZ] * temperature[sampleX * 16 + sampleZ];
                double shaped = 1.0D - climate;
                shaped *= shaped;
                shaped *= shaped;
                shaped = 1.0D - shaped;

                double depth = (depthField[cellIndex] + 256.0D) / 512.0D * shaped;
                if (depth > 1.0D) {
                    depth = 1.0D;
                }
                double scale = scaleField[cellIndex] / 8000.0D;
                if (scale < 0.0D) {
                    scale = -scale * 0.3D;
                }
                scale = scale * 3.0D - 2.0D;
                int worldX = (x + cellX) << 2;
                int worldZ = (z + cellZ) << 2;
                scale = BetaClimate.shapeDepth(worldX, worldZ, scale);
                if (scale < 0.0D) {
                    scale /= 2.0D;
                    if (scale < -1.0D) {
                        scale = -1.0D;
                    }
                    scale /= 1.4D;
                    scale /= 2.0D;
                    depth = 0.0D;
                } else {
                    if (scale > 1.0D) {
                        scale = 1.0D;
                    }
                    scale /= 8.0D;
                }
                if (depth < 0.0D) {
                    depth = 0.0D;
                }
                depth += 0.5D;
                scale = scale * ySize / 16.0D;
                double center = ySize / 2.0D + scale * 4.0D;
                center = BetaClimate.shapeHeight(worldX, worldZ, center);
                cellIndex++;

                for (int cellY = 0; cellY < ySize; cellY++) {
                    double offset = (cellY - center) * 12.0D / depth;
                    if (offset < 0.0D) {
                        offset *= 4.0D;
                    }
                    double low = lowField[densityIndex] / 512.0D;
                    double high = highField[densityIndex] / 512.0D;
                    double selector = (selectorField[densityIndex] / 10.0D + 1.0D) / 2.0D;
                    double value;
                    if (selector < 0.0D) {
                        value = low;
                    } else if (selector > 1.0D) {
                        value = high;
                    } else {
                        value = low + (high - low) * selector;
                    }
                    value -= offset;
                    if (cellY > ySize - 4) {
                        double fade = (cellY - (ySize - 4)) / 3.0D;
                        value = value * (1.0D - fade) + -10.0D * fade;
                    }
                    density[densityIndex] = value;
                    densityIndex++;
                }
            }
        }
        return density;
    }

    private static void fillHeights(double[] density, int[] height) {
        int xSize = 5;
        int ySize = 17;
        int zSize = 5;
        for (int cellX = 0; cellX < 4; cellX++) {
            for (int cellZ = 0; cellZ < 4; cellZ++) {
                for (int cellY = 0; cellY < 16; cellY++) {
                    double base = densityValue(density, cellX, cellZ, cellY, xSize, ySize, zSize);
                    double east = densityValue(density, cellX + 1, cellZ, cellY, xSize, ySize, zSize);
                    double south = densityValue(density, cellX, cellZ + 1, cellY, xSize, ySize, zSize);
                    double southEast = densityValue(density, cellX + 1, cellZ + 1, cellY, xSize, ySize, zSize);
                    double above = densityValue(density, cellX, cellZ, cellY + 1, xSize, ySize, zSize);
                    double aboveEast = densityValue(density, cellX + 1, cellZ, cellY + 1, xSize, ySize, zSize);
                    double aboveSouth = densityValue(density, cellX, cellZ + 1, cellY + 1, xSize, ySize, zSize);
                    double aboveSouthEast = densityValue(density, cellX + 1, cellZ + 1, cellY + 1, xSize, ySize, zSize);
                    double step = 0.125D;
                    double riseBase = (above - base) * step;
                    double riseSouth = (aboveSouth - south) * step;
                    double riseEast = (aboveEast - east) * step;
                    double riseSouthEast = (aboveSouthEast - southEast) * step;
                    for (int subY = 0; subY < 8; subY++) {
                        int y = cellY * 8 + subY;
                        double horizontal = 0.25D;
                        double edgeZ0 = base;
                        double edgeZ1 = south;
                        double stepX0 = (east - base) * horizontal;
                        double stepX1 = (southEast - south) * horizontal;
                        for (int stepX = 0; stepX < 4; stepX++) {
                            double along = edgeZ0;
                            double stepZ = (edgeZ1 - edgeZ0) * horizontal;
                            int x = cellX * 4 + stepX;
                            for (int subZ = 0; subZ < 4; subZ++) {
                                if (along > 0.0D) {
                                    height[(cellZ * 4 + subZ) * 16 + x] = y;
                                }
                                along += stepZ;
                            }
                            edgeZ0 += stepX0;
                            edgeZ1 += stepX1;
                        }
                        base += riseBase;
                        south += riseSouth;
                        east += riseEast;
                        southEast += riseSouthEast;
                    }
                }
            }
        }
    }

    private static double densityValue(double[] density, int x, int z, int y, int xSize, int ySize, int zSize) {
        return density[(x * zSize + z) * ySize + y];
    }

    static int category(float temperature, float rainfall) {
        rainfall *= temperature;
        if (temperature < 0.1F) {
            return SNOW;
        }
        if (rainfall < 0.2F) {
            if (temperature < 0.5F) {
                return SNOW;
            }
            if (temperature < 0.95F) {
                return GRASS;
            }
            return DESERT;
        }
        if (rainfall > 0.5F && temperature < 0.7F) {
            return FOREST;
        }
        if (temperature < 0.5F) {
            return SNOW;
        }
        if (temperature < 0.97F) {
            return rainfall < 0.35F ? GRASS : FOREST;
        }
        if (rainfall < 0.45F) {
            return GRASS;
        }
        return FOREST;
    }
}
