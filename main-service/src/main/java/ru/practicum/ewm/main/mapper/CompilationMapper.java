package ru.practicum.ewm.main.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.main.dto.compilation.CompilationDto;
import ru.practicum.ewm.main.dto.compilation.NewCompilationRequest;
import ru.practicum.ewm.main.dto.event.EventShortDto;
import ru.practicum.ewm.main.model.Compilation;
import ru.practicum.ewm.main.model.Event;

import java.util.List;
import java.util.Set;

@UtilityClass
public class CompilationMapper {

    public static Compilation toCompilation(NewCompilationRequest request, Set<Event> events) {
        return Compilation.builder()
                .title(request.getTitle())
                .pinned(Boolean.TRUE.equals(request.getPinned()))
                .events(events)
                .build();
    }

    public static CompilationDto toCompilationDto(Compilation compilation, List<EventShortDto> events) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.isPinned())
                .events(events)
                .build();
    }
}
