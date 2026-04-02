package nl.uu.maze.util;

import java.util.List;
import java.util.HashMap;

/**
 * Prefix tree data structure, also called trie, used to efficiently query if
 * a string is a prefix of a set of trees
 */
public class PrefixTree<T> {
    private class TrieNode {
        HashMap<T, TrieNode> children;
        // private ArrayList<T> content;
        
        // Indicates if the list up to this node is a full string in the original input
        private boolean isList = false;

        public TrieNode () {
            children = new HashMap<T, TrieNode>();
        }
    }

    private TrieNode root;

    public PrefixTree (){
        root = new TrieNode();
    }

    /** insert list into tree, does not add duplicates */
    public void insert(List<T> input) {
        TrieNode current = root;
        for (T l: input) {
            var next_node = current.children.computeIfAbsent(l, c -> new TrieNode());
            current.children.put(l, next_node);
            current = next_node;
        }
        current.isList = true;
    }

    /** checks if input is a prefix of any of the lists in the tree */
    public boolean isPrefix(List<T> input) {
        return isPrefix2(input, 0);
    }

    /** checks if input starting from range is a prefix of any of the lists in the tree */
    private boolean isPrefix2(List<T> input, int start_range) {
        TrieNode current = root;
        for (int i = start_range; i < input.size(); i++) {
            var next_node = current.children.get(input.get(i));
            if (next_node == null) {
                return false;
            }
        }
        return true;
    }

    /** checks if any endings of the input are a prefix of any of the lists in the tree */
    public boolean containsPrefix(List<T> input) {
        for (int i = 0; i < input.size(); i++) {
            if (isPrefix2(input, i)) {
                return true;
            }
        }
        return false;
    }
}
