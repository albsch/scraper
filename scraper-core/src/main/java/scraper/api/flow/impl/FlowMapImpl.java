package scraper.api.flow.impl;

import com.google.common.reflect.TypeToken;
import org.slf4j.Logger;
import scraper.annotations.NotNull;
import scraper.annotations.Nullable;
import scraper.api.exceptions.TemplateException;
import scraper.api.flow.FlowMap;
import scraper.api.reflect.T;
import scraper.core.IdentityEvaluator;
import scraper.core.Template;
import scraper.utils.IdentityFlowMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static scraper.util.TemplateUtil.listOf;
import static scraper.util.TemplateUtil.mapOf;

public class FlowMapImpl extends IdentityEvaluator implements FlowMap {

    private @NotNull final ConcurrentMap<String, Object> privateMap;
    private @NotNull final ConcurrentMap<String, TypeToken<?>> privateTypeMap;
    private UUID parentId;
    private Integer parentSequence;
    private int sequence = 0;
    private UUID uuid;

    public FlowMapImpl(
            @NotNull ConcurrentMap<String, Object> privateMap,
            @NotNull ConcurrentMap<String, TypeToken<?>> privateTokenMap,
            UUID parentId,
            UUID uuid,
            Integer parentSequence,
            int sequence
    ) {
        this.privateMap = privateMap;
        this.privateTypeMap = privateTokenMap;
        this.parentId = parentId;
        this.parentSequence = parentSequence;
        this.uuid = uuid;
        this.sequence = sequence;
    }

    public FlowMapImpl(@NotNull UUID parentId, Integer parentSequence) {
        privateMap = new ConcurrentHashMap<>();
        privateTypeMap = new ConcurrentHashMap<>();
        this.parentId = parentId;
        this.parentSequence = parentSequence;
        uuid = UUID.randomUUID();
    }

    public FlowMapImpl() {
        privateMap = new ConcurrentHashMap<>();
        privateTypeMap = new ConcurrentHashMap<>();
        parentId = null;
        parentSequence = null;
        uuid = UUID.randomUUID();
    }


    public static FlowMap origin() {
        return new FlowMapImpl();
    }

    public static FlowMap origin(Map<String, Object> args) {
        FlowMap o = new FlowMapImpl();
        args.forEach(o::output);
        return o;
    }

    @NotNull @Override
    public void remove(@NotNull String location) {
        privateMap.remove(location);
        privateTypeMap.remove(location);
    }

    @NotNull
    @Override
    public Optional<Object> get(@NotNull String expected) {
        return Optional.ofNullable(privateMap.get(expected));
    }

    @Override
    public int size() {
        return privateMap.size();
    }

    @Override
    public void clear() {
        privateMap.clear();
        privateTypeMap.clear();
    }

    @Override
    public @NotNull Set<String> keySet() {
        return privateMap.keySet();
    }

    @Override
    public @NotNull String toString() {
        return "[Map: "+privateMap.toString()+"]";
    }


    public @NotNull ConcurrentMap<String, Object> getMap() {
        return new ConcurrentHashMap<>(privateMap);
    }

    public boolean containsElements(@NotNull final FlowMap expectedOutput) {
        for (String key : expectedOutput.keySet()) {
            Object thisElement = privateMap.get(key);
            Object thatElement = ((FlowMapImpl) expectedOutput).privateMap.get(key);
            if(thisElement != null && thatElement != null) {
                if(!compareElement(thisElement, thatElement)) return false;
            } else if(thisElement == null && thatElement != null) return false;
        }

        return true;
    }

    @Override
    public Optional<UUID> getParentId() {
        return Optional.ofNullable(parentId);
    }

    @Override
    public Optional<Integer> getParentSequence() {
        return Optional.ofNullable(parentSequence);
    }

    @Override
    public int getSequence() {
        return sequence;
    }

    @Override
    public void nextSequence() {
        sequence++;
    }

    @NotNull
    @Override public UUID getId() { return uuid; }

    @NotNull @Override
    public <A> A eval(@NotNull T<A> template) {
        A evaluated = Template.eval(template, this);
        if(evaluated == null)
            throw new TemplateException("Template was evaluated to null but not expected, " +
                    "either wrong node implementation/usage or type safety violated");
        return evaluated;
    }

    @NotNull @Override
    public <A> A evalOrDefault(@NotNull T<A> template, @NotNull A object) {
        A eval = Template.eval(template, this);
        if(eval == null) return object;
        return eval;
    }

