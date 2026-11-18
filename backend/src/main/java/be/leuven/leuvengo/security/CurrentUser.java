package be.leuven.leuvengo.security;

import be.leuven.leuvengo.domain.Role;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the authenticated {@link be.leuven.leuvengo.domain.AppUser} into a
 * controller method. Missing or expired sessions get a 401; a user without
 * the required role gets a 403. With {@code required = false} the parameter
 * is simply null for anonymous callers.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {

    boolean required() default true;

    /** Roles allowed to call the endpoint; empty means any logged-in user. */
    Role[] role() default {};
}
