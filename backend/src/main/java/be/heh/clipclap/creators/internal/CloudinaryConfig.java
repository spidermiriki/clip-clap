package be.heh.clipclap.creators.internal;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CloudinaryProperties.class)
class CloudinaryConfig {

    @Bean
    UploadSigner uploadSigner(CloudinaryProperties properties) {
        return new CloudinaryUploadSigner(properties, Clock.systemUTC());
    }

    @Bean
    MediaStorage mediaStorage(CloudinaryProperties properties) {
        return new CloudinaryMediaStorage(properties, RestClient.builder(), Clock.systemUTC());
    }
}
