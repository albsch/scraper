package scraper.api.specification.impl;

import org.slf4j.Logger;
import scraper.annotations.NotNull;
import scraper.annotations.Nullable;
import scraper.api.exceptions.ValidationException;
import scraper.api.node.Address;
import scraper.api.node.GraphAddress;
import scraper.api.node.InstanceAddress;
import scraper.api.node.NodeAddress;
import scraper.api.node.container.NodeContainer;
import scraper.api.node.type.Node;
import scraper.api.service.ExecutorsService;
import scraper.api.service.FileService;
import scraper.api.service.HttpService;
import scraper.api.service.ProxyReservation;
import scraper.api.specification.ScrapeInstance;
import scraper.util.NodeUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ScrapeInstaceImpl implements ScrapeInstance {

    private @NotNull static final Logger log = org.slf4j.LoggerFactory.getLogger(ScrapeInstaceImpl.class);

    /** Name of the jobPojo */
    private @NotNull String name = "NoName";

    private GraphAddress entry;

    /** Generated nodes of the jobPojo */
    private @NotNull Map<GraphAddress, List<NodeContainer<? extends Node>>> graphs = new HashMap<>();

    /** Initial input arguments */
    private @NotNull Map<String, Object> initialArguments = new HashMap<>();

    /** Arguments applied to all nodes of given type */
    private @NotNull Map<String, Map<String, Object>> globalNodeConfigurations = new HashMap<>();

    /** Addressable instances */
    private @NotNull Map<InstanceAddress, ScrapeInstance> importedInstances = new HashMap<>();

    /**
     * Gets the next node specified by {@code o}. If o is parsable as an integer, it is used as the next stage index.
     * Otherwise, its used as a goTo label.
     *
     * @param target Stage index or goTo label
     * @return Node goTo
     * @throws RuntimeException If node goTo can not be found
     */



    @Override
    public @NotNull Map<GraphAddress, List<NodeContainer<? extends Node>>> getGraphs() {
        return graphs;
    }

    @Override
    public @NotNull List<NodeContainer<? extends Node>> getEntryGraph() {
        return getGraph(entry);
    }

    @Override
    public @NotNull List<NodeContainer<? extends Node>> getGraph(@NotNull final GraphAddress address) {
        return getGraphs().get(address);
    }



    private ExecutorsService executors;
    private HttpService httpService;
    private ProxyReservation proxyReservation;
    private FileService fileService;

    public void init() throws ValidationException {
        log.info("Initializing graphs '{}'", getName());
        for (GraphAddress k : getGraphs().keySet()) {
            for (NodeContainer node : getGraph(k)) {
                node.init(this);
            }
        }
    }


    @NotNull
    @Override
    public Map<InstanceAddress, ScrapeInstance> getImportedInstances() {
        return this.importedInstances;
    }

    public void setImportedInstances(Map<InstanceAddress, ScrapeInstance> spec) {
        this.importedInstances = spec;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public Map<String, Object> getInitialArguments() {
        return this.initialArguments;
    }

    @NotNull
    @Override
    public Map<String, Map<String, Object>> getGlobalNodeConfigurations() {
        return globalNodeConfigurations;
    }

    @NotNull
    public ExecutorsService getExecutors() {
        return this.executors;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public void setEntry(@NotNull GraphAddress entryGraph) {
        this.entry = entryGraph;
    }

    public void setInitialArguments(@NotNull Map<String, Object> initialArguments) {
        this.initialArguments = initialArguments;
    }

    public void setGlobalNodeConfigurations(@NotNull Map<String, Map<String, Object>> globalNodeConfigurations) {
        this.globalNodeConfigurations = globalNodeConfigurations;
    }

    public void setExecutors(ExecutorsService executors) {
        this.executors = executors;
    }

    @NotNull
    public HttpService getHttpService() {
        return httpService;
    }

    public void setHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    @NotNull
    public ProxyReservation getProxyReservation() {
        return proxyReservation;
    }

    public void setProxyReservation(ProxyReservation proxyReservation) {
        this.proxyReservation = proxyReservation;
    }

    @NotNull
    public FileService getFileService() {
        return fileService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    @Override @NotNull
    public Optional<NodeContainer<? extends Node>> getNodeRelative(@NotNull NodeAddress origin, @NotNull Address target) {
        return NodeUtil.getNode(target, graphs, importedInstances);
    }

    @Override @NotNull
    public Optional<NodeContainer<? extends Node>> getNodeAbsolute(@NotNull NodeAddress origin) {
        throw new IllegalStateException();
//        return NodeUtil.getNode(target, graphs, importedInstances);
    }
}
