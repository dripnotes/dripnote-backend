package dripnote.home.payload.response;

import dripnote.home.payload.dto.HomeBeanDTO;
import dripnote.home.payload.dto.HomeTastingsDTO;

import java.util.List;

public record HomeResponse(
        List<HomeTastingsDTO> tastings,
        List<HomeBeanDTO> beans
) {
}
