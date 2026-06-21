package com.fleetmatch;

import com.fleetmatch.common.HealthController;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FleetmatchBackendApplicationTests {

    @Test
    void healthEndpointReturnsRunningMessage() {
        HealthController controller = new HealthController();

        assertThat(controller.health())
                .isEqualTo("FleetMatch backend is running");
    }
}
