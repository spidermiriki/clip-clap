package be.heh.clipclap.offerings.internal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
class OfferingsConfig {

    /** Heure courante du préavis (#28) : remplacée par une horloge figée dans les tests. */
    @Bean
    Clock offeringsClock() {
        return Clock.systemUTC();
    }
}
