package kg.qalab;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper mapper;
    String token;
    String email;

    @BeforeEach void register() throws Exception {
        email="qa-"+UUID.randomUUID()+"@example.test";
        token=field(mvc.perform(json(post("/api/auth/register"),"""
            {"email":"%s","password":"LearnOnly123!","name":"QA Student"}
            """.formatted(email))).andExpect(status().isCreated()).andReturn(),"token");
    }
    @AfterEach void removeUser() throws Exception {
        mvc.perform(authed(delete("/api/me")));
    }
    MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder request) { return request.header("Authorization","Bearer "+token); }
    MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request,String body) { return request.contentType(MediaType.APPLICATION_JSON).content(body); }
    String field(MvcResult result,String field) throws Exception { return mapper.readTree(result.getResponse().getContentAsString()).get(field).asText(); }
    String product(int stock) throws Exception {
        return field(mvc.perform(json(authed(post("/api/products")),"{\"name\":\"QA mug\",\"price\":250,\"stock\":"+stock+"}"))
            .andExpect(status().isCreated()).andReturn(),"id");
    }
    String order(String product,int quantity) throws Exception {
        return field(mvc.perform(json(authed(post("/api/orders")),"{\"productId\":\""+product+"\",\"quantity\":"+quantity+"}"))
            .andExpect(status().isCreated()).andReturn(),"id");
    }
    @Test void publicCatalogAndHealth() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/api/catalog")).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(3));
    }
    @Test void unauthenticatedIs401() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate","Bearer"))
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
    @Test void normalUserCannotBecomeAdmin() throws Exception {
        mvc.perform(authed(get("/api/admin/stats"))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(json(authed(patch("/api/me")),"{\"name\":\"QA\",\"role\":\"ADMIN\"}"))
            .andExpect(status().isBadRequest());
    }
    @Test void passwordIsNeverReturned() throws Exception {
        mvc.perform(authed(get("/api/me"))).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.hash").doesNotExist());
    }
    @Test void duplicateEmailIsCaseInsensitive() throws Exception {
        mvc.perform(json(post("/api/auth/register"),"{\"email\":\""+email.toUpperCase()+"\",\"password\":\"LearnOnly123!\",\"name\":\"QA\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_EXISTS"));
    }
    @Test void invalidRegistrationRejected() throws Exception {
        mvc.perform(json(post("/api/auth/register"),"{\"email\":\"not-email\",\"password\":\"tiny\",\"name\":\"\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.email").exists());
    }
    @Test void correctAndIncorrectLogin() throws Exception {
        mvc.perform(json(post("/api/auth/login"),"{\"email\":\""+email+"\",\"password\":\"wrong-password\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        mvc.perform(json(post("/api/auth/login"),"{\"email\":\""+email+"\",\"password\":\"LearnOnly123!\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
    }
    @Test void logoutRevokesToken() throws Exception {
        mvc.perform(authed(post("/api/auth/logout"))).andExpect(status().isNoContent());
        mvc.perform(authed(get("/api/me"))).andExpect(status().isUnauthorized());
    }
    @Test void createProductHasLocation() throws Exception {
        mvc.perform(json(authed(post("/api/products")),"{\"name\":\"Книга\",\"price\":100,\"stock\":0}"))
            .andExpect(status().isCreated()).andExpect(header().exists("Location")).andExpect(jsonPath("$.currency").value("KGS"));
    }
    @Test void rejectBadFields() throws Exception {
        mvc.perform(json(authed(post("/api/products")),"{\"name\":\"\",\"price\":-1,\"stock\":1001}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.name").exists()).andExpect(jsonPath("$.fields.price").exists())
            .andExpect(jsonPath("$.fields.stock").exists());
    }
    @Test void rejectMalformedUnknownAndWrongTypes() throws Exception {
        for(String body:new String[]{"{broken", "{\"name\":\"QA\",\"price\":\"100\",\"stock\":2}",
                "{\"name\":\"QA\",\"price\":100,\"stock\":2,\"admin\":true}","null"}) {
            mvc.perform(json(authed(post("/api/products")),body)).andExpect(status().isBadRequest());
        }
    }
    @Test void putRequiresAllFieldsAndPatchPreservesOthers() throws Exception {
        String id=product(8);
        mvc.perform(json(authed(put("/api/products/"+id)),"{\"stock\":2}"))
            .andExpect(status().isBadRequest());
        mvc.perform(json(authed(patch("/api/products/"+id)),"{\"stock\":2}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("QA mug"))
            .andExpect(jsonPath("$.price").value(250)).andExpect(jsonPath("$.stock").value(2));
    }
    @Test void deletionReturnsEmpty204Then404() throws Exception {
        String id=product(1);
        mvc.perform(authed(delete("/api/products/"+id))).andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(authed(get("/api/products/"+id))).andExpect(status().isNotFound());
    }
    @Test void paginationAndFilter() throws Exception {
        product(5);
        mvc.perform(authed(get("/api/products?q=QA&page=0&size=1"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.total").value(1));
        mvc.perform(authed(get("/api/products?size=0"))).andExpect(status().isBadRequest());
        mvc.perform(authed(get("/api/products?page=oops"))).andExpect(status().isBadRequest());
    }
    @Test void idempotentOrderAndBodyConflict() throws Exception {
        String id=product(5),body="{\"productId\":\""+id+"\",\"quantity\":2}";
        MvcResult first=mvc.perform(json(authed(post("/api/orders").header("Idempotency-Key","test-key")),body))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(500)).andReturn();
        mvc.perform(json(authed(post("/api/orders").header("Idempotency-Key","test-key")),body))
            .andExpect(status().isOk()).andExpect(header().string("Idempotency-Replayed","true"))
            .andExpect(jsonPath("$.id").value(field(first,"id")));
        mvc.perform(json(authed(post("/api/orders").header("Idempotency-Key","test-key")),body.replace(":2}",":3}")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
        mvc.perform(authed(get("/api/products/"+id))).andExpect(jsonPath("$.stock").value(3));
    }
    @Test void noOverselling() throws Exception {
        String id=product(1);
        mvc.perform(json(authed(post("/api/orders")),"{\"productId\":\""+id+"\",\"quantity\":2}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("OUT_OF_STOCK"));
        mvc.perform(authed(get("/api/products/"+id))).andExpect(jsonPath("$.stock").value(1));
        mvc.perform(authed(get("/api/orders"))).andExpect(jsonPath("$.items.length()").value(0));
    }
    @Test void cancellationRestocksOnlyOnce() throws Exception {
        String id=product(5),order=order(id,2);
        mvc.perform(authed(delete("/api/products/"+id))).andExpect(status().isConflict());
        for(int i=0;i<2;i++) mvc.perform(authed(post("/api/orders/"+order+"/cancel")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(authed(get("/api/products/"+id))).andExpect(jsonPath("$.stock").value(5));
    }
    @Test void paymentDeclineThenSuccessAndConflict() throws Exception {
        String order=order(product(3),1);
        mvc.perform(json(authed(post("/api/orders/"+order+"/pay")),"{\"outcome\":\"DECLINED\"}"))
            .andExpect(status().isPaymentRequired());
        mvc.perform(authed(get("/api/orders/"+order))).andExpect(jsonPath("$.status").value("CREATED"));
        mvc.perform(json(authed(post("/api/orders/"+order+"/pay")),"{\"outcome\":\"APPROVED\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
        mvc.perform(authed(post("/api/orders/"+order+"/cancel"))).andExpect(status().isConflict());
    }
    @Test void otherAccountCannotReadChangeOrDeleteResources() throws Exception {
        String id=product(5),order=order(id,1),ownerToken=token;
        register();
        String otherToken=token;
        try {
            mvc.perform(authed(get("/api/products/"+id))).andExpect(status().isNotFound());
            mvc.perform(json(authed(patch("/api/products/"+id)),"{\"stock\":0}" )).andExpect(status().isNotFound());
            mvc.perform(authed(delete("/api/products/"+id))).andExpect(status().isNotFound());
            mvc.perform(authed(get("/api/orders/"+order))).andExpect(status().isNotFound());
            mvc.perform(authed(post("/api/orders/"+order+"/cancel"))).andExpect(status().isNotFound());
        } finally {mvc.perform(delete("/api/me").header("Authorization","Bearer "+otherToken));token=ownerToken;}
    }
    @Test void resetIsIsolatedAndKeepsLogin() throws Exception {
        String id=product(2),owner=token;
        register();String other=token;
        try {
            mvc.perform(authed(post("/api/sandbox/reset"))).andExpect(status().isOk());
            mvc.perform(get("/api/products/"+id).header("Authorization","Bearer "+owner)).andExpect(status().isOk());
            mvc.perform(authed(get("/api/me"))).andExpect(status().isOk());
        } finally {mvc.perform(delete("/api/me").header("Authorization","Bearer "+other));token=owner;}
    }
    @Test void requestIdAndSecurityHeaders() throws Exception {
        MvcResult result=mvc.perform(authed(get("/api/products/missing"))).andExpect(status().isNotFound())
            .andExpect(header().string("Cache-Control","no-store")).andExpect(header().exists("Content-Security-Policy")).andReturn();
        assertThat(field(result,"requestId")).isEqualTo(result.getResponse().getHeader("X-Request-Id"));
    }
    @Test void unsupportedMediaMethodAndLargeBody() throws Exception {
        mvc.perform(authed(post("/api/products")).contentType("text/plain").content("hello")).andExpect(status().isUnsupportedMediaType());
        mvc.perform(authed(put("/api/catalog"))).andExpect(status().isMethodNotAllowed());
        mvc.perform(json(authed(post("/api/lab/echo")),"x".repeat(17000))).andExpect(status().isContentTooLarge());
    }
    @Test void staticAssetsPublicAndHeadWorks() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/swagger-ui.html")).andExpect(status().isFound());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.info.title").value("QA Lab API"));
        mvc.perform(get("/v3/api-docs.yaml")).andExpect(status().isOk());
        // MockMvc is not a servlet container; wire-level HEAD is covered by smoke.py.
        mvc.perform(head("/api/catalog")).andExpect(status().isOk());
    }
}
