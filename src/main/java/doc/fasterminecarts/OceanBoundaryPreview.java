package doc.fasterminecarts;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

/**
 * Draws the climate-shifted beta terrain map with the ocean boundary applied.
 * Snow is white, grassland light green, forest dark green, desert yellow.
 *
 * java -cp Giscraft-1.3.15.jar doc.fasterminecarts.OceanBoundaryPreview --seed 123 --out preview.png
 */
public final class OceanBoundaryPreview {
    private static final int TICK_BLOCKS = 1024;
    private static final int AXIS_LEFT = 52;
    private static final int AXIS_BOTTOM = 48;
    private static final File DEFAULT_CONFIG = new File(
            "C:/Users/alexi/curseforge/minecraft/Instances/1.7.10/config/giscraft.cfg");

    private OceanBoundaryPreview() {
    }

    public static void main(String[] args) throws IOException {
        if (has(args, "--help") || has(args, "-h")) {
            System.out.println("Usage: java -cp Giscraft-1.3.15.jar doc.fasterminecarts.OceanBoundaryPreview [options]");
            System.out.println("  --seed <long>          World seed. Terrain and coastline noise depend on it.");
            System.out.println("  --config <giscraft.cfg>  Read ocean_boundary. Default: the 1.7.10 instance config.");
            System.out.println("  --out <file.png>       Output image. Default: ocean-preview.png");
            System.out.println("  --radius <blocks>      Half-width of the map around the center.");
            System.out.println("  --pixels <n>           Map width and height. Default: 320 with terrain, 1000 for zones.");
            System.out.println("  --owg <jar>            NostalgiaGenerator jar. Default: the 1.7.10 instance mods folder.");
            System.out.println("  --zone                 Draw only the ocean zones, without beta terrain.");
            System.out.println("  --threads <n>          Terrain sampling threads. Default: all processors.");
            System.out.println("  --centerX --centerZ");
            System.out.println("  --landRadius --coastFade --shallowOcean --deepOcean --iceBeyond --flowerBiome");
            System.out.println("    Each width is added onto the ring inside it. landRadius is where the continent is still whole.");
            System.out.println("  --frozenLead --icebergLead --pyramidInset");
            System.out.println("    Distances inward from the ice wall.");
            System.out.println("  --betaEnd --continentEnd --climateStart --climateEnd --dryStart --dryEnd");
            System.out.println("    Percents of the coastline. 100 is where the continent has faded to sea.");
            System.out.println("  --centralSea --riverWidth");
            System.out.println("  --seaLevel --oceanFloor --oceanFloorVariation");
            System.out.println("  --coastlineAmplitude --coastlineScale --deepSlope");
            System.out.println("  --deepOceanFloor --iceWallHeight --bedrockExtra --iceWaveAmplitude --iceHeightJitter");
            System.out.println("  --circular true|false --coastlineNoise true|false --useDeepOcean true|false");
            return;
        }

        OceanBoundaryMath.Settings settings = defaults();
        String configPath = option(args, "--config");
        File configFile = configPath != null ? new File(configPath) : DEFAULT_CONFIG;
        if (configFile.isFile()) {
            applyConfig(settings, configFile);
            System.out.println("Config " + configFile.getAbsolutePath());
        } else if (configPath != null) {
            throw new IOException("Missing config " + configFile.getAbsolutePath());
        }
        applyOverrides(settings, args);
        OceanBoundaryConfig.resolveRings(
                OceanBoundaryConfig.landRadius,
                OceanBoundaryConfig.coastFade,
                OceanBoundaryConfig.shallowOcean,
                OceanBoundaryConfig.deepOcean,
                OceanBoundaryConfig.iceBeyond,
                OceanBoundaryConfig.flowerBiome,
                OceanBoundaryConfig.frozenLead,
                OceanBoundaryConfig.icebergReach,
                OceanBoundaryConfig.pyramidInset);
        OceanBoundaryConfig.copyBoundary(settings);
        ZoneScale.resolve(settings.fullOceanRadius);
        System.out.println(
                (settings.circular ? "Circle" : "Square")
                        + "  land " + settings.transitionStart
                        + "  fade +" + OceanBoundaryConfig.coastFade
                        + "  shallow +" + OceanBoundaryConfig.shallowOcean
                        + "  deep +" + OceanBoundaryConfig.deepOcean
                        + "  ice +" + OceanBoundaryConfig.iceBeyond
                        + "  flowers +" + OceanBoundaryConfig.flowerBiome
                        + "  coast +/- " + settings.coastlineAmplitude);
        System.out.println(
                "Continent radius " + OceanBoundaryConfig.continentalRadius
                        + "  beta " + OceanBoundaryConfig.betaEnd
                        + "  continents " + OceanBoundaryConfig.continentEnd
                        + "  climate " + OceanBoundaryConfig.climateStart + "-" + OceanBoundaryConfig.climateEnd
                        + "  dry " + OceanBoundaryConfig.dryStart + "-" + OceanBoundaryConfig.dryEnd
                        + "  sea " + OceanBoundaryConfig.centralSea
                        + "  rivers " + OceanBoundaryConfig.riverWidth);

        long seed = Long.parseLong(option(args, "--seed", "0"));
        int radius = Integer.parseInt(option(args, "--radius", defaultRadius(settings)));
        File owg = findOwg(args);
        boolean terrain = !has(args, "--zone") && owg != null && previewClassesPresent();
        if (!has(args, "--zone") && !terrain) {
            System.out.println("Beta terrain preview needs the NostalgiaGenerator jar. Drawing ocean zones instead.");
            System.out.println("Pass --owg path\\to\\NostalgiaGenerator.jar to use the real generator.");
        }
        int pixels = Integer.parseInt(option(args, "--pixels", terrain ? "320" : "1000"));
        File output = new File(option(args, "--out", "ocean-preview.png"));
        if (pixels < 16 || radius < 1) {
            throw new IllegalArgumentException("pixels must be at least 16 and radius at least 1");
        }

        BufferedImage image = terrain
                ? renderTerrain(settings, seed, pixels, radius, owg, threadCount(args))
                : renderZones(settings, seed, pixels, radius);
        ImageIO.write(image, "png", output);
        System.out.println("Wrote " + output.getAbsolutePath());
        if (!terrain) {
            System.out.println("Green is unmodified Old World Gen. The picture does not show Beta hills or biomes.");
        }
    }

