package be.heh.clipclap.creators.internal;

import be.heh.clipclap.creators.Album;
import be.heh.clipclap.creators.Album.Photo;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
class AlbumRepository {

    private final JdbcClient jdbc;

    AlbumRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    long create(long creatorId, String name) {
        return jdbc.sql("INSERT INTO albums (creator_id, name) VALUES (:creatorId, :name) RETURNING id")
                .param("creatorId", creatorId)
                .param("name", name)
                .query(Long.class)
                .single();
    }

    /** Faux si l'album n'existe pas ou appartient à un autre créateur. */
    boolean rename(long albumId, long creatorId, String name) {
        return jdbc.sql("UPDATE albums SET name = :name WHERE id = :albumId AND creator_id = :creatorId")
                .param("name", name)
                .param("albumId", albumId)
                .param("creatorId", creatorId)
                .update() == 1;
    }

    boolean isOwnedBy(long albumId, long creatorId) {
        return jdbc.sql("SELECT COUNT(*) FROM albums WHERE id = :albumId AND creator_id = :creatorId")
                .param("albumId", albumId)
                .param("creatorId", creatorId)
                .query(Long.class)
                .single() == 1;
    }

    Photo addPhoto(long albumId, String url, String publicId, String mediaType) {
        return jdbc.sql("""
                INSERT INTO media (album_id, url, cloudinary_public_id, media_type)
                VALUES (:albumId, :url, :publicId, :mediaType)
                RETURNING id, url, media_type
                """)
                .param("albumId", albumId)
                .param("url", url)
                .param("publicId", publicId)
                .param("mediaType", mediaType)
                .query(Photo.class)
                .single();
    }

    /** Média tel que stocké : de quoi le supprimer aussi chez Cloudinary (#94). */
    record StoredMedia(long id, String publicId, String mediaType) {}

    Optional<StoredMedia> findMedia(long albumId, long mediaId) {
        return jdbc.sql("""
                SELECT id, cloudinary_public_id AS public_id, media_type FROM media
                WHERE id = :mediaId AND album_id = :albumId
                """)
                .param("mediaId", mediaId)
                .param("albumId", albumId)
                .query(StoredMedia.class)
                .optional();
    }

    List<StoredMedia> findMedia(long albumId) {
        return jdbc.sql("""
                SELECT id, cloudinary_public_id AS public_id, media_type FROM media
                WHERE album_id = :albumId
                ORDER BY id
                """)
                .param("albumId", albumId)
                .query(StoredMedia.class)
                .list();
    }

    void deleteMedia(long mediaId) {
        jdbc.sql("DELETE FROM media WHERE id = :mediaId").param("mediaId", mediaId).update();
    }

    void deleteAlbum(long albumId) {
        jdbc.sql("DELETE FROM albums WHERE id = :albumId").param("albumId", albumId).update();
    }

    /** Albums du créateur (du plus ancien au plus récent) avec leurs photos, en deux requêtes. */
    List<Album> findByCreator(long creatorId) {
        Map<Long, String> names = new LinkedHashMap<>();
        jdbc.sql("SELECT id, name FROM albums WHERE creator_id = :creatorId ORDER BY created_at, id")
                .param("creatorId", creatorId)
                .query((rs, rowNum) -> names.put(rs.getLong("id"), rs.getString("name")))
                .list();
        if (names.isEmpty()) {
            return List.of();
        }

        Map<Long, List<Photo>> photos = new LinkedHashMap<>();
        names.keySet().forEach(id -> photos.put(id, new ArrayList<>()));
        jdbc.sql("""
                SELECT id, album_id, url, media_type FROM media
                WHERE album_id IN (:albumIds)
                ORDER BY created_at, id
                """)
                .param("albumIds", names.keySet())
                .query((rs, rowNum) -> photos.get(rs.getLong("album_id")).add(new Photo(rs.getLong("id"), rs.getString("url"), rs.getString("media_type"))))
                .list();

        return names.entrySet().stream()
                .map(e -> new Album(e.getKey(), e.getValue(), List.copyOf(photos.get(e.getKey()))))
                .toList();
    }
}
