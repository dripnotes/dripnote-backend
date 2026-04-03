package dripnote.home.service;

import dripnote.bean.domain.Bean;
import dripnote.bean.domain.BeanImage;
import dripnote.bean.domain.BeanTastingNote;
import dripnote.bean.domain.TastingNote;
import dripnote.bean.enums.ImageType;
import dripnote.bean.repository.BeanImagesRepository;
import dripnote.bean.repository.BeanTastingNotesRepository;
import dripnote.bean.repository.BeansRepository;
import dripnote.bean.repository.TastingNoteRepository;
import dripnote.home.payload.dto.HomeBeanDTO;
import dripnote.home.payload.response.HomeResponse;
import dripnote.home.payload.dto.HomeTastingsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final BeansRepository beansRepository;
    private final TastingNoteRepository tastingNoteRepository;
    private final BeanTastingNotesRepository beanTastingNotesRepository;
    private final BeanImagesRepository beanImagesRepository;

    // 메인 페이지 향미, 원두 정보 전송
    public HomeResponse getHome() {
        List<HomeTastingsDTO> tastings = getTastings();
        List<HomeBeanDTO> beans = getBeans();

        return new HomeResponse(tastings, beans);
    }

    // 향미 목록 조회
    public List<HomeTastingsDTO> getTastings() {
        List<TastingNote> tastingNotes = tastingNoteRepository.findTop4ByOrderByTastingNoteIdAsc();

        return tastingNotes.stream()
                .map(tastingNote -> HomeTastingsDTO.builder()
                        .tasting_name(tastingNote.getNameKo())
                        .tasting_link("/bean?tastingId=" + tastingNote.getTastingNoteId())
                        .build()
                ).toList();
    }

    // 원두 목록 조회
    private List<HomeBeanDTO> getBeans() {
        List<Bean> beans = beansRepository.findTop4ByOrderByCreatedAtDesc();

        if (beans.isEmpty()) {
            return List.of();
        }

        List<Long> beanIds = beans.stream()
                .map(Bean::getBeanId)
                .toList();

        List<BeanTastingNote> beanTastingNotes =
                beanTastingNotesRepository.findByBean_BeanIdIn(beanIds);

        List<BeanImage> beanImages =
                beanImagesRepository.findByBean_BeanIdInAndImageType(beanIds, ImageType.MAIN);

        Map<Long, List<String>> beanTastingMap = new LinkedHashMap<>();
        for (BeanTastingNote beanTastingNote : beanTastingNotes) {
            Long beanId = beanTastingNote.getBean().getBeanId();
            String tastingName = beanTastingNote.getTastingNote().getNameKo();

            beanTastingMap
                    .computeIfAbsent(beanId, key -> new ArrayList<>())
                    .add(tastingName);
        }

        Map<Long, String> beanMainImageMap = new LinkedHashMap<>();
        for (BeanImage beanImage : beanImages) {
            Long beanId = beanImage.getBean().getBeanId();

            beanMainImageMap.putIfAbsent(beanId, beanImage.getImageUrl());
        }

        return beans.stream()
                .map(bean -> HomeBeanDTO.builder()
                        .bean_name(bean.getNameKo())
                        .bean_tasting(beanTastingMap.getOrDefault(bean.getBeanId(), List.of()))
                        .bean_link("/beans/detail/" + bean.getBeanId())
                        .bean_image_link(beanMainImageMap.get(bean.getBeanId()))
                        .build()
                ).toList();
    }
}
