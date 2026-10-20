package be.heh.clipclap.catalog.internal;

import be.heh.clipclap.catalog.CreatorSummaryPage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
class GalleryController {

	private final GalleryService galleryService;

	GalleryController(GalleryService galleryService) {
		this.galleryService = galleryService;
	}

	@GetMapping("/creators")
	CreatorSummaryPage getCreators(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return galleryService.findCreators(page, size);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(ex.getMessage());
	}
}
