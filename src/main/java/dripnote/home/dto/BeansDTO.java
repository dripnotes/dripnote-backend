package dripnote.home.dto;

import java.util.List;

public record BeansDTO(
        String bean_name,
        List<String> bean_tasting,
        String bean_image_link,
        String bean_link
) {
}
