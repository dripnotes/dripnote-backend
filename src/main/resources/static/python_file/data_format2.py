import re
import pandas as pd

INPUT_CSV = "beanbrothers_coffewiki_data.csv"
OUTPUT_CSV = "beanbrothers_coffeewiki_data_v2.csv"


def clean_text(text):
    if pd.isna(text):
        return ""
    return re.sub(r"\s+", " ", str(text)).strip().strip('"').strip("'")


def clamp(value, low=0, high=100):
    return max(low, min(high, round(value)))


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


def parse_altitude_mean(altitude_text):
    text = clean_text(altitude_text)
    if not text:
        return None

    numbers = re.findall(r"\d[\d,]*", text)
    if not numbers:
        return None

    nums = [int(n.replace(",", "")) for n in numbers]
    return sum(nums) / len(nums)


def normalize_roasting(roasting):
    text = clean_text(roasting).lower()

    if not text:
        return ""

    if "medium light" in text or "미디엄 라이트" in text:
        return "미디엄 라이트"
    if "medium dark" in text or "미디엄 다크" in text:
        return "미디엄 다크"
    if text == "medium" or text == "미디엄":
        return "미디엄"
    if text == "light" or text == "라이트":
        return "라이트"
    if text == "dark" or text == "다크":
        return "다크"

    if "라이트" in text and "미디엄" not in text:
        return "라이트"
    if "다크" in text and "미디엄" not in text:
        return "다크"
    if "미디엄" in text and "라이트" in text:
        return "미디엄 라이트"
    if "미디엄" in text and "다크" in text:
        return "미디엄 다크"
    if "미디엄" in text:
        return "미디엄"

    return ""


def normalize_process(process):
    text = clean_text(process).lower()

    if not text:
        return ""

    if "anaerobic" in text or "무산소" in text or "탄산" in text or "carbonic" in text:
        return "무산소/실험가공"
    if "washed" in text or "워시드" in text:
        return "워시드"
    if "honey" in text or "허니" in text:
        return "허니"
    if "natural" in text or "내추럴" in text:
        return "내추럴"
    if "pulped" in text or "펄프드" in text:
        return "펄프드 내추럴"

    return clean_text(process)


def detect_note_groups(notes):
    joined = " ".join(notes).lower()

    groups = {
        "floral": [
            "자스민", "재스민", "라벤더", "제비꽃", "꽃", "플로럴",
            "오렌지 블로섬", "오렌지블라썸", "아카시아", "허니서클",
            "장미", "로즈", "히비스커스"
        ],
        "sharp_citrus": [
            "레몬", "자몽", "핑크 자몽", "핑크자몽", "유자", "라임"
        ],
        "soft_citrus": [
            "베르가못", "오렌지", "감귤", "귤", "만다린", "시트러스"
        ],
        "berry": [
            "블루베리", "딸기", "라즈베리", "체리", "베리",
            "블랙커런트", "크랜베리"
        ],
        "stonefruit": [
            "복숭아", "천도복숭아", "살구", "자두", "백도", "건자두", "넥타린"
        ],
        "tropical": [
            "망고", "건망고", "구아바", "핑크 구아바", "핑크구아바",
            "파인애플", "리치", "패션프루트", "패션후르츠",
            "파파야", "람부탄", "망고스틴"
        ],
        "sweet": [
            "꿀", "야생꿀", "아카시아꿀", "브라운 슈거", "브라운슈가",
            "캐러멜", "카라멜", "시럽", "메이플", "누가",
            "설탕", "달콤한", "sugary", "슈가리"
        ],
        "choco_nut": [
            "초콜릿", "밀크초콜릿", "다크초콜릿", "카카오", "코코아",
            "트러플", "아몬드", "헤이즐넛", "피칸", "마카다미아",
            "호두", "견과", "피넛버터"
        ],
        "tea_wine": [
            "홍차", "백차", "얼그레이", "와인", "샴페인",
            "메를로", "리슬링"
        ],
        "grain_roasty": [
            "구운 보리", "누룽지", "토스트", "곡물", "오트밀", "미숫가루"
        ],
    }

    found = {}
    for group, keywords in groups.items():
        hit_count = 0
        for keyword in keywords:
            if keyword.lower() in joined:
                hit_count += 1
        if hit_count > 0:
            found[group] = min(hit_count, 2)

    return found


def weighted_mean(pairs, default_value=50):
    valid = [(v, w) for v, w in pairs if v is not None and w > 0]
    if not valid:
        return default_value

    total_weight = sum(w for _, w in valid)
    return sum(v * w for v, w in valid) / total_weight


