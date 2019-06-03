package XMLparser;

import com.sun.istack.internal.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

public class Node {

    static final String DOC_TAG = "<?xml?>";
    private static final String NOTHING_TAG = "##NOTHING##";

    private Node parent;
    private ArrayList<Node> children;
    private ArrayList<String> attributesKeys;
    private HashMap<String, String> attributes;
    private String tagName;
    private String innerText;

    public Node(String tagName) {
        this.tagName = tagName;
        children = new ArrayList<>();
        attributes = new HashMap<>();
        attributesKeys = new ArrayList<>();
        innerText = "";
        parent = null;
    }

    public void appendChild(Node child) {
        if (child.tagName.equals(NOTHING_TAG))
            return;
        if (this == child)
            return;
        child.parent = this;
        children.add(child);
    }

    public void removeChild(Node child) {
        children.remove(child);
    }

    public void setAttribute(String name, String value) {
        if (!attributes.containsKey(name))
            attributesKeys.add(name);
        attributes.put(name, value);
    }

    public boolean hasAttribute(String name) {
        return attributes.containsKey(name);
    }

    public String getAttribute(String name) {
        return attributes.getOrDefault(name, "");
    }

    public void removeAttribute(String name) {

    }

    public ArrayList<String> getAttributesKeys() {
        return attributesKeys;
    }

    public ArrayList<Node> getNodesByTagName(@NotNull String tagName) {
        if (tagName.equals(DOC_TAG)) {
            ArrayList<Node> empty = new ArrayList<>();
            empty.add(new Node(NOTHING_TAG));
        }
        ArrayList<Node> byTag = new ArrayList<>(children.size());
        for (Node node : children) {
            if (node.tagName.equals(tagName))
                byTag.add(node);
            byTag.addAll(node.getNodesByTagName(tagName));
        }
        byTag.trimToSize();
        return byTag;
    }

    public ArrayList<Node> getNodesByAttributeEqual(String attribute, String value) {
        ArrayList<Node> byAttr = new ArrayList<>(children.size());
        for (Node node : children) {
            if (node.getAttribute(attribute).equals(value))
                byAttr.add(node);
            byAttr.addAll(node.getNodesByAttributeEqual(attribute, value));
        }
        byAttr.trimToSize();
        return byAttr;
    }

    public ArrayList<Node> getNodesByTagNameAndAttributeEqual(@NotNull String tagName, String attribute, String value) {
        ArrayList<Node> byTag = this.getNodesByTagName(tagName);
        ArrayList<Node> byAttr = new ArrayList<>(byTag.size());
        for (Node nodeByTag : byTag) {
            if (nodeByTag.getAttribute(attribute).equals(value))
                byAttr.add(nodeByTag);
        }
        byAttr.trimToSize();
        return byAttr;
    }

    public ArrayList<Node> getChildren() {
        return children;
    }

    public String getInnerText() {
        return innerText;
    }

    public void setInnerText(String innerText) {
        this.innerText = innerText;
    }

    void appendInnerText(String appendText) {
        innerText += appendText;
    }

    public Node getParent() {
        return parent;
    }

    public String getTagName() {
        return tagName;
    }

    public HashMap<String, String> getAttributes() {
        return attributes;
    }

    protected Node copyNode() {
        return this;
    }
}