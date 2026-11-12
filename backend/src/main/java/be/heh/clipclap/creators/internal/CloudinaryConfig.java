package be.heh.clipclap.creators.internal;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CloudinaryProperties.class)
class CloudinaryConfig {

    @Bean
    UploadSigner uploadSigner(CloudinaryProperties properties) {
        return new CloudinaryUploadSigner(properties, Clock.systemUTC());
    }
}
