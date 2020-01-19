package scraper.nodes.core.functional;

import junitparams.JUnitParamsRunner;
import org.junit.runner.RunWith;
import scraper.api.exceptions.TemplateException;
import scraper.nodes.core.test.annotations.Functional;
import scraper.nodes.core.test.helper.FunctionalTest;

import java.util.Map;
import java.util.Set;


@RunWith(JUnitParamsRunner.class)
public class ContainedInCollectionNodeTest extends FunctionalTest {

    @Functional(value = ContainedInCollectionNode.class, expectException = TemplateException.class)
    public Object[] emptyContains() {
        Map nIn = Map.of();
        Map fIn = Map.of("object", "important");
        Map fOut = Map.of("flag", false);

        return new Object[]{ nIn, fIn, fOut };
    }

    @Functional(ContainedInCollectionNode.class)
    public Object[] simpleContains() {
        Map nIn = Map.of();
        Map fIn = Map.of(
                "collection", Set.of("important", "notImportant"),
                "object", "important"
        );
        Map fOut = Map.of("flag", true);

        return new Object[]{ nIn, fIn, fOut };
    }

    @Functional(ContainedInCollectionNode.class)
    public Object[] complexObjectContains() {
        Map nIn = Map.of();
        Map fIn = Map.of(
                "collection", Set.of("important", Set.of("hello", "world")),
                "object", Set.of("hello", "world")
        );
        Map fOut = Map.of("flag", true);

        return new Object[]{ nIn, fIn, fOut };
    }

    @Functional(ContainedInCollectionNode.class)
    public Object[] complexObjectNotContains() {
        Map nIn = Map.of();
        Map fIn = Map.of(
                "collection", Set.of("important", Set.of("hello", "world")),
                "object", Set.of("hello")
        );
        Map fOut = Map.of("flag", false);

        return new Object[]{ nIn, fIn, fOut };
    }
}
