package ru.l1ratch.loaderwork;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.UUID;

public class PickupProcess {

    private final UUID playerId;
    private final String jobId;
    private final Material blockType;
    private final Location blockLocation;
    private final Location anchorLocation;
    private int elapsedTicks;

    public PickupProcess(UUID playerId, String jobId, Material blockType, Location blockLocation, Location anchorLocation) {
        this.playerId = playerId;
        this.jobId = jobId;
        this.blockType = blockType;
        this.blockLocation = blockLocation.clone();
        this.anchorLocation = anchorLocation.clone();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getJobId() {
        return jobId;
    }

    public Material getBlockType() {
        return blockType;
    }

    public Location getBlockLocation() {
        return blockLocation.clone();
    }

    public Location getAnchorLocation() {
        return anchorLocation.clone();
    }

    public int getElapsedTicks() {
        return elapsedTicks;
    }

    public void addElapsedTicks(int ticks) {
        elapsedTicks += ticks;
    }
}
