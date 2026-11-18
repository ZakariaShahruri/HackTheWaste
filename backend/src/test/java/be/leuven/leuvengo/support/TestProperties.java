package be.leuven.leuvengo.support;

import be.leuven.leuvengo.config.LeuvenGoProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

/** Builds {@link LeuvenGoProperties} the way Spring would: defaults plus overrides. */
public final class TestProperties {

    private TestProperties() {}

    public static LeuvenGoProperties defaults() {
        return with(Map.of());
    }

    /** Keys are relative to {@code leuvengo.}, e.g. {@code "traffic.cluster-radius-m"}. */
    public static LeuvenGoProperties with(Map<String, String> overrides) {
        Map<String, String> prefixed = new java.util.HashMap<>();
        overrides.forEach((k, v) -> prefixed.put("leuvengo." + k, v));
        return new Binder(new MapConfigurationPropertySource(prefixed))
                .bindOrCreate("leuvengo", LeuvenGoProperties.class);
    }
}
