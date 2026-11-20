package be.leuven.leuvengo.demo;

import be.leuven.leuvengo.config.LeuvenGoProperties;
import be.leuven.leuvengo.domain.Faculty;
import be.leuven.leuvengo.domain.RewardItem;
import be.leuven.leuvengo.domain.StreetSegment;
import be.leuven.leuvengo.repository.FacultyRepository;
import be.leuven.leuvengo.repository.ReportRepository;
import be.leuven.leuvengo.repository.RewardItemRepository;
import be.leuven.leuvengo.repository.StreetSegmentRepository;
import be.leuven.leuvengo.repository.WorkOrderRepository;
import be.leuven.leuvengo.security.AuthService;
import be.leuven.leuvengo.service.SegmentScorer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/**
 * Fills an empty database with the {@link DemoCity} (always: the app needs
 * clans and streets to work) and, on demo instances, a crew login and a week
 * of {@link DemoActivity}. Each part is only added when it's missing, so
 * restarts don't duplicate anything; a week that has gone stale is replayed.
 */
@Component
class DemoDataSeeder implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final FacultyRepository faculties;
    private final StreetSegmentRepository segments;
    private final RewardItemRepository rewards;
    private final ReportRepository reports;
    private final WorkOrderRepository workOrders;
    private final DemoActivitySeeder activity;
    private final DemoReset reset;
    private final AuthService auth;
    private final LeuvenGoProperties props;
    private final TransactionTemplate transaction;
    private final Clock clock;

    DemoDataSeeder(FacultyRepository faculties, StreetSegmentRepository segments, RewardItemRepository rewards,
                   ReportRepository reports, WorkOrderRepository workOrders, DemoActivitySeeder activity,
                   DemoReset reset, AuthService auth, LeuvenGoProperties props, TransactionTemplate transaction, Clock clock) {
        this.faculties = faculties;
        this.segments = segments;
        this.rewards = rewards;
        this.reports = reports;
        this.workOrders = workOrders;
        this.activity = activity;
        this.reset = reset;
        this.auth = auth;
        this.props = props;
        this.transaction = transaction;
        this.clock = clock;
    }

    /**
     * Runs once every bean exists but before the web server starts, so the
     * first request after a deploy never sees a half-seeded city.
     */
    @Override
    public void afterSingletonsInstantiated() {
        transaction.executeWithoutResult(status -> seed());
    }

    private void seed() {
        if (faculties.count() == 0) {
            seedCity();
            log.info("Seeded the demo city: {} clans, {} streets, {} rewards",
                    faculties.count(), segments.count(), rewards.count());
        }
        if (!props.demo().enabled()) return;

        auth.ensureCrewAccount(props.demo().crewEmail(), props.demo().crewPassword());
        if (reports.count() == 0) {
            activity.seed();
            log.info("Seeded a week of demo activity: {} reports, {} work orders",
                    reports.count(), workOrders.count());
        } else if (reset.isStale()) {
            reset.reset();
        }
    }

    private void seedCity() {
        Map<String, Faculty> byCode = new HashMap<>();
        for (DemoCity.Clan clan : DemoCity.CLANS) {
            byCode.put(clan.code(), faculties.save(faculty(clan)));
        }
        for (DemoCity.Street street : DemoCity.STREETS) {
            Faculty owner = DemoCity.clanAt(street.lat(), street.lng())
                    .map(clan -> byCode.get(clan.code()))
                    .orElse(null);
            segments.save(segment(street, owner));
        }
        DemoCity.REWARDS.forEach(reward -> rewards.save(reward(reward)));
    }

    private static Faculty faculty(DemoCity.Clan clan) {
        Faculty f = new Faculty();
        f.setName(clan.name());
        f.setShortCode(clan.code());
        f.setColor(clan.color());
        f.setEmoji(clan.emoji());
        f.setMembers(clan.members());
        f.setPoints(0);
        f.setTerritoryGeoJson(DemoCity.geoJson(clan.territory()));
        return f;
    }

    private StreetSegment segment(DemoCity.Street street, Faculty owner) {
        StreetSegment s = new StreetSegment();
        s.setName(street.name());
        s.setDistrict(street.district());
        s.setCenterLat(street.lat());
        s.setCenterLng(street.lng());
        s.setAiCleanlinessScore(SegmentScorer.PRIOR_SCORE);
        s.setRecentReportCount(0);
        s.setLastEvaluatedAt(clock.instant());
        s.setFaculty(owner);
        return s;
    }

    private static RewardItem reward(DemoCity.Reward reward) {
        RewardItem r = new RewardItem();
        r.setTitle(reward.title());
        r.setDescription(reward.description());
        r.setSponsor(reward.sponsor());
        r.setCategory(reward.category());
        r.setCostPoints(reward.costPoints());
        r.setStock(reward.stock());
        return r;
    }
}
