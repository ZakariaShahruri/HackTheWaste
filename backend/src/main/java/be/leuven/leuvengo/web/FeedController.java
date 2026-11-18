package be.leuven.leuvengo.web;

import be.leuven.leuvengo.repository.ReportRepository;
import be.leuven.leuvengo.web.dto.Dtos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public photo feed: every report that came with a photo, newest first. */
@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private static final int MAX_PAGE_SIZE = 48;

    private final ReportRepository reports;

    public FeedController(ReportRepository reports) {
        this.reports = reports;
    }

    /** {@code ?clan=APO} narrows the feed to one clan. */
    @GetMapping
    public Page<Dtos.FeedItemDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String clan) {

        PageRequest request = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        var results = clan == null || clan.isBlank()
                ? reports.findByPhotoUrlIsNotNullOrderByReportedAtDesc(request)
                : reports.findByPhotoUrlIsNotNullAndCreditedFacultyShortCodeOrderByReportedAtDesc(clan.trim(), request);
        return results.map(Dtos::feedItem);
    }
}
