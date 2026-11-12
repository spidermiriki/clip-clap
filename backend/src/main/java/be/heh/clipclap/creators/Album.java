package be.heh.clipclap.creators;

import java.util.List;

/** Album d'un créateur et ses photos, dans l'ordre d'ajout. */
public record Album(long id, String name, List<Photo> photos) {

    public record Photo(long id, String url) {}
}
