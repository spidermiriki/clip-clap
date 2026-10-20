package be.heh.clipclap.creators;

import java.util.List;

public record CreatorProfilePage(
		List<CreatorProfile> items,
		int page,
		int size,
		long totalElements
) {}
