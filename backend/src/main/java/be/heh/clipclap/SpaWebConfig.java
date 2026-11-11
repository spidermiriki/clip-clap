package be.heh.clipclap;

import java.io.IOException;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Sert le build React (copié dans {@code classpath:/static} par le Dockerfile) et renvoie
 * {@code index.html} pour les routes du front ({@code /mon-profil}, {@code /callback}…), pour
 * qu'un rechargement de page ou le retour de Zitadel ne finisse pas en 404.
 * <p>
 * Placé à la racine, hors des modules métier : c'est de la configuration web transverse.
 */
@Configuration(proxyBeanMethods = false)
class SpaWebConfig implements WebMvcConfigurer {

	// Ces chemins gardent leurs vrais 404 : une API inconnue ne doit jamais renvoyer du HTML
	private static final List<String> PREFIXES_SERVEUR = List.of("api", "actuator", "swagger-ui", "v3");

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
			.addResourceLocations("classpath:/static/")
			.resourceChain(true)
			.addResolver(new SpaFallbackResolver());
	}

	/** Chemin sans « / » initial ; un dernier segment avec un point désigne un fichier. */
	static boolean estRouteDuFront(String chemin) {
		boolean cheminServeur = PREFIXES_SERVEUR.stream()
			.anyMatch(prefixe -> chemin.equals(prefixe) || chemin.startsWith(prefixe + "/"));
		String dernierSegment = chemin.substring(chemin.lastIndexOf('/') + 1);
		return !cheminServeur && !dernierSegment.contains(".");
	}

	private static final class SpaFallbackResolver extends PathResourceResolver {

		@Override
		protected Resource getResource(String resourcePath, Resource location) throws IOException {
			Resource resource = super.getResource(resourcePath, location);
			if (resource != null || !estRouteDuFront(resourcePath)) {
				return resource;
			}
			return super.getResource("index.html", location);
		}

	}

}
