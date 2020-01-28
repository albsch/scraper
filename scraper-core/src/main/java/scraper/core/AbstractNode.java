package scraper.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scraper.annotations.NotNull;
import scraper.annotations.Nullable;
import scraper.annotations.node.Argument;
import scraper.annotations.node.EnsureFile;
import scraper.annotations.node.FlowKey;
import scraper.annotations.node.NodePlugin;
import scraper.api.exceptions.NodeException;
import scraper.api.exceptions.ValidationException;
import scraper.api.flow.FlowMap;
import scraper.api.flow.impl.FlowStateImpl;
import scraper.api.node.*;
import scraper.api.node.impl.NodeAddressImpl;
import scraper.api.specification.ScrapeInstance;
import scraper.util.NodeUtil;
import scraper.utils.ClassUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static scraper.core.NodeLogLevel.*;


/**
 * Basic abstract implementation of a Node with labeling and goTo support.
 * <p>
 * Provides following utility functions:
 * <ul>
 *     <li>Node factory method depending on the defined type</li>
 *     <li>Node coordination</li>
 *     <li>Argument evaluation</li>
 *     <li>Key reservation</li>
 *     <li>Ensure file</li>
 *     <li>Basic ControlFlow implementation</li>
 *     <li>Thread service pool management</li>
 * </ul>
 * </p>
 */
@SuppressWarnings({"WeakerAccess", "unused"}) // abstract implementation
@NodePlugin("1.0.1")
public abstract class AbstractNode implements Node, NodeInitializable {
    /** Logger with the actual class name */
    protected Logger l = LoggerFactory.getLogger(getClass());

    /** Node type. Is used once to create an instance of an actual node implementation. */
    @FlowKey(mandatory = true)
    protected String type;

    /** Decide log level threshold for this node */
    @FlowKey(defaultValue = "\"INFO\"")
    protected NodeLogLevel logLevel;

    /** Log statement to be printed */
    @FlowKey
    protected Template<Object> log = new Template<>(){};

    /** Label of a node which can be used as a goto reference */
    @FlowKey
    protected NodeAddress address = new NodeAddressImpl();

    /** Indicates if forward has any effect or not. */
    @FlowKey(defaultValue = "true")
    protected Boolean forward;

    /** Target label */
    @FlowKey
    protected Address goTo;

    /** Reference to its parent job */
    @JsonIgnore
    protected ScrapeInstance jobPojo;

    /** Index of the node in the process list. Is set on init. */
    protected int stageIndex;

    /** If set, returns a thread pool with given name and {@link #threads} */
    @FlowKey(defaultValue = "\"main\"")
    protected String service;
    /** Number of worker threads for given executor service pool {@link #service} */
    @FlowKey(defaultValue = "25") @Argument
    protected Integer threads;

    /** All ensureFile fields of this node */
    private final ConcurrentMap<Field, EnsureFile> ensureFileFields = new ConcurrentHashMap<>();

    /** Current node configuration */
    @JsonIgnore
    protected Map<String, Object> nodeConfiguration;

    /** Set during init of node */
    @JsonIgnore
    private GraphAddress graphKey;

    /** Target if a dispatched flow exception occurs */
    @FlowKey
    protected Address onForkException;

    /**
     * Initializes the {@link #stageIndex} and all fields marked with {@link FlowKey}. Evaluates
     * actual values for fields marked with {@link Argument} with the initial argument map.
     *
     * @param job Job that this node belongs to
     * @throws ValidationException If a JSON parse error or a reflection error occurs
     */
    @Override
    public void init(@NotNull final ScrapeInstance job) throws ValidationException {
//        Runtime.getRuntime().addShutdownHook(new Thread(this::nodeShutdown));

        // set stage indices
        this.jobPojo = job;
        job.getGraphs().forEach((k, graph) -> {
            for (int i = 0; i < graph.size(); i++) {
                if(job.getGraph(k).get(i) == this) {
                    this.stageIndex = i;
                    address = new NodeAddressImpl(address.getLabel(), stageIndex);
                    break;
                }
            }
        });

        // set logger name
        String number = String.valueOf(job.getGraph(getGraphKey()).size());
        int indexLength = number.toCharArray().length;
        initLogger(indexLength);
        log(TRACE,"Start init {}", this);

        // initialize fields with arguments
        Set<String> expectedFields = initFields(getNodeConfiguration(), job.getInitialArguments());

        // check actual fields against expected fields
        for (String actualField : getNodeConfiguration().keySet()) {
            if (!expectedFields.contains(actualField)) {
                log(WARN,"Found field defined in flow, but not expected in implementation of node: {}", actualField);
            }
        }

        log(TRACE,"Finished init {}", this);
    }

