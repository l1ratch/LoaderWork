package ru.l1ratch.loaderwork;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RegionService {

    private final LoaderWork plugin;
    private final Map<String, CuboidRegion> regions = new LinkedHashMap<String, CuboidRegion>();

    public RegionService(LoaderWork plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        regions.clear();

        ConfigurationSection regionsSection = plugin.getConfig().getConfigurationSection("regions");
        if (regionsSection == null) {
            return;
        }

        for (String rawRegionId : regionsSection.getKeys(false)) {
            String regionId = normalize(rawRegionId);
            CuboidRegion region = loadRegion(regionsSection.getConfigurationSection(rawRegionId));
            if (region != null) {
                regions.put(regionId, region);
            }
        }
    }

    public boolean isInRegion(Location location, String regionId) {
        if (regionId == null || regionId.trim().isEmpty()) {
            return true;
        }

        CuboidRegion region = regions.get(normalize(regionId));
        return region != null && region.contains(location);
    }

    public Location getRegionCenter(World world, String regionId) {
        if (regionId == null || regionId.trim().isEmpty()) {
            return null;
        }

        CuboidRegion region = regions.get(normalize(regionId));
        return region == null ? null : region.getCenter(world);
    }

    public CuboidRegion getRegion(String regionId) {
        if (regionId == null || regionId.trim().isEmpty()) {
            return null;
        }

        return regions.get(normalize(regionId));
    }

    public List<String> getRegionIds() {
        return Collections.unmodifiableList(new ArrayList<String>(regions.keySet()));
    }

    public boolean setCorner(String regionId, RegionCorner corner, Location location) {
        String normalizedRegionId = normalize(regionId);
        CuboidRegion current = regions.get(normalizedRegionId);
        if (current != null && current.getWorldName() != null && location != null && location.getWorld() != null
                && !current.getWorldName().equalsIgnoreCase(location.getWorld().getName())) {
            return false;
        }

        CuboidRegion updated = current == null ? new CuboidRegion(null, null, null, null, null, null, null) : current;
        updated = updated.withCorner(corner, location);
        saveRegion(normalizedRegionId, updated);
        reload();
        return true;
    }

    public void clearRegion(String regionId) {
        String normalizedRegionId = normalize(regionId);
        plugin.getConfig().set("regions." + normalizedRegionId, null);
        plugin.saveConfig();
        reload();
    }

    public String describeRegion(String regionId) {
        if (regionId == null || regionId.trim().isEmpty()) {
            return "не задано";
        }

        CuboidRegion region = regions.get(normalize(regionId));
        return region == null ? "отсутствует" : region.describe();
    }

    private CuboidRegion loadRegion(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        String worldName = section.getString("world", null);
        Integer pos1X = readInt(section, "pos1.x");
        Integer pos1Y = readInt(section, "pos1.y");
        Integer pos1Z = readInt(section, "pos1.z");
        Integer pos2X = readInt(section, "pos2.x");
        Integer pos2Y = readInt(section, "pos2.y");
        Integer pos2Z = readInt(section, "pos2.z");

        if (worldName == null && pos1X == null && pos1Y == null && pos1Z == null && pos2X == null && pos2Y == null && pos2Z == null) {
            return null;
        }

        return new CuboidRegion(worldName, pos1X, pos1Y, pos1Z, pos2X, pos2Y, pos2Z);
    }

    private void saveRegion(String regionId, CuboidRegion region) {
        String path = "regions." + regionId;
        FileConfiguration config = plugin.getConfig();
        config.set(path + ".world", region.getWorldName());
        config.set(path + ".pos1.x", region.getPos1X());
        config.set(path + ".pos1.y", region.getPos1Y());
        config.set(path + ".pos1.z", region.getPos1Z());
        config.set(path + ".pos2.x", region.getPos2X());
        config.set(path + ".pos2.y", region.getPos2Y());
        config.set(path + ".pos2.z", region.getPos2Z());
        plugin.saveConfig();
    }

    private Integer readInt(ConfigurationSection section, String path) {
        if (section == null || !section.contains(path)) {
            return null;
        }

        Object value = section.get(path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
