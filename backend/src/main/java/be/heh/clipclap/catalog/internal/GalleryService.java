package be.heh.clipclap.catalog.internal;

import be.heh.clipclap.catalog.CreatorSummary;
import be.heh.clipclap.catalog.CreatorSummaryPage;
import be.heh.clipclap.creators.CreatorProfileService;
import org.springframework.stereotype.Service;

@Service
class GalleryService {

	private final CreatorProfileService creatorProfileService;

	GalleryService(CreatorProfileService creatorProfileService) {
		this.creatorProfileService = creatorProfileService;
	}

	CreatorSummaryPage findCreators(int page, int size) {
		var profilePage = creatorProfileService.findAllValidated(page, size);
		var summaries = profilePage.items().stream()
				.map(p -> new CreatorSummary(p.userId(), p.displayName(), p.photoUrl(), p.city(), p.country()))
				.toList();
		return new CreatorSummaryPage(summaries, profilePage.page(), profilePage.size(), profilePage.totalElements());
	}
}
