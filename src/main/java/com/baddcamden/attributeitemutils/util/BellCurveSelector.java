package com.baddcamden.attributeitemutils.util;

import com.baddcamden.attributeitemutils.gear.WeightedItem;

import java.util.List;
import java.util.Random;

public class BellCurveSelector {

    private static final double MIN_WIDTH = 1.0e-6;

    private final Random random;

    public BellCurveSelector() {
        this(new Random());
    }

    public BellCurveSelector(Random random) {
        this.random = random;
    }

    public WeightedItem select(List<WeightedItem> options, double targetWeight, double steepness, double range) {
        if (options.isEmpty()) {
            return null;
        }

        double safeSteepness = normalizePositive(steepness);
        double safeRange = normalizePositive(range);
        double total = 0;
        double[] weights = new double[options.size()];
        for (int i = 0; i < options.size(); i++) {
            WeightedItem item = options.get(i);
            double distance = Math.abs(item.weight() - targetWeight);
            double normalized = Math.max(0, 1 - (distance / safeRange));
            double bell = Math.exp(-Math.pow(distance, 2) / (2 * safeSteepness * safeSteepness));
            double w = normalized * bell;
            weights[i] = Double.isFinite(w) ? w : 0;
            total += weights[i];
        }
        if (!Double.isFinite(total) || total <= 0) {
            return fallback(options);
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

    private WeightedItem fallback(List<WeightedItem> options) {
        return options.get(random.nextInt(options.size()));
    }

    private double normalizePositive(double value) {
        if (!Double.isFinite(value) || value <= MIN_WIDTH) {
            return MIN_WIDTH;
        }
        return value;
    }
}
