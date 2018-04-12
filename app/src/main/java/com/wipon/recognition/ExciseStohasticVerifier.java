package com.wipon.recognition;

import android.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExciseStohasticVerifier {

    private List<String> candidates = new ArrayList<>();
    private String[] charCandidates = new String[9];
    private int minimumCandidatesCount;
    private Double bottomProbabilityThreshold;

    private Map<String, String> map = new HashMap<>();

    public String lastPossibleNumber = "";

    public Double[] charProbability = new Double[9];

    ExciseStohasticVerifier(int _minimumCandidatesCount, Double _bottomProbabilityThreshold){
        minimumCandidatesCount = _minimumCandidatesCount;
        bottomProbabilityThreshold = _bottomProbabilityThreshold;

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
        map.put("g", "9");
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
        map.put("q", "4");
    }

    private double calculatePenalty(int x){
        double penalty = 0.0;
        // Use logistic function or sigmoid curve for penalty (https://en.wikipedia.org/wiki/Sigmoid_function)
        if (x > 0){
            penalty = 1/(1+Math.exp((6-x)/3)); // Reserved logarythm penalty Math.log10(len+1)/(4*Math.log10(2))
        }

        if (penalty < 0){
            penalty = 0.0;
        }

        if (penalty > 0.96){
            penalty = 1.0;
        }

        return penalty;
    }

    private Double correctProbability(double penalty, Double realProbability){
        Double result = penalty * realProbability;

        if (result < bottomProbabilityThreshold){
            return 0.0;
        }

        return (result - bottomProbabilityThreshold) / (1.0 - bottomProbabilityThreshold);
    }

    private Pair<Character, Double> getMaxOccuringChar(String str)
    {
        int count[] = new int[255];

        int len = str.length();
        double penalty = calculatePenalty(len);
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

        Double realProbability = ((double) max) / len;

        Double showedProbability = correctProbability(penalty, realProbability);

        return new Pair<>(result, showedProbability);
    }

    @SuppressWarnings("unused")
    public void setMinimumCandidatesCount(int minimumCandidatesCount) {
        this.minimumCandidatesCount = minimumCandidatesCount;
    }

    public void setBottomProbabilityThreshold(Double bottomProbabilityThreshold){
        this.bottomProbabilityThreshold = bottomProbabilityThreshold;
    }

    public void clearAccumulator(){
        charProbability = new Double[9];
        lastPossibleNumber = "";
        candidates = new ArrayList<>();
        charCandidates = new String[9];
    }

    public String calculatePossibleNumber(){
        if (candidates.size() < minimumCandidatesCount){
            lastPossibleNumber = "";
            return "";
        }

        StringBuilder possibleNumber = new StringBuilder();

        // Calculations
        for (int i = 0; i < 9; i++){
            if (charCandidates[i] != null) {
                if (charCandidates[i].length() > 0) {
                    Pair<Character, Double> prediction = getMaxOccuringChar(charCandidates[i]);
                    possibleNumber.append(prediction.first);
                    charProbability[i] = prediction.second;
                }
            }
        }
        lastPossibleNumber = possibleNumber.toString();

        return possibleNumber.toString();
    }

    @SuppressWarnings("unused")
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
