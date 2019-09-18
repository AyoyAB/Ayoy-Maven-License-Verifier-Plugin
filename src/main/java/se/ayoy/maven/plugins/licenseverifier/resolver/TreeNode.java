package se.ayoy.maven.plugins.licenseverifier.resolver;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A generic tree node class.
 * @param <T> the type of the trees content.
 */
public class TreeNode<T> implements Iterable<TreeNode<T>> {

    private T data;
    private TreeNode<T> parent;
    private List<TreeNode<T>> children;

    public boolean isRoot() {
        return parent == null;
    }

    public boolean isLeaf() {
        return children.size() == 0;
    }

    private List<TreeNode<T>> elementsIndex;

    /**
     * Create the instance.
     * @param data the content.
     */
    public TreeNode(T data) {
        this.data = data;
        this.children = new LinkedList<TreeNode<T>>();
        this.elementsIndex = new LinkedList<TreeNode<T>>();
        this.elementsIndex.add(this);
        this.parent = null;
    }

    /**
     * Add a child.
     * @param child the data.
     * @return the node.
     */
    public TreeNode<T> addChild(T child) {
        TreeNode<T> childNode = new TreeNode<T>(child);
        childNode.parent = this;
        this.children.add(childNode);
        this.registerChildForSearch(childNode);
        return childNode;
    }

    /**
     * Returns the level in the tree.
     * @return the level in the tree.
     */
    public int getLevel() {
        if (this.isRoot()) {
            return 0;
        } else {
            return parent.getLevel() + 1;
        }
    }

    private void registerChildForSearch(TreeNode<T> node) {
        elementsIndex.add(node);
        if (parent != null) {
            parent.registerChildForSearch(node);
        }
    }

    @Override
    public String toString() {
        return data != null ? data.toString() : "[data null]";
    }

    @Override
    public Iterator<TreeNode<T>> iterator() {
        return children.iterator();
    }

    public List<TreeNode<T>> getChildren() {
        return this.children;
    }

    public TreeNode<T> getParent() {
        return this.parent;
    }

    public T getData() {
        return this.data;
    }

    /**
     * Remove a child.
     * @param childNode the child to remove.
     */
    public void removeChild(TreeNode<T> childNode) {
        this.children.remove(childNode);
    }
}
