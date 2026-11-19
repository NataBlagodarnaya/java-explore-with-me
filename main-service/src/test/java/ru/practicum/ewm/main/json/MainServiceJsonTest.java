package ru.practicum.ewm.main.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.ewm.main.dto.CategoryDto;
import ru.practicum.ewm.main.dto.EventFullDto;
import ru.practicum.ewm.main.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.main.dto.EventShortDto;
import ru.practicum.ewm.main.dto.LocationDto;
import ru.practicum.ewm.main.dto.NewCompilationRequest;
import ru.practicum.ewm.main.dto.NewEventRequest;
import ru.practicum.ewm.main.dto.ParticipationRequestDto;
import ru.practicum.ewm.main.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.main.dto.UserShortDto;
import ru.practicum.ewm.main.exception.ErrorResponse;
import ru.practicum.ewm.main.model.AdminStateAction;
import ru.practicum.ewm.main.model.EventState;
import ru.practicum.ewm.main.model.RequestUpdateStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JsonTest
class MainServiceJsonTest {

    private static final LocalDateTime EVENT_DATE = LocalDateTime.of(2030, 1, 15, 12, 30, 0);

    @Autowired
    private JacksonTester<NewEventRequest> newEventJson;

    @Autowired
    private JacksonTester<EventFullDto> fullEventJson;

    @Autowired
    private JacksonTester<EventShortDto> shortEventJson;

    @Autowired
    private JacksonTester<ParticipationRequestDto> requestJson;

    @Autowired
    private JacksonTester<NewCompilationRequest> newCompilationJson;

    @Autowired
    private JacksonTester<UpdateEventAdminRequest> adminUpdateJson;

    @Autowired
    private JacksonTester<EventRequestStatusUpdateRequest> statusUpdateJson;

    @Autowired
    private JacksonTester<ErrorResponse> errorJson;

    @Test
    void newEventRequest_shouldBeParsedWithDefaults_whenOptionalFieldsAreMissing() throws Exception {
        String content = "{\"annotation\": \"Концерт под открытым небом\", \"category\": 2, "
                + "\"description\": \"Большой концерт для всех желающих\", "
                + "\"eventDate\": \"2030-01-15 12:30:00\", "
                + "\"location\": {\"lat\": 55.75, \"lon\": 37.5}, "
                + "\"title\": \"Концерт группы\"}";

        NewEventRequest result = newEventJson.parseObject(content);

        assertEquals("Концерт под открытым небом", result.getAnnotation());
        assertEquals(2L, result.getCategory());
        assertEquals(EVENT_DATE, result.getEventDate());
        assertEquals(55.75f, result.getLocation().getLat());
        assertEquals(37.5f, result.getLocation().getLon());
        assertFalse(result.getPaid());
        assertEquals(0, result.getParticipantLimit());
        assertTrue(result.getRequestModeration());
    }

    @Test
    void newEventRequest_shouldKeepPassedValues_whenOptionalFieldsAreSet() throws Exception {
        String content = "{\"annotation\": \"Концерт под открытым небом\", \"category\": 2, "
                + "\"description\": \"Большой концерт для всех желающих\", "
                + "\"eventDate\": \"2030-01-15 12:30:00\", "
                + "\"location\": {\"lat\": 55.75, \"lon\": 37.5}, "
                + "\"paid\": true, \"participantLimit\": 15, \"requestModeration\": false, "
                + "\"title\": \"Концерт группы\"}";

        NewEventRequest result = newEventJson.parseObject(content);

        assertTrue(result.getPaid());
        assertEquals(15, result.getParticipantLimit());
        assertFalse(result.getRequestModeration());
    }

    @Test
    void newEventRequest_shouldFail_whenDateIsInIsoFormat() {
        String content = "{\"annotation\": \"Концерт под открытым небом\", \"category\": 2, "
                + "\"description\": \"Большой концерт для всех желающих\", "
                + "\"eventDate\": \"2030-01-15T12:30:00\", "
                + "\"location\": {\"lat\": 55.75, \"lon\": 37.5}, "
                + "\"title\": \"Концерт группы\"}";

        assertThrows(JsonProcessingException.class, () -> newEventJson.parseObject(content));
    }

