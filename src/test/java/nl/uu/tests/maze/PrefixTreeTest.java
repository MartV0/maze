package nl.uu.tests.maze;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import nl.uu.maze.util.PrefixTree;

import static org.junit.jupiter.api.Assertions.*;

class PrefixTreeTest {
    @Test
    void testPrefixTree() {
        PrefixTree<Integer> tree = new PrefixTree<Integer>();
        var list1 = new ArrayList<Integer>();
        list1.add(1);
        list1.add(2);
        list1.add(3);
        list1.add(4);
        list1.add(5);
        tree.insert(list1);
        var list2 = new ArrayList<Integer>();
        list2.add(1);
        list2.add(2);
        list2.add(8);
        list2.add(9);
        list2.add(10);
        tree.insert(list2);
        var list3 = new ArrayList<Integer>();
        list3.add(1);
        list3.add(2);
        list3.add(8);
        list3.add(12);
        list3.add(13);
        tree.insert(list3);

        var prefix1 = new ArrayList<Integer>();
        prefix1.add(1);
        prefix1.add(2);
        assertTrue(tree.isPrefix(prefix1));
        var prefix2 = new ArrayList<Integer>();
        prefix2.add(1);
        prefix2.add(2);
        prefix2.add(8);
        prefix2.add(12);
        assertTrue(tree.isPrefix(prefix2));
        var notprefix = new ArrayList<Integer>();
        notprefix.add(1);
        notprefix.add(2);
        notprefix.add(8);
        notprefix.add(10);
        assertFalse(tree.isPrefix(notprefix));

        var removelist = new ArrayList<Integer>();
        removelist.add(1);
        removelist.add(2);
        removelist.add(8);
        removelist.add(12);
        removelist.add(13);
        assertTrue(tree.remove(removelist));
        var res = tree.isPrefix(prefix2);
        assertFalse(res);

        var removelist2 = new ArrayList<Integer>();
        removelist2.add(1);
        removelist2.add(2);
        assertFalse(tree.remove(removelist2));
        assertTrue(tree.isPrefix(prefix1));
    }

    @Test
    void testEmptyPrefixTree() {
        PrefixTree<Integer> tree = new PrefixTree<Integer>();
        var prefix1 = new ArrayList<Integer>();
        prefix1.add(1);
        prefix1.add(2);
        prefix1.add(3);
        prefix1.add(4);
        assertFalse(tree.isPrefix(prefix1));
        assertFalse(tree.containsPrefix(prefix1));
        assertFalse(tree.contains(prefix1));
    }

    @Test
    void testEmptyPrefixTree2() {
        PrefixTree<Integer> tree = new PrefixTree<Integer>();
        var list1 = new ArrayList<Integer>();
        var prefix1 = new ArrayList<Integer>();
        tree.insert(list1);
        assertTrue(tree.isPrefix(prefix1));
        assertTrue(tree.containsPrefix(prefix1));
        tree.remove(new ArrayList<Integer>());
        // tree.removeSublists(new ArrayList<Integer>());
        assertFalse(tree.isPrefix(prefix1));
        assertFalse(tree.containsPrefix(prefix1));
    }

    @Test
    void testPrefixTreeSublists() {
        PrefixTree<Integer> tree = new PrefixTree<Integer>();
        var list1 = new ArrayList<Integer>();
        list1.add(1);
        list1.add(2);
        list1.add(3);
        list1.add(4);
        list1.add(5);
        tree.insert(list1);
        var list2 = new ArrayList<Integer>();
        list2.add(1);
        list2.add(2);
        list2.add(8);
        list2.add(9);
        list2.add(10);
        tree.insert(list2);
        var list3 = new ArrayList<Integer>();
        list3.add(1);
        list3.add(2);
        list3.add(8);
        list3.add(12);
        list3.add(13);
        tree.insert(list3);
        var list4 = new ArrayList<Integer>();
        list4.add(1);
        list4.add(2);
        tree.insert(list4);
        var list5 = new ArrayList<Integer>();
        list5.add(1);
        list5.add(2);
        list5.add(8);
        tree.insert(list5);

        var prefix1 = new ArrayList<Integer>();
        prefix1.add(42);
        prefix1.add(414141);
        prefix1.add(1);
        prefix1.add(2);
        assertTrue(tree.containsPrefix(prefix1));

        var removelist = new ArrayList<Integer>();
        removelist.add(31415);
        removelist.add(42);
        removelist.add(1);
        removelist.add(2);
        removelist.add(8);
        removelist.add(12);
        assertTrue(tree.removeSublists(removelist));
        assertTrue(tree.contains(list1));
        assertTrue(tree.contains(list2));
        assertTrue(tree.contains(list3));
        assertFalse(tree.contains(list4));
        assertFalse(tree.contains(list5));

        var removelist2 = new ArrayList<Integer>();
        removelist2.add(31415);
        removelist2.add(42);
        removelist2.add(1);
        removelist2.add(2);
        removelist2.add(8);
        removelist2.add(9);
        removelist2.add(10);
        removelist2.add(11);
        removelist2.add(12);
        var res = tree.removeSublists(removelist2);
        assertTrue(res);
        assertTrue(tree.contains(list1));
        assertFalse(tree.contains(list2));
        assertTrue(tree.contains(list3));
        assertFalse(tree.contains(list4));
        assertFalse(tree.contains(list5));
    }
}
