package ru.practicum.ewm.main.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewCompilationRequest {
    private Set<Long> events;

    private Boolean pinned = false;

    @NotBlank
    @Size(min = 1, max = 50)
    private String title;
}