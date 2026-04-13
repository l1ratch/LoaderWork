package ru.l1ratch.loaderwork;

public class PluginSettings {

    private final boolean autoSelectJobByRegion;
    private final int updateIntervalTicks;
    private final double holdMaxMoveDistance;
    private final int carryParticleIntervalTicks;

    public PluginSettings(boolean autoSelectJobByRegion, int updateIntervalTicks, double holdMaxMoveDistance, int carryParticleIntervalTicks) {
        this.autoSelectJobByRegion = autoSelectJobByRegion;
        this.updateIntervalTicks = updateIntervalTicks;
        this.holdMaxMoveDistance = holdMaxMoveDistance;
        this.carryParticleIntervalTicks = carryParticleIntervalTicks;
    }

    public boolean isAutoSelectJobByRegion() {
        return autoSelectJobByRegion;
    }

    public int getUpdateIntervalTicks() {
        return updateIntervalTicks;
    }

    public double getHoldMaxMoveDistance() {
        return holdMaxMoveDistance;
    }

    public int getCarryParticleIntervalTicks() {
        return carryParticleIntervalTicks;
    }
}
