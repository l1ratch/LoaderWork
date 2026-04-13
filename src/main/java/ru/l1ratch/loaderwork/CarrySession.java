package ru.l1ratch.loaderwork;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class CarrySession {

    private final UUID playerId;
    private final String jobId;
    private final Material blockType;
    private final Location originLocation;
    private final ItemStack previousMainHand;
    private BlockDisplay display;
    private int navigationElapsedTicks;
    private int carryParticleElapsedTicks;
    private int dropoffElapsedTicks;
    private Location dropoffAnchor;

    public CarrySession(UUID playerId, String jobId, Material blockType, Location originLocation, ItemStack previousMainHand, BlockDisplay display) {
        this.playerId = playerId;
        this.jobId = jobId;
        this.blockType = blockType;
        this.originLocation = originLocation.clone();
        this.previousMainHand = previousMainHand == null ? null : previousMainHand.clone();
        this.display = display;
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

    public Location getOriginLocation() {
        return originLocation.clone();
    }

    public ItemStack getPreviousMainHand() {
        return previousMainHand == null ? null : previousMainHand.clone();
    }

    public BlockDisplay getDisplay() {
        return display;
    }

    public void setDisplay(BlockDisplay display) {
        this.display = display;
    }

    public int getNavigationElapsedTicks() {
        return navigationElapsedTicks;
    }

    public void addNavigationElapsedTicks(int ticks) {
        navigationElapsedTicks += ticks;
    }

    public void resetNavigationElapsedTicks() {
        navigationElapsedTicks = 0;
    }

    public int getCarryParticleElapsedTicks() {
        return carryParticleElapsedTicks;
    }

    public void addCarryParticleElapsedTicks(int ticks) {
        carryParticleElapsedTicks += ticks;
    }

    public void resetCarryParticleElapsedTicks() {
        carryParticleElapsedTicks = 0;
    }

    public int getDropoffElapsedTicks() {
        return dropoffElapsedTicks;
    }

    public void addDropoffElapsedTicks(int ticks) {
        dropoffElapsedTicks += ticks;
    }

    public void resetDropoffProgress() {
        dropoffElapsedTicks = 0;
        dropoffAnchor = null;
    }

    public Location getDropoffAnchor() {
        return dropoffAnchor == null ? null : dropoffAnchor.clone();
    }

    public void setDropoffAnchor(Location dropoffAnchor) {
        this.dropoffAnchor = dropoffAnchor == null ? null : dropoffAnchor.clone();
    }
}