    private static BufferedImage renderTerrain(
            OceanBoundaryMath.Settings settings,
            long seed,
            int pixels,
            int radius,
            File owgJar,
            int threads) throws IOException {
        double span = radius * 2.0D;
        int[] chunkX = new int[pixels * pixels];
        int[] chunkZ = new int[pixels * pixels];
        int[] column = new int[pixels * pixels];
        Map<Long, Integer> slots = new HashMap<Long, Integer>();
        for (int py = 0; py < pixels; py++) {
            int blockZ = settings.centerZ - radius + (int) Math.floor((py + 0.5D) * span / pixels);
            for (int px = 0; px < pixels; px++) {
                int blockX = settings.centerX - radius + (int) Math.floor((px + 0.5D) * span / pixels);
                int cx = blockX >> 4;
                int cz = blockZ >> 4;
                int index = py * pixels + px;
                chunkX[index] = cx;
                chunkZ[index] = cz;
                column[index] = ((blockZ & 15) << 4) | (blockX & 15);
                slots.put(Long.valueOf((((long) cz) << 32) ^ (cx & 0xffffffffL)), Integer.valueOf(0));
            }
        }

        int count = slots.size();
        int[] uniqueX = new int[count];
        int[] uniqueZ = new int[count];
        int slot = 0;
        for (int index = 0; index < chunkX.length; index++) {
            long key = (((long) chunkZ[index]) << 32) ^ (chunkX[index] & 0xffffffffL);
            Integer existing = slots.get(Long.valueOf(key));
            if (existing.intValue() == 0) {
                uniqueX[slot] = chunkX[index];
                uniqueZ[slot] = chunkZ[index];
                slots.put(Long.valueOf(key), Integer.valueOf(slot + 1));
                slot++;
            }
        }
        count = slot;
        System.out.println("Sampling " + count + " beta chunks on " + threads + " threads. A finer --pixels takes longer.");

        byte[] heights = new byte[count * 256];
        byte[] biomes = new byte[count * 256];
        OceanBoundaryConfig.centerX = settings.centerX;
        OceanBoundaryConfig.centerZ = settings.centerZ;
        OceanBoundaryConfig.circular = settings.circular;
        OceanBoundaryConfig.coastlineNoise = settings.coastlineNoise;
        OceanBoundaryConfig.coastlineAmplitude = settings.coastlineAmplitude;
        OceanBoundaryConfig.seaLevel = settings.seaLevel;
        OceanBoundaryConfig.fullOceanRadius = settings.fullOceanRadius;
        BetaClimate.setSeed(seed);
        sampleChunks(owgJar, seed, uniqueX, uniqueZ, count, heights, biomes, threads);

        int legend = 140;
        int legendTop = pixels + AXIS_BOTTOM;
        BufferedImage image = new BufferedImage(AXIS_LEFT + pixels, legendTop + legend, BufferedImage.TYPE_INT_RGB);
        for (int py = 0; py < pixels; py++) {
            for (int px = 0; px < pixels; px++) {
                int index = py * pixels + px;
                long key = (((long) chunkZ[index]) << 32) ^ (chunkX[index] & 0xffffffffL);
                int chunkSlot = slots.get(Long.valueOf(key)).intValue() - 1;
                int local = column[index];
                int localX = local & 15;
                int localZ = local >> 4;
                int base = chunkSlot * 256 + localX * 16 + localZ;
                int surface = heights[base] & 255;
                int biome = biomes[base] & 255;
                int blockX = settings.centerX - radius + (int) Math.floor((px + 0.5D) * span / pixels);
                int blockZ = settings.centerZ - radius + (int) Math.floor((py + 0.5D) * span / pixels);
                image.setRGB(AXIS_LEFT + px, py, terrainColor(settings, seed, blockX, blockZ, surface, biome));
            }
        }

        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int centerPixel = AXIS_LEFT + pixels / 2;
        int centerRow = pixels / 2;
        graphics.setColor(Color.BLACK);
        graphics.fillRect(centerPixel - 4, centerRow, 9, 1);
        graphics.fillRect(centerPixel, centerRow - 4, 1, 9);
        graphics.setColor(new Color(220, 40, 40));
        graphics.fillRect(centerPixel - 3, centerRow, 7, 1);
        graphics.fillRect(centerPixel, centerRow - 3, 1, 7);
        drawAxes(graphics, AXIS_LEFT, pixels, radius, settings.centerX, settings.centerZ);

        graphics.setColor(new Color(16, 16, 16));
        graphics.fillRect(0, legendTop, image.getWidth(), legend);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        graphics.setColor(Color.WHITE);
        String shape = settings.circular ? "circle" : "square";
        graphics.drawString("Seed " + seed + "   " + shape + "   land " + settings.transitionStart, 8, legendTop + 16);
        graphics.drawString("ocean " + settings.fullOceanRadius + "   deep " + settings.deepOceanStart, 8, legendTop + 32);
        drawSwatch(graphics, 8, legendTop + 42, new Color(243, 246, 248), "Snow");
        drawSwatch(graphics, 78, legendTop + 42, new Color(182, 211, 106), "Grass");
        drawSwatch(graphics, 158, legendTop + 42, new Color(46, 106, 52), "Forest");
        drawSwatch(graphics, 8, legendTop + 64, new Color(226, 200, 74), "Desert");
        drawSwatch(graphics, 88, legendTop + 64, new Color(46, 110, 168), "Water");
        drawSwatch(graphics, 168, legendTop + 64, new Color(26, 78, 122), "Deep");
        drawSwatch(graphics, 8, legendTop + 86, new Color(232, 244, 250), "Ice");
        drawSwatch(graphics, 78, legendTop + 86, new Color(110, 174, 69), "Meadow");
        drawSwatch(graphics, 178, legendTop + 86, new Color(5, 6, 10), "Void");
        graphics.setColor(new Color(180, 180, 180));
        graphics.drawString("North is up. Lighter land is higher.", 8, legendTop + 116);
        graphics.drawString("X along the bottom, Z on the left, every 1024.", 8, legendTop + 130);
        graphics.dispose();
        return image;
    }

