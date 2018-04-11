package com.wipon.recognition;

import android.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExciseStohasticVerifier {

    private List<String> candidates = new ArrayList<>();
    private String[] charCandidates = new String[9];

    private Map<String, String> map = new HashMap<>();

    public String lastPossibleNumber = "";

    public Integer[] charProbability = new Integer[9];

    ExciseStohasticVerifier(){
        // Must have
        map.put("|", "");

        // Optional
        map.put("O", "0");
        map.put("o", "0");
        map.put("Z", "7");
        map.put("z", "7");
        map.put("A", "4");
        map.put("T", "7");
        map.put("S", "5");
        map.put("s", "5");
        map.put("g", "4");
        map.put("D", "0");
        map.put("p", "0");
        map.put("e", "8");
        map.put("B", "8");
        map.put("b", "6");
        map.put("G", "6");

        // Very optional
        map.put("t", "1");
        map.put("a", "4");
        map.put("X", "7");
    }

    private Pair<Character, Integer> getMaxOccuringChar(String str)
    {
        int count[] = new int[255];

        int len = str.length();
        for (int i = 0; i < len; i++)
            count[str.charAt(i)]++;

        int max = -1;
        char result = ' ';

        for (int i = 0; i < len; i++) {
            if (max < count[str.charAt(i)]) {
                max = count[str.charAt(i)];
                result = str.charAt(i);
            }
        }

        return new Pair<>(result, (100 * max / len));
    }

    public void clearAccumulator(){
        charProbability = new Integer[9];
        lastPossibleNumber = "";
        candidates = new ArrayList<>();
        charCandidates = new String[9];
    }

    public String calculatePossibleNumber(){
        StringBuilder possibleNumber = new StringBuilder();

        // Calculations
        for (int i = 0; i < 9; i++){
            if (charCandidates[i] != null) {
                if (charCandidates[i].length() > 0) {
                    Pair<Character, Integer> prediction = getMaxOccuringChar(charCandidates[i]);
                    possibleNumber.append(prediction.first);
                    charProbability[i] = prediction.second;
                }
            }
        }
        lastPossibleNumber = possibleNumber.toString();

        // Clear accumulators
        candidates = new ArrayList<>();
        charCandidates = new String[9];

        return possibleNumber.toString();
    }

    public int getCandidatesCount(){
        return candidates.size();
    }

    @SuppressWarnings("unused")
    public String lastAddedNumber(){
        if (candidates.isEmpty()) {
            return "";
        } else {
            return candidates.get(candidates.size() - 1);
        }
    }

    public boolean addNumber(String str){
        for (Map.Entry<String, String> entry : map.entrySet()) {
            str = str.replace( entry.getKey(), entry.getValue());
        }

        String number = str.replaceAll("[^0-9]+", "");
        if (number.length() > 8){
            candidates.add(number);
            for (int j = 1; j < 10; j++){
                charCandidates[9 - j] += number.charAt(number.length() - j);
            }
            return true;
        }

        return false;
    }
}