    @NotNull @Override
    public <A> Optional<A> evalMaybe(@NotNull T<A> template) {
        return Optional.ofNullable(Template.eval(template, this));
    }


    @NotNull @Override
    public <A> A evalIdentity(@NotNull T<A> t) {
        A evaluated = Template.eval(t, new IdentityFlowMap());
        if(evaluated == null)
            throw new TemplateException("Template was evaluated to null but not expected, " +
                    "either wrong node implementation/usage or type safety violated");
        return evaluated;
    }

    @NotNull @Override
    public <A> Optional<A> evalIdentityMaybe(@NotNull T<A> template) {
        return Optional.ofNullable(Template.eval(template, new IdentityFlowMap()));
    }

    @Override
    public <A> void output(@NotNull T<A> locationAndType, A object) {
        // for now output templates are only strings
        String json = locationAndType.getRawJson();
        privateMap.put(json, object);
        privateTypeMap.put(json, TypeToken.of(locationAndType.get()));
    }

    @Override
    public void output(String location, Object outputObject) {
        try {
            TypeToken<?> inferredType = inferType(outputObject);
            log.info("Inferred type for {}: {}", location, inferredType);
            privateTypeMap.put(location, inferredType);
            privateMap.put(location, outputObject);
        } catch (Exception e) {
            log.error("Could not infer type for key '{}' and actual object '{}': {}", location, outputObject, e.getMessage());
            throw new TemplateException(e, "Could not infer type for key '"+location+"' and actual object '"+outputObject+"': "+ e.getMessage());
        }
    }


    @Override
    public FlowMap copy() {
        return copy(this);
    }

    @Override
    public FlowMap newFlow() {
        return new FlowMapImpl(privateMap, privateTypeMap, uuid, UUID.randomUUID(), sequence, 0);
    }

    private boolean descendMap(@NotNull final Map<?,?> currentMap, @NotNull final Map<?,?> otherMap) {
        for (Object s : otherMap.keySet()) {
            Object otherElement = otherMap.get(s);
            if(otherElement == null) return false;

            Object thisElement = currentMap.get(s);

            if(!compareElement(thisElement, otherElement)) {
                return false;
            }
        }

        return true;
    }

    private boolean compareElement(@Nullable final Object thisElement, @Nullable final Object otherElement) {
        if(thisElement == null || otherElement == null) return false;

        if(Map.class.isAssignableFrom(thisElement.getClass()) && Map.class.isAssignableFrom(otherElement.getClass())) {
            return descendMap((Map<?, ?>) thisElement, (Map<?, ?>) otherElement);
        } else
        if(Collection.class.isAssignableFrom(thisElement.getClass()) && Collection.class.isAssignableFrom(otherElement.getClass())) {
            return descendCollection((Collection<?>) thisElement, (Collection<?>) otherElement);
        } else {
            return thisElement.equals(otherElement);
        }

    }

    private boolean descendCollection(@NotNull final Collection<?> currentCollection, @NotNull final Collection<?> otherCollection) {
        // if empty, current collection should also be empty
        if(otherCollection.isEmpty() && !currentCollection.isEmpty()) return false;

        for (Object otherObject : otherCollection) {
            boolean foundAtLeastOneMatch = false;

            for (Object currentObject : currentCollection) {
                foundAtLeastOneMatch = compareElement(currentObject, otherObject);
                if(foundAtLeastOneMatch) break;
            }

            if (!foundAtLeastOneMatch) return false;

        }

        return true;
    }

//    public static synchronized @NotNull FlowMapImpl of(final @NotNull Map<String, Object> initialArguments) {
//        return new FlowMapImpl(new ConcurrentHashMap<>(Objects.requireNonNullElseGet(initialArguments, Map::of)));
//    }

