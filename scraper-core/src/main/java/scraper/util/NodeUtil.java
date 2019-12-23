package scraper.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import scraper.api.converter.StringToClassConverter;
import scraper.api.exceptions.ValidationException;
import scraper.api.flow.FlowMap;
import scraper.api.flow.impl.FlowMapImpl;
import scraper.api.node.Address;
import scraper.api.node.GraphAddress;
import scraper.api.node.NodeAddress;
import scraper.api.node.impl.AddressImpl;
import scraper.api.node.impl.GraphAddressImpl;
import scraper.api.node.impl.NodeAddressImpl;
import scraper.core.Template;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class NodeUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Evaluates the base template with the given map. Does nothing if base is not of type String.
     */
    public static Object replaceArguments(Object base, Map<String, Object> args){
        if(base == null) return null;
        if(!(base instanceof String)) return base;

        String replaced = (String) base;

        for (String arg : args.keySet()) {
            try {
                String ik = String.valueOf(args.get(arg));
                Pattern argsPattern = Pattern.compile("\\{('(.*?)')?"+Pattern.quote(arg)+"('(.*?)')?}");
                Matcher argsMatcher = argsPattern.matcher(replaced);
                if(args.get(arg) == null) {
                    replaced = argsMatcher.replaceAll("");
                } else {
                    replaced = argsMatcher.replaceAll("$2"+Matcher.quoteReplacement(ik)+"$4");
                }
            } catch (ClassCastException ignored) {}
        }

        return replaced;
    }

    public static FlowMap flowOf(Map<String, Object> initialArguments) {
        return FlowMapImpl.of(initialArguments);
    }

    public static FlowMap flowOf(FlowMap o) {
        return FlowMapImpl.copy(o);
    }

    public static Address addressOf(String label) {
        return new AddressImpl(label);
    }

    public static GraphAddress graphAddressOf(String label) {
        return new GraphAddressImpl(label);
    }

    public static Object getValueForField(final Class<?> fieldType, final Object fieldValue,
                                          final Object jsonValue,
                                          final Object globalValue,
                                          final boolean mandatory,
                                          final String defaultAnnotationValue,
                                          final boolean isArgument,
                                          final Class<?> argumentConverter,
                                          final Map<String, Object> arguments
    ) throws ValidationException {
        try {
            Object value;

            // use global value as least precedence
            value = globalValue;

            // prefer local JSON value over global parsed value
            if(jsonValue != null) value = jsonValue;

            // value has to be defined if mandatory
            if (mandatory && value == null) throw new ValidationException("Value has to be defined if mandatory: " + fieldType);

            // --------- current state of value
            // value != null: from JSON file
            // value == null: no JSON file definition

            // read optional JSON raw value if optional
            if(!mandatory && value == null) {
                Object converted;
                // TODO document what this code block does
                if(Template.class.isAssignableFrom(fieldType)) {
                    converted = mapper.readValue(defaultAnnotationValue, Object.class);
                    value = converted;
                } else if (Enum.class.isAssignableFrom(fieldType)) {
                    converted = mapper.readValue(defaultAnnotationValue, String.class);
                    value = converted;
                } else {
                    converted = mapper.readValue(defaultAnnotationValue, fieldType);
                    value = converted;
                }
            }

            // argument annotation :
            //     String -> goTo primitive type or enum (default converter)
            //     String -> custom goTo                 (custom converter)
            if (isArgument && value != null) {

                // get template converter
                Method convert;
                if(argumentConverter == null || argumentConverter.equals(void.class))
                    convert = getConverter(StringToClassConverter.class);
                else
                    convert = getConverter(argumentConverter);

                // --- JSON string or some parsed value
                // if parsed value is a String (or value is JSON) replace arguments
                if (String.class.isAssignableFrom(value.getClass())) {
                    value = NodeUtil.replaceArguments(value, arguments);

                    // if replaced value is null, treat as disabled argument and set field value to null
                    if(((String) value).isEmpty()) {
                        return null;
                    }
                } // replace directly with the parsed value
                else {
                    // check if parsed value is an Integer but expected value is a Long
                    if(Integer.class.isAssignableFrom(value.getClass()) && Long.class.isAssignableFrom(fieldType)) {
                        // Integer passes as a Long
                        return ((Integer) value).longValue();
                    } else {
                        // else use parsed value
                        return value;
                    }
                }

                // --- String
                // convert the string value to the defined field type
                return invokeConverter(value, fieldType, convert);
            }

            // --------
            // value == null: no JSON definition, no optional definition: field null
            // value != null: parsed json object or field default (Template, MapKey)
            if(value == null) return null;

            // check if template
            if (Template.class.isAssignableFrom(fieldType)) {
                Template<?> template = (Template<?>) fieldValue;
                template.setParsedJson(convert(template.type, value));
                return null;
            } // if enum: try convert
            else if (Enum.class.isAssignableFrom(fieldType)) {
                value = Enum.valueOf(fieldType.asSubclass(Enum.class), String.valueOf(value));
            } // type match
            else //noinspection StatementWithEmptyBody readability
                if (fieldType.isAssignableFrom(value.getClass())) {
                // value is correct
            } // check if field type is an NodeAddress
            else if (String.class.isAssignableFrom(value.getClass()) && NodeAddress.class.isAssignableFrom(fieldType)) {
                value = new NodeAddressImpl((String) value);
            } // check if field type is an GraphAddress
            else if (String.class.isAssignableFrom(value.getClass()) && GraphAddress.class.isAssignableFrom(fieldType)) {
                value = new GraphAddressImpl((String) value);
            } // check if field type is an GraphAddress
            else if (String.class.isAssignableFrom(value.getClass()) && Address.class.isAssignableFrom(fieldType)) {
                value = NodeUtil.addressOf((String) value);
            } // try converting as a last resort
            else { // TODO #23 test this branch for full coverage
                value = StringToClassConverter.convert(String.valueOf(value), fieldType);
            }

//        log(TRACE,"Field '{}' initialized: {} ({})", field.getName(), field.get(this), (field.get(this) == null?"nullp":field.get(this).getClass()));

            return value;
        } catch (Exception e) {
            throw new ValidationException("Could not get value for field ", e);
        }
    }

    public static Method getConverter(final Class<?> converter) throws ValidationException {
        Method convert;
        try { convert = converter.getMethod("convert", Object.class, Class.class); }
        catch (NoSuchMethodException e) { throw new ValidationException("Unknown template converter: " + e); }
        return convert;
    }

    public static Object invokeConverter(Object value, Class<?> fieldType, Method convert) throws ReflectiveOperationException {
        try {
            return convert.invoke(null, String.valueOf(value), fieldType);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ReflectiveOperationException(e);
        }
    }

    public static Object convert(TypeToken<?> templ, Object value) throws ValidationException {
        //null: return null
        if(value == null) return null;

        //check JSON object types
        //JSON list
        if(List.class.isAssignableFrom(value.getClass()) &&
                (
                        List.class.isAssignableFrom(templ.getRawType()) || templ.getRawType().equals(Object.class)
                )) {
            // create a list with converted values
            TypeToken<?> elementType = (templ.getRawType().equals(Object.class) ? templ : templ.resolveType(List.class.getTypeParameters()[0]));
            List<?> valueList = (List<?>) value;
//            log(TRACE,"Converting list: {}, expecting {}", valueList, elementType);

            List<Object> resultList = new ArrayList<>();

            for (Object o : valueList) {
                resultList.add(convert(elementType, o));
            }

            return resultList;
        } // JSON map
        else if(Map.class.isAssignableFrom(value.getClass()) &&
                (
                        Map.class.isAssignableFrom(templ.getRawType()) || templ.getRawType().equals(Object.class) // descend into object
                )) {
            // create a map with checked values
            TypeToken<?> elementType = (templ.getRawType().equals(Object.class) ? templ : templ.resolveType(Map.class.getTypeParameters()[1]));
            Map<?, ?> valueMap = (Map<?, ?>) value;
//            log(TRACE,"Converting map: {}, expecting value type {}", valueMap, elementType);

            Map<Object, Object> resultMap = new LinkedHashMap<>();
            for (Object key : valueMap.keySet()) {
                resultMap.put(key, convert(elementType, valueMap.get(key)));
            }

            return resultMap;
        } // JSON primitive
        else {
            // raw type
            if(templ.getRawType().isAssignableFrom(value.getClass()) && !String.class.isAssignableFrom(value.getClass())) {
                // same types, return actual object
                return value;
            } else if (String.class.isAssignableFrom(value.getClass())) {
                // string template found
                return TemplateUtil.parseTemplate(((String) value), templ);
            } else {
                throw new ValidationException("Argument type mismatch! Expected String or "+templ+", but found "+ value.getClass());
            }
        }
    }

}
