package be.heh.clipclap.catalog;

import java.util.List;

public record CreatorSummaryPage(
		List<CreatorSummary> items,
		int page,
		int size,
		long totalElements
) {}
