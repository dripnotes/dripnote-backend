package dripnote.home.payload.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record HomeBeanDTO(
        String bean_name,
        List<String> bean_tasting,
        String bean_image_link,
        String bean_link
) {
}
