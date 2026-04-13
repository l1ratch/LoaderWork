package ru.l1ratch.loaderwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RewardDefinition {

    private static final RewardDefinition EMPTY = new RewardDefinition(0.0D, 0, Collections.<String>emptyList(), Collections.<ItemReward>emptyList());

    private final double money;
    private final int experience;
    private final List<String> commands;
    private final List<ItemReward> items;

    public RewardDefinition(double money, int experience, List<String> commands, List<ItemReward> items) {
        this.money = money;
        this.experience = experience;
        this.commands = Collections.unmodifiableList(new ArrayList<String>(commands));
        this.items = Collections.unmodifiableList(new ArrayList<ItemReward>(items));
    }

    public static RewardDefinition empty() {
        return EMPTY;
    }

    public double getMoney() {
        return money;
    }

    public int getExperience() {
        return experience;
    }

    public List<String> getCommands() {
        return commands;
    }

    public List<ItemReward> getItems() {
        return items;
    }

    public boolean isEmpty() {
        return money == 0.0D && experience == 0 && commands.isEmpty() && items.isEmpty();
    }
}
