package doc.fasterminecarts;

/**
 * A round sea at the world center, and one river from it toward each pyramid.
 * The rivers meander and run out to the outer ocean. North is negative Z.
 */
final class InlandWater {
    static final int NONE = 0;
    static final int SEA = 1;
    static final int RIVER = 2;

    private static final int RIVERS = 4;
    private static final double DIAGONAL = 0.7071067811865476D;
    /** Northeast, southeast, southwest, northwest. Matches the pyramid order. */
    private static final double[] DIR_X = {DIAGONAL, DIAGONAL, -DIAGONAL, -DIAGONAL};
    private static final double[] DIR_Z = {-DIAGONAL, DIAGONAL, DIAGONAL, -DIAGONAL};

    private static long seed;

    private InlandWater() {
    }

    static void bind(long worldSeed) {
        seed = worldSeed;
    }

    static double carveDepth(int x, int z, double scale) {
        if (cover(x, z) < 0.72D) {
            return scale;
        }
        double dug = -1.2D;
        if (dug < scale) {
            return dug;
        }
        return scale;
    }

    static double carveHeight(int x, int z, double center) {
        double amount = cover(x, z);
        if (amount <= 0.0D) {
            return center;
        }
        double floor = floorY(x, z) / 8.0D;
        if (floor < center) {
            center += (floor - center) * amount;
        }
        return center;
    }

    static boolean reachesChunk(int originX, int originZ) {
        double limit = coastReach();
        double closest = Double.POSITIVE_INFINITY;
        for (int cornerX = 0; cornerX <= 15; cornerX += 15) {
            for (int cornerZ = 0; cornerZ <= 15; cornerZ += 15) {
                double distance = boundaryDistance(originX + cornerX, originZ + cornerZ);
                if (distance < closest) {
                    closest = distance;
                }
            }
        }
        return closest <= limit;
    }

    /** Open water only: the sea, or the full width of a river, not the sloping bank. */
    static int openKind(int x, int z) {
        Place place = place(x, z);
        if (place.seaOpen) {
            return SEA;
        }
        if (place.riverOpen) {
            return RIVER;
        }
        return NONE;
    }

    static int floorY(int x, int z) {
        int bed = OceanBoundaryConfig.seaLevel - 14;
        double wobble = OceanBoundaryMath.noise(x * 0.006D, z * 0.006D, seed ^ 0x51BEDL);
        int y = bed + (int) Math.round(wobble * 2.0D);
        int shallowest = OceanBoundaryConfig.seaLevel - 8;
        if (y > shallowest) {
            y = shallowest;
        }
        if (y < 8) {
            y = 8;
        }
        return y;
    }

    private static double cover(int x, int z) {
        return place(x, z).cover;
    }

    private static Place place(int x, int z) {
        Place place = new Place();
        if (boundaryDistance(x, z) > coastReach() + 48.0D) {
            return place;
        }
        double dx = x - OceanBoundaryConfig.centerX;
        double dz = z - OceanBoundaryConfig.centerZ;
        double dist = Math.sqrt(dx * dx + dz * dz);

        double seaRadius = seaRadius();
        if (seaRadius > 0.0D) {
            double edge = shoreEdge(dx, dz, dist, seaRadius);
            double shore = shoreWidth(seaRadius);
            if (dist <= edge) {
                place.seaOpen = true;
                place.cover = 1.0D;
            } else if (dist < edge + shore) {
                place.cover = 1.0D - smooth(dist, edge, edge + shore);
            }
        }

        double reach = coastReach();
        if (boundaryDistance(x, z) > reach) {
            return place;
        }
        double half = halfWidth();
        double bank = half * 0.45D;
        if (bank < 14.0D) {
            bank = 14.0D;
        }
        double mouth = seaRadius * 0.62D;
        for (int index = 0; index < RIVERS; index++) {
            double along = dx * DIR_X[index] + dz * DIR_Z[index];
            if (along < mouth) {
                continue;
            }
            double across = dx * -DIR_Z[index] + dz * DIR_X[index];
            double wander = wander(along, index);
            double delta = Math.abs(across - wander);
            double channel = half * widthScale(along, index) * flare(along, seaRadius, dist, reach);
            double amount;
            boolean open;
            if (delta <= channel) {
                amount = 1.0D;
                open = true;
            } else if (delta < channel + bank) {
                amount = 1.0D - smooth(delta, channel, channel + bank);
                open = false;
            } else {
                continue;
            }
            if (amount > place.cover) {
                place.cover = amount;
            }
            if (open) {
                place.riverOpen = true;
            }
        }
        return place;
    }

