package be.heh.clipclap.creators;

import java.util.List;

/** Album d'un créateur et ses médias (photos et vidéos, #94), dans l'ordre d'ajout. */
public record Album(long id, String name, List<Photo> photos) {

    /** Un média de l'album ; {@code mediaType} vaut {@code PHOTO} ou {@code VIDEO}. */
    public record Photo(long id, String url, String mediaType) {}
}
