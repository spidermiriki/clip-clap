package be.heh.clipclap.creators;

import java.util.List;

/** Lecture des albums d'un créateur par les autres modules (profil public de catalog). */
public interface AlbumService {

    List<Album> findAlbumsOf(long creatorId);
}
