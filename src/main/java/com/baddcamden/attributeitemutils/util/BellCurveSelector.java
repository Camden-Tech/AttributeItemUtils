package com.baddcamden.attributeitemutils.util;

import com.baddcamden.attributeitemutils.gear.WeightedItem;

import java.util.List;
import java.util.Random;

public class BellCurveSelector {

    /** Minimum width used when steepness or range are non-positive to prevent divide-by-zero issues. */
    private static final double MIN_WIDTH = 1.0e-6;

    /** Source of randomness for selection rolls, injected for deterministic testing. */
    private final Random random;

    /**
     * Creates a selector backed by a new random instance.
     */
    public BellCurveSelector() {
        this(new Random());
    }

    /**
     * Creates a selector that relies on the provided random generator.
     */
    public BellCurveSelector(Random random) {
        this.random = random;
    }

    /**
     * Chooses a weighted item using a bell-curve distribution centered on the target weight.
     */
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
            //VAGUE/IMPROVEMENT NEEDED Multiplying a linear falloff by a Gaussian may distort the intended bell distribution; confirm combined weighting model.
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

    /**
     * Provides a uniform random selection when calculated weights are invalid.
     */
    private WeightedItem fallback(List<WeightedItem> options) {
        return options.get(random.nextInt(options.size()));
    }

    /**
     * Ensures parameters used in exponential calculations stay positive and finite.
     */
    private double normalizePositive(double value) {
        if (!Double.isFinite(value) || value <= MIN_WIDTH) {
            return MIN_WIDTH;
        }
        return value;
    }
}