    @Test
    void eventFullDto_shouldBeSerializedWithDatesInSpaceFormat() throws Exception {
        EventFullDto dto = EventFullDto.builder()
                .id(10L)
                .annotation("Концерт под открытым небом")
                .category(new CategoryDto(2L, "Концерты"))
                .confirmedRequests(3L)
                .createdOn(LocalDateTime.of(2029, 12, 1, 10, 0, 0))
                .description("Большой концерт для всех желающих")
                .eventDate(EVENT_DATE)
                .initiator(new UserShortDto(1L, "Иван Петров"))
                .location(new LocationDto(55.75f, 37.5f))
                .paid(false)
                .participantLimit(10)
                .publishedOn(LocalDateTime.of(2029, 12, 2, 9, 0, 0))
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .title("Концерт группы")
                .views(7L)
                .build();

        JsonContent<EventFullDto> result = fullEventJson.write(dto);

        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(10);
        assertThat(result).extractingJsonPathStringValue("$.eventDate").isEqualTo("2030-01-15 12:30:00");
        assertThat(result).extractingJsonPathStringValue("$.createdOn").isEqualTo("2029-12-01 10:00:00");
        assertThat(result).extractingJsonPathStringValue("$.publishedOn").isEqualTo("2029-12-02 09:00:00");
        assertThat(result).extractingJsonPathStringValue("$.state").isEqualTo("PUBLISHED");
        assertThat(result).extractingJsonPathStringValue("$.category.name").isEqualTo("Концерты");
        assertThat(result).extractingJsonPathStringValue("$.initiator.name").isEqualTo("Иван Петров");
        assertThat(result).extractingJsonPathNumberValue("$.location.lat").isEqualTo(55.75);
        assertThat(result).extractingJsonPathNumberValue("$.location.lon").isEqualTo(37.5);
        assertThat(result).extractingJsonPathBooleanValue("$.paid").isFalse();
        assertThat(result).extractingJsonPathNumberValue("$.participantLimit").isEqualTo(10);
        assertThat(result).extractingJsonPathBooleanValue("$.requestModeration").isTrue();
        assertThat(result).extractingJsonPathNumberValue("$.confirmedRequests").isEqualTo(3);
        assertThat(result).extractingJsonPathNumberValue("$.views").isEqualTo(7);
    }

    @Test
    void eventFullDto_shouldHaveEmptyPublishedOn_whenEventIsNotPublished() throws Exception {
        EventFullDto dto = EventFullDto.builder()
                .id(10L)
                .eventDate(EVENT_DATE)
                .state(EventState.PENDING)
                .build();

        JsonContent<EventFullDto> result = fullEventJson.write(dto);

        assertThat(result).hasEmptyJsonPathValue("$.publishedOn");
        assertThat(result).extractingJsonPathStringValue("$.state").isEqualTo("PENDING");
    }

    @Test
    void eventShortDto_shouldBeSerializedWithDateInSpaceFormat() throws Exception {
        EventShortDto dto = EventShortDto.builder()
                .id(10L)
                .annotation("Концерт под открытым небом")
                .category(new CategoryDto(2L, "Концерты"))
                .confirmedRequests(3L)
                .eventDate(EVENT_DATE)
                .initiator(new UserShortDto(1L, "Иван Петров"))
                .paid(true)
                .title("Концерт группы")
                .views(7L)
                .build();

        JsonContent<EventShortDto> result = shortEventJson.write(dto);

        assertThat(result).extractingJsonPathStringValue("$.eventDate").isEqualTo("2030-01-15 12:30:00");
        assertThat(result).extractingJsonPathStringValue("$.title").isEqualTo("Концерт группы");
        assertThat(result).extractingJsonPathBooleanValue("$.paid").isTrue();
        assertThat(result).extractingJsonPathNumberValue("$.views").isEqualTo(7);
        assertThat(result).extractingJsonPathNumberValue("$.confirmedRequests").isEqualTo(3);
        assertThat(result).extractingJsonPathStringValue("$.initiator.name").isEqualTo("Иван Петров");
    }