    private static int terrainColor(
            OceanBoundaryMath.Settings settings,
            long seed,
            int x,
            int z,
            int surface,
            int biome) {
        int band = OceanBoundaryMath.bandAt(x, z, seed, settings);
        if (band == OceanBoundaryMath.BAND_VOID) {
            return 0x05060A;
        }
        if (band == OceanBoundaryMath.BAND_BEDROCK) {
            return meadowColor(x, z, seed);
        }
        if (band == OceanBoundaryMath.BAND_OCEAN || band == OceanBoundaryMath.BAND_FROZEN) {
            int pyramid = OceanBoundaryMath.pyramidTop(x, z, settings);
            if (pyramid >= settings.seaLevel) {
                return 0xB85A45;
            }
            int berg = OceanBoundaryMath.icebergHeight(x, z, seed, settings);
            if (berg > 0) {
                double shade = 0.78D + Math.min(berg, 30) / 90.0D;
                return scaleColor(0xF4FBFF, shade);
            }
        }
        if (band == OceanBoundaryMath.BAND_ICE) {
            int crest = OceanBoundaryMath.iceCrest(x, z, seed, settings);
            double shade = (crest - settings.seaLevel) / 50.0D;
            if (shade < 0.0D) {
                shade = 0.0D;
            }
            if (shade > 1.0D) {
                shade = 1.0D;
            }
            return scaleColor(0xF4FBFF, 0.72D + shade * 0.40D);
        }
        double amount = OceanBoundaryMath.transitionAt(x, z, seed, settings);
        int floor = OceanBoundaryMath.columnFloor(x, z, seed, settings);
        int target = OceanBoundaryMath.blendHeight(surface, floor, amount);
        boolean water = target < settings.seaLevel - 1;
        if (water) {
            boolean deep = settings.useDeepOcean
                    && OceanBoundaryMath.distanceAt(x, z, settings) >= settings.deepOceanStart;
            int color = waterColor(target, settings.seaLevel, deep);
            if (band == OceanBoundaryMath.BAND_FROZEN) {
                return blend(color, 0xE7F6FF, OceanBoundaryMath.freezeAmount(
                        OceanBoundaryMath.intoWall(x, z, seed, settings), settings));
            }
            return color;
        }
        return landColor(biome, target);
    }

    private static int meadowColor(int x, int z, long seed) {
        int cover = OceanBoundaryMath.meadowCover(x, z, seed);
        if (cover == OceanBoundaryMath.MEADOW_POND) {
            return 0x3D7EA6;
        }
        if (cover == OceanBoundaryMath.MEADOW_TOMB) {
            return 0x8E928C;
        }
        if (cover == OceanBoundaryMath.MEADOW_FLOWER) {
            return flowerColor(OceanBoundaryMath.flowerKind(x, z, seed));
        }
        return 0x6EAE45;
    }

    private static int flowerColor(int kind) {
        switch (kind) {
            case 0:
                return 0xF2E15A;
            case 1:
                return 0xD23A32;
            case 2:
                return 0x3E8FDB;
            case 3:
                return 0xB06AD4;
            case 4:
                return 0xF4F7F8;
            case 5:
                return 0xE24B4B;
            case 6:
                return 0xF08A2A;
            case 7:
                return 0xF7F7F2;
            case 8:
                return 0xF3A7C5;
            case 9:
                return 0xF4F0C8;
            case 10:
                return 0xE6C83A;
            case 11:
                return 0xC07AD8;
            case 12:
                return 0xC43B48;
            case 13:
                return 0xE7A0C4;
            default:
                return 0x6EAE45;
        }
    }

    private static int landColor(int biome, int height) {
        int base;
        if (biome == 0) {
            base = 0xF3F6F8;
        } else if (biome == 1) {
            base = 0xB6D36A;
        } else if (biome == 2) {
            base = 0x2E6A34;
        } else {
            base = 0xE2C84A;
        }
        double shade = (height - 54) / 70.0D;
        if (shade < 0.0D) {
            shade = 0.0D;
        }
        if (shade > 1.0D) {
            shade = 1.0D;
        }
        return scaleColor(base, 0.72D + shade * 0.40D);
    }

    private static int waterColor(int height, int seaLevel, boolean deep) {
        int depth = seaLevel - 1 - height;
        if (depth < 0) {
            depth = 0;
        }
        if (depth > 36) {
            depth = 36;
        }
        double amount = depth / 36.0D;
        if (deep) {
            return blend(0x2A6A9C, 0x1A4E7A, amount);
        }
        return blend(0x4C9ED0, 0x12304E, amount);
    }

    private static int scaleColor(int color, double scale) {
        int red = clamp((int) (((color >> 16) & 255) * scale));
        int green = clamp((int) (((color >> 8) & 255) * scale));
        int blue = clamp((int) ((color & 255) * scale));
        return (red << 16) | (green << 8) | blue;
    }

    private static int clamp(int channel) {
        if (channel < 0) {
            return 0;
        }
        if (channel > 255) {
            return 255;
        }
        return channel;
    }

