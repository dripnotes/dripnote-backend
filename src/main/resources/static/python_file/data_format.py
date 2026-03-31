import re
import pandas as pd

INPUT_CSV = "beanbrothers_coffeewiki.csv"
OUTPUT_CSV = "beanbrothers_coffeewiki_narrative_full.csv"


def clean_text(text):
    if pd.isna(text):
        return ""
    return re.sub(r"\s+", " ", str(text)).strip().strip('"').strip("'")


def split_notes(text):
    text = clean_text(text)
    if not text:
        return []

    parts = re.split(r",|/|\||\n", text)
    notes = []
    for part in parts:
        part = re.sub(r"\([^)]*\)", "", part)
        part = clean_text(part)
        if part:
            notes.append(part)
    return notes


def has_batchim(word):
    word = clean_text(word)
    if not word:
        return False

    last = word[-1]
    if not ("가" <= last <= "힣"):
        return False

    return (ord(last) - ord("가")) % 28 != 0


def josa(word, pair):
    first, second = pair.split("/")
    return first if has_batchim(word) else second


def with_josa(word, pair):
    return f"{word}{josa(word, pair)}"


def first_note(notes, idx, default_value):
    return notes[idx] if len(notes) > idx else default_value


def parse_altitude_mean(altitude_text):
    text = clean_text(altitude_text)
    if not text:
        return None

    numbers = re.findall(r"\d[\d,]*", text)
    if not numbers:
        return None

    nums = [int(n.replace(",", "")) for n in numbers]
    return sum(nums) / len(nums)


def detect_note_families(notes):
    joined = " ".join(notes)

    groups = {
        "floral": ["자스민", "재스민", "라벤더", "제비꽃", "히비스커스", "오렌지 블로섬", "허니서클", "분홍 장미", "꽃", "플로럴", "라일락"],
        "citrus": ["베르가못", "오렌지", "레몬", "자몽", "감귤", "만다린", "시트러스", "핑크 자몽"],
        "berry": ["블루베리", "딸기", "라즈베리", "체리", "블랙커런트", "베리"],
        "stonefruit": ["복숭아", "천도복숭아", "살구", "자두", "백도", "건자두"],
        "tropical": ["망고", "구아바", "파인애플", "건망고", "파파야", "망고스틴", "리치", "패션프루트", "람부탄"],
        "tea_wine": ["홍차", "백차", "샴페인", "메를로", "리슬링", "와인"],
        "sweet": ["꿀", "아카시아꿀", "야생꿀", "메이플 시럽", "시럽", "야자당", "누가", "달콤한", "Sugary"],
        "choco_nut": ["초콜릿", "밀크초콜릿", "카카오", "코코아", "트러플", "피칸", "마카다미아", "아몬드", "헤이즐넛", "견과"],
    }

    found = set()
    for group, keywords in groups.items():
        if any(keyword in joined for keyword in keywords):
            found.add(group)

    return found


def process_expression(process):
    process = clean_text(process)

    if not process:
        return "가공 방식이 만드는 결도 과하지 않게 정돈되어"
    if "무산소" in process or "탄산" in process or "공동 발효" in process:
        return "발효에서 비롯된 입체적인 인상이 더해져 향미의 층위가 한층 깊어지고"
    if "워시드" in process:
        return "워시드 특유의 투명한 질감이 향미의 경계를 또렷하게 세워 주고"
    if "허니" in process:
        return "허니 프로세스가 단맛과 질감을 부드럽게 이어 주며 컵의 결을 매끈하게 다듬고"
    if "내추럴" in process or "펄프" in process:
        return "과실의 농도감이 자연스럽게 확장되며 보다 풍성한 단맛으로 이어지고"
    return "가공 방식이 지닌 개성이 컵 전반에 은은하게 스며들며"


def variety_expression(variety):
    variety = clean_text(variety)

    if not variety:
        return "품종이 지닌 고유한 개성 또한 컵의 균형을 안정감 있게 받쳐 줍니다."

    if "게이샤" in variety:
        return "게이샤 품종 특유의 섬세한 플로럴함과 길고 우아한 여운이 중심을 잡습니다."
    if "핑크 버번" in variety:
        return "핑크 버번이 보여 주는 화사한 향과 달콤한 산미가 컵의 인상을 더욱 세련되게 만듭니다."
    if "버번" in variety:
        return "버번 계열 특유의 부드러운 단맛과 둥근 질감이 전체 인상을 한층 매끄럽게 정리합니다."
    if "SL28" in variety or "SL34" in variety:
        return "SL 계열 품종이 만들어 내는 선명한 산미와 짙은 과실감이 컵의 중심을 또렷하게 세웁니다."
    if "루이루11" in variety or "바티안" in variety:
        return "케냐 계열 품종의 구조감이 더해져 산미와 단맛의 대비가 보다 분명하게 살아납니다."
    if "티피카" in variety:
        return "티피카 특유의 투명하고 우아한 균형이 컵 전체를 정갈하게 정리합니다."
    if "카투아이" in variety:
        return "카투아이 계열의 친숙한 단맛과 안정적인 밸런스가 편안한 인상을 더합니다."
    if "카투라" in variety:
        return "카투라가 지닌 정돈된 산미와 단맛의 균형이 컵을 안정감 있게 이끕니다."
    if "파카마라" in variety:
        return "파카마라 특유의 넓은 향의 폭과 구조감이 컵의 존재감을 한층 크게 만듭니다."
    if "마라카투라" in variety:
        return "마라카투라의 화려한 향과 넓은 질감이 보다 입체적인 인상을 완성합니다."
    if "에어룸" in variety or "토착종" in variety or "74158" in variety:
        return "에티오피아 계열 품종 특유의 야생화 같은 향과 복합성이 잔 안에서 자연스럽게 펼쳐집니다."
    if "카스티요" in variety:
        return "카스티요가 보여 주는 안정적인 단맛과 부드러운 컵 프로파일이 밸런스를 잘 받쳐 줍니다."
    if "아라라" in variety:
        return "아라라 품종이 지닌 밝고 경쾌한 과일 인상이 전체 향미를 한층 생기 있게 만듭니다."

    if "," in variety:
        return "여러 품종이 겹쳐 만들어 내는 복합성이 향미의 층을 보다 풍부하게 채워 줍니다."

    return f"{variety} 품종이 지닌 고유한 성격이 컵의 개성을 또렷하게 만들어 줍니다."


