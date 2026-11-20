package musictracker.Service;

import java.util.List;

public final class CommunityMath {

    private CommunityMath() {
    }

    public static double percent(long part, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return round(part * 100.0 / total);
    }

    public static double topPercent(List<Long> listenCounts, long yourCount) {
        if (listenCounts.isEmpty()) {
            return 0.0;
        }
        long higher = listenCounts.stream().filter(count -> count > yourCount).count();
        return percent(higher + 1, listenCounts.size());
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
