package scraper.api.service;


import scraper.annotations.NotNull;
import scraper.annotations.Nullable;
import scraper.api.service.proxy.GroupInfo;
import scraper.api.service.proxy.ProxyMode;
import scraper.api.service.proxy.ReservationToken;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * Reservation service to provide reservations for local HTTP and proxied HTTP requests.
 *
 * @see HttpService
 * @since 1.0.0
 */
public interface ProxyReservation {

    /** Adds all proxies to given group */
    void addProxies(@NotNull Set<String> proxiesAsSet, @NotNull String proxyGroup);
    /** Adds all proxies to given group */
    void addProxies(@NotNull String proxyFile, @NotNull String proxyGroup) throws IOException;

    /** Waits until a token for given proxy mode and group is free */
    ReservationToken reserveToken(@NotNull String proxyGroup, @NotNull ProxyMode proxyMode) throws InterruptedException;
    /** Waits until a token for given proxy mode and group is free with a timeout */
    ReservationToken reserveToken(@NotNull String proxyGroup, @NotNull ProxyMode proxyMode, int timeout, int holdOnReservation)
            throws InterruptedException, TimeoutException;

    /** Retrieves proxy info for given group */
    @Nullable
    GroupInfo getInfoForGroup(@NotNull String group);



}
