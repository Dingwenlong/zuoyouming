package com.library.seat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.library.seat.modules.reservation.entity.Appeal;
import com.library.seat.modules.reservation.entity.Reservation;
import com.library.seat.modules.seat.entity.Seat;
import com.library.seat.modules.sys.entity.SysUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
// @Transactional // H2 In-Memory so we might not want rollback if we rely on state between tests? 
// Actually, standard JUnit tests should be independent, but here we are simulating a flow.
// If we use @Transactional, changes are rolled back after each test.
// Since we want to test a "flow" across methods, we should NOT use @Transactional at class level, 
// OR we write one big test method.
// Writing one big test method is safer for "flow" testing without complex setup/teardown.
public class SystemFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOperations;
    
    @MockBean
    private SimpMessagingTemplate simpMessagingTemplate;

    private static String adminToken;
    private static String studentToken;
    private static Long studentUserId;
    private static Long seatId;
    private static Long reservationId;

    @BeforeEach
    public void setup() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
    }

    @Test
    @Order(1)
    public void testFullSystemFlow() throws Exception {
        // ==========================================
        // 1. Admin Login & User Management
        // ==========================================
        
        // 1.1 Admin Login
        Map<String, String> loginMap = new HashMap<>();
        loginMap.put("username", "admin");
        loginMap.put("password", "123456");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.toJSONString(loginMap)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        JSONObject loginResp = JSON.parseObject(loginResult.getResponse().getContentAsString());
        adminToken = loginResp.getJSONObject("data").getString("token");
        System.out.println("Admin Token: " + adminToken);

        // 1.2 Create Student User
        SysUser student = new SysUser();
        student.setUsername("student_test");
        student.setPassword("123456");
        student.setRealName("Test Student");
        student.setRole("student");
        student.setStatus("active");

        mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.toJSONString(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 1.3 Verify Student Exists
        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken)
                .param("username", "student_test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].username").value("student_test"));

        // ==========================================
        // 2. Seat Management (Admin)
        // ==========================================

        // 2.1 Get Seat Map
        MvcResult seatResult = mockMvc.perform(get("/api/v1/seats/map")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].seatNo").value("A-01"))
                .andReturn();
        
        JSONObject seatResp = JSON.parseObject(seatResult.getResponse().getContentAsString());
        seatId = seatResp.getJSONArray("data").getJSONObject(0).getLong("id");
        System.out.println("Seat ID: " + seatId);

        // 2.2 Maintenance Mode
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("status", "maintenance");
        mockMvc.perform(put("/api/v1/seats/" + seatId + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.toJSONString(statusMap)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        
        // 2.3 Restore to Available
        statusMap.put("status", "available");
        mockMvc.perform(put("/api/v1/seats/" + seatId + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.toJSONString(statusMap)))
                .andExpect(status().isOk());

        // ==========================================
        // 3. Student Reservation Lifecycle
        // ==========================================

        // 3.1 Student Login
        loginMap.put("username", "student_test");
        loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.toJSONString(loginMap)))
                .andExpect(status().isOk())
                .andReturn();
        
        loginResp = JSON.parseObject(loginResult.getResponse().getContentAsString());
        studentToken = loginResp.getJSONObject("data").getString("token");
        studentUserId = loginResp.getJSONObject("data").getJSONObject("userInfo").getLong("id");
        System.out.println("Student Token: " + studentToken);

        // 3.2 Reserve Seat
        Reservation reservation = new Reservation();
        reservation.setSeatId(seatId);
        reservation.setStartTime(new java.util.Date());
        reservation.setEndTime(new java.util.Date(System.currentTimeMillis() + 3600000)); // +1 hour
        
        mockMvc.perform(post("/api/v1/reservations")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.toJSONString(reservation)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        // 3.3 Check History to get Reservation ID
        MvcResult historyResult = mockMvc.perform(get("/api/v1/reservations/my-history")
                .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andReturn();
        
        JSONObject historyResp = JSON.parseObject(historyResult.getResponse().getContentAsString());
        reservationId = historyResp.getJSONArray("data").getJSONObject(0).getLong("id");
        System.out.println("Reservation ID: " + reservationId);

        // 3.4 Check In
        mockMvc.perform(post("/api/v1/reservations/" + reservationId + "/check-in")
                .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        // 3.5 Temp Leave
        mockMvc.perform(post("/api/v1/reservations/" + reservationId + "/leave")
                .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        // 3.6 Release
        mockMvc.perform(post("/api/v1/reservations/" + reservationId + "/release")
                .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        // ==========================================
        // 4. Violation & Appeal
        // ==========================================
        
        // 4.1 Reserve Again (same seat should be available now)
        mockMvc.perform(post("/api/v1/reservations")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.toJSONString(reservation)))
                .andExpect(status().isOk());
        
        // Get new ID
        historyResult = mockMvc.perform(get("/api/v1/reservations/my-history")
                .header("Authorization", "Bearer " + studentToken))
                .andReturn();
        historyResp = JSON.parseObject(historyResult.getResponse().getContentAsString());
        Long newResId = historyResp.getJSONArray("data").getJSONObject(0).getLong("id");

        // 4.2 Appeal (Assuming violation happened - though we can't easily force violation state via API without waiting)
        // We will just test the Appeal API connectivity.
        Appeal appeal = new Appeal();
        appeal.setReason("I was late because of traffic");
        
        mockMvc.perform(post("/api/v1/reservations/" + newResId + "/appeal")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.toJSONString(appeal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        // ==========================================
        // 5. Stats (Admin)
        // ==========================================
        mockMvc.perform(get("/api/v1/stats/dashboard")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
