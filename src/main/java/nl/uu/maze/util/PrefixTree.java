package nl.uu.maze.util;

import java.util.List;
import java.util.HashMap;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Prefix tree data structure, also called trie, used to efficiently query if
 * a string is a prefix of a set of trees
 */
public class PrefixTree<T> {
    private final static Logger logger = LoggerFactory.getLogger(BranchHistory.class);

    private class TrieNode {
        HashMap<T, TrieNode> children;
        private List<T> content;
        
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
        current.content = input;
    }

    /** Attempts to remove input from tree
      * returns true if the input was present in the tree, false otherwise */
    public boolean remove(List<T> input) {
        return remove(input, 0, root, false);
    }

    /** Remove lists from the tree that are sublists of input */
    public boolean removeSublists(List<T> input) {
        boolean res = false;
        for(int i = 0; i < input.size(); i++) {
            res |= remove(input, i, root, true);
        }
        return res;
    }
    
    /** Remove input from tree
     * also remove entries that end before end of input if sublist is set to true
     * */
    private boolean remove(List<T> input, int index, TrieNode node, boolean sublist) {
        // Indicates if a list has been deleted from the tree
        boolean deleted = false;
        if (index == input.size() || sublist) {
            deleted = node.isList;
            node.isList = false;
            node.content = null;
            if (index == input.size())
                return deleted;
        }

        if (node.children.isEmpty())
            return deleted;

        var child = node.children.get(input.get(index));
        if (child == null) return deleted;
        deleted |= remove(input, index + 1, child, sublist);

        // If child is not end of list, and has no children, delete it
        if (!child.isList && child.children.isEmpty()) {
            node.children.remove(input.get(index));
        }

        return deleted;
    }

    /** checks if input is a prefix of any of the lists in the tree */
    public boolean isPrefix(List<T> input) {
        return contains(input, 0, false, false);
    }

    /** checks if input starting from range is a prefix of any of the lists in the tree
     *  if strict = true than function only returns true if entire list is in tree
     *  if sublist = true than function also returns true if a list is
     *      a prefix of input */
    private boolean contains(List<T> input, int start_range, boolean strict, boolean prefix) {
        TrieNode current = root;
        for (int i = start_range; i < input.size(); i++) {
            if (prefix && current.isList) return true;
            var next_node = current.children.get(input.get(i));
            if (next_node == null) {
                return false;
            }
            current = next_node;
        }
        return strict || input.size() == start_range ? current.isList : true;
    }

    /** checks if whole list is present in the tree */
    public boolean contains(List<T> input) {
        return contains(input, 0, true, false);
    }

    /** true iff tree is empty */
    public boolean empty() {
        return root.children.isEmpty() && !root.isList;
    }

    /** checks if any endings of the input are a prefix of any of the lists in the tree */
    public boolean containsPrefix(List<T> input) {
        for (int i = 0; i <= input.size(); i++) {
            if (contains(input, i, false, false)) {
                return true;
            }
        }
        return false;
    }

    /** Checks if any list in the tree is contained in input */
    public boolean containsSublist(List<T> input) {
        for (int i = 0; i <= input.size(); i++) {
            if (contains(input, i, true, true)) {
                return true;
            }
        }
        return false;
    }
}
