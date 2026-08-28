package com.portfolio.warehouse.work.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.portfolio.warehouse.location.domain.Location;
import com.portfolio.warehouse.location.repository.LocationRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AreaProgressServiceTest {

    @Test
    void progressUsesWholeAreaPositionNotAssignmentRelativePercent() {
        LocationRepository repository = mock(LocationRepository.class);
        AreaProgressService service = new AreaProgressService(repository);

        Location area = new Location(null, "A01", "A01", 1);
        List<Location> leaves = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            String segment = String.format("%02d", i);
            leaves.add(
                new Location(
                    area,
                    segment,
                    "A01-" + segment,
                    2
                )
            );
        }

        when(repository.findAllByFullCodeStartingWithOrderByFullCodeAsc("A01"))
            .thenReturn(leaves);

        AreaProgressService.ProgressSnapshot snapshot =
            service.snapshot(
                area,
                leaves.get(2),
                leaves.get(4)
            );

        assertThat(snapshot.startPercent()).isEqualTo(20);
        assertThat(snapshot.currentPercent()).isEqualTo(50);
        assertThat(snapshot.workedFraction()).isEqualTo(0.3);
        assertThat(snapshot.remainingFraction()).isEqualTo(0.5);
    }
}
