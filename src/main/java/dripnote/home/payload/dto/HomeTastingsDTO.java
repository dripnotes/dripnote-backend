package dripnote.home.payload.dto;

import lombok.Builder;

@Builder
public record HomeTastingsDTO(
        String tasting_name,
        String tasting_link
) {
}