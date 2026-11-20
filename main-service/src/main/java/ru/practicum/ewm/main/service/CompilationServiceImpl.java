package ru.practicum.ewm.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.dto.CompilationDto;
import ru.practicum.ewm.main.dto.EventShortDto;
import ru.practicum.ewm.main.dto.NewCompilationRequest;
import ru.practicum.ewm.main.dto.UpdateCompilationRequest;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.mapper.CompilationMapper;
import ru.practicum.ewm.main.mapper.EventMapper;
import ru.practicum.ewm.main.model.Compilation;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.model.RequestStatus;
import ru.practicum.ewm.main.repository.CompilationRepository;
import ru.practicum.ewm.main.repository.EventRepository;
import ru.practicum.ewm.main.repository.RequestRepository;

import java.util.*;

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
        return compilationRepository.findAll().stream()
                .filter(compilation -> pinned == null || compilation.isPinned() == pinned)
                .sorted(Comparator.comparing(Compilation::getId))
                .skip(from)
                .limit(size)
                .map(this::toDto)
                .toList();
    }

    @Override
    public CompilationDto getCompilation(Long compId) {
        return toDto(findCompilation(compId));
    }

    private Compilation findCompilation(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
    }

    private Set<Event> findEvents(Set<Long> eventIds) {
        Set<Event> events = new HashSet<>();
        if (eventIds == null) {
            return events;
        }
        for (Long eventId : eventIds) {
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
            events.add(event);
        }
        return events;
    }

    private CompilationDto toDto(Compilation compilation) {
        Map<Long, Long> views = eventStatsService.getViews(compilation.getEvents());
        List<EventShortDto> events = compilation.getEvents().stream()
                .sorted(Comparator.comparing(Event::getId))
                .map(event -> EventMapper.toEventShortDto(event,
                        requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED),
                        views.getOrDefault(event.getId(), 0L)))
                .toList();
        return CompilationMapper.toCompilationDto(compilation, events);
    }
}