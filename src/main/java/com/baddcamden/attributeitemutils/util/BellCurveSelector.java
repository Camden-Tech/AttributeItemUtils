package com.baddcamden.attributeitemutils.util;

import com.baddcamden.attributeitemutils.gear.WeightedItem;

import java.util.List;
import java.util.Random;

/**
 * Selects weighted items using a gaussian-style distribution around a target value.
 */
public class BellCurveSelector {

    /**
     * Minimum value allowed for steepness or range to prevent divide-by-zero and underflow issues.
     */
    private static final double MIN_WIDTH = 1.0e-6;

    private final Random random;

    /**
     * Creates a selector that relies on a new {@link Random} instance.
     */
    public BellCurveSelector() {
        this(new Random());
    }

    /**
     * Creates a selector that uses the provided random generator to support deterministic testing.
     */
    public BellCurveSelector(Random random) {
        this.random = random;
    }

    /**
     * Chooses a weighted item centered around the supplied target weight.
     * @param options candidate weighted entries to choose from
     * @param targetWeight the value the selector tries to align with
     * @param steepness controls how quickly the probability decays as distance grows
     * @param range scales the normalized distance; values less than or equal to zero are clamped
     * @return the selected weighted item, or {@code null} when no options are provided
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
            //VAGUE/IMPROVEMENT NEEDED Mixing linear falloff with gaussian weight lacks documented rationale.
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
     * Chooses a random entry when the computed bell curve cannot be used safely.
     */
    private WeightedItem fallback(List<WeightedItem> options) {
        return options.get(random.nextInt(options.size()));
    }

    /**
     * Ensures provided values are positive and finite, substituting {@link #MIN_WIDTH} when necessary.
     */
    private double normalizePositive(double value) {
        if (!Double.isFinite(value) || value <= MIN_WIDTH) {
            return MIN_WIDTH;
        }
        return value;
    }
}
