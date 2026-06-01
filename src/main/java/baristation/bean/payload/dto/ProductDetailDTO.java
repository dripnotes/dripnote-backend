package baristation.bean.payload.dto;

import baristation.bean.enums.RoastingType;
import lombok.Builder;

import java.util.List;

@Builder
public record ProductDetailDTO(
        ProductSummaryDTO beanSummary,
        RoasterDTO roaster,
        RoastingType roastingType,
        List<FlavorNoteDTO> flavorNotes,
        String description,
        String productUrl,
        Integer agtronMin,
        Integer agtronMax,
        Double acidity,
        Double sweetness,
        Double body,
        Double balance,
        List<ProductImageDTO> images,
        boolean bookmarked
) {
}

