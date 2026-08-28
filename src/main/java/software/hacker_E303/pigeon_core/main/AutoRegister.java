package software.hacker_E303.pigeon_core.main;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for automatic registration of entities, items, and other content.
 * Classes annotated with @AutoRegister will be automatically discovered and registered
 * by the library's registration system.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoRegister {

    String value();
}