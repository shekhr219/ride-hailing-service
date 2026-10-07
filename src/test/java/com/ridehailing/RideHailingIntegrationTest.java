package com.ridehailing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.dto.*;
import com.ridehailing.model.CarType;
import com.ridehailing.model.DiscountType;
import com.ridehailing.repository.CouponRepository;
import com.ridehailing.repository.DriverRepository;
import com.ridehailing.repository.RideRepository;
import com.ridehailing.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RideHailingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private CouponRepository couponRepository;

    @BeforeEach
    void setUp() {
        userRepository.clear();
        driverRepository.clear();
        rideRepository.clear();
        couponRepository.clear();
    }

    @Test
    @DisplayName("Complete E2E REST Flow: Register -> Book -> End -> Verify History")
    void testCompleteEndToEndFlow() throws Exception {
        // 1. Register User
        UserRegistrationRequest userReq = new UserRegistrationRequest("Alice", "9876543210");
        MvcResult userResult = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andReturn();

        JsonNode userJson = objectMapper.readTree(userResult.getResponse().getContentAsString());
        String userId = userJson.get("id").asText();

        // 2. Register Driver
        DriverRegistrationRequest driverReq = new DriverRegistrationRequest(
                "Bob Driver", "9876543211", CarType.SEDAN, 4.9, 12.9716, 77.5946);
        MvcResult driverResult = mockMvc.perform(post("/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(driverReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andReturn();

        JsonNode driverJson = objectMapper.readTree(driverResult.getResponse().getContentAsString());
        String driverId = driverJson.get("id").asText();

        // 3. Update Driver Location
        UpdateLocationRequest locReq = new UpdateLocationRequest(12.9720, 77.5950);
        mockMvc.perform(patch("/drivers/" + driverId + "/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(locReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentLocation.latitude").value(12.9720));

        // 4. Add Coupon
        CouponRequest couponReq = new CouponRequest("SAVE10", DiscountType.PERCENTAGE, new BigDecimal("10.00"), true);
        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(couponReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SAVE10"));

        // 5. Book Ride
        BookRideRequest bookReq = new BookRideRequest(userId, 12.9720, 77.5950, CarType.SEDAN);
        MvcResult bookResult = mockMvc.perform(post("/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rideId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("ONGOING"))
                .andExpect(jsonPath("$.driverId").value(driverId))
                .andExpect(jsonPath("$.requestedCarType").value("SEDAN"))
                .andExpect(jsonPath("$.actualCarType").value("SEDAN"))
                .andReturn();

        JsonNode rideJson = objectMapper.readTree(bookResult.getResponse().getContentAsString());
        String rideId = rideJson.get("rideId").asText();

        // Verify driver is now ON_RIDE
        mockMvc.perform(get("/drivers/" + driverId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ON_RIDE"));

        // 6. End Ride (~7 km away)
        EndRideRequest endReq = new EndRideRequest(driverId, 13.0346, 77.5950, "SAVE10");
        mockMvc.perform(post("/rides/" + rideId + "/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(endReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.distanceKm").isNumber())
                .andExpect(jsonPath("$.fare").isNumber())
                .andExpect(jsonPath("$.discountAmount").isNumber())
                .andExpect(jsonPath("$.finalFare").isNumber());

        // Verify driver is now AVAILABLE again
        mockMvc.perform(get("/drivers/" + driverId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        // 7. Verify User and Driver Ride History
        mockMvc.perform(get("/users/" + userId + "/rides"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].rideId").value(rideId))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));

        mockMvc.perform(get("/drivers/" + driverId + "/rides"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].rideId").value(rideId))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));

        // 8. Delete Coupon
        mockMvc.perform(delete("/coupons/SAVE10"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Hatchback Fallback via REST API")
    void testHatchbackFallbackViaRestApi() throws Exception {
        // Register User
        UserRegistrationRequest userReq = new UserRegistrationRequest("Charlie", "9876543222");
        MvcResult userResult = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String userId = objectMapper.readTree(userResult.getResponse().getContentAsString()).get("id").asText();

        // Register ONLY a SEDAN driver
        DriverRegistrationRequest driverReq = new DriverRegistrationRequest(
                "Dave Sedan", "9876543233", CarType.SEDAN, 4.8, 12.9716, 77.5946);
        MvcResult driverResult = mockMvc.perform(post("/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(driverReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String driverId = objectMapper.readTree(driverResult.getResponse().getContentAsString()).get("id").asText();

        // User requests HATCHBACK -> should fallback to SEDAN
        BookRideRequest bookReq = new BookRideRequest(userId, 12.9716, 77.5946, CarType.HATCHBACK);
        mockMvc.perform(post("/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.driverId").value(driverId))
                .andExpect(jsonPath("$.requestedCarType").value("HATCHBACK"))
                .andExpect(jsonPath("$.actualCarType").value("SEDAN"))
                .andExpect(jsonPath("$.status").value("ONGOING"));
    }

    @Test
    @DisplayName("Cancel Ride via REST API")
    void testCancelRideViaRestApi() throws Exception {
        // Register User
        UserRegistrationRequest userReq = new UserRegistrationRequest("Eve", "9876543255");
        MvcResult userResult = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String userId = objectMapper.readTree(userResult.getResponse().getContentAsString()).get("id").asText();

        // Register Driver
        DriverRegistrationRequest driverReq = new DriverRegistrationRequest(
                "Frank", "9876543266", CarType.SEDAN, 4.7, 12.9716, 77.5946);
        mockMvc.perform(post("/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(driverReq)))
                .andExpect(status().isCreated());

        // Book Ride
        BookRideRequest bookReq = new BookRideRequest(userId, 12.9716, 77.5946, CarType.SEDAN);
        MvcResult bookResult = mockMvc.perform(post("/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String rideId = objectMapper.readTree(bookResult.getResponse().getContentAsString()).get("rideId").asText();

        // Cancel Ride
        mockMvc.perform(post("/rides/" + rideId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationFee").value(25.00))
                .andExpect(jsonPath("$.finalFare").value(25.00));
    }
}
