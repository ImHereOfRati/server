# OOP Principles — Details & Reasoning

## 1. Tell, Don't Ask (묻지 말고 시켜라)

**Why:** 상태(Data)와 행동(Behavior)을 한 곳에 모아 캡슐화(Encapsulation)를 달성하기 위함입니다. 상태를 외부로 노출하여 밖에서 판단하게 되면 응집도가 낮아지고 결합도가 높아집니다. (조영호, *오브젝트*)

**심화 예시:**
```dart
// ❌ BAD — Service 레이어에서 판단
class CheckoutService {
  void process(Cart cart) {
    if (cart.totalPrice > 50000 && cart.items.length > 3) {
      applyDiscount();
    }
  }
}

// ✅ GOOD — 판단 로직을 객체(Cart) 내부로 이동
class Cart {
  final int totalPrice;
  final List<Item> items;
  
  bool isEligibleForDiscount() {
    return totalPrice > 50000 && items.length > 3;
  }
}

class CheckoutService {
  void process(Cart cart) {
    if (cart.isEligibleForDiscount()) {
      applyDiscount();
    }
  }
}
```

## 2. Data Structure vs Object (자료구조와 객체)

**Why:** 자료구조는 데이터를 노출하고 함수를 추가하기 쉽지만, 객체는 행동을 노출하고 새로운 타입을 추가하기 쉽습니다. (*클린 코드*)

- **DTO (service/dto):** JSON 매핑용 Data Structure. `toJson()`, `fromJson()`, `toDomain()`만 존재. 비즈니스 룰 검증 로직은 넣지 않습니다.
- **Domain (model):** 외부 인프라(JSON 등)에 의존하지 않는 순수 Dart Object.

## 3. Immutability (불변성)

**Why:** 객체의 상태가 변하지 않음을 보장하면, 사이드 이펙트(Side Effect)가 사라지고 상태 추적(특히 Riverpod 같은 상태관리 시)이 매우 쉬워집니다.

- 모든 도메인 모델 필드는 `final`이어야 합니다.
- 컬렉션 필드는 불변 컬렉션으로 다루는 것을 권장합니다 (예: `List.unmodifiable()`).

## 4. Naming (이름 짓기)

**Why:** 코드는 읽히는 시간이 쓰이는 시간보다 훨씬 많습니다. (*클린 코드*)

- **축약어 금지:** `btn` -> `button`, `calc` -> `calculate`, `req` -> `request`.
- **발음 가능한 이름:** `genymdhms` (X) -> `generationTimestamp` (O).
- **검색 가능한 이름:** 매직 넘버(Magic Number) 대신 상수(`const`)나 `enum`에 의미 있는 이름을 부여하세요.
