package nl.uu.maze.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides utility methods for working with lists
 */
public class ListUtils {
    // Get the first index equal to the target, -1 if no match is found
    public static <T> int IndexOf(List<T> list, T target) {
        for (int i = 0; i < list.size(); i++) {
            if (target == list.get(i)) return i;
        }
        return -1;
    }
}
