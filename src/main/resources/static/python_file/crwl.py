import re
import pandas as pd
import requests

from bs4 import BeautifulSoup
from urllib.parse import urljoin

BASE_URL = "https://www.beanbrothers.co.kr"
LIST_URL_TEMPLATE = BASE_URL + "/info/coffeewiki?cateCd={cate_cd}"
OUTPUT_CSV = "beanbrothers_coffeewiki.csv"
ERROR_CSV = "beanbrothers_coffeewiki_errors.csv"

CATEGORY_CODES = [
    "006009015",
    "006009014",
    "006009004",
    "006009001",
    "006009002",
    "006009003",
    "006009006",
    "006009007",
    "006009008",
    "006009009",
    "006009010",
    "006009011",
    "006009012",
    "006009013",
    "006009005",
]

HEADERS = {
    "User-Agent": "Mozilla/5.0"
}

session = requests.Session()
session.headers.update(HEADERS)


def clean_text(text: str) -> str:
    if not text:
        return ""
    return re.sub(r"\s+", " ", text).strip().strip('"').strip("'")


def get_soup(url: str) -> BeautifulSoup:
    resp = session.get(url, timeout=20)
    resp.raise_for_status()
    return BeautifulSoup(resp.text, "html.parser")


def collect_detail_links_by_cate_cd(cate_cd: str):
    url = LIST_URL_TEMPLATE.format(cate_cd=cate_cd)
    soup = get_soup(url)

    detail_urls = []
    seen = set()

    for a in soup.select('a[href*="/info/coffeewiki_detail"]'):
        href = a.get("href")
        if not href:
            continue

        full_url = urljoin(BASE_URL, href)

        if full_url in seen:
            continue

        seen.add(full_url)
        detail_urls.append(full_url)

    return detail_urls


def extract_summary_map(soup: BeautifulSoup):
    summary_map = {}

    rows = soup.select("div.summary p.d-flex")

    for row in rows:
        spans = row.find_all("span")

        if len(spans) < 2:
            continue

        key = clean_text(spans[0].get_text(" ", strip=True))
        value = clean_text(" ".join(span.get_text(" ", strip=True) for span in spans[1:]))

        if key:
            summary_map[key] = value

    return summary_map


def split_ko_en_name(text: str):
    text = clean_text(text)

    match = re.match(r"^(.*?[가-힣])\s*([A-Z][A-Z0-9\s\-\&\.\'\/,:]+)$", text)
    if match:
        name_ko = clean_text(match.group(1))
        name_en = clean_text(match.group(2))
        return name_ko, name_en

    return text, ""


def is_english_name(text: str) -> bool:
    return bool(re.fullmatch(r"[A-Z][A-Z0-9\s\-\&\.\'\/,:]+", clean_text(text)))


def unique_preserve_order(items):
    seen = set()
    result = []

    for item in items:
        cleaned = clean_text(item)
        if not cleaned:
            continue
        if cleaned in seen:
            continue
        seen.add(cleaned)
        result.append(cleaned)

    return result


def extract_names(soup: BeautifulSoup):
    name_ko = ""
    name_en = ""

    name_box = soup.select_one(".wiki-name")
    if not name_box:
        return name_ko, name_en

    parts = unique_preserve_order(list(name_box.stripped_strings))

    for part in parts:
        split_ko, split_en = split_ko_en_name(part)

        if split_en:
            if not name_ko:
                name_ko = split_ko
            if not name_en:
                name_en = split_en
            continue

        if re.search(r"[가-힣]", part) and not name_ko:
            name_ko = part
            continue

        if is_english_name(part) and not name_en:
            name_en = part
            continue

    if not name_ko or not name_en:
        flat_text = clean_text(name_box.get_text(" ", strip=True))
        split_ko, split_en = split_ko_en_name(flat_text)

        if not name_ko:
            name_ko = split_ko

        if not name_en:
            name_en = split_en

    return name_ko, name_en


def parse_detail(detail_url: str):
    soup = get_soup(detail_url)
    summary_map = extract_summary_map(soup)
    name_ko, name_en = extract_names(soup)

    return {
        "name_ko": name_ko,
        "name_en": name_en,
        "tasting_note": summary_map.get("테이스팅 노트", ""),
        "roasting": summary_map.get("로스팅", ""),
        "process": summary_map.get("가공", ""),
        "variety": summary_map.get("품종", ""),
        "altitude": summary_map.get("재배고도", ""),
        "washing_station": summary_map.get("워싱 스테이션", ""),
        "region": summary_map.get("지역", ""),
        "release_ym": summary_map.get("출시년월", ""),
        "category": summary_map.get("카테고리", ""),
        "no": summary_map.get("No.", ""),
    }


def main():
    all_detail_urls = []
    seen_detail_urls = set()
    errors = []

    print("1. cateCd별 상세 링크 수집")

    for cate_cd in CATEGORY_CODES:
        try:
            detail_urls = collect_detail_links_by_cate_cd(cate_cd)
            print(f"cateCd={cate_cd} -> {len(detail_urls)}개")

            for url in detail_urls:
                if url not in seen_detail_urls:
                    seen_detail_urls.add(url)
                    all_detail_urls.append(url)

        except Exception as e:
            errors.append({
                "stage": "collect_detail_links",
                "cate_cd": cate_cd,
                "url": LIST_URL_TEMPLATE.format(cate_cd=cate_cd),
                "error": str(e),
            })

    print(f"\n중복 제거 후 상세 링크 수: {len(all_detail_urls)}")
    print("\n2. 상세 페이지 파싱")

    rows = []

    for idx, detail_url in enumerate(all_detail_urls, start=1):
        try:
            print(f"[{idx}/{len(all_detail_urls)}] {detail_url}")
            row = parse_detail(detail_url)
            rows.append(row)
        except Exception as e:
            errors.append({
                "stage": "parse_detail",
                "url": detail_url,
                "error": str(e),
            })

    df = pd.DataFrame(rows)
    df.to_csv(OUTPUT_CSV, index=False, encoding="utf-8-sig")

    if errors:
        err_df = pd.DataFrame(errors)
        err_df.to_csv(ERROR_CSV, index=False, encoding="utf-8-sig")
        print(f"\n오류 로그 저장 완료: {ERROR_CSV}")

    print(f"\n저장 완료: {OUTPUT_CSV}")
    print(f"총 저장 건수: {len(df)}")


if __name__ == "__main__":
    main()