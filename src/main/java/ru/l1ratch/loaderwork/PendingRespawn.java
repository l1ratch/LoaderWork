package ru.l1ratch.loaderwork;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitTask;

public class PendingRespawn {

    private final Location location;
    private final Material material;
    private BukkitTask task;

    public PendingRespawn(Location location, Material material) {
        this.location = location.clone();
        this.material = material;
    }

    public Location getLocation() {
        return location.clone();
    }

    public Material getMaterial() {
        return material;
    }

    public BukkitTask getTask() {
        return task;
    }

    public void setTask(BukkitTask task) {
        this.task = task;
    }
}
