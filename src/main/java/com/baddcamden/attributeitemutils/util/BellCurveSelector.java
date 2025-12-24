package com.baddcamden.attributeitemutils.util;

import com.baddcamden.attributeitemutils.gear.WeightedItem;

import java.util.List;
import java.util.Random;

/**
 * Selects a {@link WeightedItem} based on a bell-curve distribution centered on a target weight.
 * The selection process prevents invalid or empty inputs from causing crashes.
 */
public class BellCurveSelector {

    /** Minimum width used when caller-provided curve widths are invalid or zero. */
    private static final double MIN_WIDTH = 1.0e-6;

    /** Source of randomness for item selection. */
    private final Random random;

    /**
     * Creates a selector using a new {@link Random} instance.
     */
    public BellCurveSelector() {
        this(new Random());
    }

    /**
     * Creates a selector using the provided random instance.
     *
     * @param random random number source used for weighted selection
     */
    public BellCurveSelector(Random random) {
        this.random = random;
    }

    /**
     * Chooses an item using a bell curve centered on the provided target weight.
     *
     * @param options      available weighted items
     * @param targetWeight target value to bias selection toward
     * @param steepness    controls how quickly probability drops as distance from the target grows
     * @param range        linear falloff range that combines with the Gaussian curve
     * @return a randomly selected item, or {@code null} when no options exist
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
            weights[i] = calculateWeight(options.get(i), targetWeight, safeSteepness, safeRange);
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
     * Calculates the probability weight for the provided item relative to the target.
     */
    private double calculateWeight(WeightedItem item, double targetWeight, double safeSteepness, double safeRange) {
        double distance = Math.abs(item.weight() - targetWeight);
        double normalized = Math.max(0, 1 - (distance / safeRange)); //VAGUE/IMPROVEMENT NEEDED Justify combining linear falloff with Gaussian weighting or replace with a single model
        double bell = Math.exp(-Math.pow(distance, 2) / (2 * safeSteepness * safeSteepness));
        double weightedValue = normalized * bell;
        return Double.isFinite(weightedValue) ? weightedValue : 0;
    }

    /**
     * Returns a uniformly random item when the computed weights are invalid.
     */
    private WeightedItem fallback(List<WeightedItem> options) {
        return options.get(random.nextInt(options.size()));
    }

    /**
     * Ensures provided curve parameters are positive and finite to prevent math errors.
     */
    private double normalizePositive(double value) {
        if (!Double.isFinite(value) || value <= MIN_WIDTH) {
            return MIN_WIDTH;
        }
        return value;
    }
}
