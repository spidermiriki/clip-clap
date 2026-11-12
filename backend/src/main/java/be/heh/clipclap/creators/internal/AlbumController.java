package be.heh.clipclap.creators.internal;

import be.heh.clipclap.creators.Album;
import be.heh.clipclap.creators.Album.Photo;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Albums du créateur connecté (#15). L'upload d'une photo se fait en trois temps :
 * signature demandée ici, envoi direct du navigateur à Cloudinary, puis enregistrement
 * de la réponse de Cloudinary une fois sa signature vérifiée.
 */
@RestController
@RequestMapping("/api/creators/me/albums")
class AlbumController {

    record AlbumName(String name) {}

    private final AlbumServiceImpl albumService;
    private final CurrentUser currentUser;

    AlbumController(AlbumServiceImpl albumService, CurrentUser currentUser) {
        this.albumService = albumService;
        this.currentUser = currentUser;
    }

    @GetMapping
    List<Album> list(Authentication auth) {
        return albumService.listMine(currentUser.id(auth));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Album create(Authentication auth, @RequestBody AlbumName body) {
        return albumService.create(currentUser.id(auth), body.name());
    }

    @PutMapping("/{albumId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void rename(Authentication auth, @PathVariable long albumId, @RequestBody AlbumName body) {
        albumService.rename(currentUser.id(auth), albumId, body.name());
    }

    @PostMapping("/{albumId}/upload-signature")
    UploadSignature signUpload(Authentication auth, @PathVariable long albumId) {
        return albumService.signUpload(currentUser.id(auth), albumId);
    }

    @PostMapping("/{albumId}/photos")
    @ResponseStatus(HttpStatus.CREATED)
    Photo addPhoto(Authentication auth, @PathVariable long albumId, @RequestBody AddPhotoCommand command) {
        return albumService.addPhoto(currentUser.id(auth), albumId, command);
    }
}
