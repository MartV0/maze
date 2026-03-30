package nl.uu.maze.search.strategy;

import nl.uu.maze.search.SearchTarget;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class PrimePathStrategy<T extends SearchTarget> extends SearchStrategy<T> {
    private final Queue<T> targets = new LinkedList<>();

    public String getName() {
        return "PrimePathStrategy";
    }

    @Override
    public void add(T target) {
        targets.add(target);
    }

    @Override
    public void remove(T target) {
        targets.remove(target);
    }

    @Override
    public T next() {
        return targets.isEmpty() ? null : targets.remove();
    }

    @Override
    public int size() {
        return targets.size();
    }

    @Override
    public void reset() {
        targets.clear();
    }

    @Override
    public Collection<T> getAll() {
        return targets;
    }
}