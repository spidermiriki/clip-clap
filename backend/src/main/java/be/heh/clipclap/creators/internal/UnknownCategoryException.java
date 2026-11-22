package be.heh.clipclap.creators.internal;

class UnknownCategoryException extends RuntimeException {

    UnknownCategoryException() {
        super("Au moins un des types de prestation choisis n'existe pas");
    }
}
