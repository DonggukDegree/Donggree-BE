package com.donggree.graduation.internal.domain;

import com.donggree.curriculum.GraduationRuleView;
import com.donggree.transcript.CourseRecordView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 대체 과목 이수 시 MIN_CREDITS 목표 학점을 중복 없이 조정한다. */
public final class CreditAdjustmentCalculator {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long FORBIDDEN_COST = 1_000_000_000_000_000L;

    private CreditAdjustmentCalculator() {}

    public static int adjustedTarget(GraduationRuleView rule, EvaluationContext context, int baseTarget) {
        if (baseTarget <= 0) return 0;
        try {
            JsonNode adjustments = MAPPER.readTree(rule.ruleConfig()).path("creditAdjustments");
            List<Adjustment> allAdjustments = new ArrayList<>();
            for (JsonNode adjustment : adjustments) {
                List<String> requiredCodes = readCodes(adjustment.path("requiredCourseCodes"));
                List<String> replacementCodes = readCodes(adjustment.path("replacementCourseCodes"));
                int credits = adjustment.path("credits").asInt(0);
                if (requiredCodes.isEmpty() || replacementCodes.isEmpty() || credits <= 0) continue;

                allAdjustments.add(new Adjustment(requiredCodes, replacementCodes, Math.min(credits, baseTarget)));
            }
            if (allAdjustments.isEmpty()) return baseTarget;

            List<String> passedCourseCodes = context.getPassedCoursesForRequiredCourse(rule.courseType()).stream()
                    .map(CourseRecordView::courseCode)
                    .filter(code -> code != null && !code.isBlank())
                    .distinct()
                    .sorted()
                    .toList();
            if (passedCourseCodes.isEmpty()) return baseTarget;

            // 이미 원 필수과목으로 사용된 이수 과목은 다른 필수과목의 대체 과목으로도 쓰지 않는다.
            Set<String> reservedCourseCodes = new HashSet<>();
            for (String courseCode : passedCourseCodes) {
                if (allAdjustments.stream()
                        .anyMatch(adjustment -> context.codeMatchesAny(courseCode, adjustment.requiredCourseCodes()))) {
                    reservedCourseCodes.add(courseCode);
                }
            }

            List<Adjustment> unmetAdjustments = allAdjustments.stream()
                    .filter(adjustment -> passedCourseCodes.stream()
                            .noneMatch(
                                    courseCode -> context.codeMatchesAny(courseCode, adjustment.requiredCourseCodes())))
                    .toList();
            if (unmetAdjustments.isEmpty()) return baseTarget;

            List<String> replacementCandidates = passedCourseCodes.stream()
                    .filter(courseCode -> !reservedCourseCodes.contains(courseCode))
                    .filter(courseCode -> unmetAdjustments.stream()
                            .anyMatch(adjustment ->
                                    context.codeMatchesAny(courseCode, adjustment.replacementCourseCodes())))
                    .toList();
            if (replacementCandidates.isEmpty()) return baseTarget;

            int reduction = maximumCreditReduction(unmetAdjustments, replacementCandidates, context, baseTarget);
            return Math.max(0, baseTarget - reduction);
        } catch (Exception ignored) {
            return baseTarget;
        }
    }

    /** 각 대체 과목을 최대 한 행에만 배정하고, 감면 학점 합계가 최대가 되는 조합을 선택한다. */
    private static int maximumCreditReduction(
            List<Adjustment> adjustments,
            List<String> replacementCandidates,
            EvaluationContext context,
            int baseTarget) {
        int rowCount = adjustments.size();
        int candidateCount = replacementCandidates.size();
        // 더미 열을 행 수만큼 추가해 대체 과목이 없는 행도 배정할 수 있게 한다.
        int columnCount = candidateCount + rowCount;
        long[][] costs = new long[rowCount][columnCount];
        for (int row = 0; row < rowCount; row++) {
            Adjustment adjustment = adjustments.get(row);
            for (int column = 0; column < candidateCount; column++) {
                String courseCode = replacementCandidates.get(column);
                costs[row][column] = context.codeMatchesAny(courseCode, adjustment.replacementCourseCodes())
                        ? -adjustment.credits()
                        : FORBIDDEN_COST;
            }
            Arrays.fill(costs[row], candidateCount, columnCount, 0L);
        }

        int[] assignedColumns = minimumCostAssignment(costs);
        long reduction = 0;
        for (int row = 0; row < rowCount; row++) {
            int column = assignedColumns[row];
            if (column >= 0 && column < candidateCount && costs[row][column] < 0) {
                reduction -= costs[row][column];
                if (reduction >= baseTarget) return baseTarget;
            }
        }
        return (int) reduction;
    }

    /** 직사각형 비용 행렬의 최소 비용 완전 배정(Hungarian algorithm), 반환값은 행별 열 인덱스. */
    private static int[] minimumCostAssignment(long[][] costs) {
        int rowCount = costs.length;
        int columnCount = costs[0].length;
        long[] rowPotential = new long[rowCount + 1];
        long[] columnPotential = new long[columnCount + 1];
        int[] rowByColumn = new int[columnCount + 1];
        int[] previousColumn = new int[columnCount + 1];

        for (int row = 1; row <= rowCount; row++) {
            rowByColumn[0] = row;
            long[] minReducedCost = new long[columnCount + 1];
            Arrays.fill(minReducedCost, Long.MAX_VALUE / 4);
            boolean[] usedColumns = new boolean[columnCount + 1];
            int currentColumn = 0;

            do {
                usedColumns[currentColumn] = true;
                int currentRow = rowByColumn[currentColumn];
                long delta = Long.MAX_VALUE / 4;
                int nextColumn = 0;
                for (int column = 1; column <= columnCount; column++) {
                    if (usedColumns[column]) continue;
                    long reducedCost =
                            costs[currentRow - 1][column - 1] - rowPotential[currentRow] - columnPotential[column];
                    if (reducedCost < minReducedCost[column]) {
                        minReducedCost[column] = reducedCost;
                        previousColumn[column] = currentColumn;
                    }
                    if (minReducedCost[column] < delta) {
                        delta = minReducedCost[column];
                        nextColumn = column;
                    }
                }

                for (int column = 0; column <= columnCount; column++) {
                    if (usedColumns[column]) {
                        rowPotential[rowByColumn[column]] += delta;
                        columnPotential[column] -= delta;
                    } else {
                        minReducedCost[column] -= delta;
                    }
                }
                currentColumn = nextColumn;
            } while (rowByColumn[currentColumn] != 0);

            do {
                int priorColumn = previousColumn[currentColumn];
                rowByColumn[currentColumn] = rowByColumn[priorColumn];
                currentColumn = priorColumn;
            } while (currentColumn != 0);
        }

        int[] assignedColumns = new int[rowCount];
        Arrays.fill(assignedColumns, -1);
        for (int column = 1; column <= columnCount; column++) {
            if (rowByColumn[column] != 0) assignedColumns[rowByColumn[column] - 1] = column - 1;
        }
        return assignedColumns;
    }

    private record Adjustment(List<String> requiredCourseCodes, List<String> replacementCourseCodes, int credits) {}

    private static List<String> readCodes(JsonNode node) {
        List<String> codes = new ArrayList<>();
        if (!node.isArray()) return codes;
        for (JsonNode value : node) {
            if (value.isTextual() && !value.asText().isBlank()) codes.add(value.asText());
        }
        return codes;
    }
}
