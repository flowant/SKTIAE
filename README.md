# 한국 무역 정보 분석 예제 (South Korea Trade Information Analysis Example, SKTIAE)

한국에서 세계의 각 나라로 수입/수출하는 품목들을 (HS Code로 분류 됨)  달러와 무게의 값으로 공공 데이터 포탈에서(http://www.data.go.kr) 배포한다. 이전달에 가장 많이 수출 된 품목을 정렬하여 찾거나 최근 몇개월 간 증감율이(CAGR) 높은 항목들을 분석하여 챠트 형태로 출력 및 이미지파일로 저장하는 기능을 제공한다.

특정 회사에서 수출하는 HS Code 들을 취합하여 CAGR을 구한뒤 증감율로 내림차순 정렬하여 회사들의 챠트들을 출력한다. 이때, 회사외 연관된 HS Code 값은 회사당 수출/수입에 차지하는 비율을 알 수 없기 때문에 국가 전체의값으로 사용 하였다.

본 예제는 OpenApi를 이용하여 데이타를 취합하고 분석하여 챠트로 출력하는 기능을 Java 8 버전으로 작성하였다.

### 소스에 포함 되지 않은 정보
- 공공 데이터 포탈에서 배포하는 수출입 데이터 데이타 베이스
- 특정 회사가 수출하는 HS Code

### 환경 구성 방법
- Java 8, Gradle 들을 사용하여 작성 되었다.
- - eclipse 프로젝트 생성: gradle eclipse
- mongodb를 다운 받아 설치 하고 "trade", "corporation" database를 생성 후 접근 권한을 가진 계정을 생성한다. 생성된 계정을 바탕으로 config.properties 파일의 MongoClientUri에 mongodb://id:pass@address 형태로 설정한다.
- 공공 데이터 포탈에서 (http://www.data.go.kr) 품목별 국가별 수출입실적 openAPI 사용 신청을 승인 받고 Service 키를 발급 받는다. 발급 받은 service Key를 config.properties 파일의 ServiceKeyNationItemTrade 의 값으로 설정 한다.
- 국가별 수출입실적 openApi 사용방법 문서에 포함된 iros_cipher.jar 파일을 libs 폴더에 복사 한다. 
- 국가별 수출입실적 openApi 사용방법 문서에 포함된 관세청조회코드.xls 파일에 포함된 "국가별코드", "신성질변코드" 탭들을 cvs파일로 변환하여 mongodb에 import 한다. (TradeDAO.java 참고)
- 특정 회사에서 수출입하는 HS Code 정보는 openApi등으로 얻을 수 있는 방법을 찾지 못하였기 때문에 임의로 생성한다. (CorpDAO.java 참고)

### 실행 방법
- 공공 데이터 취합 방법: 매월 중순경에 이전달의 데이타를 내려 받을 수 있으며 하루에 처리 가능한 요청 횟수가 정해져 있다. 데이터 취합 기간동안엔 매일 주기적으로 하단의 명령을 실행하는 것을 추천 한다. 
  gradle run

- 차트들 출력 방법
  TradeReport.java 의 메인 함수 실행
