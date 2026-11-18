package be.leuven.leuvengo;

import be.leuven.leuvengo.repository.FacultyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;

import static org.assertj.core.api.Assertions.assertThat;

/** The first request after a (re)deploy must already see the seeded city, not an empty one. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(StartupOrderIT.ServerStartProbe.class)
class StartupOrderIT {

    @Autowired ServerStartProbe probe;

    @Test
    void demoDataIsSeededBeforeTheServerAcceptsRequests() {
        assertThat(probe.facultiesWhenServerStarted).isPositive();
    }

    @TestConfiguration
    static class ServerStartProbe {

        private final FacultyRepository faculties;
        long facultiesWhenServerStarted = -1;

        ServerStartProbe(FacultyRepository faculties) {
            this.faculties = faculties;
        }

        @EventListener
        void onServerStarted(WebServerInitializedEvent event) {
            facultiesWhenServerStarted = faculties.count();
        }
    }
}
