package eu.bigbank.mugloar.config;

import eu.bigbank.mugloar.strategy.AdEvaluator;
import eu.bigbank.mugloar.strategy.ShopPolicy;
import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(MugloarProperties.class)
public class AppConfig {

    @Bean
    RestClient mugloarRestClient(MugloarProperties props) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(props.api().connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(props.api().readTimeout());
        return RestClient.builder()
                .baseUrl(props.api().baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    AdEvaluator adEvaluator() {
        return new AdEvaluator();
    }

    @Bean
    ShopPolicy shopPolicy() {
        return new ShopPolicy();
    }

    @Bean
    WebMvcConfigurer corsConfigurer(MugloarProperties props) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(props.web().allowedOrigins().toArray(String[]::new))
                        .allowedMethods("GET", "POST");
            }
        };
    }
}