    private static void sampleChunks(
            File owgJar,
            long seed,
            int[] chunkX,
            int[] chunkZ,
            int count,
            byte[] heights,
            byte[] biomes,
            int threads) throws IOException {
        File root = extractPreviewClasses();
        URLClassLoader loader = new URLClassLoader(
                new URL[] {root.toURI().toURL(), owgJar.toURI().toURL()},
                OceanBoundaryPreview.class.getClassLoader());
        try {
            Class<?> type = Class.forName("doc.fasterminecarts.BetaTerrainSampler", true, loader);
            final Method sample = type.getMethod("sampleChunk", int.class, int.class, int[].class, int[].class);
            final ConstructorInvoker factory = new ConstructorInvoker(type, seed);
            int workers = Math.max(1, Math.min(threads, count));
            ExecutorService pool = Executors.newFixedThreadPool(workers);
            final AtomicInteger next = new AtomicInteger();
            final AtomicInteger done = new AtomicInteger();
            final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
            final int total = count;
            final int[] workX = chunkX;
            final int[] workZ = chunkZ;
            for (int worker = 0; worker < workers; worker++) {
                pool.submit(new Runnable() {
                    public void run() {
                        try {
                            Object sampler = factory.create();
                            int[] height = new int[256];
                            int[] biome = new int[256];
                            int index;
                            while ((index = next.getAndIncrement()) < total && failure.get() == null) {
                                sample.invoke(
                                        sampler,
                                        Integer.valueOf(workX[index]),
                                        Integer.valueOf(workZ[index]),
                                        height,
                                        biome);
                                int base = index * 256;
                                for (int column = 0; column < 256; column++) {
                                    heights[base + column] = (byte) height[column];
                                    biomes[base + column] = (byte) biome[column];
                                }
                                int finished = done.incrementAndGet();
                                if (finished == total || finished % 4000 == 0) {
                                    System.out.println("  " + finished + " / " + total + " chunks");
                                }
                            }
                        } catch (Throwable error) {
                            failure.compareAndSet(null, error);
                        }
                    }
                });
            }
            pool.shutdown();
            if (!pool.awaitTermination(6, TimeUnit.HOURS)) {
                pool.shutdownNow();
                throw new IOException("Terrain sampling did not finish");
            }
            if (failure.get() != null) {
                throw new IOException("Could not sample beta terrain", failure.get());
            }
        } catch (RuntimeException failure) {
            throw new IOException("Could not sample beta terrain", failure);
        } catch (Exception failure) {
            throw new IOException("Could not sample beta terrain", failure);
        } finally {
            loader.close();
            deleteTree(root);
        }
    }

    private static final class ConstructorInvoker {
        private final java.lang.reflect.Constructor<?> constructor;
        private final long seed;

        ConstructorInvoker(Class<?> type, long seed) throws NoSuchMethodException {
            this.constructor = type.getConstructor(long.class);
            this.seed = seed;
        }

        Object create() throws Exception {
            return constructor.newInstance(Long.valueOf(seed));
        }
    }

    private static File extractPreviewClasses() throws IOException {
        File root = new File(System.getProperty("java.io.tmpdir"), "giscraft-preview-" + System.nanoTime());
        copyResource(
                "/assets/giscraft/preview/BetaTerrainSampler.bin",
                new File(root, "doc/fasterminecarts/BetaTerrainSampler.class"));
        copyResource(
                "/assets/giscraft/preview/NoiseGenerator.bin",
                new File(root, "net/minecraft/world/gen/NoiseGenerator.class"));
        return root;
    }

