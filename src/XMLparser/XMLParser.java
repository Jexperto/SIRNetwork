package XMLparser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public final class XMLParser {

    private XMLParser() {
    }

    public static Document parse(String filename) throws IOException {
        return parse(new FileInputStream(filename));
    }

    public static Document parse(InputStream inputStream) {
        Document root = new Document();
        ScannerExtension scanner = new ScannerExtension(new Scanner(inputStream));
        String xml = scanner.next();
        if (!xml.contains("<?xml"))
            return null;
        while (!xml.contains("?>") && scanner.hasNext())
            xml = scanner.next();
        scan(root, scanner);
        scanner.close();
        return root;
    }

    private static void scan(Node currentNode, ScannerExtension scanner) {
        Node child = null;
        boolean comment = false;
        while (scanner.hasNext()) {
            String string = scanner.next();
            byte closeType = (byte) ((string.lastIndexOf("/>") != -1) ? 2 : (string.lastIndexOf(">") != -1) ? 1 : 0);
            if (string.indexOf("<!--") == 0)
                comment = true;
            if (string.contains("-->")) {
                comment = false;
                continue;
            }
            if (comment)
                continue;
            if (string.indexOf("</") == 0) {
                break;
            } else if (string.indexOf("<") == 0) {
                if (closeType == 0) {
                    child = new Node(string.substring(1));
                    currentNode.appendChild(child);
                } else
                    switch (closeType) {
                        case 1: // close open tag
                            child = new Node(string.substring(1, string.length() - 1));
                            scan(child, scanner);
                            currentNode.appendChild(child);
                            child = null;
                            break;
                        case 2: // close tag
                            child = new Node(string.substring(1, string.length() - 2));
                            currentNode.appendChild(child);
                            child = null;
                            break;
                    }
            } else {
                if (child == null) {
                    currentNode.appendInnerText(string);
                    if (!currentNode.getInnerText().isEmpty())
                        currentNode.appendInnerText(" ");
                    continue;
                }
                StringBuilder stringBuilder = new StringBuilder(string);
                while (stringBuilder.indexOf("\"") == stringBuilder.lastIndexOf("\""))
                    stringBuilder.append(" ").append(scanner.next());
                string = stringBuilder.toString();
                closeType = (byte) ((string.lastIndexOf("/>") != -1) ? 2 : (string.lastIndexOf(">") != -1) ? 1 : 0);
                String[] attrKW = string.replace("\"", "").split("=");
                if (attrKW.length == 1) {
                    attrKW = new String[]{attrKW[0], ""};
                }
                switch (closeType) {
                    case 0:
                        child.setAttribute(attrKW[0], attrKW[1]);
                        break;
                    case 1:
                        attrKW[1] = attrKW[1].substring(0, attrKW[1].length() - 1);
                        child.setAttribute(attrKW[0], attrKW[1]);
                        scan(child, scanner);
                        child = null;
                        break;
                    case 2:
                        attrKW[1] = attrKW[1].substring(0, attrKW[1].length() - 2);
                        child.setAttribute(attrKW[0], attrKW[1]);
                        child = null;
                        break;
                }
            }
        }
    }

    public static void save(String filename, Document document) throws IOException {
        OutputStream outputStream = new FileOutputStream(filename);
        outputStream.write(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
        for (Node child : document.getChildren()) {
            startWriteTree(child, outputStream);
        }
        outputStream.close();
    }

    private static void startWriteTree(Node node, OutputStream outputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        writeTree(node, 0, stringBuilder);
        outputStream.write(stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void writeTree(Node node, int level, StringBuilder stringBuilder) {
        addTabs(stringBuilder, level);
        stringBuilder.append("<").append(node.getTagName());
        HashMap<String, String> attributes = node.getAttributes();
        ArrayList<String> keys = node.getAttributesKeys();
        for (String key : keys) {
            stringBuilder.append(" ").append(key).append("=\"").append(attributes.get(key)).append("\"");
        }
        ArrayList<Node> children = node.getChildren();
        if (children.size() == 0)
            stringBuilder.append("/");
        stringBuilder.append(">\n");
        for (Node child : children) {
            writeTree(child, level + 1, stringBuilder);
        }
        if (children.size() != 0) {
            addTabs(stringBuilder, level);
            stringBuilder.append("</").append(node.getTagName()).append(">\n");
        }
    }

    private static void addTabs(StringBuilder stringBuilder, int level) {
        for (int i = 0; i < level; i++) {
            stringBuilder.append("\t");
        }
    }

    private static class ScannerExtension {

        private Scanner scanner;
        private String buff;

        ScannerExtension(Scanner scanner) {
            this.scanner = scanner;
            buff = "";
        }

        String next() {
            String result;
            if (buff.isEmpty())
                buff = scanner.next();
            result = getSpecString();
            return result;
        }

        private String getSpecString() {
            String result;
            int index;
            if (buff.contains(">") && buff.contains("<") && (index = getIndexContact(buff)) != -1) {
                result = buff.substring(0, index + 1);
                buff = buff.substring(index + 1);
            } else {
                result = buff;
                buff = "";
            }
            return result;
        }

        private int getIndexContact(String string) {
            for (int i = 0; i < string.length(); i++) {
                if (string.indexOf(">", i) == string.indexOf("<", i) - 1) {
                    return string.indexOf(">", i);
                }
            }
            return -1;
        }

        void close() {
            scanner.close();
        }

        boolean hasNext() {
            if (!buff.isEmpty())
                return true;
            return scanner.hasNext();
        }
    }


}