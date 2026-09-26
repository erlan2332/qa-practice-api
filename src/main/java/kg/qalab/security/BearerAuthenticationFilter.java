package kg.qalab.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kg.qalab.model.UserAccount;
import kg.qalab.service.TokenService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class BearerAuthenticationFilter
    extends OncePerRequestFilter {

    private final TokenService tokenService;

    public BearerAuthenticationFilter(
        TokenService tokenService
    ) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization =
            request.getHeader("Authorization");

        if (
            authorization != null
                && authorization.startsWith("Bearer ")
        ) {
            String rawToken =
                authorization.substring(7);

            Optional<UserAccount> user =
                tokenService.authenticate(rawToken);

            user.ifPresent(account -> {
                var authority = new SimpleGrantedAuthority(
                    "ROLE_" + account.role().name()
                );

                var authentication =
                    new UsernamePasswordAuthenticationToken(
                        account.id(),
                        null,
                        List.of(authority)
                    );

                SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);
            });
        }

        filterChain.doFilter(request, response);
    }
}
