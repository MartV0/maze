package nl.uu.maze.examples;

public class SimpleLoop {
    public static int func_with_loop(int max) {
        int r = 0;

        for (int i = 0; i < max; i++) {
            if (i % 2 == 0) {
                r += i;
            } else {
                r++;
            }
        }

        return r;
    }
}