package eu.bigbank.mugloar.config;

import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

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
}
