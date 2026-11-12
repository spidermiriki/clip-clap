package be.heh.clipclap.catalog.internal;

import be.heh.clipclap.catalog.Category;
import be.heh.clipclap.catalog.CreatorPublicProfile;
import be.heh.clipclap.catalog.CreatorSummaryPage;
import be.heh.clipclap.creators.CreatorSearchCriteria;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
class GalleryController {

	private final GalleryService galleryService;
	private final CategoryRepository categoryRepository;

	GalleryController(GalleryService galleryService, CategoryRepository categoryRepository) {
		this.galleryService = galleryService;
		this.categoryRepository = categoryRepository;
	}

	@GetMapping("/creators")
	CreatorSummaryPage getCreators(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String city,
			@RequestParam(required = false) String country,
			@RequestParam(required = false) Integer categoryId,
			@RequestParam(required = false) String mediaType) {
		return galleryService.findCreators(page, size, new CreatorSearchCriteria(city, country, categoryId, mediaType));
	}

	@GetMapping("/creators/{userId}")
	CreatorPublicProfile getCreatorProfile(@PathVariable long userId) {
		return galleryService.findPublicProfile(userId);
	}

	@GetMapping("/categories")
	List<Category> getCategories() {
		return categoryRepository.findAll();
	}

	@ExceptionHandler(CreatorNotPublicException.class)
	ResponseEntity<ProblemDetail> handleCreatorNotPublic(CreatorNotPublicException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problem.setTitle("Créateur introuvable");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage()));
	}
}