def altitude_expression(altitude):
    mean_alt = parse_altitude_mean(altitude)

    if mean_alt is None:
        return "고도에서 오는 인상도 전체 컵을 균형감 있게 받쳐 줍니다."

    if mean_alt >= 2000:
        return "높은 재배고도는 향을 더욱 선명하고 입체적으로 드러내며, 산미를 맑고 길게 이어지게 합니다."
    if mean_alt >= 1800:
        return "충분한 재배고도 덕분에 산미와 향의 윤곽이 뚜렷하게 살아나며 컵의 투명감이 좋아집니다."
    if mean_alt >= 1600:
        return "중고도의 재배 환경이 단맛과 산미의 균형을 잘 잡아 주며 부드러운 구조감을 만들어 냅니다."
    return "상대적으로 안정적인 재배고도에서 오는 편안한 단맛과 둥근 바디가 컵 전반에 자연스럽게 남습니다."


def provenance_expression(region, washing_station):
    region = clean_text(region)
    washing_station = clean_text(washing_station)

    if region and washing_station:
        return f"{region} 지역과 {washing_station}의 배경이 겹쳐진 이 커피에서는"
    if region:
        return f"{region} 지역의 개성이 담긴 이 커피에서는"
    if washing_station:
        return f"{washing_station}에서 정리된 이 커피에서는"
    return "이 커피에서는"


def finish_expression(notes, roasting):
    families = detect_note_families(notes)
    roasting = clean_text(roasting)

    if "floral" in families and ("citrus" in families or "stonefruit" in families):
        return "끝맛은 깨끗하고 화사하게 정리되며, 잔향에는 꽃 향기 같은 가벼운 울림이 오래 남습니다."
    if "choco_nut" in families:
        return "마무리는 차분하고 매끈하게 이어지며, 달콤쌉싸름한 뉘앙스가 안정감 있게 남습니다."
    if "berry" in families or "tropical" in families:
        return "후반부로 갈수록 과실의 농도감이 부드럽게 번지며 생기 있는 피니시를 남깁니다."

    if "라이트" in roasting:
        return "끝맛은 가볍고 투명하게 정리되며, 여운은 길고 깨끗하게 이어집니다."
    if "다크" in roasting:
        return "마무리는 보다 깊고 차분하며, 응축된 단맛의 여운이 인상적으로 남습니다."
    return "끝맛은 과하지 않게 정리되면서도 전체 인상은 또렷하게 오래 남습니다."


def recommendation_expression(notes):
    families = detect_note_families(notes)

    if "floral" in families and ("citrus" in families or "stonefruit" in families):
        return "섬세한 향의 결와 맑은 산미를 선호하시는 분들께 특히 인상적으로 다가갈 원두입니다."
    if "choco_nut" in families:
        return "편안한 단맛과 안정적인 바디를 선호하시는 분들께 만족도가 높습니다."
    if "berry" in families or "tropical" in families or "tea_wine" in families:
        return "화려한 과실감과 복합적인 향미를 즐기시는 분들께 좋은 선택이 됩니다."
    return "천천히 식혀 마실수록 표정이 달라지는 매력을 지닌 원두입니다."


def make_sensory_narrative(row):
    notes = split_notes(row.get("tasting_note", ""))
    process = clean_text(row.get("process", ""))
    variety = clean_text(row.get("variety", ""))
    altitude = clean_text(row.get("altitude", ""))
    washing_station = clean_text(row.get("washing_station", ""))
    region = clean_text(row.get("region", ""))
    roasting = clean_text(row.get("roasting", ""))

    n1 = first_note(notes, 0, "첫 향")
    n2 = first_note(notes, 1, "은은한 단맛")
    n3 = first_note(notes, 2, "산뜻한 여운")

    sentence1 = (
        f"{provenance_expression(region, washing_station)} "
        f"{with_josa(n1, '이/가')} 먼저 맑게 피어나고, "
        f"이어 {with_josa(n2, '이/가')} 부드럽게 겹쳐지며 첫인상을 형성합니다."
    )

    sentence2 = (
        f"{process_expression(process)} "
        f"중반부로 갈수록 {with_josa(n3, '을/를')} 떠올리게 하는 뉘앙스가 한층 또렷해집니다."
    )

    sentence3 = (
        f"{variety_expression(variety)} "
        f"{altitude_expression(altitude)}"
    )

    sentence4 = (
        f"{finish_expression(notes, roasting)} "
        f"{recommendation_expression(notes)}"
    )

    return " ".join([sentence1, sentence2, sentence3, sentence4])


def main():
    df = pd.read_csv(INPUT_CSV)

    df["sensory_narrative"] = df.apply(make_sensory_narrative, axis=1)

    df.to_csv(OUTPUT_CSV, index=False, encoding="utf-8-sig")
    print(f"저장 완료: {OUTPUT_CSV}")
    print()
    print(df[["name_ko", "sensory_narrative"]].head(5).to_string(index=False))


if __name__ == "__main__":
    main()