package ru.l1ratch.loaderwork;

public class RewardResult {

    private final double money;
    private final int experience;
    private final int itemsGranted;
    private final int commandsExecuted;

    public RewardResult(double money, int experience, int itemsGranted, int commandsExecuted) {
        this.money = money;
        this.experience = experience;
        this.itemsGranted = itemsGranted;
        this.commandsExecuted = commandsExecuted;
    }

    public double getMoney() {
        return money;
    }

    public int getExperience() {
        return experience;
    }

    public int getItemsGranted() {
        return itemsGranted;
    }

    public int getCommandsExecuted() {
        return commandsExecuted;
    }
}
