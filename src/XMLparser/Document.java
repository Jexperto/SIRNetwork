package XMLparser;

public class Document extends Node {

    Document() {
        super(DOC_TAG);
    }

    public static Document createDocument() {
        return new Document();
    }

    Document copy() {
        Document root = new Document();


        return root;
    }

    public void showTree() {
        for (Node node : getChildren()) {
            showTreeElement(node, 0);
        }
    }

    private void showTreeElement(Node node, int level) {
        for (int i = 0; i < level; i++) {
            System.out.print(" ");
        }
        System.out.println("<" + node.getTagName() + "> : " + node.getInnerText());
        for (Node child : node.getChildren()) {
            showTreeElement(child, level + 1);
        }
    }

}