package be.heh.clipclap.offerings.internal;

class UnknownCategoryException extends RuntimeException {

    UnknownCategoryException() {
        super("La catégorie choisie pour la prestation n'existe pas");
    }
}
