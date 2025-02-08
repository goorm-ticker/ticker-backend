package com.goorm.ticker.waitlist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.ticker.restaurant.entity.Restaurant;
import com.goorm.ticker.restaurant.repository.RestaurantRepository;
import com.goorm.ticker.user.entity.User;
import com.goorm.ticker.user.repository.UserRepository;
import com.goorm.ticker.waitlist.dto.WaitListRequestDto;
import com.goorm.ticker.waitlist.entity.Status;
import com.goorm.ticker.waitlist.entity.WaitList;
import com.goorm.ticker.waitlist.repository.WaitListRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class WaitListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    WaitListRepository waitListRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    private MockHttpSession session;
    private Long testRestaurantId;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();

        User testUser = userRepository.save(User.builder()
                .loginId("testId")
                .name("testName")
                .password("testPassword")
                .build());

        Restaurant restaurant = restaurantRepository.save(Restaurant.builder()
                .restaurantName("test")
                .x("testX")
                .y("testY")
                .maxWaiting(10)
                .build());

        testRestaurantId = restaurant.getRestaurantId();
        session.setAttribute("user", testUser.getId());
    }

    @AfterEach
    void afterEach() {
        waitListRepository.deleteAll();
        userRepository.deleteAll();
        restaurantRepository.deleteAll();
    }

    @Test
    @DisplayName("대기열 등록 성공 - 200 반환")
    void registerWaiting_Success() throws Exception {
        WaitListRequestDto request = WaitListRequestDto.builder()
                .restaurantId(testRestaurantId)
                .build();

        MockHttpServletResponse response = mockMvc.perform(post("/waitlist")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .session(session))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("대기열 등록 실패 - 세션 없음 (401 Unauthorized)")
    void registerWaiting_Fail_NoSession() throws Exception {
        WaitListRequestDto request = WaitListRequestDto.builder()
                .restaurantId(testRestaurantId)
                .build();

        MockHttpServletResponse response = mockMvc.perform(post("/waitlist")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("대기열 등록 실패 - 존재하지 않는 식당 (404 Not Found)")
    void registerWaiting_Fail_InvalidRestaurant() throws Exception {
        Long invalidRestaurantId = 9999L;

        WaitListRequestDto request = WaitListRequestDto.builder()
                .restaurantId(invalidRestaurantId)
                .build();

        MockHttpServletResponse response = mockMvc.perform(post("/waitlist")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .session(session))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("대기열 등록 실패 - 이미 같은 식당에 대기 중 (400 Bad Request)")
    void registerWaiting_Fail_AlreadyWaiting() throws Exception {
        User user = userRepository.findAll().get(0);
        Restaurant restaurant = restaurantRepository.findAll().get(0);

        waitListRepository.save(WaitList.builder()
                .user(user)
                .restaurant(restaurant)
                .waitingNumber(1)
                .status(Status.WAITING)
                .build());

        WaitListRequestDto request = WaitListRequestDto.builder()
                .restaurantId(testRestaurantId)
                .build();

        MockHttpServletResponse response = mockMvc.perform(post("/waitlist")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .session(session))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("대기 순번 조회 성공 - 200 반환")
    void getUserWaiting_Success() throws Exception {
        WaitListRequestDto request = WaitListRequestDto.builder()
                .restaurantId(testRestaurantId)
                .build();

        mockMvc.perform(post("/waitlist")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .session(session));

        MockHttpServletResponse response = mockMvc.perform(get("/waitlist/{restaurantId}/position", testRestaurantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .session(session))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("대기 순번 조회 실패 - 세션 없음 (401 Unauthorized)")
    void getUserWaiting_Fail_NoSession() throws Exception {
        WaitListRequestDto request = WaitListRequestDto.builder()
                .restaurantId(testRestaurantId)
                .build();

        mockMvc.perform(post("/waitlist")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .session(session));

        MockHttpServletResponse response = mockMvc.perform(get("/waitlist/{restaurantId}/position", testRestaurantId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("대기 순번 조회 실패 - 잘못된 식당 ID (404 Not Found)")
    void getUserWaiting_Fail_InvalidRestaurantId() throws Exception {
        WaitListRequestDto request = WaitListRequestDto.builder()
                .restaurantId(testRestaurantId)
                .build();

        mockMvc.perform(post("/waitlist")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .session(session));

        MockHttpServletResponse response = mockMvc.perform(get("/waitlist/{restaurantId}/position", 9999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .session(session))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("대기 순번 조회 실패 - 대기열의 없는 사용자 (404 Not Found)")
    void getUserWaiting_Fail_UserNotWaitingList() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/waitlist/{restaurantId}/position", testRestaurantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .session(session))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("입장 완료 성공 - 200 반환")
    void completeWaiting_Success() throws Exception {
        WaitListRequestDto request = WaitListRequestDto.builder()
                .restaurantId(testRestaurantId)
                .build();

        mockMvc.perform(post("/waitlist")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .session(session));

        MockHttpServletResponse response = mockMvc.perform(patch("/waitlist/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .session(session))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString()).isEqualTo("식당 입장 완료되었습니다.");
    }

    @Test
    @DisplayName("입장 완료 실패 - 세션 없음 (401 Unauthorized)")
    void completeWaiting_Fail_NoSession() throws Exception {
        WaitListRequestDto request = WaitListRequestDto.builder()
                .restaurantId(testRestaurantId)
                .build();

        mockMvc.perform(post("/waitlist")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .session(session));

        MockHttpServletResponse response = mockMvc.perform(patch("/waitlist/complete")
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("입장 완료 실패 - 대기열의 없는 사용자 (404 Not Found)")
    void completeWaiting_Fail_UserNotInWaitingList() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(patch("/waitlist/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .session(session))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("대기 취소 성공 - 200 반환")
    void cancelWaiting_Success() throws Exception {
        WaitListRequestDto request = WaitListRequestDto.builder()
                .restaurantId(testRestaurantId)
                .build();

        mockMvc.perform(post("/waitlist")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .session(session));

        MockHttpServletResponse response = mockMvc.perform(patch("/waitlist/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .session(session))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString()).isEqualTo("대기열이 취소되었습니다.");
    }

    @Test
    @DisplayName("대기 취소 실패 - 세션 없음 (401 Unauthorized)")
    void cancelWaiting_Fail_NoSession() throws Exception {
        WaitListRequestDto request = WaitListRequestDto.builder()
                .restaurantId(testRestaurantId)
                .build();

        mockMvc.perform(post("/waitlist")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .session(session));

        MockHttpServletResponse response = mockMvc.perform(patch("/waitlist/cancel")
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("대기 취소 실패 - 대기열에 없는 사용자 (404 Not Found)")
    void cancelWaiting_Fail_UserNotInWaitingList() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(patch("/waitlist/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .session(session))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }
}