    private static double seaRadius() {
        int diameter = OceanBoundaryConfig.centralSea;
        if (diameter < 0) {
            return 0.0D;
        }
        return diameter * 0.5D;
    }

    private static double shoreWidth(double radius) {
        double shore = 36.0D;
        double limit = radius * 0.4D;
        if (shore > limit) {
            shore = limit;
        }
        if (shore < 8.0D) {
            shore = 8.0D;
        }
        return shore;
    }

    private static double shoreEdge(double dx, double dz, double dist, double radius) {
        if (dist < 1.0D) {
            return radius;
        }
        double ux = dx / dist;
        double uz = dz / dist;
        double broad = OceanBoundaryMath.noise(ux * 1.4D, uz * 1.4D, seed ^ 0x5EA11L);
        double fine = OceanBoundaryMath.noise(ux * 3.2D, uz * 3.2D, seed ^ 0x0CE11L);
        double wobble = broad * 36.0D + fine * 14.0D;
        double limit = radius * 0.16D;
        if (wobble > limit) {
            wobble = limit;
        }
        if (wobble < -limit) {
            wobble = -limit;
        }
        return radius + wobble;
    }

    private static double wander(double along, int index) {
        double cap = along * 0.16D;
        if (cap < 28.0D) {
            cap = 28.0D;
        }
        if (cap > 380.0D) {
            cap = 380.0D;
        }
        double slow = OceanBoundaryMath.noise(along * 0.0007D, index * 19.0D, seed ^ 0xA11E0L);
        double mid = OceanBoundaryMath.noise(along * 0.0021D, index * 7.0D, seed ^ 0xB10CL);
        double bend = slow * 0.62D + mid * 0.38D;
        return bend * cap;
    }

    private static double widthScale(double along, int index) {
        double n = OceanBoundaryMath.noise(along * 0.0022D, index * 5.0D, seed ^ 0xB10DL);
        return 0.86D + 0.28D * (n * 0.5D + 0.5D);
    }

    private static double flare(double along, double seaRadius, double dist, double reach) {
        double flare = 1.0D;
        if (seaRadius > 1.0D) {
            double fromSea = along - seaRadius;
            if (fromSea < 180.0D) {
                double t = 1.0D - fromSea / 180.0D;
                if (t > 1.0D) {
                    t = 1.0D;
                }
                if (t < 0.0D) {
                    t = 0.0D;
                }
                flare += 0.7D * t;
            }
        }
        double toCoast = reach - dist;
        if (toCoast < 220.0D && toCoast > 0.0D) {
            flare += 0.45D * (1.0D - toCoast / 220.0D);
        }
        return flare;
    }

    private static double halfWidth() {
        int width = OceanBoundaryConfig.riverWidth;
        if (width < 8) {
            width = 8;
        }
        return width * 0.5D;
    }

    private static double coastReach() {
        double reach = OceanBoundaryConfig.fullOceanRadius;
        if (OceanBoundaryConfig.coastlineNoise) {
            reach += OceanBoundaryConfig.coastlineAmplitude;
        }
        return reach;
    }

    private static double boundaryDistance(int x, int z) {
        return OceanBoundaryMath.distance(
                x,
                z,
                OceanBoundaryConfig.centerX,
                OceanBoundaryConfig.centerZ,
                OceanBoundaryConfig.circular);
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

    private static final class Place {
        double cover;
        boolean seaOpen;
        boolean riverOpen;
    }
}
