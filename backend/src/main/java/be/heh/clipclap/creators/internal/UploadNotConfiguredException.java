package be.heh.clipclap.creators.internal;

class UploadNotConfiguredException extends RuntimeException {

    UploadNotConfiguredException() {
        super("Upload indisponible : Cloudinary n'est pas configuré sur ce serveur");
    }
}