def roast_level_pct(roasting):
    roasting = normalize_roasting(roasting)

    mapping = {
        "라이트": 24,
        "미디엄 라이트": 39,
        "미디엄": 55,
        "미디엄 다크": 71,
        "다크": 84,
    }

    return mapping.get(roasting, 50)


def note_profile(groups):
    acidity = 50
    sweetness = 50
    body = 50

    acidity += groups.get("sharp_citrus", 0) * 8
    acidity += groups.get("soft_citrus", 0) * 5
    acidity += groups.get("floral", 0) * 5
    acidity += groups.get("stonefruit", 0) * 3
    acidity += groups.get("berry", 0) * 2
    acidity += groups.get("tea_wine", 0) * 1
    acidity -= groups.get("choco_nut", 0) * 5
    acidity -= groups.get("grain_roasty", 0) * 6
    acidity -= groups.get("sweet", 0) * 1

    sweetness += groups.get("sweet", 0) * 7
    sweetness += groups.get("stonefruit", 0) * 4
    sweetness += groups.get("berry", 0) * 3
    sweetness += groups.get("tropical", 0) * 4
    sweetness += groups.get("choco_nut", 0) * 5
    sweetness += groups.get("tea_wine", 0) * 2
    sweetness += groups.get("soft_citrus", 0) * 1

    body += groups.get("choco_nut", 0) * 7
    body += groups.get("tea_wine", 0) * 4
    body += groups.get("sweet", 0) * 3
    body += groups.get("berry", 0) * 2
    body += groups.get("tropical", 0) * 2
    body += groups.get("grain_roasty", 0) * 6
    body -= groups.get("sharp_citrus", 0) * 4
    body -= groups.get("soft_citrus", 0) * 2
    body -= groups.get("floral", 0) * 5

    return acidity, sweetness, body


def process_profile(process):
    profiles = {
        "워시드": (66, 52, 40),
        "허니": (56, 67, 58),
        "내추럴": (50, 69, 64),
        "펄프드 내추럴": (54, 63, 57),
        "무산소/실험가공": (57, 68, 66),
    }
    return profiles.get(process, (52, 55, 50))


def roast_profile(roasting):
    profiles = {
        "라이트": (72, 50, 34),
        "미디엄 라이트": (65, 56, 42),
        "미디엄": (54, 63, 56),
        "미디엄 다크": (42, 61, 68),
        "다크": (30, 52, 78),
    }
    return profiles.get(roasting, (52, 55, 52))


def altitude_profile(altitude_mean):
    if altitude_mean is None:
        return (52, 56, 52)

    if altitude_mean >= 2100:
        return (74, 56, 40)
    if altitude_mean >= 1900:
        return (70, 58, 43)
    if altitude_mean >= 1750:
        return (66, 60, 46)
    if altitude_mean >= 1600:
        return (61, 62, 50)
    if altitude_mean >= 1450:
        return (56, 61, 54)
    if altitude_mean >= 1300:
        return (52, 59, 58)
    return (48, 57, 62)


def get_origin_bias(row):
    region = clean_text(row.get("region", ""))
    name_ko = clean_text(row.get("name_ko", ""))
    combined = f"{name_ko} {region}"

    acidity = 0
    sweetness = 0
    body = 0

    if any(x in combined for x in ["에티오피아", "구지", "예가체프", "시다마", "함벨라"]):
        acidity += 3
        body -= 2

    if any(x in combined for x in ["케냐", "니에리", "키린야가", "무랑가"]):
        acidity += 4
        sweetness += 2

    if any(x in combined for x in ["파나마"]):
        acidity += 2
        sweetness += 1

    if any(x in combined for x in ["과테말라", "우에우에테낭고", "안티구아"]):
        acidity += 2
        sweetness += 2

    if any(x in combined for x in ["콜롬비아", "우일라", "카우카"]):
        acidity += 1
        sweetness += 2

    if any(x in combined for x in ["브라질"]):
        acidity -= 3
        sweetness += 3
        body += 5

    if any(x in combined for x in ["온두라스", "엘살바도르"]):
        sweetness += 2
        body += 2

    if any(x in combined for x in ["수마트라", "인도네시아"]):
        acidity -= 4
        body += 6

    return acidity, sweetness, body


