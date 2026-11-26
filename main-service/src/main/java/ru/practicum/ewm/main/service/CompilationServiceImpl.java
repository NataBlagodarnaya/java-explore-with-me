package ru.practicum.ewm.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.dto.compilation.CompilationDto;
import ru.practicum.ewm.main.dto.compilation.NewCompilationRequest;
import ru.practicum.ewm.main.dto.compilation.UpdateCompilationRequest;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.mapper.CompilationMapper;
import ru.practicum.ewm.main.mapper.EventMapper;
import ru.practicum.ewm.main.model.Compilation;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.repository.CompilationRepository;
import ru.practicum.ewm.main.repository.EventRepository;
import ru.practicum.ewm.main.repository.RequestRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final EventStatsService eventStatsService;

    @Override
    @Transactional
    public CompilationDto saveCompilation(NewCompilationRequest request) {
        if (compilationRepository.existsByTitle(request.getTitle())) {
            throw new ConflictException("Compilation title already exists: " + request.getTitle());
        }
        Set<Event> events = findEvents(request.getEvents());
        Compilation saved = compilationRepository.save(CompilationMapper.toCompilation(request, events));
        log.info("Создана подборка id={}", saved.getId());
        return toDto(saved);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        findCompilation(compId);
        compilationRepository.deleteById(compId);
        log.info("Удалена подборка id={}", compId);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest request) {
        Compilation compilation = findCompilation(compId);

        if (request.getTitle() != null) {
            if (compilationRepository.existsByTitleAndIdNot(request.getTitle(), compId)) {
                throw new ConflictException("Compilation title already exists: " + request.getTitle());
            }
            compilation.setTitle(request.getTitle());
        }
        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }
        if (request.getEvents() != null) {
            compilation.setEvents(findEvents(request.getEvents()));
        }

        log.info("Обновлена подборка id={}", compId);
        return toDto(compilation);
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        List<Long> ids = compilationRepository.findIdsWithOffset(pinned == null,
                pinned != null && pinned, from, size);
        if (ids.isEmpty()) {
            return List.of();
        }
        return toDtos(compilationRepository.findAllWithEventsByIds(ids));
    }

    @Override
    public CompilationDto getCompilation(Long compId) {
        return toDto(findCompilation(compId));
    }

    private Compilation findCompilation(Long compId) {
        return compilationRepository.findByIdWithEvents(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
    }

    private Set<Event> findEvents(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Event> found = eventRepository.findAllByIdsWithDetails(eventIds);
        Set<Long> foundIds = found.stream()
                .map(Event::getId)
                .collect(Collectors.toSet());
        for (Long eventId : eventIds) {
            if (!foundIds.contains(eventId)) {
                throw new NotFoundException("Event with id=" + eventId + " was not found");
            }
        }
        return new HashSet<>(found);
    }

    private CompilationDto toDto(Compilation compilation) {
        return toDtos(List.of(compilation)).get(0);
    }

    private List<CompilationDto> toDtos(Collection<Compilation> compilations) {
        Set<Event> events = compilations.stream()
                .flatMap(compilation -> compilation.getEvents().stream())
                .collect(Collectors.toSet());
        Map<Long, Long> confirmed = requestRepository.countConfirmedByEventIds(
                events.stream().map(Event::getId).toList());
        Map<Long, Long> views = eventStatsService.getViews(events);

        return compilations.stream()
                .map(compilation -> CompilationMapper.toCompilationDto(compilation,
                        compilation.getEvents().stream()
                                .sorted(Comparator.comparing(Event::getId))
                                .map(event -> EventMapper.toEventShortDto(event,
                                        confirmed.getOrDefault(event.getId(), 0L),
                                        views.getOrDefault(event.getId(), 0L)))
                                .toList()))
                .toList();
    }
}