package be.leuven.leuvengo.whatsapp;

import be.leuven.leuvengo.domain.Faculty;
import be.leuven.leuvengo.domain.WhatsAppSender;
import be.leuven.leuvengo.repository.FacultyRepository;
import be.leuven.leuvengo.repository.WhatsAppSenderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Which clan (faculty) a WhatsApp reporter earns points for. */
@Service
public class ClanMembership {

    private final WhatsAppSenderRepository senders;
    private final FacultyRepository faculties;
    private final Clock clock;

    public ClanMembership(WhatsAppSenderRepository senders, FacultyRepository faculties, Clock clock) {
        this.senders = senders;
        this.faculties = faculties;
        this.clock = clock;
    }

    /** Joins the clan with this code; empty if there is no such clan. */
    @Transactional
    public Optional<Faculty> join(String senderKey, String code) {
        Optional<Faculty> clan = faculties.findByShortCode(code.trim().toUpperCase(Locale.ROOT));
        clan.ifPresent(f -> {
            WhatsAppSender sender = senders.findBySenderKey(senderKey)
                    .orElseGet(() -> new WhatsAppSender(senderKey));
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

    @Transactional(readOnly = true)
    public List<Faculty> all() {
        return faculties.findAllByOrderByShortCodeAsc();
    }
}
