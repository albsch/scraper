package scraper.annotations;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.CLASS;

@Retention(value=CLASS)
@Target(value={FIELD,METHOD,PARAMETER,LOCAL_VARIABLE})
public @interface NotNull {}

