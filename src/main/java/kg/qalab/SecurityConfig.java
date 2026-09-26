package kg.qalab;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.util.*;

@Configuration
@EnableScheduling
public class SecurityConfig {
    @Bean
    org.springframework.security.core.userdetails.UserDetailsService noDefaultPasswordUser() {
        return username -> { throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Bearer authentication only"); };
    }
    @Bean
    SecurityFilterChain security(HttpSecurity http,LabStore store,JsonMapper mapper) throws Exception {
        return http
            // Tokens live in Authorization, not cookies. No ambient cookie auth => no CSRF cookie flow.
            .csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(c -> c.disable())
            .httpBasic(c -> c.disable()).formLogin(c -> c.disable()).logout(c -> c.disable())
            .headers(h -> h.contentSecurityPolicy(c -> c.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'")))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/","/index.html","/app.js","/style.css","/favicon.svg","/openapi.yaml","/postman/**","/guide.html","/swagger-ui.html","/swagger-ui/**","/v3/api-docs","/v3/api-docs.yaml","/v3/api-docs/**","/webjars/**","/error").permitAll()
                .requestMatchers("/api/health","/api/catalog","/api/auth/register","/api/auth/login").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req,res,ex) -> {
                    res.setHeader("WWW-Authenticate","Bearer");
                    write(mapper,req,res,401,"UNAUTHORIZED","Добавьте действующий Authorization: Bearer <token>");
                })
                .accessDeniedHandler((req,res,ex) -> write(mapper,req,res,403,"FORBIDDEN","Вы вошли, но роль USER не даёт доступ к этому ресурсу")))
            .addFilterBefore(new LabFilter(store,mapper),UsernamePasswordAuthenticationFilter.class)
            .build();
    }
    static void write(JsonMapper mapper,HttpServletRequest req,HttpServletResponse res,int status,String code,String message) throws IOException {
        res.setStatus(status); res.setContentType("application/json"); res.setCharacterEncoding("UTF-8");
        mapper.writeValue(res.getWriter(),ApiErrors.body(status,code,message,Map.of(),req));
    }
    /** Global bounded demo rate limits. No spoofable forwarded IP is trusted. */
    static final class LabFilter extends OncePerRequestFilter {
        private final LabStore store;
        private final JsonMapper mapper;
        private long minute=0;
        private int authRequests=0,apiRequests=0;
        LabFilter(LabStore store,JsonMapper mapper) { this.store=store; this.mapper=mapper; }
        private synchronized boolean allowed(boolean auth) {
            long now=System.currentTimeMillis()/60000;
            if (now!=minute) { minute=now; authRequests=0; apiRequests=0; }
            return auth ? ++authRequests<=60 : ++apiRequests<=1200;
        }
        @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
            String requestId=UUID.randomUUID().toString();
            req.setAttribute("requestId",requestId); res.setHeader("X-Request-Id",requestId);
            if (req.getRequestURI().startsWith("/api/")) res.setHeader("Cache-Control","no-store");
            boolean isAuth=req.getRequestURI().equals("/api/auth/login")||req.getRequestURI().equals("/api/auth/register");
            if (req.getRequestURI().startsWith("/api/") && !req.getRequestURI().equals("/api/health") && !allowed(isAuth)) {
                res.setHeader("Retry-After","60"); write(mapper,req,res,429,"RATE_LIMIT","Слишком много запросов. Подождите одну минуту"); return;
            }
            // Read at most 16 KiB, including chunked requests, before JSON parsing.
            if (req.getRequestURI().startsWith("/api/") && Set.of("POST","PUT","PATCH").contains(req.getMethod())) {
                byte[] payload=req.getInputStream().readNBytes(16385);
                if (payload.length>16384) { write(mapper,req,res,413,"PAYLOAD_TOO_LARGE","Учебное тело запроса ограничено 16 KiB"); return; }
                req=new BufferedRequest(req,payload);
            }
            String authorization=req.getHeader("Authorization");
            if (authorization!=null && authorization.startsWith("Bearer ")) {
                String userId=store.authenticate(authorization.substring(7));
                if (userId!=null) SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(userId,null,List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            }
            chain.doFilter(req,res);
        }
    }
    static final class BufferedRequest extends HttpServletRequestWrapper {
        private final byte[] payload;
        BufferedRequest(HttpServletRequest request,byte[] payload) { super(request); this.payload=payload; }
        @Override public ServletInputStream getInputStream() {
            var input=new java.io.ByteArrayInputStream(payload);
            return new ServletInputStream() {
                @Override public int read() { return input.read(); }
                @Override public boolean isFinished() { return input.available()==0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) { throw new UnsupportedOperationException("Synchronous API only"); }
            };
        }
        @Override public java.io.BufferedReader getReader() { return new java.io.BufferedReader(new java.io.InputStreamReader(getInputStream(),java.nio.charset.StandardCharsets.UTF_8)); }
    }
}
