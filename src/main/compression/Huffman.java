package main.compression;

import java.util.*;
import java.util.Arrays;
import org.w3c.dom.Node;
import java.io.*;

/**
 * Huffman instances provide reusable Huffman Encoding Maps for
 * compressing and decompressing text corpi with comparable
 * distributions of characters.
 */
public class Huffman {

    // -----------------------------------------------
    // Construction
    // -----------------------------------------------

    private HuffNode trieRoot;
    // TreeMap chosen here just to make debugging easier
    private TreeMap<Character, String> encodingMap;
    // Character that represents the end of a compressed transmission
    private static final char ETB_CHAR = 23;

    /**
     * 
     * Creates the Huffman Trie and Encoding Map using the character
     * distributions in the given text corpus
     * 
     * @param corpus A String representing a message / document corpus
     *               with distributions over characters that are implicitly used
     *               throughout the methods that follow. Note: this corpus ONLY
     *               establishes the Encoding Map; later compressed corpi may
     *               differ.
     */
    public Huffman(String corpus) {
        HashMap<Character, Integer> distribution = new HashMap<>();
        distribution.put(ETB_CHAR, 1);
        for (int i = 0; i < corpus.length(); i++) {
            if (distribution.containsKey(corpus.charAt(i))) {
                int value = distribution.get(corpus.charAt(i)) + 1;
                distribution.replace(corpus.charAt(i), value);
            } else {
                distribution.put(corpus.charAt(i), 1);
            }
        }
        PriorityQueue<HuffNode> huffmanTrie = new PriorityQueue<>();
        for (Map.Entry<Character, Integer> entry : distribution.entrySet()) {
            Character key = entry.getKey();
            Integer value = entry.getValue();
            HuffNode leaf = new HuffNode(key, value);
            huffmanTrie.add(leaf);
        }
        while (huffmanTrie.size() != 1) {
            HuffNode currZeroChild = huffmanTrie.poll();
            HuffNode currOneChild = huffmanTrie.poll();
            HuffNode parent = new HuffNode(currZeroChild.character, 0);
            parent.zeroChild = currZeroChild;
            parent.oneChild = currOneChild;
            parent.count = currZeroChild.count + currOneChild.count;
            huffmanTrie.add(parent);
        }
        trieRoot = huffmanTrie.poll();
        // huffmanTrie.add(trieRoot);
        this.encodingMap = new TreeMap<>();
        toBitCode(trieRoot, "");
        System.out.println(encodingMap);
    }

    /**
     * Converting the Binary tree of HuffNodes with characters and their count into
     * the
     * encoding map of their bitcode.
     * 
     * @param huffnode. The current huffnode we're in.
     *                  bitCode. The current bitcode
     * @return treemap. the map of each character and its bitcode
     */
    public TreeMap<Character, String> toBitCode(HuffNode current, String bitCode) {
        if (current.isLeaf()) {
            encodingMap.put(current.character, bitCode);
            return encodingMap;
        }
        toBitCode(current.zeroChild, bitCode + "0");
        toBitCode(current.oneChild, bitCode + "1");
        return encodingMap;
    }

    // -----------------------------------------------
    // Compression
    // -----------------------------------------------

    /**
     * Compresses the given String message / text corpus into its Huffman coded
     * bitstring, as represented by an array of bytes. Uses the encodingMap
     * field generated during construction for this purpose.
     * 
     * @param message String representing the corpus to compress.
     * @return {@code byte[]} representing the compressed corpus with the
     *         Huffman coded bytecode. Formatted as:
     *         (1) the bitstring containing the message itself, (2) possible
     *         0-padding on the final byte.
     */
    public byte[] compress(String message) {
        String result = "";
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (int i = 0; i < message.length(); i++) {
            String current = encodingMap.get(message.charAt(i));
            result += current;
        }
        result += encodingMap.get(ETB_CHAR);
        while (result.length() % 8 != 0) {
            result += "0";
        }
        while (result.length() != 0) {
            String eightBits = result.substring(0, 8);
            output.write((byte) Integer.parseInt(eightBits, 2));
            result = result.substring(8);
        }
        return output.toByteArray();
    }

    // -----------------------------------------------
    // Decompression
    // -----------------------------------------------

    /**
     * Decompresses the given compressed array of bytes into their original,
     * String representation. Uses the trieRoot field (the Huffman Trie) that
     * generated the compressed message during decoding.
     * 
     * @param compressedMsg {@code byte[]} representing the compressed corpus with
     *                      the
     *                      Huffman coded bytecode. Formatted as:
     *                      (1) the bitstring containing the message itself, (2)
     *                      possible
     *                      0-padding on the final byte.
     * @return Decompressed String representation of the compressed bytecode
     *         message.
     */
    public String decompress(byte[] compressedMsg) {
        String wholeString = "";
        for (int i = 0; i < compressedMsg.length; i++) {
            String binString = String.format("%8s", Integer.toBinaryString(compressedMsg[i] & 0xff)).replace(" ", "0");
            wholeString += binString;
        }
        return generateMessage(trieRoot, 0, wholeString, "");
    }

    /**
     * Takes in the parameters and generates the message
     * 
     * @param HuffNode current Node, integer current Index, String the entire
     *                 bitstring, String the decompressd Message
     * 
     * @return String of the generatedMessage
     */
    private String generateMessage(HuffNode currNode, int currIndex, String wholeString, String decompressedMsg) {
        if (currNode.isLeaf()) {
            if (currNode.character == ETB_CHAR) {
                return decompressedMsg;
            }
            decompressedMsg += currNode.character;
            currNode = trieRoot;
        }
        if (wholeString.charAt(currIndex) == '0') {
            return generateMessage(currNode.zeroChild, currIndex + 1, wholeString, decompressedMsg);
        } else {
            return generateMessage(currNode.oneChild, currIndex + 1, wholeString, decompressedMsg);
        }
    }

    // -----------------------------------------------
    // Huffman Trie
    // -----------------------------------------------

    /**
     * Huffman Trie Node class used in construction of the Huffman Trie.
     * Each node is a binary (having at most a left (0) and right (1) child),
     * contains
     * a character field that it represents, and a count field that holds the
     * number of times the node's character (or those in its subtrees) appear
     * in the corpus.
     */
    private static class HuffNode implements Comparable<HuffNode> {

        HuffNode zeroChild, oneChild;
        char character;
        int count;

        HuffNode(char character, int count) {
            this.count = count;
            this.character = character;
        }

        public boolean isLeaf() {
            return this.zeroChild == null && this.oneChild == null;
        }

        public int compareTo(HuffNode other) {
            if (this.count == other.count) {
                return this.character - other.character;
            }
            return this.count - other.count;
        }

    }

}
