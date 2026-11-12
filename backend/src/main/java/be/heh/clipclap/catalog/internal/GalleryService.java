package be.heh.clipclap.catalog.internal;

import be.heh.clipclap.catalog.CreatorSummary;
import be.heh.clipclap.catalog.CreatorSummaryPage;
import be.heh.clipclap.creators.CreatorProfileService;
import be.heh.clipclap.creators.CreatorSearchCriteria;
import be.heh.clipclap.users.UserApi;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
class GalleryService {

	private final CreatorProfileService creatorProfileService;
	private final UserApi userApi;

	GalleryService(CreatorProfileService creatorProfileService, UserApi userApi) {
		this.creatorProfileService = creatorProfileService;
		this.userApi = userApi;
	}

	/** Créateurs visibles publiquement (profil validé, compte non suspendu, #75) et correspondant aux filtres (#14). */
	CreatorSummaryPage findCreators(int page, int size, CreatorSearchCriteria criteria) {
		// Les suspendus sont exclus dans la requête paginée de creators, pas après coup,
		// pour que totalElements et le contenu des pages restent justes
		Set<Long> suspendus = userApi.findSuspendedIds(creatorProfileService.findAllValidatedUserIds());
		var profilePage = creatorProfileService.findAllValidated(page, size, suspendus, criteria);
		var summaries = profilePage.items().stream()
				.map(p -> new CreatorSummary(p.userId(), p.displayName(), p.photoUrl(), p.city(), p.country()))
				.toList();
		return new CreatorSummaryPage(summaries, profilePage.page(), profilePage.size(), profilePage.totalElements());
	}
}
