package com.dressme.dressme_back.service;

import com.dressme.dressme_back.client.DatabaseColorCompatibilityClient;
import com.dressme.dressme_back.schema.dto.ColorCompatibilityBatchRequest;
import com.dressme.dressme_back.schema.dto.ColorCompatibilityPairRequest;
import com.dressme.dressme_back.schema.dto.ColorScoreRequest;
import com.dressme.dressme_back.schema.dto.ColorScoreResponse;
import com.dressme.dressme_back.schema.dto.DatabaseCompatibilityResponse;
import com.dressme.dressme_back.schema.dto.Slot;
import com.dressme.dressme_back.schema.dto.SlotColorInput;
import com.dressme.dressme_back.schema.dto.SlotPairScore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ColorScoreService {

    private final DatabaseColorCompatibilityClient databaseClient;

    public ColorScoreResponse score(ColorScoreRequest request) {
        Map<Slot, SlotColorInput> bySlot = mapBySlot(request.items());
        List<String> warnings = new ArrayList<>();
        List<PairWeight> pairWeights = buildPairWeights(bySlot.keySet(), warnings);

        if (pairWeights.isEmpty()) {
            warnings.add("Insufficient slots to score color.");
            return new ColorScoreResponse(0.0, false, List.of(), warnings);
        }

        List<PairWeight> effectivePairs = new ArrayList<>();
        List<ColorCompatibilityPairRequest> batchItems = new ArrayList<>();

        for (PairWeight pairWeight : pairWeights) {
            SlotColorInput left = bySlot.get(pairWeight.slotA);
            SlotColorInput right = bySlot.get(pairWeight.slotB);
            if (left == null || right == null) {
                continue;
            }

            effectivePairs.add(pairWeight);
            batchItems.add(new ColorCompatibilityPairRequest(
                left.hue(), left.saturation(), left.lightness(),
                right.hue(), right.saturation(), right.lightness()
            ));
        }

        if (batchItems.isEmpty()) {
            warnings.add("Insufficient slots to score color.");
            return new ColorScoreResponse(0.0, false, List.of(), warnings);
        }

        List<DatabaseCompatibilityResponse> batchResponses = databaseClient
            .checkCompatibilityBatch(new ColorCompatibilityBatchRequest(batchItems));

        if (batchResponses.size() != batchItems.size()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Color compatibility batch response size mismatch."
            );
        }

        double totalScore = 0.0;
        List<SlotPairScore> pairScores = new ArrayList<>();

        for (int i = 0; i < effectivePairs.size(); i++) {
            PairWeight pairWeight = effectivePairs.get(i);
            double compatibilityScore = batchResponses.get(i).compatibilityScore();
            double weightedScore = compatibilityScore * pairWeight.weight;
            totalScore += weightedScore;

            pairScores.add(new SlotPairScore(
                pairWeight.slotA,
                pairWeight.slotB,
                compatibilityScore,
                pairWeight.weight,
                weightedScore
            ));
        }

        return new ColorScoreResponse(totalScore, true, pairScores, warnings);
    }

    private Map<Slot, SlotColorInput> mapBySlot(List<SlotColorInput> items) {
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one slot is required.");
        }

        Map<Slot, SlotColorInput> bySlot = new EnumMap<>(Slot.class);
        for (SlotColorInput item : items) {
            Slot existing = item.slot();
            if (bySlot.putIfAbsent(existing, item) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate slot: " + existing);
            }
        }
        return bySlot;
    }

    private List<PairWeight> buildPairWeights(Set<Slot> slots, List<String> warnings) {
        if (slots.contains(Slot.ONEPIECE)) {
            return buildOnepieceWeights(slots, warnings);
        }
        return buildSeparatePieceWeights(slots, warnings);
    }

    private List<PairWeight> buildOnepieceWeights(Set<Slot> slots, List<String> warnings) {
        boolean hasOuterwear = slots.contains(Slot.OUTERWEAR);
        boolean hasFootwear = slots.contains(Slot.FOOTWEAR);

        if (slots.contains(Slot.TOP) || slots.contains(Slot.BOTTOM)) {
            warnings.add("ONEPIECE provided with TOP/BOTTOM. TOP/BOTTOM ignored.");
        }

        List<PairWeight> pairs = new ArrayList<>();
        if (hasOuterwear && hasFootwear) {
            pairs.add(new PairWeight(Slot.ONEPIECE, Slot.OUTERWEAR, 0.60));
            pairs.add(new PairWeight(Slot.ONEPIECE, Slot.FOOTWEAR, 0.40));
            return pairs;
        }

        if (hasOuterwear) {
            pairs.add(new PairWeight(Slot.ONEPIECE, Slot.OUTERWEAR, 1.0));
        }
        if (hasFootwear) {
            pairs.add(new PairWeight(Slot.ONEPIECE, Slot.FOOTWEAR, 1.0));
        }
        return pairs;
    }

    private List<PairWeight> buildSeparatePieceWeights(Set<Slot> slots, List<String> warnings) {
        boolean hasTop = slots.contains(Slot.TOP);
        boolean hasBottom = slots.contains(Slot.BOTTOM);
        boolean hasOuterwear = slots.contains(Slot.OUTERWEAR);
        boolean hasFootwear = slots.contains(Slot.FOOTWEAR);

        if (hasTop && hasBottom) {
            List<PairWeight> pairs = new ArrayList<>();
            int extras = 0;

            if (hasOuterwear) {
                extras++;
            }
            if (hasFootwear) {
                extras++;
            }

            if (extras == 0) {
                pairs.add(new PairWeight(Slot.TOP, Slot.BOTTOM, 1.0));
                return pairs;
            }

            if (extras == 1) {
                pairs.add(new PairWeight(Slot.TOP, Slot.BOTTOM, 0.60));
                if (hasOuterwear) {
                    pairs.add(new PairWeight(Slot.TOP, Slot.OUTERWEAR, 0.25));
                    pairs.add(new PairWeight(Slot.BOTTOM, Slot.OUTERWEAR, 0.15));
                } else {
                    pairs.add(new PairWeight(Slot.TOP, Slot.FOOTWEAR, 0.25));
                    pairs.add(new PairWeight(Slot.BOTTOM, Slot.FOOTWEAR, 0.15));
                }
                return pairs;
            }

            pairs.add(new PairWeight(Slot.TOP, Slot.BOTTOM, 0.45));
            pairs.add(new PairWeight(Slot.TOP, Slot.OUTERWEAR, 0.20));
            pairs.add(new PairWeight(Slot.BOTTOM, Slot.OUTERWEAR, 0.15));
            pairs.add(new PairWeight(Slot.TOP, Slot.FOOTWEAR, 0.10));
            pairs.add(new PairWeight(Slot.BOTTOM, Slot.FOOTWEAR, 0.10));
            return pairs;
        }

        List<PairWeight> pairs = new ArrayList<>();
        if (hasTop && hasOuterwear) {
            pairs.add(new PairWeight(Slot.TOP, Slot.OUTERWEAR, 1.0));
        }
        if (hasTop && hasFootwear) {
            pairs.add(new PairWeight(Slot.TOP, Slot.FOOTWEAR, 1.0));
        }
        if (hasBottom && hasOuterwear) {
            pairs.add(new PairWeight(Slot.BOTTOM, Slot.OUTERWEAR, 1.0));
        }
        if (hasBottom && hasFootwear) {
            pairs.add(new PairWeight(Slot.BOTTOM, Slot.FOOTWEAR, 1.0));
        }

        if (pairs.size() > 1) {
            warnings.add("Fallback weighting used for partial outfit.");
            normalizeWeights(pairs);
        }

        return pairs;
    }

    private void normalizeWeights(List<PairWeight> pairs) {
        double total = pairs.stream().mapToDouble(pair -> pair.weight).sum();
        if (total <= 0.0) {
            return;
        }
        for (int i = 0; i < pairs.size(); i++) {
            PairWeight pair = pairs.get(i);
            pairs.set(i, new PairWeight(pair.slotA, pair.slotB, pair.weight / total));
        }
    }

    private static class PairWeight {
        private final Slot slotA;
        private final Slot slotB;
        private final double weight;

        private PairWeight(Slot slotA, Slot slotB, double weight) {
            this.slotA = slotA;
            this.slotB = slotB;
            this.weight = weight;
        }
    }
}
