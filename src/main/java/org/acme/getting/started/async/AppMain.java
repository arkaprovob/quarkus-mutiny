package org.acme.getting.started.async;

import io.quarkus.runtime.QuarkusApplication;
import org.acme.getting.started.async.mockio.MockPickyWebService;

public class AppMain implements QuarkusApplication {
    @Override
    public int run(String... args) throws Exception {
        MockPickyWebService.startPickyService();
        return 0;
    }
}