def get_variety_bias(variety, groups):
    variety = clean_text(variety)

    acidity = 0
    sweetness = 0
    body = 0

    if "게이샤" in variety or "geisha" in variety.lower():
        acidity += 7
        sweetness += 2
        body -= 6

    if "SL28" in variety or "SL34" in variety:
        acidity += 5
        sweetness += 1

    if "루이루11" in variety or "바티안" in variety:
        acidity += 2
        sweetness += 1

    if "티피카" in variety:
        acidity += 1
        sweetness += 2

    if "버번" in variety:
        sweetness += 3
        body += 1

    if "핑크 버번" in variety:
        acidity += 3
        sweetness += 4

    if "카투라" in variety or "카투아이" in variety:
        sweetness += 1

    if "파카마라" in variety or "마라카투라" in variety:
        body += 5
        acidity += 2

    if "토착종" in variety or "에어룸" in variety or "74158" in variety:
        if any(k in groups for k in ["floral", "soft_citrus", "sharp_citrus"]):
            acidity += 2

    return acidity, sweetness, body


def interaction_adjustments(row, groups, process, roasting, altitude_mean, variety):
    acidity = 0
    sweetness = 0
    body = 0

    if process == "워시드" and any(k in groups for k in ["floral", "sharp_citrus", "soft_citrus"]):
        acidity += 6
        body -= 3

    if process == "워시드" and altitude_mean is not None and altitude_mean >= 1800 and roasting in ["라이트", "미디엄 라이트"]:
        acidity += 5

    if process in ["내추럴", "허니"] and any(k in groups for k in ["berry", "tropical"]):
        sweetness += 5
        body += 3
        acidity -= 1

    if process in ["내추럴", "허니"] and "stonefruit" in groups:
        sweetness += 3
        body += 1

    if "choco_nut" in groups and roasting in ["미디엄", "미디엄 다크", "다크"]:
        sweetness += 3
        body += 6
        acidity -= 4

    if "grain_roasty" in groups and roasting in ["미디엄 다크", "다크"]:
        body += 4
        acidity -= 4

    if ("게이샤" in variety or "geisha" in variety.lower()) and process == "워시드" and any(k in groups for k in ["floral", "soft_citrus", "sharp_citrus"]):
        acidity += 5
        sweetness += 1
        body -= 4

    if ("SL28" in variety or "SL34" in variety) and process == "워시드" and any(k in groups for k in ["sharp_citrus", "berry"]):
        acidity += 4

    if altitude_mean is not None and altitude_mean >= 1900 and any(k in groups for k in ["floral", "sharp_citrus", "soft_citrus"]) and roasting in ["라이트", "미디엄 라이트"]:
        acidity += 4
        body -= 2

    if roasting == "다크" and any(k in groups for k in ["floral", "sharp_citrus", "soft_citrus"]):
        acidity -= 6

    return acidity, sweetness, body


def evidence_strength(row, notes, process, roasting, altitude_mean, variety):
    strength = 0.0

    strength += min(len(notes), 4) * 0.10
    if process:
        strength += 0.18
    if roasting:
        strength += 0.18
    if altitude_mean is not None:
        strength += 0.14
    if variety:
        strength += 0.08
    if clean_text(row.get("region", "")):
        strength += 0.07
    if clean_text(row.get("washing_station", "")):
        strength += 0.05

    return min(strength, 1.0)


def calibrate_score(raw_score, neutral, strength, floor, ceil):
    pull_factor = 0.55 + 0.30 * strength
    pulled = neutral + (raw_score - neutral) * pull_factor

    final_factor = 0.78 + 0.08 * strength
    adjusted = neutral + (pulled - neutral) * final_factor

    return clamp(adjusted, floor, ceil)


def pct_to_label(value):
    value = clamp(value)

    if value >= 75:
        return "High"
    if value >= 60:
        return "Medium-High"
    if value >= 40:
        return "Medium"
    return "Low"


def acidity_score(row):
    notes = split_notes(row.get("tasting_note", ""))
    groups = detect_note_groups(notes)
    process = normalize_process(row.get("process", ""))
    roasting = normalize_roasting(row.get("roasting", ""))
    altitude_mean = parse_altitude_mean(row.get("altitude", ""))
    variety = clean_text(row.get("variety", ""))

    note_a, _, _ = note_profile(groups)
    process_a, _, _ = process_profile(process)
    roast_a, _, _ = roast_profile(roasting)
    altitude_a, _, _ = altitude_profile(altitude_mean)

    raw = weighted_mean(
        [
            (note_a, 0.42 if notes else 0),
            (process_a, 0.20 if process else 0),
            (altitude_a, 0.16 if altitude_mean is not None else 0),
            (roast_a, 0.22 if roasting else 0),
        ],
        default_value=54,
    )

    origin_a, _, _ = get_origin_bias(row)
    variety_a, _, _ = get_variety_bias(variety, groups)
    inter_a, _, _ = interaction_adjustments(row, groups, process, roasting, altitude_mean, variety)

    raw += origin_a + variety_a + inter_a

    if roasting in ["미디엄 다크", "다크"]:
        raw = min(raw, 63)

    if process == "워시드" and roasting in ["라이트", "미디엄 라이트"]:
        raw = max(raw, 52)

    strength = evidence_strength(row, notes, process, roasting, altitude_mean, variety)
    return calibrate_score(raw, neutral=54, strength=strength, floor=22, ceil=84)


