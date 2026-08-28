package com.hopestar.hfms.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Common audit fields exposed to the presentation layer for any DTO backed
 * by {@link com.hopestar.hfms.common.entity.BaseEntity}. Feature-module
 * DTOs (e.g. {@code StudentResponseDTO}) extend this class rather than
 * re-declaring these fields, keeping list/detail views consistent across
 * modules.
 * <p>
 * Annotated with {@code @SuperBuilder} (not plain {@code @Builder}) so
 * every subclass can itself use {@code @SuperBuilder} and inherit these
 * fields into its own builder — {@code @SuperBuilder} requires every
 * class in the hierarchy, including this one, to carry the annotation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class BaseAuditDTO {

    private Long id;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}

