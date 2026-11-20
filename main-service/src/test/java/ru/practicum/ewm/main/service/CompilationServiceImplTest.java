package ru.practicum.ewm.main.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.main.dto.CompilationDto;
import ru.practicum.ewm.main.dto.NewCompilationRequest;
import ru.practicum.ewm.main.dto.UpdateCompilationRequest;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.model.Category;
import ru.practicum.ewm.main.model.Compilation;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.model.User;
import ru.practicum.ewm.main.repository.CompilationRepository;
import ru.practicum.ewm.main.repository.EventRepository;
import ru.practicum.ewm.main.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompilationServiceImplTest {

    @Mock
    private CompilationRepository compilationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private EventStatsService eventStatsService;

    @InjectMocks
    private CompilationServiceImpl compilationService;

    @Test
    void saveCompilation_shouldSaveCompilationWithEvents() {
        Event event = createEvent(10L);
        when(compilationRepository.existsByTitle("Летние концерты")).thenReturn(false);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        mockSave();

        CompilationDto result = compilationService.saveCompilation(
                new NewCompilationRequest(Set.of(10L), true, "Летние концерты"));

        assertEquals(1L, result.getId());
        assertEquals("Летние концерты", result.getTitle());
        assertTrue(result.isPinned());
        assertEquals(1, result.getEvents().size());
        assertEquals(10L, result.getEvents().get(0).getId());
    }

    @Test
    void saveCompilation_shouldSaveEmptyUnpinnedCompilation_whenEventsAndPinnedAreNotPassed() {
        when(compilationRepository.existsByTitle("Выходные в городе")).thenReturn(false);
        mockSave();

        CompilationDto result = compilationService.saveCompilation(
                new NewCompilationRequest(null, null, "Выходные в городе"));

        assertFalse(result.isPinned());
        assertTrue(result.getEvents().isEmpty());
    }

    @Test
    void saveCompilation_shouldThrowConflict_whenTitleAlreadyExists() {
        when(compilationRepository.existsByTitle("Летние концерты")).thenReturn(true);

        assertThrows(ConflictException.class, () -> compilationService.saveCompilation(
                new NewCompilationRequest(null, false, "Летние концерты")));

        verify(compilationRepository, never()).save(any(Compilation.class));
    }

    @Test
    void saveCompilation_shouldThrowNotFound_whenEventDoesNotExist() {
        when(compilationRepository.existsByTitle("Летние концерты")).thenReturn(false);
        when(eventRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> compilationService.saveCompilation(
                new NewCompilationRequest(Set.of(10L), false, "Летние концерты")));

        verify(compilationRepository, never()).save(any(Compilation.class));
    }

    @Test
    void deleteCompilation_shouldDeleteCompilation_whenItExists() {
        when(compilationRepository.findById(1L))
                .thenReturn(Optional.of(createCompilation(1L, "Летние концерты", true)));

        compilationService.deleteCompilation(1L);

        verify(compilationRepository).deleteById(1L);
    }

    @Test
    void deleteCompilation_shouldThrowNotFound_whenCompilationDoesNotExist() {
        when(compilationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> compilationService.deleteCompilation(1L));

        verify(compilationRepository, never()).deleteById(any());
    }

    @Test
    void updateCompilation_shouldChangeOnlyPassedFields() {
        Compilation compilation = createCompilation(1L, "Летние концерты", true);
        when(compilationRepository.findById(1L)).thenReturn(Optional.of(compilation));
        when(compilationRepository.existsByTitleAndIdNot("Зимние концерты", 1L)).thenReturn(false);

        CompilationDto result = compilationService.updateCompilation(1L,
                new UpdateCompilationRequest(null, null, "Зимние концерты"));

        assertEquals("Зимние концерты", result.getTitle());
        assertTrue(result.isPinned());
    }

    @Test
    void updateCompilation_shouldReplaceEvents_whenEventsArePassed() {
        Compilation compilation = createCompilation(1L, "Летние концерты", true);
        compilation.setEvents(new HashSet<>(Set.of(createEvent(10L))));
        when(compilationRepository.findById(1L)).thenReturn(Optional.of(compilation));
        when(eventRepository.findById(20L)).thenReturn(Optional.of(createEvent(20L)));

        CompilationDto result = compilationService.updateCompilation(1L,
                new UpdateCompilationRequest(Set.of(20L), null, null));

        assertEquals(1, result.getEvents().size());
        assertEquals(20L, result.getEvents().get(0).getId());
    }

    @Test
    void updateCompilation_shouldClearEvents_whenEmptySetIsPassed() {
        Compilation compilation = createCompilation(1L, "Летние концерты", true);
        compilation.setEvents(new HashSet<>(Set.of(createEvent(10L))));
        when(compilationRepository.findById(1L)).thenReturn(Optional.of(compilation));

        CompilationDto result = compilationService.updateCompilation(1L,
                new UpdateCompilationRequest(Set.of(), null, null));

        assertTrue(result.getEvents().isEmpty());
    }

    @Test
    void updateCompilation_shouldThrowConflict_whenTitleIsTakenByAnotherCompilation() {
        Compilation compilation = createCompilation(1L, "Летние концерты", true);
        when(compilationRepository.findById(1L)).thenReturn(Optional.of(compilation));
        when(compilationRepository.existsByTitleAndIdNot("Зимние концерты", 1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> compilationService.updateCompilation(1L,
                new UpdateCompilationRequest(null, null, "Зимние концерты")));

        assertEquals("Летние концерты", compilation.getTitle());
    }

    @Test
    void updateCompilation_shouldThrowNotFound_whenCompilationDoesNotExist() {
        when(compilationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> compilationService.updateCompilation(1L,
                new UpdateCompilationRequest(null, true, null)));
    }

    @Test
    void getCompilations_shouldReturnAllSortedById_whenPinnedIsNotPassed() {
        Compilation first = createCompilation(1L, "Первая", true);
        Compilation second = createCompilation(2L, "Вторая", false);
        Compilation third = createCompilation(3L, "Третья", true);
        when(compilationRepository.findAll()).thenReturn(List.of(third, first, second));

        List<CompilationDto> result = compilationService.getCompilations(null, 0, 10);

        assertEquals(3, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        assertEquals(3L, result.get(2).getId());
    }

    @Test
    void getCompilations_shouldReturnOnlyPinned_whenPinnedIsTrue() {
        Compilation first = createCompilation(1L, "Первая", true);
        Compilation second = createCompilation(2L, "Вторая", false);
        Compilation third = createCompilation(3L, "Третья", true);
        when(compilationRepository.findAll()).thenReturn(List.of(first, second, third));

        List<CompilationDto> result = compilationService.getCompilations(true, 0, 10);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(3L, result.get(1).getId());
    }

    @Test
    void getCompilations_shouldReturnOnlyNotPinned_whenPinnedIsFalse() {
        Compilation first = createCompilation(1L, "Первая", true);
        Compilation second = createCompilation(2L, "Вторая", false);
        when(compilationRepository.findAll()).thenReturn(List.of(first, second));

        List<CompilationDto> result = compilationService.getCompilations(false, 0, 10);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getId());
    }

    @Test
    void getCompilations_shouldCutPage_whenFromIsNotMultipleOfSize() {
        Compilation first = createCompilation(1L, "Первая", true);
        Compilation second = createCompilation(2L, "Вторая", true);
        Compilation third = createCompilation(3L, "Третья", true);
        when(compilationRepository.findAll()).thenReturn(List.of(first, second, third));

        List<CompilationDto> result = compilationService.getCompilations(null, 1, 1);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getId());
    }

    @Test
    void getCompilation_shouldReturnCompilationWithViews() {
        Compilation compilation = createCompilation(1L, "Летние концерты", true);
        compilation.setEvents(new HashSet<>(Set.of(createEvent(10L))));
        when(compilationRepository.findById(1L)).thenReturn(Optional.of(compilation));
        when(eventStatsService.getViews(any())).thenReturn(Map.of(10L, 4L));

        CompilationDto result = compilationService.getCompilation(1L);

        assertEquals(1, result.getEvents().size());
        assertEquals(4L, result.getEvents().get(0).getViews());
    }

    @Test
    void getCompilation_shouldThrowNotFound_whenCompilationDoesNotExist() {
        when(compilationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> compilationService.getCompilation(1L));
    }

    private Compilation createCompilation(Long id, String title, boolean pinned) {
        return Compilation.builder()
                .id(id)
                .title(title)
                .pinned(pinned)
                .events(new HashSet<>())
                .build();
    }

    private Event createEvent(Long id) {
        return Event.builder()
                .id(id)
                .title("Концерт группы")
                .annotation("Концерт под открытым небом")
                .category(Category.builder().id(2L).name("Концерты").build())
                .initiator(User.builder().id(1L).name("Иван Петров").email("ivan@practicummail.ru").build())
                .eventDate(LocalDateTime.now().plusDays(2))
                .paid(false)
                .build();
    }

    private void mockSave() {
        when(compilationRepository.save(any(Compilation.class))).thenAnswer(invocation -> {
            Compilation compilation = invocation.getArgument(0);
            compilation.setId(1L);
            return compilation;
        });
    }
}