def sweetness_score(row):
    notes = split_notes(row.get("tasting_note", ""))
    groups = detect_note_groups(notes)
    process = normalize_process(row.get("process", ""))
    roasting = normalize_roasting(row.get("roasting", ""))
    altitude_mean = parse_altitude_mean(row.get("altitude", ""))
    variety = clean_text(row.get("variety", ""))

    _, note_s, _ = note_profile(groups)
    _, process_s, _ = process_profile(process)
    _, roast_s, _ = roast_profile(roasting)
    _, altitude_s, _ = altitude_profile(altitude_mean)

    raw = weighted_mean(
        [
            (note_s, 0.38 if notes else 0),
            (process_s, 0.24 if process else 0),
            (altitude_s, 0.10 if altitude_mean is not None else 0),
            (roast_s, 0.18 if roasting else 0),
        ],
        default_value=56,
    )

    _, origin_s, _ = get_origin_bias(row)
    _, variety_s, _ = get_variety_bias(variety, groups)
    _, inter_s, _ = interaction_adjustments(row, groups, process, roasting, altitude_mean, variety)

    raw += origin_s + variety_s + inter_s

    if process in ["허니", "내추럴"] and raw < 52:
        raw = 52

    if roasting == "다크":
        raw = min(raw, 80)

    strength = evidence_strength(row, notes, process, roasting, altitude_mean, variety)
    return calibrate_score(raw, neutral=56, strength=strength, floor=28, ceil=85)


def body_score(row):
    notes = split_notes(row.get("tasting_note", ""))
    groups = detect_note_groups(notes)
    process = normalize_process(row.get("process", ""))
    roasting = normalize_roasting(row.get("roasting", ""))
    altitude_mean = parse_altitude_mean(row.get("altitude", ""))
    variety = clean_text(row.get("variety", ""))

    _, _, note_b = note_profile(groups)
    _, _, process_b = process_profile(process)
    _, _, roast_b = roast_profile(roasting)
    _, _, altitude_b = altitude_profile(altitude_mean)

    raw = weighted_mean(
        [
            (note_b, 0.34 if notes else 0),
            (process_b, 0.24 if process else 0),
            (altitude_b, 0.14 if altitude_mean is not None else 0),
            (roast_b, 0.22 if roasting else 0),
        ],
        default_value=52,
    )

    _, _, origin_b = get_origin_bias(row)
    _, _, variety_b = get_variety_bias(variety, groups)
    _, _, inter_b = interaction_adjustments(row, groups, process, roasting, altitude_mean, variety)

    raw += origin_b + variety_b + inter_b

    if roasting in ["라이트", "미디엄 라이트"]:
        raw = min(raw, 66)

    if process == "워시드" and roasting in ["라이트", "미디엄 라이트"]:
        raw = min(raw, 58)

    if process in ["내추럴", "허니"] and roasting in ["미디엄", "미디엄 다크", "다크"]:
        raw = max(raw, 48)

    strength = evidence_strength(row, notes, process, roasting, altitude_mean, variety)
    return calibrate_score(raw, neutral=52, strength=strength, floor=20, ceil=82)


def main():
    df = pd.read_csv(INPUT_CSV)

    df["acidity_pct"] = df.apply(acidity_score, axis=1)
    df["sweetness_pct"] = df.apply(sweetness_score, axis=1)
    df["body_pct"] = df.apply(body_score, axis=1)
    df["roast_level_pct"] = df["roasting"].apply(roast_level_pct)

    df["acidity_label"] = df["acidity_pct"].apply(pct_to_label)
    df["sweetness_label"] = df["sweetness_pct"].apply(pct_to_label)
    df["body_label"] = df["body_pct"].apply(pct_to_label)
    df["roast_level_label"] = df["roast_level_pct"].apply(pct_to_label)

    df.to_csv(OUTPUT_CSV, index=False, encoding="utf-8-sig")

    print(f"저장 완료: {OUTPUT_CSV}")
    print()
    print(
        df[
            [
                "name_ko",
                "acidity_pct",
                "acidity_label",
                "sweetness_pct",
                "sweetness_label",
                "body_pct",
                "body_label",
                "roast_level_pct",
                "roast_level_label",
            ]
        ].head(15).to_string(index=False)
    )


if __name__ == "__main__":
    main()