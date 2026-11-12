package be.heh.clipclap.creators.internal;

import be.heh.clipclap.creators.Album;
import be.heh.clipclap.creators.Album.Photo;
import be.heh.clipclap.creators.AlbumService;
import be.heh.clipclap.creators.CreatorProfileNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class AlbumServiceImpl implements AlbumService {

    static final int NAME_MAX_LENGTH = 100;

    private final AlbumRepository albums;
    private final CreatorProfileRepository profiles;
    private final UploadSigner uploadSigner;

    AlbumServiceImpl(AlbumRepository albums, CreatorProfileRepository profiles, UploadSigner uploadSigner) {
        this.albums = albums;
        this.profiles = profiles;
        this.uploadSigner = uploadSigner;
    }

    @Override
    public List<Album> findAlbumsOf(long creatorId) {
        return albums.findByCreator(creatorId);
    }

    List<Album> listMine(long userId) {
        requireProfile(userId);
        return albums.findByCreator(userId);
    }

    Album create(long userId, String name) {
        requireProfile(userId);
        String nom = validName(name);
        return new Album(albums.create(userId, nom), nom, List.of());
    }

    void rename(long userId, long albumId, String name) {
        String nom = validName(name);
        if (!albums.rename(albumId, userId, nom)) {
            throw new AlbumNotFoundException(albumId);
        }
    }

    /** Étape 1 : signature d'un upload limité au dossier de cet album. */
    UploadSignature signUpload(long userId, long albumId) {
        if (!uploadSigner.isConfigured()) {
            throw new UploadNotConfiguredException();
        }
        requireOwned(userId, albumId);
        return uploadSigner.sign(folder(userId, albumId));
    }

    /** Étape 3 : enregistrement d'une photo, seulement si la réponse de Cloudinary est authentique et cohérente. */
    Photo addPhoto(long userId, long albumId, AddPhotoCommand command) {
        if (!uploadSigner.isConfigured()) {
            throw new UploadNotConfiguredException();
        }
        requireOwned(userId, albumId);

        String publicId = required(command.publicId(), "publicId");
        String version = required(command.version(), "version");
        String signature = required(command.signature(), "signature");
        String secureUrl = required(command.secureUrl(), "secureUrl");

        if (!publicId.startsWith(folder(userId, albumId) + "/")) {
            throw new InvalidUploadException("La photo n'a pas été envoyée dans le dossier de cet album");
        }
        if (!version.matches("\\d{1,20}")) {
            throw new InvalidUploadException("Version de la photo invalide");
        }
        String prefix = "https://res.cloudinary.com/" + uploadSigner.cloudName() + "/image/upload/";
        if (!secureUrl.startsWith(prefix) || !secureUrl.contains("/v" + version + "/" + publicId)) {
            throw new InvalidUploadException("L'URL de la photo ne correspond pas à l'upload");
        }
        if (!uploadSigner.isAuthentic(publicId, version, signature)) {
            throw new InvalidUploadException("La signature de Cloudinary est invalide");
        }
        return albums.addPhoto(albumId, secureUrl, publicId);
    }

    static String folder(long userId, long albumId) {
        return "clicclap/creators/" + userId + "/albums/" + albumId;
    }

    private void requireProfile(long userId) {
        if (profiles.findByUserId(userId).isEmpty()) {
            throw new CreatorProfileNotFoundException(userId);
        }
    }

    private void requireOwned(long userId, long albumId) {
        if (!albums.isOwnedBy(albumId, userId)) {
            throw new AlbumNotFoundException(albumId);
        }
    }

    private static String validName(String name) {
        String nom = name == null ? "" : name.trim();
        if (nom.isEmpty()) {
            throw new IllegalArgumentException("Le nom de l'album est obligatoire");
        }
        if (nom.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("Le nom de l'album ne peut pas dépasser " + NAME_MAX_LENGTH + " caractères");
        }
        return nom;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidUploadException("Champ manquant : " + field);
        }
        return value;
    }
}
