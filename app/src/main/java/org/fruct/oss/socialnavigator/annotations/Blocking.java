package org.fruct.oss.socialnavigator.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method that can do time-consuming work
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Blocking {
}
