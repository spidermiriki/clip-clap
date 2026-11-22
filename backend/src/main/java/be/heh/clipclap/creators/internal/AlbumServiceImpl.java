package be.heh.clipclap.creators.internal;

import be.heh.clipclap.creators.Album;
import be.heh.clipclap.creators.Album.Photo;
import be.heh.clipclap.creators.AlbumService;
import be.heh.clipclap.creators.CreatorProfileNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
class AlbumServiceImpl implements AlbumService {

    static final int NAME_MAX_LENGTH = 100;

    private final AlbumRepository albums;
    private final CreatorProfileRepository profiles;
    private final UploadSigner uploadSigner;
    private final MediaStorage mediaStorage;

    AlbumServiceImpl(AlbumRepository albums, CreatorProfileRepository profiles, UploadSigner uploadSigner,
                     MediaStorage mediaStorage) {
        this.albums = albums;
        this.profiles = profiles;
        this.uploadSigner = uploadSigner;
        this.mediaStorage = mediaStorage;
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

    /** Étape 1 : signature d'un upload limité au dossier de cet album et aux formats du type de média (#94). */
    UploadSignature signUpload(long userId, long albumId, String type) {
        if (!uploadSigner.isConfigured()) {
            throw new UploadNotConfiguredException();
        }
        requireOwned(userId, albumId);
        return uploadSigner.sign(folder(userId, albumId), mediaType(type));
    }

    /** Étape 3 : enregistrement d'une photo ou d'une vidéo, seulement si la réponse de Cloudinary est authentique et cohérente. */
    Photo addPhoto(long userId, long albumId, AddPhotoCommand command) {
        if (!uploadSigner.isConfigured()) {
            throw new UploadNotConfiguredException();
        }
        requireOwned(userId, albumId);

        String type = mediaType(command.mediaType());
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
        // Une vidéo n'est servie que sous /video/upload/, une photo sous /image/upload/ : un type
        // annoncé qui ne correspond pas à l'URL renvoyée par Cloudinary est refusé (#94)
        String resourceType = "VIDEO".equals(type) ? "video" : "image";
        String prefix = "https://res.cloudinary.com/" + uploadSigner.cloudName() + "/" + resourceType + "/upload/";
        if (!secureUrl.startsWith(prefix) || !secureUrl.contains("/v" + version + "/" + publicId)) {
            throw new InvalidUploadException("L'URL du média ne correspond pas à l'upload");
        }
        if (!uploadSigner.isAuthentic(publicId, version, signature)) {
            throw new InvalidUploadException("La signature de Cloudinary est invalide");
        }
        return albums.addPhoto(albumId, secureUrl, publicId, type);
    }

    // ── Suppression (#94) ───────────────────────────────────────────────────
    // Cloudinary d'abord, la base ensuite : si Cloudinary échoue, rien n'est retiré de la base.
    // Pas de transaction englobante : un album à moitié supprimé garde ses médias restants,
    // et il suffit de relancer la suppression pour la terminer.

    void deletePhoto(long userId, long albumId, long mediaId) {
        requireOwned(userId, albumId);
        var media = albums.findMedia(albumId, mediaId).orElseThrow(() -> new MediaNotFoundException(mediaId));
        removeFromStorage(userId, albumId, media);
        albums.deleteMedia(media.id());
    }

    void deleteAlbum(long userId, long albumId) {
        requireOwned(userId, albumId);
        var medias = albums.findMedia(albumId);
        int supprimes = 0;
        for (var media : medias) {
            try {
                removeFromStorage(userId, albumId, media);
            } catch (StorageUnavailableException e) {
                throw StorageUnavailableException.partielle(supprimes, medias.size(), e);
            }
            albums.deleteMedia(media.id());
            supprimes++;
        }
        albums.deleteAlbum(albumId);
    }

    /**
     * Seuls les fichiers envoyés dans le dossier de l'album sont supprimés chez Cloudinary : les
     * images d'exemple des données de démonstration ({@code samples/...}) ne sont jamais touchées.
     */
    private void removeFromStorage(long userId, long albumId, AlbumRepository.StoredMedia media) {
        if (media.publicId().startsWith(folder(userId, albumId) + "/")) {
            mediaStorage.delete(media.publicId(), media.mediaType());
        }
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

    /** {@code PHOTO} par défaut ; tout autre type que {@code PHOTO} ou {@code VIDEO} est refusé (400). */
    private static String mediaType(String type) {
        if (type == null || type.isBlank()) {
            return "PHOTO";
        }
        String valeur = type.trim().toUpperCase(Locale.ROOT);
        if (!valeur.equals("PHOTO") && !valeur.equals("VIDEO")) {
            throw new IllegalArgumentException("Le type de média doit valoir PHOTO ou VIDEO");
        }
        return valeur;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidUploadException("Champ manquant : " + field);
        }
        return value;
    }
}
