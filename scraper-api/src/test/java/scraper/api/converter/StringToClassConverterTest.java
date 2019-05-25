package scraper.api.converter;

import org.junit.Assert;
import org.junit.Test;
import scraper.api.exceptions.ValidationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.Random;

import static junit.framework.TestCase.assertTrue;

/**
 * Tests the default StringToClassConverter
 *
 * @since 1.0.0
 */
public class StringToClassConverterTest {

    @Test
    public void booleansTest() throws Exception {
        String[] input = new String[]{"true", "false","TRUE", "FALSE", "falSE","tRuE"};
        for (String bool : input) {
            Class<?> target = Boolean.class;
            Object convert = StringToClassConverter.convert(bool, target);

            Assert.assertTrue(Boolean.class.isAssignableFrom(convert.getClass()));
            Assert.assertEquals(Boolean.valueOf(bool), convert);
        }
    }

    @Test
    public void intTest() throws Exception {
        String[] input = new String[]{"-1", "0","1"};
        for (String i : input) {
            Class<?> target = Integer.class;
            Object convert = StringToClassConverter.convert(i, target);

            Assert.assertTrue(Integer.class.isAssignableFrom(convert.getClass()));
            Assert.assertEquals(Integer.valueOf(i), convert);
        }
    }

    @Test
    public void longTest() throws Exception {
        String[] input = new String[]{"-1", "0","1", "2147483648"};
        for (String i : input) {
            Class<?> target = Long.class;
            Object convert = StringToClassConverter.convert(i, target);

            Assert.assertTrue(Long.class.isAssignableFrom(convert.getClass()));
            Assert.assertEquals(Long.valueOf(i), convert);
        }
    }

    @Test
    public void doubleTest() throws Exception {
        Assert.assertEquals(1.0, StringToClassConverter.convert("1", Double.class));
        Assert.assertEquals(0.0, StringToClassConverter.convert("0", Double.class));
        Assert.assertEquals(1.5, StringToClassConverter.convert("1.5", Double.class));
        Assert.assertEquals(1.2345, StringToClassConverter.convert("1.2345", Double.class));
    }

    @Test
    public void objectTest() {
        // TODO #18 use case? String convert to target class object?
    }

    @Test(expected = ValidationException.class)
    public void longOutOfBoundsTest() throws Exception {
        StringToClassConverter.convert("9223372036854775808", Long.class);
    }

    @Test(expected = ValidationException.class)
    public void intOutOfBoundsTest() throws Exception {
        StringToClassConverter.convert("2147483648", Integer.class);
    }

    @Test
    public void nullJsonTest() throws Exception {
        String nullJson = "null";
        Class<?> target = Object.class;
        Object convert = StringToClassConverter.convert(nullJson, target);
        Assert.assertNull(convert);
    }

    @Test
    public void nullStringTest() throws Exception {
        String nullString = null;
        Class<?> target = Object.class;
        Object convert = StringToClassConverter.convert(nullString, target);
        Assert.assertNull(convert);
    }

    @Test
    public void enumTest() throws Exception {
        String enumString = "test";
        Class<?> target = HelloEnum.class;
        Object convert = StringToClassConverter.convert(enumString, target);
        Assert.assertEquals(convert, HelloEnum.test);
    }

    @Test(expected = ValidationException.class)
    public void enumFailTest() throws Exception {
        String enumString = "Test";
        Class<?> target = HelloEnum.class;
        Object convert = StringToClassConverter.convert(enumString, target);
        Assert.assertEquals(convert, HelloEnum.test);
    }

    @Test
    public void stringFuzzer() {
        Class[] targetClasses = new Class[]{null, void.class, Object.class, Boolean.class, Integer.class, Double.class, Long.class, Enum.class, String.class};
        Random r = new Random();

        for (int i = 0; i < 10000; i++) {
            // get random target class
            Class<?> targetClass = targetClasses[r.nextInt(targetClasses.length)];

            // get random utf-8 string
            byte[] array = new byte[10];
            new Random().nextBytes(array);
            String generatedString = new String(array, Charset.forName("UTF-8"));

            // converting should only produce ValidationExceptions
            try {
                StringToClassConverter.convert(generatedString, targetClass);
            } catch (ValidationException ignored) {}
        }
    }

    @Test
    public void testConstructorIsPrivate() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<StringToClassConverter> constructor = StringToClassConverter.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }
}