    @Test
    void participationRequestDto_shouldBeSerializedWithIsoDate() throws Exception {
        ParticipationRequestDto dto = ParticipationRequestDto.builder()
                .id(3L)
                .created(EVENT_DATE)
                .event(1L)
                .requester(2L)
                .status("PENDING")
                .build();

        JsonContent<ParticipationRequestDto> result = requestJson.write(dto);

        assertThat(result).extractingJsonPathStringValue("$.created").isEqualTo("2030-01-15T12:30:00");
        assertThat(result).extractingJsonPathNumberValue("$.event").isEqualTo(1);
        assertThat(result).extractingJsonPathNumberValue("$.requester").isEqualTo(2);
        assertThat(result).extractingJsonPathStringValue("$.status").isEqualTo("PENDING");
    }

    @Test
    void newCompilationRequest_shouldRemoveDuplicateEventsAndSetDefaultPinned() throws Exception {
        String content = "{\"events\": [1, 2, 2], \"title\": \"Летние концерты\"}";

        NewCompilationRequest result = newCompilationJson.parseObject(content);

        assertEquals(2, result.getEvents().size());
        assertTrue(result.getEvents().containsAll(List.of(1L, 2L)));
        assertFalse(result.getPinned());
        assertEquals("Летние концерты", result.getTitle());
    }

    @Test
    void updateEventAdminRequest_shouldParseStateActionAndLeaveOtherFieldsEmpty() throws Exception {
        UpdateEventAdminRequest result = adminUpdateJson.parseObject("{\"stateAction\": \"PUBLISH_EVENT\"}");

        assertEquals(AdminStateAction.PUBLISH_EVENT, result.getStateAction());
        assertNull(result.getTitle());
        assertNull(result.getEventDate());
        assertNull(result.getPaid());
    }

    @Test
    void updateEventAdminRequest_shouldParseEventDateInSpaceFormat() throws Exception {
        UpdateEventAdminRequest result = adminUpdateJson.parseObject("{\"eventDate\": \"2030-01-15 12:30:00\"}");

        assertEquals(EVENT_DATE, result.getEventDate());
    }

    @Test
    void eventRequestStatusUpdateRequest_shouldParseIdsAndStatus() throws Exception {
        EventRequestStatusUpdateRequest result =
                statusUpdateJson.parseObject("{\"requestIds\": [11, 12], \"status\": \"CONFIRMED\"}");

        assertEquals(List.of(11L, 12L), result.getRequestIds());
        assertEquals(RequestUpdateStatus.CONFIRMED, result.getStatus());
    }

    @Test
    void errorResponse_shouldBeSerializedWithAllFieldsAndSpaceDateFormat() throws Exception {
        ErrorResponse dto = ErrorResponse.builder()
                .errors(List.of())
                .message("Event with id=99 was not found")
                .reason("The required object was not found.")
                .status("NOT_FOUND")
                .timestamp(EVENT_DATE)
                .build();

        JsonContent<ErrorResponse> result = errorJson.write(dto);

        assertThat(result).extractingJsonPathStringValue("$.status").isEqualTo("NOT_FOUND");
        assertThat(result).extractingJsonPathStringValue("$.reason").isEqualTo("The required object was not found.");
        assertThat(result).extractingJsonPathStringValue("$.message").isEqualTo("Event with id=99 was not found");
        assertThat(result).extractingJsonPathStringValue("$.timestamp").isEqualTo("2030-01-15 12:30:00");
        assertThat(result).extractingJsonPathArrayValue("$.errors").isEmpty();
    }
}