    private Set<String> initFields(Map<String, Object> spec, Map<String,Object> initialArguments) throws ValidationException {
        // collect expected fields to check against
        Set<String> expectedFields = new HashSet<>();

        try { // ensure templated arguments
            List<Field> allFields = ClassUtil.getAllFields(new LinkedList<>(), getClass());

            for (Field field : allFields) {
                log(TRACE,"Initializing field {} of {}", field.getName(), this);

                FlowKey flowKey = field.getAnnotation(FlowKey.class);
                Argument ann = field.getAnnotation(Argument.class);

                if (flowKey != null) {
                    EnsureFile ensureFile = field.getAnnotation(EnsureFile.class);
                    if(ensureFile != null) ensureFileFields.put(field, ensureFile);

                    // save name for actual<->expected field comparison
                    expectedFields.add(field.getName());

                    // initialize field
                    initField(field, flowKey, ann, spec, initialArguments);
                }

            }


            log(TRACE,"Finished initializing fields for {}", this);
        }
        catch (IllegalAccessException e) {
            throw new ValidationException("Reflection not implemented correctly", e);
        }

        return expectedFields;
    }

    @NotNull
    public Map<String, Object> getNodeConfiguration() {
        return nodeConfiguration;
    }

    public void setNodeConfiguration(@NotNull Map<String, Object> configuration, @NotNull GraphAddress graphKey) {
        nodeConfiguration = configuration;
        this.graphKey = graphKey;
    }

    public void initLogger(int indexLength) {
        String loggerName =
                String.format("%s > %s%"+indexLength+"s | %s",
                        getJobPojo().getName(),
                        getAddress().getLabel() + " @ ",
                        getAddress().getIndex(),
                        getClass().getSimpleName().substring(0, getClass().getSimpleName().length()-4));

        l = LoggerFactory.getLogger(loggerName);
    }

    protected ExecutorService getService() {
        return getJobPojo().getExecutors().getService(getJobPojo().getName(),service, threads);
    }

    /**
     * Initializes a field with its actual value. If it is a template, its value is evaluated with the given map.
     * @param field Field of the node to initialize
     * @param flowKey indicates optional value
     * @param ann Indicates a template field
     * @param args The input map
     * @throws ValidationException If there is a class mismatch between JSON and node definition
     * @throws IllegalAccessException If reflection is implemented incorrectly
     */
    private void initField(Field field,
                           FlowKey flowKey, Argument ann,
                           Map<String, Object> spec,
                           Map<String, Object> args)
            throws ValidationException, IllegalAccessException {
        // enable reflective access
        field.setAccessible(true);

        // this is the value which will get assigned to the field after evaluation
        Object value;
        Map<String, Object> allFields = null;
        Object jsonValue = spec.get(field.getName());
        Object globalValue = null;

        // TODO why is second condition always true?
        if(jobPojo != null && jobPojo.getGlobalNodeConfigurations() != null) {
            jobPojo.getGlobalNodeConfigurations();
            String nodeName = getClass().getSimpleName();

            //check if regex matches, and apply if valid
            for (String maybeRegex : jobPojo.getGlobalNodeConfigurations().keySet()) {
                if (maybeRegex.startsWith("/") && maybeRegex.endsWith("/")) {
                    String regex = maybeRegex.substring(1, maybeRegex.length() - 1);

                    boolean result = Pattern.compile(regex).matcher(nodeName).results()
                            .findAny().isPresent();

                    if (result) allFields = jobPojo.getGlobalNodeConfigurations().get(maybeRegex);

                    // fetch global value, if any
                    if (allFields != null) {
                        Object globalKey = allFields.get(field.getName());
                        if (globalKey != null) {
                            globalValue = globalKey;
                        }
                    }
                }
            }

            allFields = jobPojo.getGlobalNodeConfigurations().get(nodeName);

            // fetch global value, if any
            if (allFields != null) {
                Object globalKey = allFields.get(field.getName());
                if (globalKey != null) {
                    globalValue = globalKey;
                }
            }
        }

        try {
            value = NodeUtil.getValueForField(
                    field.getType(), field.get(this), jsonValue, globalValue,
                    flowKey.mandatory(), flowKey.defaultValue(), flowKey.output(),
                    ann != null, (ann != null ? ann.converter() : null),
                    args);
        } catch (ValidationException e){
            log(ERROR, "Bad field definition: '{}'", field.getName());
            throw e;
        }

        if(value != null) field.set(this, value);
    }

