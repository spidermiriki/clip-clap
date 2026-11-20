package be.heh.clipclap.catalog.internal;

import be.heh.clipclap.catalog.CreatorPublicProfile;
import be.heh.clipclap.catalog.CreatorSummary;
import be.heh.clipclap.catalog.CreatorSummaryPage;
import be.heh.clipclap.creators.AlbumService;
import be.heh.clipclap.creators.CreatorProfileService;
import be.heh.clipclap.creators.CreatorSearchCriteria;
import be.heh.clipclap.offerings.OfferingApi;
import be.heh.clipclap.users.UserApi;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
class GalleryService {

	private final CreatorProfileService creatorProfileService;
	private final AlbumService albumService;
	private final UserApi userApi;
	private final OfferingApi offeringApi;

	GalleryService(CreatorProfileService creatorProfileService, AlbumService albumService, UserApi userApi,
			OfferingApi offeringApi) {
		this.creatorProfileService = creatorProfileService;
		this.albumService = albumService;
		this.userApi = userApi;
		this.offeringApi = offeringApi;
	}

	/** Créateurs visibles publiquement (profil validé, compte non suspendu, #75) et correspondant aux filtres (#14). */
	CreatorSummaryPage findCreators(int page, int size, CreatorSearchCriteria criteria) {
		// Les suspendus sont exclus dans la requête paginée de creators, pas après coup,
		// pour que totalElements et le contenu des pages restent justes. Le coût dépend du
		// nombre de suspendus, plus du nombre de créateurs validés (#98)
		Set<Long> suspendus = userApi.findAllSuspendedIds();
		var profilePage = creatorProfileService.findAllValidated(page, size, suspendus, criteria);
		var summaries = profilePage.items().stream()
				.map(p -> new CreatorSummary(p.userId(), p.displayName(), p.photoUrl(), p.city(), p.country()))
				.toList();
		return new CreatorSummaryPage(summaries, profilePage.page(), profilePage.size(), profilePage.totalElements());
	}

	/** Profil public (#17), albums (#15) et prestations (#27) : mêmes règles de visibilité que la galerie (validé, non suspendu). */
	CreatorPublicProfile findPublicProfile(long userId) {
		return creatorProfileService.findByUserId(userId)
				.filter(p -> "VALIDATED".equals(p.validationStatus()))
				.filter(p -> userApi.findSuspendedIds(Set.of(userId)).isEmpty())
				.map(p -> new CreatorPublicProfile(p.userId(), p.displayName(), p.photoUrl(), p.bio(),
						p.city(), p.country(), p.serviceAreas(), albumService.findAlbumsOf(userId),
						offeringApi.findOfferingsOf(userId)))
				.orElseThrow(() -> new CreatorNotPublicException(userId));
	}
}
