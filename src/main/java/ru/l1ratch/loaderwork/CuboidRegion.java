package ru.l1ratch.loaderwork;

import org.bukkit.Location;
import org.bukkit.World;

public class CuboidRegion {

    private final String worldName;
    private final Integer pos1X;
    private final Integer pos1Y;
    private final Integer pos1Z;
    private final Integer pos2X;
    private final Integer pos2Y;
    private final Integer pos2Z;

    public CuboidRegion(String worldName, Integer pos1X, Integer pos1Y, Integer pos1Z, Integer pos2X, Integer pos2Y, Integer pos2Z) {
        this.worldName = worldName;
        this.pos1X = pos1X;
        this.pos1Y = pos1Y;
        this.pos1Z = pos1Z;
        this.pos2X = pos2X;
        this.pos2Y = pos2Y;
        this.pos2Z = pos2Z;
    }

    public String getWorldName() {
        return worldName;
    }

    public Integer getPos1X() {
        return pos1X;
    }

    public Integer getPos1Y() {
        return pos1Y;
    }

    public Integer getPos1Z() {
        return pos1Z;
    }

    public Integer getPos2X() {
        return pos2X;
    }

    public Integer getPos2Y() {
        return pos2Y;
    }

    public Integer getPos2Z() {
        return pos2Z;
    }

    public boolean isComplete() {
        return worldName != null
                && pos1X != null
                && pos1Y != null
                && pos1Z != null
                && pos2X != null
                && pos2Y != null
                && pos2Z != null;
    }

    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null || !isComplete()) {
            return false;
        }

        if (!location.getWorld().getName().equalsIgnoreCase(worldName)) {
            return false;
        }

        int minX = Math.min(pos1X, pos2X);
        int maxX = Math.max(pos1X, pos2X);
        int minY = Math.min(pos1Y, pos2Y);
        int maxY = Math.max(pos1Y, pos2Y);
        int minZ = Math.min(pos1Z, pos2Z);
        int maxZ = Math.max(pos1Z, pos2Z);

        int blockX = location.getBlockX();
        int blockY = location.getBlockY();
        int blockZ = location.getBlockZ();

        return blockX >= minX && blockX <= maxX
                && blockY >= minY && blockY <= maxY
                && blockZ >= minZ && blockZ <= maxZ;
    }

    public Location getCenter(World world) {
        if (world == null || !isComplete() || !world.getName().equalsIgnoreCase(worldName)) {
            return null;
        }

        int minX = Math.min(pos1X, pos2X);
        int maxX = Math.max(pos1X, pos2X);
        int minY = Math.min(pos1Y, pos2Y);
        int maxY = Math.max(pos1Y, pos2Y);
        int minZ = Math.min(pos1Z, pos2Z);
        int maxZ = Math.max(pos1Z, pos2Z);

        double centerX = (minX + maxX + 1.0D) / 2.0D;
        double centerY = (minY + maxY + 1.0D) / 2.0D;
        double centerZ = (minZ + maxZ + 1.0D) / 2.0D;
        return new Location(world, centerX, centerY, centerZ);
    }

    public CuboidRegion withCorner(RegionCorner corner, Location location) {
        if (corner == null || location == null || location.getWorld() == null) {
            return this;
        }

        String targetWorld = location.getWorld().getName();
        if (worldName != null && !worldName.equalsIgnoreCase(targetWorld)) {
            return this;
        }

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        if (corner == RegionCorner.POS1) {
            return new CuboidRegion(targetWorld, x, y, z, pos2X, pos2Y, pos2Z);
        }

        return new CuboidRegion(targetWorld, pos1X, pos1Y, pos1Z, x, y, z);
    }

    public String describe() {
        if (worldName == null) {
            return "неполный";
        }

        return "мир=" + worldName
                + ", pos1=" + formatPoint(pos1X, pos1Y, pos1Z)
                + ", pos2=" + formatPoint(pos2X, pos2Y, pos2Z);
    }

    private String formatPoint(Integer x, Integer y, Integer z) {
        if (x == null || y == null || z == null) {
            return "не задано";
        }
        return x + "," + y + "," + z;
    }
}