    /**
     * <ul>
     *     <li>Evaluates templates</li>
     *     <li>Ensures that each file described by an {@link EnsureFile} field exists</li>
     * </ul>
     *
     * @param map The current forwarded map
     */
    protected void start(FlowMap map) throws NodeException {
        // update FlowState
        updateFlowInfo(map, this, "start");

        // evaluate and write log message if any
        try {
            Object logString = log.eval(map);
            if(logString != null) log(logLevel, logString.toString());
        } catch (Exception e) {
            log(ERROR, "Could not evaluate log template: {}", e.getMessage());
        }

        // ensure files exist
        try {
            for (Field ensureFileField : ensureFileFields.keySet()) {
                ensureFileField.setAccessible(true);

                String path;
                if(Template.class.isAssignableFrom(ensureFileField.getType())) {
                    Template<?> templ = (Template) ensureFileField.get(this);
                    path = (String) templ.eval(map);
                } else {
                    path = (String) ensureFileField.get(this);
                }

                if (path == null) continue; //TODO check if optional // TODO what does the TODO mean

                log(TRACE,"Ensure file of field {} at {}", ensureFileField.getName(), path);
                if(ensureFileFields.get(ensureFileField).ensureDir())
                    getJobPojo().getFileService().ensureDirectory(new File(path));

                if(path.endsWith(File.separator)) {
                    getJobPojo().getFileService().ensureDirectory(new File(path+"."));
                } else {
                    getJobPojo().getFileService().ensureFile(path);
                }
            }

        } catch (IllegalAccessException e) {
            throw new RuntimeException("Implemented reflection badly: ", e);
        } catch (IOException e) {
            log(ERROR,"Failed ensure file: {}", e.getMessage());
            throw new NodeException("Failed ensuring directory or file: " + e.getMessage());
        }
    }

    /**
     * @param o The current map
     */
    protected void finish(final FlowMap o) {
        updateFlowInfo(o, this, "finish");
    }

    private void updateFlowInfo(FlowMap map, AbstractNode abstractNode, String phase) {
        if(logLevel.worseOrEqual(WARN)) {
            // only history of start is tracked
            if(phase.equalsIgnoreCase("start")) return;
        }

        FlowStateImpl state = new FlowStateImpl(phase);
        state.log("address", getAddress());
        state.log("graph", getGraphKey());

        if(logLevel.worseOrEqual(INFO)) {
            state.log("keys", new HashSet<>(map.keySet()));
        }

        if(logLevel.worseOrEqual(DEBUG)) {
            Map<String, Object> keyValues = new HashMap<>();

            for (String key : map.keySet()) {
                if(map.get(key) instanceof String) {
                    // trim string
                    keyValues.put(key, ((String) Objects.requireNonNull(map.get(key)))
                            .substring(0, Math.min(100, ((String) Objects.requireNonNull(map.get(key))).length())));
                } else {
                    // else just put class name
                    keyValues.put(key, Objects.requireNonNull(map.get(key)).getClass().getName());
                }
            }
            state.log("key-values", keyValues);
        }

        // TODO implement TRACE deep copy keys
    }

