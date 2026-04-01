package dripnote.home.dto;

import java.util.List;

public record HomeResponseDTO(
        List<TastingsDTO> tastings,
        List<BeansDTO> beans
) {
}
