package be.leuven.leuvengo.security;

import be.leuven.leuvengo.domain.AppUser;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final AuthService auth;

    public CurrentUserArgumentResolver(AuthService auth) {
        this.auth = auth;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && AppUser.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public AppUser resolveArgument(MethodParameter parameter, ModelAndViewContainer mav,
                                   NativeWebRequest request, WebDataBinderFactory binders) {
        CurrentUser spec = parameter.getParameterAnnotation(CurrentUser.class);
        AppUser user = auth.authenticate(request.getHeader(HttpHeaders.AUTHORIZATION)).orElse(null);

        if (user == null) {
            if (spec.required()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in first");
            }
            return null;
        }
        if (spec.role().length > 0 && Arrays.stream(spec.role()).noneMatch(r -> r == user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account can't do this");
        }
        return user;
    }
}