    /** Dispatches an action in an own thread, ignoring the result and possible exceptions. */
    protected CompletableFuture<FlowMap> dispatch(Supplier<FlowMap> o) {
        return CompletableFuture.supplyAsync(o, getService());
    }


    protected void log(NodeLogLevel debug, String msg, Object... args) {
        log(l, logLevel, debug, msg, args);
    }

    // ----------------------------
    // GETTER AND UTILITY FUNCTIONS
    // ----------------------------

    @Override public String toString(){ return "<"+getAddress().getLabel()+"@"+getStageIndex()+">"; }

    // node shutdown
//    @Override
//    public void nodeShutdown() {
//        log(NodeLogLevel.DEBUG, "{}@{} stopped gracefully", getClass().getSimpleName(), stageIndex);
//    }

    public Logger getL() {
        return l;
    }

    public static void log(Logger log, NodeLogLevel threshold, NodeLogLevel level, String msg, Object... args) {
        switch (level){
            case TRACE:
                // only l if trace
                if(threshold != TRACE) return;
                log.info(msg, args);
                return;
            case DEBUG:
                // only l if level is debug or higher
                if(threshold == TRACE || threshold == NodeLogLevel.DEBUG) log.debug(msg, args);
                return;
            case INFO:
                if(threshold == WARN || threshold == ERROR) return;
                log.info(msg, args);
                return;
            case WARN:
                if(threshold == ERROR) return;
                log.warn(msg, args);
                return;
            case ERROR:
                log.error(msg, args);
        }
    }


    @NotNull
    @Override
    public FlowMap forward(@NotNull final FlowMap o) throws NodeException {
        // do nothing
        if(!getForward()) return o;
        if(getGoTo() != null) {
            return jobPojo.getNode(getGoTo()).accept(o);
        }

        return o;
    }

    @NotNull
    @Override
    public FlowMap eval(@NotNull final FlowMap o, @NotNull final Address target) throws NodeException {
        return jobPojo.getNode(target).accept(o);
    }

    @Override
    public void forkDispatch(@NotNull final FlowMap o, @NotNull final Address target) {
        dispatch(() -> {
            try {
                return eval(o, target);
            } catch (Exception e) {
                log(ERROR, "Dispatch terminated exceptionally {}: {}", target, e);
                if(onForkException != null) {
                    try {
                        return eval(o, onForkException);
                    } catch (NodeException ex) {
                        log(ERROR, "OnException fork target terminated exceptionally.", target, e);
                        throw new RuntimeException(e);
                    }
                } else {
                    log(ERROR, "Fork dispatch to goTo '{}' terminated exceptionally.", target, e);
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @NotNull
    @Override
    public CompletableFuture<FlowMap> forkDepend(@NotNull final FlowMap o, @NotNull final Address target) {
        return dispatch(() -> {
            try {
                return eval(o, target);
            } catch (Exception e) {
                log(ERROR, "Fork depend to goTo '{}' terminated exceptionally.", target, e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Object getKeySpec(@NotNull final String argument) {
        return nodeConfiguration.get(argument);
    }

    public String getType() {
        return this.type;
    }

    public NodeLogLevel getLogLevel() {
        return this.logLevel;
    }

    @NotNull
    @Override
    public NodeAddress getAddress() {
        return address;
    }

    public Boolean getForward() {
        return this.forward;
    }

    public ScrapeInstance getJobPojo() {
        if(jobPojo == null) throw new IllegalStateException("Node is not associated to a job");
        return this.jobPojo;
    }

    public int getStageIndex() {
        return this.stageIndex;
    }

    @Override
    public @Nullable Address getGoTo() {
        return NodeUtil.getNextNode(getAddress(), goTo, getJobPojo().getGraphs());
    }

    @NotNull
    @Override
    public GraphAddress getGraphKey() {
        return this.graphKey;
    }

    @NotNull
    @Override
    public Collection<NodeHook> beforeHooks() {
        return Set.of(this::start);
    }

    @NotNull
    @Override
    public Collection<NodeHook> afterHooks() {
        return Set.of(this::finish);
    }

    @Override
    public boolean isForward() {
        return forward;
    }
}
