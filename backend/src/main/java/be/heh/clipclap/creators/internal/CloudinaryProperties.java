package be.heh.clipclap.creators.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties("clicclap.cloudinary")
record CloudinaryProperties(String cloudName, String apiKey, String apiSecret) {

    boolean isConfigured() {
        return StringUtils.hasText(cloudName) && StringUtils.hasText(apiKey) && StringUtils.hasText(apiSecret);
    }
}
