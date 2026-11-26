package com.lostfoundhub.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostfoundhub.model.User;
import com.lostfoundhub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests of the REST API with the real Spring context, security, JPA and an in-memory H2 database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LostFoundHubIntegrationTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final String PASSWORD = "Passw0rd123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;

    // ---------------------------------------------------------------- helpers

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Test User", "email", email, "password", PASSWORD))))
                .andExpect(status().isCreated());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).at("/data/token").asText();
    }

    /** Registers a fresh user and returns a Bearer header value. */
    private String newUserToken(String prefix) throws Exception {
        String email = uniqueEmail(prefix);
        register(email);
        return "Bearer " + login(email, PASSWORD);
    }

    private String adminToken() throws Exception {
        return "Bearer " + login("admin@test.com", "Admin@12345");
    }

    private MockHttpServletRequestBuilder itemForm(MockHttpServletRequestBuilder builder, String name, String category,
                                                   String location, String type) {
        return builder.param("itemName", name).param("category", category).param("description", "Black leather item")
                .param("location", location).param("eventDate", "2026-01-15").param("itemType", type);
    }

    private long createItem(String token, String name, String category, String location, String type) throws Exception {
        MvcResult result = mockMvc.perform(itemForm(multipart("/api/items")
                                .file(new MockMultipartFile("image", "pic.png", "image/png", PNG)), name, category, location, type)
                        .header("Authorization", token))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).at("/data/id").asLong();
    }

    private long submitClaim(String token, long itemId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/claims").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":" + itemId + ",\"proofDescription\":\"It has my name written inside\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).at("/data/id").asLong();
    }

    // ---------------------------------------------------------------- authentication

    @Test
    void registration_login_profile_andPasswordIsHashed() throws Exception {
        String email = uniqueEmail("reg");
        register(email);

        // duplicate email -> 409
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Dup", "email", email.toUpperCase(), "password", PASSWORD))))
                .andExpect(status().isConflict());

        // invalid input -> 400 with field errors
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"bad\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.email").exists())
                .andExpect(jsonPath("$.data.password").exists());

        // wrong password -> 401
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "WrongPass1"))))
                .andExpect(status().isUnauthorized());

        String token = "Bearer " + login(email, PASSWORD);
        mockMvc.perform(get("/api/users/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.password").doesNotExist());

        User stored = userRepository.findByEmail(email).orElseThrow();
        assertTrue(stored.getPassword().startsWith("$2"), "password must be a BCrypt hash");
    }

    @Test
    void protectedEndpoints_rejectMissingAndInvalidTokens() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired JWT token"));
    }

    @Test
    void logout_invalidatesTheToken() throws Exception {
        String token = newUserToken("logout");
        mockMvc.perform(get("/api/users/me").header("Authorization", token)).andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout").header("Authorization", token)).andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me").header("Authorization", token)).andExpect(status().isUnauthorized());
    }

    @Test
    void user_canUpdateOwnProfile_butCannotReadOthers() throws Exception {
        String tokenA = newUserToken("pa");
        String tokenB = newUserToken("pb");
        long idB = json(mockMvc.perform(get("/api/users/me").header("Authorization", tokenB)).andReturn()).at("/data/id").asLong();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/users/me")
                        .header("Authorization", tokenA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Name\",\"phone\":\"9876543210\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("New Name"));

        mockMvc.perform(get("/api/users/" + idB).header("Authorization", tokenA)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/" + idB).header("Authorization", adminToken())).andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- authorization

    @Test
    void adminEndpoints_areAdminOnly() throws Exception {
        String user = newUserToken("noadmin");

        mockMvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", user)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users").header("Authorization", user)).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/admin/claims/1/verify").header("Authorization", user)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());

        String admin = adminToken();
        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").isNumber());
        mockMvc.perform(get("/api/admin/users").header("Authorization", admin)).andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/items").header("Authorization", admin)).andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/claims/pending").header("Authorization", admin)).andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- items

    @Test
    void items_uploadImage_search_filter_sort_andPagination() throws Exception {
        String token = newUserToken("poster");
        String admin = adminToken();
        String tag = UUID.randomUUID().toString().substring(0, 8);
        String category = "cat" + tag;
        String location = "Library " + tag;

        // Normal user cannot post a FOUND item -> 403
        mockMvc.perform(itemForm(multipart("/api/items")
                                .file(new MockMultipartFile("image", "wallet.png", "image/png", PNG)),
                        "Brown Wallet " + tag, category, location, "FOUND")
                        .header("Authorization", token))
                .andExpect(status().isForbidden());

        // Admin posts the FOUND item
        MvcResult created = mockMvc.perform(itemForm(multipart("/api/admin/found-items")
                                .file(new MockMultipartFile("image", "wallet.png", "image/png", PNG)),
                        "Brown Wallet " + tag, category, location, "FOUND")
                        .header("Authorization", admin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.itemType").value("FOUND"))
                .andReturn();
        String imageUrl = json(created).at("/data/imageUrl").asText();
        assertTrue(imageUrl.startsWith("/api/items/images/"));
        createItem(token, "Red Umbrella " + tag, category, "Canteen " + tag, "LOST");

        // image is publicly downloadable
        mockMvc.perform(get(imageUrl)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));

        // search by name (public, no token)
        mockMvc.perform(get("/api/items").param("keyword", "wallet " + tag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
        // filters
        mockMvc.perform(get("/api/items").param("category", category))
                .andExpect(jsonPath("$.data.totalElements").value(2));
        mockMvc.perform(get("/api/items").param("category", category).param("location", "canteen"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(get("/api/items").param("category", category).param("type", "LOST"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(get("/api/items").param("category", category).param("status", "RETURNED"))
                .andExpect(jsonPath("$.data.totalElements").value(0));
        // pagination + sorting
        mockMvc.perform(get("/api/items").param("category", category).param("size", "1")
                        .param("sortBy", "itemName").param("direction", "asc"))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.content[0].itemName").value("Brown Wallet " + tag));
        // bad sort / bad enum -> 400
        mockMvc.perform(get("/api/items").param("sortBy", "password")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/items").param("type", "BROKEN")).andExpect(status().isBadRequest());
        // unknown item -> 404
        mockMvc.perform(get("/api/items/99999999")).andExpect(status().isNotFound());
    }

    @Test
    void items_validation_authentication_andImageRules() throws Exception {
        String token = newUserToken("rules");

        // no login -> 401
        mockMvc.perform(itemForm(multipart("/api/items"), "X", "c", "l", "LOST")).andExpect(status().isUnauthorized());

        // missing fields -> 400 with details
        mockMvc.perform(multipart("/api/items").param("itemName", "Only name").header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.category").exists())
                .andExpect(jsonPath("$.data.itemType").exists());

        // wrong file type -> 400
        mockMvc.perform(itemForm(multipart("/api/items")
                                .file(new MockMultipartFile("image", "evil.exe", "application/octet-stream", new byte[]{1, 2, 3})),
                        "Item", "Bags", "Gate", "LOST").header("Authorization", token))
                .andExpect(status().isBadRequest());

        // text renamed to .png -> 400
        mockMvc.perform(itemForm(multipart("/api/items")
                                .file(new MockMultipartFile("image", "fake.png", "image/png", "just text".getBytes())),
                        "Item", "Bags", "Gate", "LOST").header("Authorization", token))
                .andExpect(status().isBadRequest());

        // item without an image is fine
        mockMvc.perform(itemForm(multipart("/api/items"), "No Photo", "Bags", "Gate", "LOST").header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.imageUrl").doesNotExist());
    }

    @Test
    void items_onlyOwnerOrAdminCanEditAndDelete() throws Exception {
        String owner = newUserToken("owner");
        String other = newUserToken("other");
        long itemId = createItem(owner, "Phone", "Electronics", "Hostel", "LOST");

        mockMvc.perform(itemForm(multipart(HttpMethod.PUT, "/api/items/{id}", itemId), "Hacked", "Electronics", "Hostel", "LOST")
                        .header("Authorization", other))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/items/" + itemId).header("Authorization", other)).andExpect(status().isForbidden());

        mockMvc.perform(itemForm(multipart(HttpMethod.PUT, "/api/items/{id}", itemId), "Phone Updated", "Electronics", "Hostel", "LOST")
                        .header("Authorization", owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemName").value("Phone Updated"))
                .andExpect(jsonPath("$.data.imageUrl").exists());   // old image kept

        mockMvc.perform(delete("/api/items/" + itemId).header("Authorization", owner)).andExpect(status().isOk());
        mockMvc.perform(get("/api/items/" + itemId)).andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- claims + admin verification

    @Test
    void claimWorkflow_approveRejectsOthers_thenReturned_andRulesAreEnforced() throws Exception {
        String claimerA = newUserToken("claimerA");
        String claimerB = newUserToken("claimerB");
        String claimerC = newUserToken("claimerC");
        String admin = adminToken();
        long itemId = createItem(admin, "Student ID Card", "Documents", "Block A", "FOUND");

        // poster (admin) cannot claim own item
        mockMvc.perform(post("/api/claims").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":" + itemId + ",\"proofDescription\":\"This is my own card\"}"))
                .andExpect(status().isBadRequest());

        long claimA = submitClaim(claimerA, itemId);
        long claimB = submitClaim(claimerB, itemId);

        // duplicate pending claim -> 409
        mockMvc.perform(post("/api/claims").header("Authorization", claimerA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":" + itemId + ",\"proofDescription\":\"Second attempt for same item\"}"))
                .andExpect(status().isConflict());

        // claimant can see own claim (PENDING); a stranger cannot
        mockMvc.perform(get("/api/claims/" + claimA).header("Authorization", claimerA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("PENDING"));
        mockMvc.perform(get("/api/claims/" + claimA).header("Authorization", claimerC)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/claims/my").header("Authorization", claimerA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));

        // normal user cannot verify
        mockMvc.perform(patch("/api/admin/claims/" + claimA + "/verify").header("Authorization", claimerB)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());

        // rejecting requires remarks
        mockMvc.perform(patch("/api/admin/claims/" + claimB + "/verify").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isBadRequest());

        // admin approves A
        mockMvc.perform(patch("/api/admin/claims/" + claimA + "/verify").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\",\"adminRemarks\":\"Name matches the card\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.itemStatus").value("CLAIMED"));

        // B was closed automatically
        mockMvc.perform(get("/api/claims/" + claimB).header("Authorization", claimerB))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
        // approving the same claim twice fails
        mockMvc.perform(patch("/api/admin/claims/" + claimA + "/verify").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isBadRequest());
        // item is no longer claimable
        mockMvc.perform(post("/api/claims").header("Authorization", claimerC).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":" + itemId + ",\"proofDescription\":\"Late claim attempt here\"}"))
                .andExpect(status().isBadRequest());

        // notification for claimant A mentions approval and can be marked as read
        MvcResult notifications = mockMvc.perform(get("/api/notifications").header("Authorization", claimerA))
                .andExpect(status().isOk()).andReturn();
        JsonNode list = json(notifications).at("/data/content");
        boolean approvedMessage = false;
        for (JsonNode n : list) {
            approvedMessage |= n.get("message").asText().contains("APPROVED");
        }
        assertTrue(approvedMessage, "claimant should be notified about the approval");
        long notificationId = list.get(0).get("id").asLong();
        mockMvc.perform(patch("/api/notifications/" + notificationId + "/read").header("Authorization", claimerA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.read").value(true));
        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", claimerA))
                .andExpect(status().isOk());
        // other users cannot touch someone else's notification
        mockMvc.perform(patch("/api/notifications/" + notificationId + "/read").header("Authorization", claimerB))
                .andExpect(status().isNotFound());

        // mark as returned -> RETURNED, and nobody can claim it again
        mockMvc.perform(patch("/api/admin/items/" + itemId + "/returned").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("RETURNED"));
        mockMvc.perform(post("/api/claims").header("Authorization", claimerC).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":" + itemId + ",\"proofDescription\":\"Claim after returned\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/admin/items/" + itemId + "/returned").header("Authorization", admin))
                .andExpect(status().isBadRequest());

        // dashboard reflects the activity
        MvcResult stats = mockMvc.perform(get("/api/admin/dashboard").header("Authorization", admin)).andReturn();
        assertTrue(json(stats).at("/data/returnedItems").asLong() >= 1);
        assertEquals(true, json(stats).at("/success").asBoolean());
    }

    @Test
    void rejectedClaim_keepsItemOpen_andUserCanClaimAgain() throws Exception {
        String claimer = newUserToken("c2");
        String admin = adminToken();
        long itemId = createItem(admin, "Water Bottle", "Others", "Ground", "FOUND");
        long claimId = submitClaim(claimer, itemId);

        mockMvc.perform(patch("/api/admin/claims/" + claimId + "/verify").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\",\"adminRemarks\":\"Proof does not match\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.itemStatus").value("OPEN"));

        mockMvc.perform(get("/api/claims/" + claimId + "/status").header("Authorization", claimer))
                .andExpect(jsonPath("$.data.adminRemarks").value("Proof does not match"));

        // a new claim is allowed because the old one is no longer pending
        submitClaim(claimer, itemId);
    }
}
