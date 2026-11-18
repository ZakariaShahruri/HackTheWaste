package be.leuven.leuvengo.config;

import be.leuven.leuvengo.security.CurrentUserArgumentResolver;
import be.leuven.leuvengo.security.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LeuvenGoProperties props;
    private final CurrentUserArgumentResolver currentUserResolver;
    private final RateLimitInterceptor rateLimitInterceptor;

    public WebConfig(LeuvenGoProperties props,
                     CurrentUserArgumentResolver currentUserResolver,
                     RateLimitInterceptor rateLimitInterceptor) {
        this.props = props;
        this.currentUserResolver = currentUserResolver;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = props.cors().allowedOrigins().stream()
                .map(String::trim)
                .filter(o -> !o.isEmpty())
                .toArray(String[]::new);
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Retry-After");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserResolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/**");
    }
}
