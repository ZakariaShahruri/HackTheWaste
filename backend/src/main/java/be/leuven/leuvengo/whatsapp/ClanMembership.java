package be.leuven.leuvengo.whatsapp;

import be.leuven.leuvengo.domain.Faculty;
import be.leuven.leuvengo.domain.WhatsAppSender;
import be.leuven.leuvengo.repository.FacultyRepository;
import be.leuven.leuvengo.repository.WhatsAppSenderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Which clan (faculty) a WhatsApp reporter earns points for. */
@Service
public class ClanMembership {

    /** How long after seeing the clan list a plain message counts as the pick. */
    static final Duration CHOICE_TTL = Duration.ofMinutes(30);

    /** A clan's place on the leaderboard. */
    public record Standing(Faculty clan, int points, long rank) {}

    private final WhatsAppSenderRepository senders;
    private final FacultyRepository faculties;
    private final Clock clock;

    public ClanMembership(WhatsAppSenderRepository senders, FacultyRepository faculties, Clock clock) {
        this.senders = senders;
        this.faculties = faculties;
        this.clock = clock;
    }

    /** Joins the clan with this code or name; empty if there is no such clan. */
    @Transactional
    public Optional<Faculty> join(String senderKey, String codeOrName) {
        Optional<Faculty> clan = find(codeOrName.strip());
        clan.ifPresent(f -> {
            WhatsAppSender sender = getOrCreate(senderKey);
            sender.joinClan(f.getShortCode(), clock.instant());
            senders.save(sender);
        });
        return clan;
    }

    @Transactional(readOnly = true)
    public Optional<Faculty> clanOf(String senderKey) {
        return senders.findBySenderKey(senderKey)
                .map(WhatsAppSender::getFacultyShortCode)
                .flatMap(faculties::findByShortCode);
    }

    /** Remembers that the sender is looking at the clan list. */
    @Transactional
    public void startChoosing(String senderKey) {
        WhatsAppSender sender = getOrCreate(senderKey);
        sender.startChoosing(clock.instant());
        senders.save(sender);
    }

    @Transactional
    public void stopChoosing(String senderKey) {
        senders.findBySenderKey(senderKey).ifPresent(s -> s.stopChoosing(clock.instant()));
    }

    @Transactional(readOnly = true)
    public boolean isChoosing(String senderKey) {
        Instant cutoff = clock.instant().minus(CHOICE_TTL);
        return senders.findBySenderKey(senderKey)
                .map(WhatsAppSender::getChoosingSince)
                .filter(since -> since.isAfter(cutoff))
                .isPresent();
    }

    @Transactional(readOnly = true)
    public Standing standing(Faculty clan) {
        int points = faculties.findByShortCode(clan.getShortCode()).map(Faculty::getPoints).orElse(0);
        return new Standing(clan, points, faculties.countByPointsGreaterThan(points) + 1);
    }

    @Transactional(readOnly = true)
    public List<Faculty> all() {
        return faculties.findAllByOrderByShortCodeAsc();
    }

    private Optional<Faculty> find(String codeOrName) {
        return all().stream()
                .filter(f -> f.getShortCode().equalsIgnoreCase(codeOrName) || f.getName().equalsIgnoreCase(codeOrName))
                .findFirst();
    }

    private WhatsAppSender getOrCreate(String senderKey) {
        return senders.findBySenderKey(senderKey).orElseGet(() -> new WhatsAppSender(senderKey));
    }
}
