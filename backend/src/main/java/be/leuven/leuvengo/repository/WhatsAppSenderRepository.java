package be.leuven.leuvengo.repository;

import be.leuven.leuvengo.domain.WhatsAppSender;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WhatsAppSenderRepository extends JpaRepository<WhatsAppSender, Long> {

    Optional<WhatsAppSender> findBySenderKey(String senderKey);
}
