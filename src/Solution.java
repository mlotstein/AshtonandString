import java.io.*;
import java.util.*;

/**
 * A memory-conservative solution to the Ashton and String problem on HackerRank
 * Created by Max Lotstein on 4-7-2016
 */

public class Solution {

    static String word;
    final int startCharIndex;
    int curEndIndex;

    public Solution(int startCharIndex) {
        this.startCharIndex = startCharIndex;
        curEndIndex = startCharIndex + 1;
    }

    public boolean hasNext() {
        return curEndIndex <= word.length();
    }

    public String peekNextSubstring() {
        return word.substring(startCharIndex, curEndIndex);
    }

    public void next() {
        curEndIndex++;
    }

    public int nextSubstringLength() {
        return peekNextSubstring().length();
    }

    public static void main(String[] args) {
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));

        try {
            final int T = Integer.parseInt(r.readLine());
            char[] output = new char[T];
            String curWord;
            int K;
            for (int i = 0; i < T; i++) {
                // read in the word
                curWord = r.readLine();
                Solution.word = curWord;
                //read in K
                K = Integer.parseInt(r.readLine()); // - 1? TODO analyze
                // do work, save to an array
                output[i] = findKthChar(curWord, K);
            }

            for (char c : output) {
                System.out.println(c);
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }

    }

    /**
     * Accepts a w and finds the kth character in the lexicographically ordered and concatenated
     * set of all substrings
     * @param w a word
     * @param K an index in the concatenation of the lexicographically ordered sequence of substrings generated from w
     * @return the kth character in this sequence
     */
    public static char findKthChar(String w, int K) {
        // Terminology / Conventions:
        // original word: w
        // character: an alphabetic character
        // char@i: a character that occurred at index i in w
        // substring block: section of concatenated sequence of substrings that begin with the same character, but which
        //                  need not be the same char@i
        // substring block starter: the character that begins all substrings in the substring block
        // substring sequence: alphabetically ordered list of substrings generated from the same char@i

        // Strategy:
        // Given that we can compute the number of characters in each substring block
        // Phase 1: Determine the substring block in which k falls without actually generating any substrings
        //          while keeping a running total of the length of all previous substring blocks
        // Phase 2: Because holding in memory all substring sequences in a substring block may not be possible
        //          create objects capable of generating the next substring in each sequence
        //          and crawl through these substrings until we reach the substring that holds K
        //          again, keeping a running total of length of all previous substrings
        // Phase 3: Return the character at the appropriate index of the substring in which K falls

        //************************
        // Phase 1
        //************************

        // Determine the set of characters in the word which will be of size 26 or less
        Set<Character> charSet = new HashSet<>(26);
        Map<Character, List<Integer>> charsAt = new HashMap<>(26);
        List<Integer> indices;
        char c;
        for (int i = 0; i < w.length(); i++) {
            c = w.charAt(i);
            charSet.add(c);
            if (charsAt.containsKey(c)) {
                indices = charsAt.get(c);
                indices.add(i);
            } else {
                indices = new ArrayList<>();
                indices.add(i);
            }
            charsAt.put(c, indices);
        }

        // Create a list that we can iterate through
        List<Character> l = new ArrayList<>(charSet);
        // and order it lexicographically
        Collections.sort(l);

        // while the running total of characters is < K
        // Iterate through the set of ordered characters
        // For each, 1) retrieve all indices at which the character occurs in the word
        //           2) for each index, determine the length of the substring sequence for that character at that index
        //           3) sum the lengths of all substring sequences to get the substring block length
        //           4) if the substring block length + the lengths of prior substring blocks is greater than K
        //              stop

        // An iterator over the ordered sequence of letters in the word
        Iterator<Character> charIter = l.iterator();

        // The cumulative character length of all substring blocks seen up to this point
        int runningLength = 0;
        // The current (unique) character from w
        c = charIter.next();
        final int wordLength = w.length();
        // The length of the next substring block
        int nextSubstrBlkLen = computeSubstrBlkLen(wordLength, charsAt.get(c));
        while (runningLength + nextSubstrBlkLen < K) {
            runningLength += nextSubstrBlkLen;
            c = charIter.next();
            nextSubstrBlkLen = computeSubstrBlkLen(wordLength, charsAt.get(c));
        }

        //********************
        // Phase 2
        //********************

        // Keeping all substrings within a block in memory is still potentially problematic, so instead of
        // generating all substrings and merging them, we will exploit the fact that it's easy to compute the next
        // substring in a substring sequence, given a record of the previous one.
        //
        // The object, Solution facilitates this. It stores the starting index of the substring w.r.t. the original
        // word, which doesn't change, and the ending index, which does, as we progress though the sequence. (Imagine
        // it getting progressively bigger.)
        //
        // The strategy then is to create an array of Solutions, each one tied to an index at which the substring
        // block starter occurred, and to advance through the substrings in each sequence by always taking
        // lexicographically minimal substring.
        //
        // To ensure that we can do this, we have a Comparator for Solutions, which is based on lexicographic
        // order of the next word in each Solution, and a PriorityQueue that keeps the minimal one on top.
        //
        //
        // so, we have to 1) generate the first substring beginning at index at which c occurs in w
        //                2) take the lexicographically smallest of these and, for whichever index generate that
        //                   substring, generate the next one
        //                3) repeat until we reach the kth character
        indices = charsAt.get(c);
        // This array represents the index of the final character in the next substring for all substring sub-blocks
        Solution[] substrGenerators = new Solution[indices.size()];
        for (int i = 0; i < substrGenerators.length; i++) {
            substrGenerators[i] = new Solution(indices.get(i));
        }


        Comparator<Solution> comparator = new Comparator<Solution>() {
            @Override
            public int compare(Solution o1, Solution o2) {
                return o1.peekNextSubstring().compareTo(o2.peekNextSubstring());
            }
        };

        // Sort the substring generators based on the lexicographic ordering of the next substring they generate
        // This
        PriorityQueue<Solution> nextSubstr = new PriorityQueue<>(indices.size(), comparator);

        for (Solution s : substrGenerators) {
            if (s.hasNext()) {
                nextSubstr.add(s);
                //s.next();
            }
        }
        // the length of the next lexicographically ordered substring
        int nextSubstrLen = nextSubstr.peek().nextSubstringLength();
        Solution s;
        String previousSubstring = "";
        // After taking a word, we'll need to remove all of the sequences from the priority queue,
        // advance them until they point a different word
        // and then put them back.
        // One idea is to do a number of pairwise merges
        // and at eac pass, to remove / skip elts that are repeated
        // maybe treat one line as the primary and the other as the secondary?
        // can we remove duplicates first?

        // there's no way to get around having to remove
        // we can use clear
        while (runningLength + nextSubstrLen < K) {
            s = nextSubstr.poll();
            if (!s.peekNextSubstring().equals(previousSubstring)) {
                runningLength += nextSubstrLen;
                previousSubstring = s.peekNextSubstring();
            }
            // Remove the generator from the priority queue

            //previousSubstring = s.peekNextSubstring();
            // advance it
            s.next();
            // if it can generate more
            if (s.hasNext()) {
                // and add it back to the queue
                nextSubstr.add(s);
            }

            // determine how many characters the next substring would add to the running length
            nextSubstrLen = nextSubstr.peek().nextSubstringLength();
        }

        // nextSubstr.poll() should get us the next Solution, which in turn should get us the next substring
        // determine how many characters until the kth character
        int remainingCharacters = K - runningLength;
        s = nextSubstr.poll();
        //System.out.println(s.peekNextSubstring());
        return s.peekNextSubstring().charAt(remainingCharacters - 1);
    }


    /**
     * Computes the sum of lengths of the substring blocks formed the characters at the positions @indices
     * in
     * @param wordLength the length of that word
     * @param indices the indices in a word where a particular character may occur
     * @return the sum of lengths of all substrings formed when starting at these indices
     */
    public static int computeSubstrBlkLen(int wordLength, List<Integer> indices) {
        // Stream over the indices and add to a running sum
        // Using formula for calculating the length of all substrings for a character at given index
        // which is equivalent to the sum of 1 + 2 + 3 .. N = N * (N + 1) / 2
        return indices.stream()
                .reduce(0, (sum, p) -> sum += (wordLength - p) * (wordLength - p + 1) / 2, (sum1, sum2) -> sum1 + sum2);
    }
}