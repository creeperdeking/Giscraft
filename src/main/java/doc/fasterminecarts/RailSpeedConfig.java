package doc.fasterminecarts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public final class RailSpeedConfig {
    private static final float DEFAULT_SPEED_MPS = 16.0F;
    private static final float MAX_SPEED_MPS = 24.0F;
    private static final String KEY = "maxSpeedMetersPerSecond";

    public static volatile float speedBlocksPerTick = DEFAULT_SPEED_MPS / 20.0F;

    private RailSpeedConfig() {
    }

    static synchronized void load(File minecraftDirectory) {
        File configDirectory = new File(minecraftDirectory, "config");
        File configFile = new File(configDirectory, "FasterVanillaMinecarts.cfg");

        Properties properties = new Properties();
        float speedMps = DEFAULT_SPEED_MPS;

        if (configFile.isFile()) {
            try (InputStream in = new FileInputStream(configFile)) {
                properties.load(in);
                String raw = properties.getProperty(KEY);
                if (raw != null) {
                    speedMps = Float.parseFloat(raw.trim());
                }
            } catch (IOException | NumberFormatException e) {
                System.err.println("[FasterVanillaMinecarts] Could not read config; using "
                        + DEFAULT_SPEED_MPS + " m/s.");
            }
        }

        if (!(speedMps > 0.0F) || speedMps > MAX_SPEED_MPS) {
            System.err.println("[FasterVanillaMinecarts] " + KEY
                    + " must be > 0 and <= " + MAX_SPEED_MPS
                    + "; using " + DEFAULT_SPEED_MPS + " m/s.");
            speedMps = DEFAULT_SPEED_MPS;
        }

        speedBlocksPerTick = speedMps / 20.0F;

        properties.setProperty(KEY, Float.toString(speedMps));
        if (!configDirectory.exists() && !configDirectory.mkdirs()) {
            System.err.println("[FasterVanillaMinecarts] Could not create config directory: "
                    + configDirectory);
            return;
        }

        try (OutputStream out = new FileOutputStream(configFile)) {
            properties.store(out,
                    "Faster Vanilla Minecarts 1.7.10\n"
                            + "Vanilla: 8.0 m/s. Suggested: 12.0-16.0 m/s. Maximum supported by Forge's cart cap: 24.0 m/s.");
        } catch (IOException e) {
            System.err.println("[FasterVanillaMinecarts] Could not write config: "
                    + e.getMessage());
        }

        System.out.println("[FasterVanillaMinecarts] Rail speed limit: " + speedMps + " m/s");
    }
}