    public static synchronized @NotNull FlowMapImpl copy(final @NotNull FlowMap o) {
        return new FlowMapImpl(
                new ConcurrentHashMap<>(((FlowMapImpl) o).privateMap),
                new ConcurrentHashMap<>(((FlowMapImpl) o).privateTypeMap),
                ((FlowMapImpl) o).parentId,
                ((FlowMapImpl) o).uuid,
                ((FlowMapImpl) o).parentSequence,
                ((FlowMapImpl) o).sequence
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlowMapImpl flowMap = (FlowMapImpl) o;
        return privateMap.equals(flowMap.privateMap);
//                &&
//                flowState.equals(flowMap.flowState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(privateMap);
    }






    private @NotNull static final Logger log = org.slf4j.LoggerFactory.getLogger("TypeInfer");

    // ======
    // Runtime type checking
    // ======
    public <K> Optional<K> getWithType(String targetKey, TypeToken<K> targetType) {
        Object targetObject = privateMap.get(targetKey);
        if(targetObject == null) return Optional.empty();

        // for each inserted element there should be an associated type token
        assert privateTypeMap.containsKey(targetKey);
        TypeToken<?> knownType = privateTypeMap.get(targetKey);


        if(targetType.isSupertypeOf(knownType)) {
            @SuppressWarnings("unchecked") // checked with subtype relation
            K targetO = (K) targetObject;
            return Optional.of(targetO);
        }

        if(targetType.isSubtypeOf(knownType)) {
            log.warn("Downcasting '{}' from '{}' -> '{}'. This may indicate a node implementation error or an insufficient type infer of that key",
                    targetKey, knownType, targetType);
            @SuppressWarnings("unchecked") // warned user about potentially bad downcast
            Optional<K> castObject = Optional.of((K) targetObject);
            return castObject;
        }

        throw new TemplateException(String.format("Bad typing for key %s. Expected type '%s', got type '%s'",
                targetKey,
                targetType.toString(),
                knownType.toString()
        ));
    }

    /**
     * Infers the type of the current object strengthening the type for this flow map for that key
     */
    private <K> void inferTypeAgainstActualObject(String targetKey, TypeToken<?> knownType, TypeToken<K> targetType, Object targetObject) {
        assert targetType.isSubtypeOf(knownType);
        log.debug("Inferring type of '{}' -> '{}' with actual object '{}'", knownType, targetType, targetObject);
//
//
//        Class<? super K> targetRawType = targetType.getRawType();
//        if(targetRawType.isAssignableFrom(List.class)) {
//            log.info("Descending list type");
//            if(((List) targetObject).isEmpty()) {
//                log.info("Actual list empty, inferring type by promoting {}: {} -> {}", targetKey, knownType, targetType);
//                privateTypeMap.put(targetKey, targetType);
//            }
//
//            Type typp = ((ParameterizedType) targetType.getType()).getActualTypeArguments()[0];
//            TypeToken<?> typeTokenListGenericType = TypeToken.of(typp);
//            System.out.println(typeTokenListGenericType);
//
//
//
//
//        } else if (targetRawType.isAssignableFrom(Map.class)) {
//            log.info("Descending map type");
//            if(((Map) targetObject).isEmpty()) {
//                log.info("Actual map empty, inferring type by promoting {}: {} -> {}", targetKey, knownType, targetType);
//                privateTypeMap.put(targetKey, targetType);
//            }
//
//
//
//        } else {
//            log.info("Primitive type ");
//        }
//
//
//        throw new TemplateException("Could not infer type for "+targetKey+"("+knownType+"->"+targetType+"): Error");
    }

    @SuppressWarnings("rawtypes")
    private TypeToken<?> inferType(Object o) {
        if(o instanceof List) {
            List oList = ((List) o);
            if(((List) o).isEmpty()) return TypeToken.of(List.class);

            // full check
            TypeToken<?> commonType = inferType(oList.get(0));
            for (int i = 1; i < oList.size(); i++) {
                TypeToken<?> nextType = inferType(oList.get(i));
                if(!commonType.isSubtypeOf(nextType) || !nextType.isSubtypeOf(commonType))
                    throw new TemplateException("Types of elements of a list have to match exactly: "+commonType + " != "+ nextType);
            }

            return listOf(commonType);
        } else if (o instanceof Map) {
            Map oMap = ((Map) o);
            if(((Map) o).isEmpty()) return TypeToken.of(Map.class);

            // full check
            Iterator iter = oMap.keySet().iterator();
            TypeToken<?> commonType = inferType(iter.next());
            while(iter.hasNext()) {
                TypeToken<?> nextType = inferType(iter.next());
                if(!commonType.isSubtypeOf(nextType) || !nextType.isSubtypeOf(commonType))
                    throw new TemplateException("Types of elements of a map have to match exactly: "+commonType + " != "+ nextType);
            }

            return mapOf(TypeToken.of(String.class), commonType);

        } else {
            // raw type
            return TypeToken.of(o.getClass());
        }
    }
}
