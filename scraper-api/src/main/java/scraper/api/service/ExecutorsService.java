package scraper.api.service;

import java.util.concurrent.ExecutorService;

/**
 * Service managing the execution and scheduling of flows
 *
 * @since 1.0.0
 */
public interface ExecutorsService {

    /**
     * Returns the executor service for given group.
     * If the service for the group was not instantiated yet,
     * creates a new ExecutorService with the specified limit of threads
     */
    ExecutorService getService(String group, Integer count);

    /**
     * Gets the executor service for given group.
     * If service was not instatiated yet, creates a new ExecutorService with the default limit of threads
     */
    ExecutorService getService(String group);
}