    private static void copyResource(String resource, File dest) throws IOException {
        InputStream input = OceanBoundaryPreview.class.getResourceAsStream(resource);
        if (input == null) {
            throw new IOException("Missing " + resource + " in the mod jar");
        }
        dest.getParentFile().mkdirs();
        try {
            FileOutputStream output = new FileOutputStream(dest);
            try {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    output.write(buffer, 0, read);
                }
            } finally {
                output.close();
            }
        } finally {
            input.close();
        }
    }

    private static void deleteTree(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (int index = 0; index < children.length; index++) {
                deleteTree(children[index]);
            }
        }
        file.delete();
    }

    private static boolean previewClassesPresent() {
        return OceanBoundaryPreview.class.getResource("/assets/giscraft/preview/BetaTerrainSampler.bin") != null;
    }

    private static File findOwg(String[] args) {
        String path = option(args, "--owg");
        if (path != null) {
            return new File(path);
        }
        File mods = new File("C:/Users/alexi/curseforge/minecraft/Instances/1.7.10/mods");
        File[] jars = mods.listFiles();
        if (jars == null) {
            return null;
        }
        File match = null;
        for (int index = 0; index < jars.length; index++) {
            String name = jars[index].getName();
            if (name.startsWith("NostalgiaGenerator") && name.endsWith(".jar")) {
                match = jars[index];
            }
        }
        return match;
    }

    private static int threadCount(String[] args) {
        String value = option(args, "--threads");
        if (value != null) {
            return Math.max(1, Integer.parseInt(value));
        }
        return Math.max(1, Runtime.getRuntime().availableProcessors());
    }

    private static BufferedImage renderZones(
            OceanBoundaryMath.Settings settings,
            long seed,
            int pixels,
            int radius) {
        int legend = 100;
        int legendTop = pixels + AXIS_BOTTOM;
        BufferedImage image = new BufferedImage(AXIS_LEFT + pixels, legendTop + legend, BufferedImage.TYPE_INT_RGB);
        double span = radius * 2.0D;
        int lowFloor = settings.oceanFloor;
        int highFloor = settings.oceanFloor;
        if (settings.useDeepOcean) {
            lowFloor = Math.min(lowFloor, settings.deepOceanFloor);
            highFloor = Math.max(highFloor, settings.deepOceanFloor);
        }
        int floorMin = Math.max(2, lowFloor - settings.oceanFloorVariation);
        int floorMax = Math.min(settings.seaLevel - 2, highFloor + settings.oceanFloorVariation);
        if (floorMax <= floorMin) {
            floorMax = floorMin + 1;
        }

        for (int py = 0; py < pixels; py++) {
            int blockZ = settings.centerZ - radius + (int) Math.floor((py + 0.5D) * span / pixels);
            for (int px = 0; px < pixels; px++) {
                int blockX = settings.centerX - radius + (int) Math.floor((px + 0.5D) * span / pixels);
                image.setRGB(AXIS_LEFT + px, py, colorAt(settings, seed, blockX, blockZ, floorMin, floorMax));
            }
        }

        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setColor(Color.WHITE);
        int centerPixel = AXIS_LEFT + pixels / 2;
        int centerRow = pixels / 2;
        graphics.fillRect(centerPixel - 3, centerRow, 7, 1);
        graphics.fillRect(centerPixel, centerRow - 3, 1, 7);
        drawAxes(graphics, AXIS_LEFT, pixels, radius, settings.centerX, settings.centerZ);

        graphics.setColor(new Color(16, 16, 16));
        graphics.fillRect(0, legendTop, image.getWidth(), legend);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        graphics.setColor(Color.WHITE);
        String shape = settings.circular ? "circle" : "square";
        graphics.drawString(
                "Seed " + seed + "   " + shape
                        + "   land to " + settings.transitionStart
                        + "   ocean at " + settings.fullOceanRadius
                        + "   deep at " + settings.deepOceanStart
                        + "   +/- " + settings.coastlineAmplitude + " coast",
                12,
                legendTop + 20);
        drawSwatch(graphics, 12, legendTop + 34, new Color(47, 107, 58), "Unmodified land");
        drawSwatch(graphics, 170, legendTop + 34, new Color(194, 178, 128), "Transition");
        drawSwatch(graphics, 300, legendTop + 34, new Color(61, 126, 166), "Ocean");
        drawSwatch(graphics, 400, legendTop + 34, new Color(36, 96, 150), "Deep ocean");
        drawSwatch(graphics, 12, legendTop + 56, new Color(183, 220, 236), "Frozen");
        drawSwatch(graphics, 110, legendTop + 56, new Color(231, 244, 250), "Ice wall");
        drawSwatch(graphics, 220, legendTop + 56, new Color(110, 174, 69), "Meadow");
        drawSwatch(graphics, 330, legendTop + 56, new Color(5, 6, 10), "Void");
        drawSwatch(graphics, 420, legendTop + 56, new Color(184, 90, 69), "Pyramid");
        graphics.setColor(new Color(180, 180, 180));
        graphics.drawString(
                "North is up. The white mark is the center. Ticks are every 1024 blocks from spawn.",
                12,
                legendTop + 86);
        graphics.dispose();
        return image;
    }

    private static int colorAt(
            OceanBoundaryMath.Settings settings,
            long seed,
            int x,
            int z,
            int floorMin,
            int floorMax) {
        int band = OceanBoundaryMath.bandAt(x, z, seed, settings);
        if (band == OceanBoundaryMath.BAND_VOID) {
            return 0x05060A;
        }
        if (band == OceanBoundaryMath.BAND_BEDROCK) {
            int cover = OceanBoundaryMath.meadowCover(x, z, seed);
            if (cover == OceanBoundaryMath.MEADOW_POND) {
                return 0x3D7EA6;
            }
            if (cover == OceanBoundaryMath.MEADOW_TOMB) {
                return 0x8E928C;
            }
            if (cover == OceanBoundaryMath.MEADOW_FLOWER) {
                return flowerColor(OceanBoundaryMath.flowerKind(x, z, seed));
            }
            return 0x6EAE45;
        }
        if (band == OceanBoundaryMath.BAND_ICE) {
            int crest = OceanBoundaryMath.iceCrest(x, z, seed, settings);
            double shade = (crest - settings.seaLevel) / 55.0D;
            if (shade < 0.0D) {
                shade = 0.0D;
            }
            if (shade > 1.0D) {
                shade = 1.0D;
            }
            return scaleColor(0xE7F4FA, 0.72D + shade * 0.40D);
        }
        if (band == OceanBoundaryMath.BAND_OCEAN || band == OceanBoundaryMath.BAND_FROZEN) {
            int pyramid = OceanBoundaryMath.pyramidTop(x, z, settings);
            if (pyramid >= settings.seaLevel) {
                return 0xB85A45;
            }
            if (pyramid > OceanBoundaryMath.columnFloor(x, z, seed, settings)) {
                return 0x7A3E32;
            }
            int berg = OceanBoundaryMath.icebergHeight(x, z, seed, settings);
            if (berg > 0) {
                return 0xF7FBFF;
            }
        }
        if (band == OceanBoundaryMath.BAND_FROZEN) {
            return 0xB7DCEC;
        }
        double amount = OceanBoundaryMath.transitionAt(x, z, seed, settings);
        if (amount <= 0.0D) {
            return 0x2F6B3A;
        }
        if (amount < 1.0D) {
            return blend(0xC2B280, 0x3D7EA6, amount);
        }
        double depth = (OceanBoundaryMath.columnFloor(x, z, seed, settings) - floorMin) / (double) (floorMax - floorMin);
        if (depth < 0.0D) {
            depth = 0.0D;
        }
        if (depth > 1.0D) {
            depth = 1.0D;
        }
        boolean deep = settings.useDeepOcean
                && OceanBoundaryMath.distanceAt(x, z, settings) >= settings.deepOceanStart;
        if (deep) {
            return blend(0x1A4A78, 0x2E6C9E, depth);
        }
        return blend(0x1E4C73, 0x6AA4C8, depth);
    }

    private static int blend(int from, int to, double amount) {
        int red = (int) (((from >> 16) & 255) * (1.0D - amount) + ((to >> 16) & 255) * amount);
        int green = (int) (((from >> 8) & 255) * (1.0D - amount) + ((to >> 8) & 255) * amount);
        int blue = (int) ((from & 255) * (1.0D - amount) + (to & 255) * amount);
        return (red << 16) | (green << 8) | blue;
    }

    private static void drawAxes(Graphics2D graphics, int mapLeft, int pixels, int radius, int centerX, int centerZ) {
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        graphics.setColor(new Color(210, 210, 210));
        graphics.drawLine(mapLeft, pixels, mapLeft + pixels, pixels);
        graphics.drawLine(mapLeft - 1, 0, mapLeft - 1, pixels);

        int slots = Math.max(32, (radius / TICK_BLOCKS + 4) * 4);
        drawAxisLabels(graphics, mapLeft, pixels, radius, centerX, true, new int[slots]);
        drawAxisLabels(graphics, mapLeft, pixels, radius, centerZ, false, new int[slots]);
    }

    private static int drawAxisLabels(
            Graphics2D graphics,
            int mapLeft,
            int pixels,
            int radius,
            int center,
            boolean horizontal,
            int[] placed) {
        FontMetrics metrics = graphics.getFontMetrics();
        int edge = center - radius;
        int mark = firstTick(edge);
        int limit = center + radius;
        int[] marks = new int[Math.max(1, (limit - mark) / TICK_BLOCKS + 2)];
        int count = 0;
        for (int value = mark; value <= limit && count < marks.length; value += TICK_BLOCKS) {
            marks[count] = value;
            count++;
        }
        int used = 0;
        for (int distance = 0; distance <= limit - mark + TICK_BLOCKS; distance += TICK_BLOCKS) {
            for (int index = 0; index < count; index++) {
                int value = marks[index];
                if (Math.abs(value) != distance && !(distance == 0 && value == 0)) {
                    continue;
                }
                double pixel = (value - center + radius) * (double) pixels / (radius * 2.0D) - 0.5D;
                if (pixel < -0.5D || pixel > pixels - 0.5D) {
                    continue;
                }
                int tick = (int) Math.round(pixel);
                if (tick < 0 || tick >= pixels) {
                    continue;
                }
                String label = Integer.toString(value);
                if (horizontal) {
                    graphics.setColor(new Color(210, 210, 210));
                    graphics.drawLine(mapLeft + tick, pixels, mapLeft + tick, pixels + 5);
                    int width = metrics.stringWidth(label);
                    int thickness = metrics.getHeight();
                    int start = mapLeft + tick - thickness / 2;
                    int end = start + thickness;
                    if (fits(placed, used, start, end)) {
                        AffineTransform saved = graphics.getTransform();
                        graphics.translate(mapLeft + tick + 4, pixels + 8 + width);
                        graphics.rotate(-Math.PI / 2.0D);
                        graphics.drawString(label, 0, 0);
                        graphics.setTransform(saved);
                        placed[used * 2] = start - 1;
                        placed[used * 2 + 1] = end + 1;
                        used++;
                    }
                } else {
                    graphics.setColor(new Color(210, 210, 210));
                    graphics.drawLine(mapLeft - 6, tick, mapLeft - 1, tick);
                    int width = metrics.stringWidth(label);
                    int top = tick - 5;
                    int bottom = tick + 6;
                    if (top < 0) {
                        bottom -= top;
                        top = 0;
                    }
                    if (bottom > pixels) {
                        top -= bottom - pixels;
                        bottom = pixels;
                    }
                    if (fits(placed, used, top, bottom)) {
                        graphics.drawString(label, mapLeft - 8 - width, tick + 4);
                        placed[used * 2] = top;
                        placed[used * 2 + 1] = bottom;
                        used++;
                    }
                }
            }
        }
        return used;
    }

    private static boolean fits(int[] placed, int used, int start, int end) {
        for (int index = 0; index < used; index++) {
            if (start < placed[index * 2 + 1] && end > placed[index * 2]) {
                return false;
            }
        }
        return true;
    }

    private static int firstTick(int edge) {
        if (edge >= 0) {
            return ((edge + TICK_BLOCKS - 1) / TICK_BLOCKS) * TICK_BLOCKS;
        }
        return (edge / TICK_BLOCKS) * TICK_BLOCKS;
    }

    private static void drawSwatch(Graphics2D graphics, int x, int y, Color color, String label) {
        graphics.setColor(color);
        graphics.fillRect(x, y, 14, 14);
        graphics.setColor(Color.WHITE);
        graphics.drawString(label, x + 20, y + 12);
    }

    private static OceanBoundaryMath.Settings defaults() {
        OceanBoundaryMath.Settings settings = new OceanBoundaryMath.Settings();
        settings.transitionStart = OceanBoundaryConfig.DEFAULT_LAND;
        settings.fullOceanRadius = OceanBoundaryConfig.DEFAULT_LAND + OceanBoundaryConfig.DEFAULT_FADE;
        settings.seaLevel = 64;
        settings.oceanFloor = 48;
        settings.oceanFloorVariation = 5;
        settings.coastlineNoise = true;
        settings.coastlineAmplitude = 200;
        settings.coastlineScale = 0.0015D;
        settings.circular = true;
        settings.useDeepOcean = true;
        settings.deepOceanStart = OceanBoundaryConfig.DEFAULT_LAND
                + OceanBoundaryConfig.DEFAULT_FADE
                + OceanBoundaryConfig.DEFAULT_SHALLOW;
        settings.deepOceanFloor = 33;
        settings.deepOceanTransition = 20;
        settings.iceWallGap = OceanBoundaryConfig.DEFAULT_DEEP;
        settings.iceSnowLead = OceanBoundaryConfig.DEFAULT_FROZEN;
        settings.icebergLead = OceanBoundaryConfig.DEFAULT_ICEBERG;
        settings.pyramidInset = OceanBoundaryConfig.DEFAULT_PYRAMID;
        settings.iceShelfLength = OceanBoundaryConfig.DEFAULT_ICE;
        settings.bedrockRun = OceanBoundaryConfig.DEFAULT_FLOWER;
        settings.iceWallHeight = 30;
        settings.bedrockExtra = 12;
        settings.iceWaveAmplitude = 48;
        settings.iceHeightJitter = 8;
        return settings;
    }

    private static String defaultRadius(OceanBoundaryMath.Settings settings) {
        int edge = OceanBoundaryMath.deepEdge(settings) + settings.iceWallGap
                + settings.iceShelfLength + settings.bedrockRun + settings.iceWaveAmplitude;
        if (settings.fullOceanRadius > edge) {
            edge = settings.fullOceanRadius;
        }
        edge += settings.coastlineNoise ? settings.coastlineAmplitude : 0;
        return Integer.toString(edge + 400);
    }

    private static void applyOverrides(OceanBoundaryMath.Settings settings, String[] args) {
        settings.centerX = integerOption(args, "--centerX", settings.centerX);
        settings.centerZ = integerOption(args, "--centerZ", settings.centerZ);
        OceanBoundaryConfig.landRadius = integerOption(args, "--landRadius", OceanBoundaryConfig.landRadius);
        OceanBoundaryConfig.coastFade = integerOption(args, "--coastFade", OceanBoundaryConfig.coastFade);
        OceanBoundaryConfig.shallowOcean = integerOption(args, "--shallowOcean", OceanBoundaryConfig.shallowOcean);
        OceanBoundaryConfig.deepOcean = integerOption(args, "--deepOcean", OceanBoundaryConfig.deepOcean);
        OceanBoundaryConfig.iceBeyond = integerOption(args, "--iceBeyond", OceanBoundaryConfig.iceBeyond);
        OceanBoundaryConfig.flowerBiome = integerOption(args, "--flowerBiome", OceanBoundaryConfig.flowerBiome);
        OceanBoundaryConfig.frozenLead = integerOption(args, "--frozenLead", OceanBoundaryConfig.frozenLead);
        OceanBoundaryConfig.icebergReach = integerOption(args, "--icebergLead", OceanBoundaryConfig.icebergReach);
        OceanBoundaryConfig.pyramidInset = integerOption(args, "--pyramidInset", OceanBoundaryConfig.pyramidInset);
        settings.seaLevel = integerOption(args, "--seaLevel", settings.seaLevel);
        settings.oceanFloor = integerOption(args, "--oceanFloor", settings.oceanFloor);
        settings.oceanFloorVariation = integerOption(args, "--oceanFloorVariation", settings.oceanFloorVariation);
        settings.coastlineAmplitude = integerOption(args, "--coastlineAmplitude", settings.coastlineAmplitude);
        settings.deepOceanFloor = integerOption(args, "--deepOceanFloor", settings.deepOceanFloor);
        settings.deepOceanTransition = integerOption(
                args, "--deepSlope", integerOption(args, "--deepOceanTransition", settings.deepOceanTransition));
        OceanBoundaryConfig.deepOceanTransition = settings.deepOceanTransition;
        settings.iceWallHeight = integerOption(args, "--iceWallHeight", settings.iceWallHeight);
        settings.bedrockExtra = integerOption(args, "--bedrockExtra", settings.bedrockExtra);
        settings.iceWaveAmplitude = integerOption(args, "--iceWaveAmplitude", settings.iceWaveAmplitude);
        settings.iceHeightJitter = integerOption(args, "--iceHeightJitter", settings.iceHeightJitter);
        String scale = option(args, "--coastlineScale");
        if (scale != null) {
            settings.coastlineScale = Double.parseDouble(scale);
        }
        String circular = option(args, "--circular");
        if (circular != null) {
            settings.circular = Boolean.parseBoolean(circular);
        }
        String noise = option(args, "--coastlineNoise");
        if (noise != null) {
            settings.coastlineNoise = Boolean.parseBoolean(noise);
        }
        String deep = option(args, "--useDeepOcean");
        if (deep != null) {
            settings.useDeepOcean = Boolean.parseBoolean(deep);
        }
        OceanBoundaryConfig.betaEndPercent = floatOption(args, "--betaEnd", OceanBoundaryConfig.betaEndPercent);
        OceanBoundaryConfig.continentEndPercent = floatOption(args, "--continentEnd", OceanBoundaryConfig.continentEndPercent);
        OceanBoundaryConfig.climateStartPercent = floatOption(args, "--climateStart", OceanBoundaryConfig.climateStartPercent);
        OceanBoundaryConfig.climateEndPercent = floatOption(args, "--climateEnd", OceanBoundaryConfig.climateEndPercent);
        OceanBoundaryConfig.dryStartPercent = floatOption(args, "--dryStart", OceanBoundaryConfig.dryStartPercent);
        OceanBoundaryConfig.dryEndPercent = floatOption(args, "--dryEnd", OceanBoundaryConfig.dryEndPercent);
        OceanBoundaryConfig.centralSea = integerOption(args, "--centralSea", OceanBoundaryConfig.centralSea);
        OceanBoundaryConfig.riverWidth = integerOption(args, "--riverWidth", OceanBoundaryConfig.riverWidth);
    }

    private static void applyConfig(OceanBoundaryMath.Settings settings, File file) throws IOException {
        Map<String, String> values = new HashMap<String, String>();
        Map<String, String> continent = new HashMap<String, String>();
        Map<String, String> inland = new HashMap<String, String>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String category = "";
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                int comment = line.indexOf('#');
                if (comment >= 0) {
                    line = line.substring(0, comment);
                }
                line = line.trim();
                if (line.endsWith("{")) {
                    category = line.substring(0, line.length() - 1).trim();
                    continue;
                }
                if (line.startsWith("}")) {
                    category = "";
                    continue;
                }
                if (category.length() == 0) {
                    continue;
                }
                int split = line.indexOf('=');
                if (split <= 0) {
                    continue;
                }
                String key = line.substring(0, split).trim();
                int colon = key.indexOf(':');
                if (colon >= 0) {
                    key = key.substring(colon + 1);
                }
                String value = line.substring(split + 1).trim();
                if ("continent".equals(category)) {
                    continent.put(key, value);
                } else if ("inland".equals(category)) {
                    inland.put(key, value);
                } else if ("ocean_boundary".equals(category)) {
                    values.put(key, value);
                }
            }
        } finally {
            reader.close();
        }

        settings.centerX = configInt(values, "centerX", settings.centerX);
        settings.centerZ = configInt(values, "centerZ", settings.centerZ);
        if (values.containsKey("landRadius")) {
            OceanBoundaryConfig.landRadius = configInt(values, "landRadius", OceanBoundaryConfig.landRadius);
            OceanBoundaryConfig.coastFade = configInt(values, "coastFade", OceanBoundaryConfig.coastFade);
            OceanBoundaryConfig.shallowOcean = configInt(values, "shallowOcean", OceanBoundaryConfig.shallowOcean);
            OceanBoundaryConfig.deepOcean = configInt(values, "deepOcean", OceanBoundaryConfig.deepOcean);
            OceanBoundaryConfig.iceBeyond = configInt(values, "iceBeyond", OceanBoundaryConfig.iceBeyond);
            OceanBoundaryConfig.flowerBiome = configInt(values, "flowerBiome", OceanBoundaryConfig.flowerBiome);
            OceanBoundaryConfig.frozenLead = configInt(values, "frozenLead", OceanBoundaryConfig.frozenLead);
            OceanBoundaryConfig.icebergReach = configInt(values, "icebergLead", OceanBoundaryConfig.icebergReach);
            OceanBoundaryConfig.pyramidInset = configInt(values, "pyramidInset", OceanBoundaryConfig.pyramidInset);
        } else {
            int land = configInt(values, "transitionStart", OceanBoundaryConfig.landRadius);
            int shore = configInt(values, "fullOceanRadius", land + OceanBoundaryConfig.coastFade);
            int deepStart = configInt(values, "deepOceanStart", shore + OceanBoundaryConfig.shallowOcean);
            OceanBoundaryConfig.landRadius = land;
            OceanBoundaryConfig.coastFade = shore > land ? shore - land : 0;
            OceanBoundaryConfig.shallowOcean = deepStart > shore ? deepStart - shore : 0;
            OceanBoundaryConfig.deepOcean = configInt(values, "iceWallGap", OceanBoundaryConfig.deepOcean);
            OceanBoundaryConfig.iceBeyond = configInt(values, "iceShelfLength", OceanBoundaryConfig.iceBeyond);
            OceanBoundaryConfig.flowerBiome = configInt(values, "bedrockRun", OceanBoundaryConfig.flowerBiome);
            OceanBoundaryConfig.frozenLead = configInt(values, "iceSnowLead", OceanBoundaryConfig.frozenLead);
            OceanBoundaryConfig.icebergReach = configInt(values, "icebergLead", OceanBoundaryConfig.icebergReach);
            OceanBoundaryConfig.pyramidInset = OceanBoundaryConfig.deepOcean / 2;
        }
        settings.seaLevel = configInt(values, "seaLevel", settings.seaLevel);
        settings.oceanFloor = configInt(values, "oceanFloor", settings.oceanFloor);
        settings.oceanFloorVariation = configInt(values, "oceanFloorVariation", settings.oceanFloorVariation);
        settings.coastlineAmplitude = configInt(values, "coastlineAmplitude", settings.coastlineAmplitude);
        settings.deepOceanFloor = configInt(values, "deepOceanFloor", settings.deepOceanFloor);
        if (values.containsKey("deepSlope")) {
            settings.deepOceanTransition = configInt(values, "deepSlope", settings.deepOceanTransition);
        } else {
            settings.deepOceanTransition = configInt(values, "deepOceanTransition", settings.deepOceanTransition);
        }
        OceanBoundaryConfig.deepOceanTransition = settings.deepOceanTransition;
        settings.iceWallHeight = configInt(values, "iceWallHeight", settings.iceWallHeight);
        settings.bedrockExtra = configInt(values, "bedrockExtra", settings.bedrockExtra);
        settings.iceWaveAmplitude = configInt(values, "iceWaveAmplitude", settings.iceWaveAmplitude);
        settings.iceHeightJitter = configInt(values, "iceHeightJitter", settings.iceHeightJitter);
        settings.coastlineScale = configDouble(values, "coastlineScale", settings.coastlineScale);
        OceanBoundaryConfig.betaEndPercent = configFloat(continent, "betaEnd", OceanBoundaryConfig.betaEndPercent);
        OceanBoundaryConfig.continentEndPercent = configFloat(continent, "continentEnd", OceanBoundaryConfig.continentEndPercent);
        OceanBoundaryConfig.climateStartPercent = configFloat(continent, "climateStart", OceanBoundaryConfig.climateStartPercent);
        OceanBoundaryConfig.climateEndPercent = configFloat(continent, "climateEnd", OceanBoundaryConfig.climateEndPercent);
        OceanBoundaryConfig.dryStartPercent = configFloat(continent, "dryStart", OceanBoundaryConfig.dryStartPercent);
        OceanBoundaryConfig.dryEndPercent = configFloat(continent, "dryEnd", OceanBoundaryConfig.dryEndPercent);
        OceanBoundaryConfig.centralSea = configInt(inland, "centralSea", OceanBoundaryConfig.centralSea);
        OceanBoundaryConfig.riverWidth = configInt(inland, "riverWidth", OceanBoundaryConfig.riverWidth);
        settings.circular = configBoolean(values, "circular", settings.circular);
        settings.coastlineNoise = configBoolean(values, "coastlineNoise", settings.coastlineNoise);
        settings.useDeepOcean = configBoolean(values, "useDeepOcean", settings.useDeepOcean);
    }

    private static int configInt(Map<String, String> values, String key, int fallback) {
        String value = values.get(key);
        return value == null ? fallback : Integer.parseInt(value);
    }

    private static float configFloat(Map<String, String> values, String key, float fallback) {
        String value = values.get(key);
        return value == null ? fallback : Float.parseFloat(value);
    }

    private static double configDouble(Map<String, String> values, String key, double fallback) {
        String value = values.get(key);
        return value == null ? fallback : Double.parseDouble(value);
    }

    private static boolean configBoolean(Map<String, String> values, String key, boolean fallback) {
        String value = values.get(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    private static float floatOption(String[] args, String name, float fallback) {
        String value = option(args, name);
        return value == null ? fallback : Float.parseFloat(value);
    }

    private static int integerOption(String[] args, String name, int fallback) {
        String value = option(args, name);
        return value == null ? fallback : Integer.parseInt(value);
    }

    private static String option(String[] args, String name, String fallback) {
        String value = option(args, name);
        return value == null ? fallback : value;
    }

    private static String option(String[] args, String name) {
        for (int index = 0; index < args.length - 1; index++) {
            if (name.equals(args[index])) {
                return args[index + 1];
            }
        }
        return null;
    }

    private static boolean has(String[] args, String name) {
        for (int index = 0; index < args.length; index++) {
            if (name.equalsIgnoreCase(args[index])) {
                return true;
            }
        }
        return false;
    }
}
