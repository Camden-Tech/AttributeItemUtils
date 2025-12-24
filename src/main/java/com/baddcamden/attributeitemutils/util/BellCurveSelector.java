package com.baddcamden.attributeitemutils.util;

import com.baddcamden.attributeitemutils.gear.WeightedItem;

import java.util.List;
import java.util.Random;

public class BellCurveSelector {

    private final Random random = new Random();

    public WeightedItem select(List<WeightedItem> options, double targetWeight, double steepness, double range) {
        if (options.isEmpty()) {
            return null;
        }
        double total = 0;
        double[] weights = new double[options.size()];
        for (int i = 0; i < options.size(); i++) {
            WeightedItem item = options.get(i);
            double distance = Math.abs(item.weight() - targetWeight);
            double normalized = Math.max(0, 1 - (distance / range));
            double bell = Math.exp(-Math.pow(distance, 2) / (2 * steepness * steepness));
            double w = normalized * bell;
            weights[i] = w;
            total += w;
        }
        double roll = random.nextDouble() * total;
        for (int i = 0; i < weights.length; i++) {
            roll -= weights[i];
            if (roll <= 0) {
                return options.get(i);
            }
        }
        return options.get(options.size() - 1);
    }
}
